package ru.dins.scalaschool.wishlist.test.db

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.scalatest.Assertion
import ru.dins.scalaschool.wishlist.db.{UserRepo, UserRepoImpl}
import ru.dins.scalaschool.wishlist.models.ApiError
import ru.dins.scalaschool.wishlist.models.Models.{User, UserUpdate}
import ru.dins.scalaschool.wishlist.test.TestExamples._

class UserRepoTest extends MyTestContainerForAll {

  def resetStorage(test: (UserRepo[IO], Transactor.Aux[IO, Unit]) => IO[Assertion]): Unit =
    withContainers { container =>
      val xa       = createTransactor(container)
      val truncate = sql"truncate users cascade".update.run
      val storage  = UserRepoImpl(xa)

      val result = for {
        _         <- truncate.transact(xa)
        assertion <- test(storage, xa)
      } yield assertion

      result.unsafeRunSync()
    }

  "getById" should "return Left(Not found) if storage is empty" in resetStorage { case (storage, _) =>
    storage.getById(exampleUserId).map(_ shouldBe Left(ApiError.userNotFound(exampleUserId)))
  }

  it should "return User if user with matching id exists" in resetStorage { case (storage, xa) =>
    for {
      userId <- insertUser().transact(xa)
      user   <- storage.getById(userId)
    } yield user should matchPattern { case Right(User(_, "username", Some("email"), Some("@username"))) => }
  }

  "getByUsername" should "return Left(Not found) if storage is empty" in resetStorage { case (storage, _) =>
    storage.getByUsername("username").map(_ shouldBe Left(ApiError.userNotFound("username")))
  }

  it should "return User if user with matching username exists" in resetStorage { case (storage, xa) =>
    for {
      _    <- insertUser().transact(xa)
      user <- storage.getByUsername("username")
    } yield user should matchPattern { case Right(User(_, "username", Some("email"), Some("@username"))) => }
  }

  "save" should "return User if storing successful" in resetStorage { case (storage, _) =>
    storage
      .save(exampleNewUser)
      .map(_ should matchPattern { case Right(User(_, "username", Some("email"), Some("@username"))) => })
  }

  it should "return LogicError if user with such username already exist" in resetStorage { case (storage, xa) =>
    for {
      _     <- insertUser().transact(xa)
      error <- storage.save(exampleNewUser)
    } yield error shouldBe Left(ApiError.usernameAlreadyTaken("username"))
  }

  "update" should "return User if storing successful" in resetStorage { case (storage, xa) =>
    for {
      userId <- insertUser().transact(xa)
      user <- storage.update(
        userId,
        UserUpdate(Some("new username"), Some("new email"), Some("modified @username")),
      )
    } yield user should matchPattern {
      case Right(User(_, "new username", Some("new email"), Some("modified @username"))) =>
    }
  }

  it should "return LogicError if user with such username already exist" in resetStorage { case (storage, xa) =>
    for {
      userId <- insertUser().transact(xa)
      _      <- insertUser(username = "user", telegramId = "@user").transact(xa)
      error  <- storage.update(userId, UserUpdate(Some("user"), Some("new email"), Some("modified @username")))
    } yield error shouldBe Left(ApiError.usernameAlreadyTaken("user"))
  }

  it should "return Left(Not found) if storage is empty" in resetStorage { case (storage, _) =>
    storage
      .update(exampleUserId, UserUpdate(Some("user"), Some("new email"), Some("modified @username")))
      .map(_ shouldBe Left(ApiError.userNotFound(exampleUserId)))
  }

}
