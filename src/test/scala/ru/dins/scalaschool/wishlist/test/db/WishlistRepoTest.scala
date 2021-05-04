package ru.dins.scalaschool.wishlist.test.db

import cats.effect.IO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import org.scalatest.Assertion
import ru.dins.scalaschool.wishlist.db.{WishlistRepo, WishlistRepoImpl}
import ru.dins.scalaschool.wishlist.models.ApiError
import ru.dins.scalaschool.wishlist.models.Models.{Access, Wishlist, WishlistOption}
import ru.dins.scalaschool.wishlist.test.TestExamples._

class WishlistRepoTest extends MyTestContainerForAll{

  def resetStorage(test: (WishlistRepo[IO], Transactor.Aux[IO, Unit]) => IO[Assertion]): Unit =
    withContainers { container =>
      val xa       = createTransactor(container)
      val truncate = sql"truncate wishlist cascade; truncate users cascade".update.run
      val storage  = WishlistRepoImpl(xa)

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

  "save" should "return Right(Wishlist) if save successful" in resetStorage { case (storage, xa) =>
    for {
      _      <- insertUser.transact(xa)
      sample <- storage.save(exampleNewWishlist)
    } yield sample should matchPattern {
      case Right(Wishlist(_, _, "My wishlist", Access.public, Some("For my birthday"), _)) =>
    }
  }

  it should "return Left(NotFound) if user with userId not found" in resetStorage { case (storage, _) =>
    storage.save(exampleNewWishlist).map(_ shouldBe Left(ApiError.userNotFound(exampleUserId)))
  }

  "delete" should "return Unit if note deleted successful" in resetStorage { case (storage, xa) =>
    for {
      _      <- insertUser.transact(xa)
      _      <- insertWishlist.transact(xa)
      result <- storage.remove(exampleUUID)
      sample <- storage.get(exampleUUID)
    } yield (result, sample) shouldBe (Right(()), Left(ApiError.notFound(exampleUUID)))
  }

  it should "return Unit if nothing deleted" in resetStorage { case (storage, _) =>
    storage.remove(exampleUUID).map(_ shouldBe Right(()))
  }

  "get" should "return Left(Not found) if storage is empty" in resetStorage { case (storage, _) =>
    storage.get(exampleUUID).map(_ shouldBe Left(ApiError.notFound(exampleUUID)))
  }

  it should "return Right(Wishlist) if storage return something" in resetStorage { case (storage, xa) =>
    for {
      _      <- insertUser.transact(xa)
      _      <- insertWishlist.transact(xa)
      sample <- storage.get(exampleUUID)
    } yield sample should matchPattern { case Right(Wishlist(_, _, "wishlist", Access.public, Some("comment"), _)) =>
    }
  }

  "clear" should "return Unit if note deleted successful" in resetStorage { case (storage, xa) =>
    for {
      _      <- insertUser.transact(xa)
      _      <- insertWishlist.transact(xa)
      id     <- insertWish.transact(xa)
      result <- storage.clear(exampleUUID)
    } yield result shouldBe Right(())
  }

  it should "return Unit if nothing deleted" in resetStorage { case (storage, _) =>
    storage.clear(exampleUUID).map(_ shouldBe Right(()))
  }

  "findAll" should "return list of wishlists if storage return something" in resetStorage { case (storage, xa) =>
    for {
      _      <- insertUser.transact(xa)
      _      <- insertWishlist.transact(xa)
      result <- storage.findAll
    } yield result should matchPattern {
      case Right(List(Wishlist(_, _, "wishlist", Access.public, Some("comment"), _))) =>
    }
  }

  it should "return empty list if none match" in resetStorage { case (storage, _) =>
    storage.findAll.map(_ shouldBe Right(List()))
  }

  "update" should "return Wishlist with modified parameters if update successful" in resetStorage {
    case (storage, xa) =>
      for {
        _      <- insertUser.transact(xa)
        _      <- insertWishlist.transact(xa)
        result <- storage.update(exampleUUID, exampleWishlistOption)
      } yield result should matchPattern {
        case Right(Wishlist(_, _, "new name", Access.public, Some("new comment"), _)) =>
      }
  }

  it should "return Wishlist with modified name if update successful" in resetStorage { case (storage, xa) =>
    for {
      _      <- insertUser.transact(xa)
      _      <- insertWishlist.transact(xa)
      result <- storage.update(exampleUUID, WishlistOption(Some("new name"), None))
    } yield result should matchPattern { case Right(Wishlist(_, _, "new name", Access.public, Some("comment"), _)) =>
    }
  }

  "update" should "return Wishlist with modified access if update successful" in resetStorage { case (storage, xa) =>
    for {
      _      <- insertUser.transact(xa)
      _      <- insertWishlist.transact(xa)
      result <- storage.update(exampleUUID, Access.privat)
    } yield result should matchPattern { case Right(Wishlist(_, _, "wishlist", Access.privat, Some("comment"), _)) =>
    }
  }

  it should "return Left(Not found) if storage is empty" in resetStorage { case (storage, _) =>
    storage.update(exampleUUID, Access.privat).map(_ shouldBe Left(ApiError.notFound(exampleUUID)))
  }
}
