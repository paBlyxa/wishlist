package ru.dins.scalaschool.wishlist.db

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import doobie.implicits._
import doobie.util.fragments.{setOpt, whereAndOpt}
import doobie.postgres.implicits._
import doobie.postgres.sqlstate
import doobie.util.invariant.UnexpectedEnd
import doobie.util.transactor.Transactor.Aux
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}
import ru.dins.scalaschool.wishlist.models._
import ru.dins.scalaschool.wishlist.models.Models._

import java.util.UUID

trait WishlistRepo[F[_]] {

  def save(userId: UserId, wishlist: NewWishlist): F[Either[ApiError, WishlistSaved]]

  def remove(wishlistId: WishlistId): F[Either[ApiError, Unit]]

  def get(wishlistId: WishlistId): F[Either[ApiError, WishlistSaved]]

  def clear(wishlistId: WishlistId): F[Either[ApiError, Unit]]

  def findAll(
      userId: UserId,
      filter: FilterList,
  ): F[Either[ApiError, List[WishlistWeb]]]

  def update(wishlistId: WishlistId, wishlist: WishlistUpdate): F[Either[ApiError, WishlistSaved]]

  def updateAccess(wishlistId: WishlistId, access: Access): F[Either[ApiError, WishlistSaved]]
}

case class WishlistRepoImpl[F[_]: Sync](xa: Aux[F, Unit]) extends WishlistRepo[F] {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val wishlistColumns = List("id", "user_id", "name", "access", "comment", "created_at", "event_date")

  override def save(userId: UserId, wishlist: NewWishlist): F[Either[ApiError, WishlistSaved]] = {
    val query = for {
      uuid <- generateUUID
      query =
        sql"""insert into wishlist (id, user_id, name, access, comment, event_date)
          values ($uuid, $userId, ${wishlist.name}, ${wishlist.access}, ${wishlist.comment}, ${wishlist.eventDate})"""
    } yield query.update
    query.flatMap(
      _.withUniqueGeneratedKeys[WishlistSaved](wishlistColumns: _*)
        .transact(xa)
        .attemptSql
        .map {
          case Left(e: PSQLException) if e.getSQLState.equals(sqlstate.class23.FOREIGN_KEY_VIOLATION.value) =>
            Left(ApiError.userNotFound(userId))
          case Left(err) =>
            logger.error("An error occurred while saving wishlist", err)
            Left(ApiError.unexpectedError)
          case Right(wishlist) => Right(wishlist)
        },
    )
  }

  override def remove(wishlistId: WishlistId): F[Either[ApiError, Unit]] =
    sql"delete from wishlist where id = $wishlistId".update.run.transact(xa).map(_ => Right(()))

  override def get(wishlistId: WishlistId): F[Either[ApiError, WishlistSaved]] =
    sql"""select * from wishlist where id = $wishlistId"""
      .query[WishlistSaved]
      .option
      .transact(xa)
      .map {
        case Some(wishlist) => Right(wishlist)
        case None           => Left(ApiError.notFound(wishlistId))
      }

  override def clear(wishlistId: WishlistId): F[Either[ApiError, Unit]] =
    sql"delete from wish where wishlist_id = $wishlistId".update.run.transact(xa).attempt.map(_ => Right(()))

  override def findAll(
      userId: UserId,
      filter: FilterList,
  ): F[Either[ApiError, List[WishlistWeb]]] = {

    import filter._

    val usernameFilter = username
      .map(username => s"%$username%")
      .map(username => fr"u.username ILIKE $username")
    //.map(username => fr"w.user_id IN (select id from users us where us.username ILIKE $username)")
    val nameFilter = name.map(name => s"%$name%").map(name => fr"w.name ILIKE $name")
    val frOrderBy = orderBy match {
      case Some(WishlistOrder.Username) => fr"order by u.username"
      case Some(WishlistOrder.Name)     => fr"order by w.name"
      case _                            => fr"order by w.created_at"
    }
    val frOrderDir = orderDir match {
      case Some(OrderDir.Desc) => fr"desc"
      case _                   => fr"asc"
    }
    val accessControl = Some(
      fr"(w.access = 'public' OR w.user_id = $userId OR w.id IN (select wishlist_id from users_access ua where ua.user_id = $userId))",
    )

    val q =
      fr"select w.id, u.username, w.name, w.access, w.comment, w.event_date from wishlist w " ++
        fr"left join users u ON w.user_id = u.id" ++ whereAndOpt(
          usernameFilter,
          nameFilter,
          accessControl,
        ) ++ frOrderBy ++ frOrderDir

    q.query[WishlistWeb]
      .stream
      .compile
      .toList
      .transact(xa)
      .attemptSql
      .map {
        case Left(e) =>
          logger.error("An error occurred while looking for wishlist", e)
          Left(ApiError.unexpectedError)
        case Right(list) => Right(list)
      }
  }

  override def update(wishlistId: WishlistId, wishlist: WishlistUpdate): F[Either[ApiError, WishlistSaved]] = {
    val frSetName      = wishlist.name.map(value => fr"name = $value")
    val frSetComment   = wishlist.comment.map(value => fr"comment = $value")
    val frSetAccess    = wishlist.access.map(value => fr"access = $value")
    val frSetEventDate = wishlist.eventDate.map(value => fr"event_date = $value")
    val frWhere        = fr"where id = $wishlistId"

    val q = fr"update wishlist" ++ setOpt(frSetName, frSetComment, frSetAccess, frSetEventDate) ++ frWhere

    q.update
      .withUniqueGeneratedKeys[WishlistSaved](wishlistColumns: _*)
      .transact(xa)
      .attempt
      .map {
        case Left(UnexpectedEnd) => Left(ApiError.notFound(wishlistId))
        case Left(e) =>
          logger.error("An error occurred while updating wishlist", e)
          Left(ApiError.unexpectedError)
        case Right(wishlist) => Right(wishlist)
      }
  }

  override def updateAccess(wishlistId: WishlistId, access: Access): F[Either[ApiError, WishlistSaved]] =
    sql"update wishlist set access = $access where id = $wishlistId".update
      .withUniqueGeneratedKeys[WishlistSaved](wishlistColumns: _*)
      .transact(xa)
      .attempt
      .map {
        case Left(UnexpectedEnd) => Left(ApiError.notFound(wishlistId))
        case Left(e) =>
          logger.error("An error occurred while updating wishlist", e)
          Left(ApiError.unexpectedError)
        case Right(wishlist) => Right(wishlist)
      }

  private def generateUUID = Sync[F].delay(UUID.randomUUID())
}
