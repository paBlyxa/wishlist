package ru.dins.scalaschool.wishlist.db

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.applicativeError._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.postgres.sqlstate
import doobie.util.fragments.setOpt
import doobie.util.invariant.UnexpectedEnd
import doobie.util.transactor.Transactor.Aux
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}
import ru.dins.scalaschool.wishlist.models._
import ru.dins.scalaschool.wishlist.models.Models.{NewWish, Wish, WishUpdate}

trait WishRepo[F[_]] {

  def get(id: Long): F[Either[ApiError, Wish]]

  def getAllByWishlistId(wishlistId: WishlistId): F[Either[ApiError, List[Wish]]]

  def save(wishlistId: WishlistId, wish: NewWish): F[Either[ApiError, Wish]]

  def remove(id: Long): F[Either[ApiError, Unit]]

  def update(id: Long, wish: WishUpdate): F[Either[ApiError, Wish]]

  def updateStatus(userId: UserId, id: Long, wishStatus: WishStatus): F[Either[ApiError, Wish]]

  def getUserBooked(wishId: Long): F[List[UserId]]
}

case class WishRepoImpl[F[_]: Sync](xa: Aux[F, Unit]) extends WishRepo[F] {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val wishColumns = List("id", "wishlist_id", "name", "link", "price", "comment", "status", "created_at")

  override def get(id: Long): F[Either[ApiError, Wish]] =
    sql"""select * from wish where id = $id"""
      .query[Wish]
      .option
      .transact(xa)
      .map {
        case Some(wish) => Right(wish)
        case None       => Left(ApiError.wishNotFound(id))
      }

  override def getAllByWishlistId(wishlistId: WishlistId): F[Either[ApiError, List[Wish]]] =
    sql"""select * from wish where wishlist_id = $wishlistId"""
      .query[Wish]
      .stream
      .compile
      .toList
      .transact(xa)
      .attemptSql
      .map {
        case Left(_)     => Left(ApiError.unexpectedError)
        case Right(list) => Right(list)
      }

  override def save(wishlistId: WishlistId, wish: NewWish): F[Either[ApiError, Wish]] =
    sql"""insert into wish (wishlist_id, name, link, price, comment)
         values ($wishlistId, ${wish.name}, ${wish.link}, ${wish.price}, ${wish.comment})
       """.update
      .withUniqueGeneratedKeys[Wish](wishColumns: _*)
      .transact(xa)
      .attemptSql
      .map {
        case Left(e: PSQLException) if e.getSQLState.equals(sqlstate.class23.FOREIGN_KEY_VIOLATION.value) =>
          Left(ApiError.notFound(wishlistId))
        case Left(err) =>
          logger.error("An error occurred while saving wish", err)
          Left(ApiError.unexpectedError)
        case Right(wish) => Right(wish)
      }

  override def remove(id: Long): F[Either[ApiError, Unit]] =
    sql"delete from wish where id = $id".update.run.transact(xa).attempt.map(_ => Right(()))

  override def update(id: Long, wish: WishUpdate): F[Either[ApiError, Wish]] = {
    val frSetName    = wish.name.map(value => fr"name = $value")
    val frSetLink    = wish.link.map(value => fr"link = $value")
    val frSetPrice   = wish.price.map(value => fr"price = $value")
    val frSetComment = wish.comment.map(value => fr"comment = $value")
    val frWhere      = fr"where id = $id"

    val q = fr"update wish" ++ setOpt(frSetName, frSetLink, frSetPrice, frSetComment) ++ frWhere

    q.update
      .withUniqueGeneratedKeys[Wish](wishColumns: _*)
      .transact(xa)
      .attempt
      .map {
        case Left(UnexpectedEnd) => Left(ApiError.wishNotFound(id))
        case Left(e) =>
          logger.error("An error occurred while updating wish", e)
          Left(ApiError.unexpectedError)
        case Right(wish) => Right(wish)
      }
  }

  override def updateStatus(userId: UserId, wishId: Long, wishStatus: WishStatus): F[Either[ApiError, Wish]] =
    (for {
      wish <- setStatus(wishId, wishStatus)
      _    <- if (wishStatus == WishStatus.Free) removeUsersWish(wishId) else insertUsersWish(userId, wishId)
    } yield wish)
      .transact(xa)
      .attempt
      .map {
        case Left(UnexpectedEnd) => Left(ApiError.wishNotFound(wishId))
        case Left(e) =>
          logger.error("An error occurred while updating wish", e)
          Left(ApiError.unexpectedError)
        case Right(wish) => Right(wish)
      }

  private def setStatus(id: Long, wishStatus: WishStatus): doobie.ConnectionIO[Wish] =
    sql"update wish set status = $wishStatus where id = $id".update
      .withUniqueGeneratedKeys[Wish](wishColumns: _*)

  private def insertUsersWish(userId: UserId, wishId: Long): doobie.ConnectionIO[Int] =
    sql"insert into users_wish (user_id, wish_id) values ($userId, $wishId) ON CONFLICT DO NOTHING".update.run

  private def removeUsersWish(wishId: Long): doobie.ConnectionIO[Int] =
    sql"delete from users_wish where wish_id = $wishId".update.run

  override def getUserBooked(wishId: Long): F[List[UserId]] =
    sql"select user_id from users_wish where wish_id = $wishId".query[UserId].stream.compile.toList.transact(xa)
}
