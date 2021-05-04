package ru.dins.scalaschool.wishlist.db

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import doobie.implicits._
import doobie.util.fragments.setOpt
import doobie.postgres.implicits._
import doobie.postgres.sqlstate
import doobie.util.invariant.UnexpectedEnd
import doobie.util.transactor.Transactor.Aux
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}
import ru.dins.scalaschool.wishlist.models.ApiError
import ru.dins.scalaschool.wishlist.models.Models.Access.Access
import ru.dins.scalaschool.wishlist.models.Models.{NewWishlist, Wishlist, WishlistOption}

import java.util.UUID

trait WishlistRepo[F[_]] {

  def save(wishlist: NewWishlist): F[Either[ApiError, Wishlist]]

  def remove(uuid: UUID): F[Either[ApiError, Unit]]

  def get(uuid: UUID): F[Either[ApiError, Wishlist]]

  def clear(uuid: UUID): F[Either[ApiError, Unit]]

  def findAll: F[Either[ApiError, List[Wishlist]]]

  def update(uuid: UUID, wishlist: WishlistOption): F[Either[ApiError, Wishlist]]

  def update(uuid: UUID, access: Access): F[Either[ApiError, Wishlist]]
}

case class WishlistRepoImpl[F[_]: Sync](xa: Aux[F, Unit]) extends WishlistRepo[F] {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val wishlistColumns = List("id", "user_id", "name", "access", "comment", "created_at")

  override def save(wishlist: NewWishlist): F[Either[ApiError, Wishlist]] = {
    val query = for {
      uuid <- generateUUID
      query =
        sql"""insert into wishlist (id, user_id, name, access, comment)
          values ($uuid, ${wishlist.userId}, ${wishlist.name}, ${wishlist.access}, ${wishlist.comment})
           """
    } yield query.update
    query.flatMap(
      _.withUniqueGeneratedKeys[Wishlist](wishlistColumns: _*)
        .transact(xa)
        .attemptSql
        .map {
          case Left(e: PSQLException) if e.getSQLState.equals(sqlstate.class23.FOREIGN_KEY_VIOLATION.value) =>
            Left(ApiError.userNotFound(wishlist.userId))
          case Left(err) =>
            logger.error("An error occurred while saving wishlist", err)
            Left(ApiError.unexpectedError)
          case Right(wishlist) => Right(wishlist)
        },
    )
  }

  override def remove(uuid: UUID): F[Either[ApiError, Unit]] =
    sql"delete from wishlist where id = $uuid".update.run.transact(xa).map(_ => Right(()))

  override def get(uuid: UUID): F[Either[ApiError, Wishlist]] =
    sql"""select * from wishlist where id = $uuid limit 1"""
      .query[Wishlist]
      .option
      .transact(xa)
      .map {
        case Some(wishlist) => Right(wishlist)
        case None           => Left(ApiError.notFound(uuid))
      }

  override def clear(uuid: UUID): F[Either[ApiError, Unit]] =
    sql"delete from wish where wishlist_id = $uuid".update.run.transact(xa).attempt.map(_ => Right(()))

  override def findAll: F[Either[ApiError, List[Wishlist]]] =
    sql"select * from wishlist"
      .query[Wishlist]
      .stream
      .compile
      .toList
      .transact(xa)
      .attemptSql
      .map {
        case Left(_)     => Left(ApiError.unexpectedError)
        case Right(list) => Right(list)
      }

  override def update(uuid: UUID, wishlist: WishlistOption): F[Either[ApiError, Wishlist]] = {
    val frSetName    = wishlist.name.map(value => fr"name = $value")
    val frSetComment = wishlist.comment.map(value => fr"comment = $value")
    val frWhere      = fr"where id = $uuid"

    val q = fr"update wishlist" ++ setOpt(frSetName, frSetComment) ++ frWhere

    q.update
      .withUniqueGeneratedKeys[Wishlist](wishlistColumns: _*)
      .transact(xa)
      .attempt
      .map {
        case Left(UnexpectedEnd) => Left(ApiError.notFound(uuid))
        case Left(e) =>
          logger.error("An error occurred while updating wishlist", e)
          Left(ApiError.unexpectedError)
        case Right(wishlist) => Right(wishlist)
      }
  }

  override def update(uuid: UUID, access: Access): F[Either[ApiError, Wishlist]] =
    sql"update wishlist set access = $access where id = $uuid".update
      .withUniqueGeneratedKeys[Wishlist](wishlistColumns: _*)
      .transact(xa)
      .attempt
      .map {
        case Left(UnexpectedEnd) => Left(ApiError.notFound(uuid))
        case Left(e) =>
          logger.error("An error occurred while updating wishlist", e)
          Left(ApiError.unexpectedError)
        case Right(wishlist) => Right(wishlist)
      }

  private def generateUUID = Sync[F].delay(UUID.randomUUID())
}
