package ru.dins.scalaschool.wishlist.test.db

import cats.effect.IO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import org.scalatest.Assertion
import ru.dins.scalaschool.wishlist.db.{WishRepo, WishRepoImpl}
import ru.dins.scalaschool.wishlist.models.Models._
import ru.dins.scalaschool.wishlist.models._
import ru.dins.scalaschool.wishlist.test.TestExamples._


class WishRepoTest extends MyTestContainerForAll {

  def resetStorage(test: (WishRepo[IO], Transactor.Aux[IO, Unit]) => IO[Assertion]): Unit =
    withContainers { container =>
      val xa       = createTransactor(container)
      val truncate = sql"truncate wishlist cascade; truncate users cascade".update.run
      val storage  = WishRepoImpl(xa)

      val result = for {
        _         <- truncate.transact(xa)
        assertion <- test(storage, xa)
      } yield assertion

      result.unsafeRunSync()
    }

  private val insertUser =
    sql"insert into users values ($exampleUserId, 'username', 'email', '@username')".update.run
  private val insertWishlist =
    sql"insert into wishlist values ($exampleUUID, $exampleUserId, 'wishlist', ${Access.public}, 'comment', $exampleLDT)".update.run
  private val insertWish =
    sql"insert into wish (wishlist_id, name, link, price, comment) values ($exampleUUID, 'present', 'some link', '123.45', 'comment')".update
      .withUniqueGeneratedKeys[Long]("id")

  "saveWish" should "return Right(Wish) if save successful" in resetStorage { case (storage, xa) =>
    for {
      _      <- insertUser.transact(xa)
      _      <- insertWishlist.transact(xa)
      sample <- storage.save(exampleUUID, exampleNewWish)
    } yield sample should matchPattern { case Right(Wish(_, _, "present", Some("some link"), _, Some("comment"), _)) =>
    }
  }

  it should "return Left(NotFound) if wishlist with id not found" in resetStorage { case (storage, _) =>
    storage.save(exampleUUID, exampleNewWish).map(_ shouldBe Left(ApiError.notFound(exampleUUID)))
  }

  "removeWish" should "return Unit if note deleted successful" in resetStorage { case (storage, xa) =>
    for {
      _      <- insertUser.transact(xa)
      _      <- insertWishlist.transact(xa)
      id     <- insertWish.transact(xa)
      result <- storage.remove(id)
      sample <- storage.get(id)
    } yield (result, sample) shouldBe (Right(()), Left(ApiError.wishNotFound(id)))
  }

  it should "return Unit if nothing deleted" in resetStorage { case (storage, _) =>
    storage.remove(1).map(_ shouldBe Right(()))
  }

  "getWish" should "return Left(Not found) if storage is empty" in resetStorage { case (storage, _) =>
    storage.get(1).map(_ shouldBe Left(ApiError.wishNotFound(1)))
  }

  it should "return Right(Wish) if storage return something" in resetStorage { case (storage, xa) =>
    for {
      _      <- insertUser.transact(xa)
      _      <- insertWishlist.transact(xa)
      id     <- insertWish.transact(xa)
      sample <- storage.get(id)
    } yield sample should matchPattern { case Right(Wish(_, _, "present", Some("some link"), _, Some("comment"), _)) =>
    }
  }

  "updateWish" should "return Wish with modified parameters if update successful" in resetStorage {
    case (storage, xa) =>
      for {
        _      <- insertUser.transact(xa)
        _      <- insertWishlist.transact(xa)
        id     <- insertWish.transact(xa)
        result <- storage.update(id, exampleWishOption)
      } yield result should matchPattern {
        case Right(Wish(_, _, "new present", Some("new link"), _, Some("modified comment"), _)) =>
      }
  }

  it should "return Wish with modified comment if update successful" in resetStorage { case (storage, xa) =>
    for {
      _      <- insertUser.transact(xa)
      _      <- insertWishlist.transact(xa)
      id     <- insertWish.transact(xa)
      result <- storage.update(id, WishOption(None, None, None, Some("modified comment")))
    } yield result should matchPattern {
      case Right(Wish(_, _, "present", Some("some link"), _, Some("modified comment"), _)) =>
    }
  }
}
