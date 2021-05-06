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
import ru.dins.scalaschool.wishlist.models.Models.{NewUser, User, UserUpdate}

import java.util.UUID

trait UserRepo[F[_]] {

  def getById(uuid: UUID): F[Either[ApiError, User]]

  def getByUsername(username: String): F[Either[ApiError, User]]

  def save(user: NewUser): F[Either[ApiError, User]]

  def update(uuid: UUID, user: UserUpdate): F[Either[ApiError, User]]
}

case class UserRepoImpl[F[_]: Sync](xa: Aux[F, Unit]) extends UserRepo[F] {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val userColumns = List("id", "username", "email", "telegram_id")

  override def getById(uuid: UUID): F[Either[ApiError, User]] =
    sql"""select * from users where id = $uuid"""
      .query[User]
      .option
      .transact(xa)
      .map {
        case Some(user) => Right(user)
        case None       => Left(ApiError.userNotFound(uuid))
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

  override def update(uuid: UUID, user: UserUpdate): F[Either[ApiError, User]] = {
    val frSetUsername   = user.username.map(value => fr"username = $value")
    val frSetEmail      = user.email.map(value => fr"email = $value")
    val frSetTelegramId = user.telegramId.map(value => fr"telegram_id = $value")
    val frWhere         = fr"where id = $uuid"

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
        case Left(UnexpectedEnd) => Left(ApiError.userNotFound(uuid))
        case Left(e) =>
          logger.error("An error occurred while updating user", e)
          Left(ApiError.unexpectedError)
        case Right(user) => Right(user)
      }

  }

  private def generateUUID = Sync[F].delay(UUID.randomUUID())
}
