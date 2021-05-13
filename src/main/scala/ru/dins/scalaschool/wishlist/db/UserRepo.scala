package ru.dins.scalaschool.wishlist.db

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import doobie.util.transactor.Transactor.Aux
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.postgres.sqlstate
import doobie.util.fragments.setOpt
import doobie.util.invariant.UnexpectedEnd
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}
import ru.dins.scalaschool.wishlist.models._
import ru.dins.scalaschool.wishlist.models.Models._

import java.util.UUID

trait UserRepo[F[_]] {

  def getById(userId: UserId): F[Either[ApiError, User]]

  def getByUsername(username: String): F[Either[ApiError, User]]

  def save(user: NewUser): F[Either[ApiError, User]]

  def update(userId: UserId, user: UserUpdate): F[Either[ApiError, User]]

  def saveUserAccess(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Unit]]

  def removeUserAccess(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Unit]]

  def hasUserAccess(userId: UserId, wishlistId: WishlistId): F[Option[Long]]
}

case class UserRepoImpl[F[_]: Sync](xa: Aux[F, Unit]) extends UserRepo[F] {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val userColumns = List("id", "username", "email", "telegram_id")

  override def getById(userId: UserId): F[Either[ApiError, User]] =
    sql"""select * from users where id = $userId"""
      .query[User]
      .option
      .transact(xa)
      .map {
        case Some(user) => Right(user)
        case None       => Left(ApiError.userNotFound(userId))
      }

  override def getByUsername(username: String): F[Either[ApiError, User]] =
    sql"""select * from users where username = $username"""
      .query[User]
      .option
      .transact(xa)
      .map {
        case Some(user) => Right(user)
        case None       => Left(ApiError.userNotFound(username))
      }

  override def save(user: NewUser): F[Either[ApiError, User]] = {
    val query = for {
      uuid <- generateUUID
      query =
        sql"""insert into users (id, username, email, telegram_id)
          values ($uuid, ${user.username}, ${user.email}, ${user.telegramId})
           """
    } yield query.update
    query.flatMap(
      _.withUniqueGeneratedKeys[User](userColumns: _*)
        .transact(xa)
        .attemptSql
        .map {
          case Left(e: PSQLException) if e.getSQLState.equals(sqlstate.class23.UNIQUE_VIOLATION.value) =>
            Left(ApiError.usernameAlreadyTaken(user.username))
          case Left(e) =>
            logger.error("An error occurred while saving user", e)
            Left(ApiError.unexpectedError)
          case Right(user) => Right(user)
        },
    )
  }

  override def update(userId: UserId, user: UserUpdate): F[Either[ApiError, User]] = {
    val frSetUsername   = user.username.map(value => fr"username = $value")
    val frSetEmail      = user.email.map(value => fr"email = $value")
    val frSetTelegramId = user.telegramId.map(value => fr"telegram_id = $value")
    val frWhere         = fr"where id = $userId"

    val q = fr"update users" ++ setOpt(frSetUsername, frSetEmail, frSetTelegramId) ++ frWhere

    q.update
      .withUniqueGeneratedKeys[User](userColumns: _*)
      .transact(xa)
      .attempt
      .map {
        case Left(e: PSQLException) if e.getSQLState.equals(sqlstate.class23.UNIQUE_VIOLATION.value) =>
          user.username match {
            case Some(username) => Left(ApiError.usernameAlreadyTaken(username))
            case None           => Left(ApiError.unexpectedError)
          }
        case Left(UnexpectedEnd) => Left(ApiError.userNotFound(userId))
        case Left(e) =>
          logger.error("An error occurred while updating user", e)
          Left(ApiError.unexpectedError)
        case Right(user) => Right(user)
      }

  }

  override def saveUserAccess(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Unit]] =
    sql"""insert into users_access (user_id, wishlist_id) values ($userId, $wishlistId)""".update.run
      .transact(xa)
      .attemptSql
      .map {
        case Left(e: PSQLException) if e.getSQLState.equals(sqlstate.class23.FOREIGN_KEY_VIOLATION.value) =>
          logger.debug("An error occurred", e)
          Left(ApiError.userNotFound(userId))
        case Left(e) =>
          logger.error("An error occurred while saving user's access", e)
          Left(ApiError.unexpectedError)
        case _ => Right(())
      }

  override def removeUserAccess(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Unit]] =
    sql"""delete from users_access where user_id = $userId and wishlist_id = $wishlistId""".update.run
      .transact(xa)
      .attemptSql
      .map {
        case Left(e: PSQLException) if e.getSQLState.equals(sqlstate.class23.FOREIGN_KEY_VIOLATION.value) =>
          logger.debug("An error occurred", e)
          Left(ApiError.userNotFound(userId))
        case Left(e) =>
          logger.error("An error occurred while delete user's access", e)
          Left(ApiError.unexpectedError)
        case _ => Right(())
      }

  override def hasUserAccess(userId: UserId, wishlistId: WishlistId): F[Option[Long]] =
    sql"select from users_access where user_id = $userId and wishlist_id = $wishlistId".query[Long].option.transact(xa)

  private def generateUUID = Sync[F].delay(UUID.randomUUID())
}
