package ru.dins.scalaschool.wishlist.test.db

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.scalatest.Assertion
import ru.dins.scalaschool.wishlist.db.{WishlistRepo, WishlistRepoImpl}
import ru.dins.scalaschool.wishlist.models.{Access, ApiError}
import ru.dins.scalaschool.wishlist.models.Models._
import ru.dins.scalaschool.wishlist.test.TestExamples._

class WishlistRepoTest extends MyTestContainerForAll {

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

  "save" should "return Right(Wishlist) if save successful" in resetStorage { case (storage, xa) =>
    for {
      _      <- insertUser(exampleUserId).transact(xa)
      sample <- storage.save(exampleUserId, exampleNewWishlist)
    } yield sample should matchPattern {
      case Right(WishlistSaved(_, _, "My wishlist", Access.Public, Some("For my birthday"), _)) =>
    }
  }

  it should "return Left(NotFound) if user with userId not found" in resetStorage { case (storage, _) =>
    storage.save(exampleUserId, exampleNewWishlist).map(_ shouldBe Left(ApiError.userNotFound(exampleUserId)))
  }

  "delete" should "return Unit if note deleted successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      result     <- storage.remove(wishlistId)
      sample     <- storage.get(wishlistId)
    } yield (result, sample) shouldBe (Right(()), Left(ApiError.notFound(wishlistId)))
  }

  it should "return Unit if nothing deleted" in resetStorage { case (storage, _) =>
    storage.remove(exampleUUID).map(_ shouldBe Right(()))
  }

  "get" should "return Left(Not found) if storage is empty" in resetStorage { case (storage, _) =>
    storage.get(exampleUUID).map(_ shouldBe Left(ApiError.notFound(exampleUUID)))
  }

  it should "return Right(Wishlist) if storage return something" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      sample     <- storage.get(wishlistId)
    } yield sample should matchPattern {
      case Right(WishlistSaved(_, _, "wishlist", Access.Public, Some("comment"), _)) =>
    }
  }

  "clear" should "return Unit if note deleted successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      _          <- insertWish(wishlistId).transact(xa)
      result     <- storage.clear(wishlistId)
    } yield result shouldBe Right(())
  }

  it should "return Unit if nothing deleted" in resetStorage { case (storage, _) =>
    storage.clear(exampleUUID).map(_ shouldBe Right(()))
  }

  "findAll" should "return list of wishlists if storage return something" in resetStorage { case (storage, xa) =>
    for {
      userId <- insertUser().transact(xa)
      _      <- insertWishlist(userId = userId).transact(xa)
      result <- storage.findAll
    } yield result should matchPattern {
      case Right(List(WishlistSaved(_, _, "wishlist", Access.Public, Some("comment"), _))) =>
    }
  }

  it should "return empty list if none match" in resetStorage { case (storage, _) =>
    storage.findAll.map(_ shouldBe Right(List()))
  }

  "update" should "return Wishlist with modified parameters if update successful" in resetStorage {
    case (storage, xa) =>
      for {
        userId     <- insertUser().transact(xa)
        wishlistId <- insertWishlist(userId = userId).transact(xa)
        result     <- storage.update(wishlistId, exampleWishlistOption)
      } yield result should matchPattern {
        case Right(WishlistSaved(_, _, "new name", Access.Public, Some("new comment"), _)) =>
      }
  }

  it should "return Wishlist with modified name if update successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      result <- storage.update(wishlistId, WishlistUpdate(Some("new name"), None))
    } yield result should matchPattern {
      case Right(WishlistSaved(_, _, "new name", Access.Public, Some("comment"), _)) =>
    }
  }

  "update" should "return Wishlist with modified access if update successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      result <- storage.update(wishlistId, Access.Private)
    } yield result should matchPattern {
      case Right(WishlistSaved(_, _, "wishlist", Access.Private, Some("comment"), _)) =>
    }
  }

  it should "return Left(Not found) if storage is empty" in resetStorage { case (storage, _) =>
    storage.update(exampleUUID, Access.Private).map(_ shouldBe Left(ApiError.notFound(exampleUUID)))
  }
}
