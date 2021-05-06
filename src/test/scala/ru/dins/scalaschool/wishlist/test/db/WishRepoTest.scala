package ru.dins.scalaschool.wishlist.test.db

import cats.effect.IO
import doobie.implicits._
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

  "saveWish" should "return Right(Wish) if save successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      sample     <- storage.save(wishlistId, exampleNewWish)
    } yield sample should matchPattern {
      case Right(Wish(_, _, "present", Some("some link"), _, Some("comment"), WishStatus.Free, _)) =>
    }
  }

  it should "return Left(NotFound) if wishlist with id not found" in resetStorage { case (storage, _) =>
    storage.save(exampleUUID, exampleNewWish).map(_ shouldBe Left(ApiError.notFound(exampleUUID)))
  }

  "removeWish" should "return Unit if note deleted successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      id         <- insertWish(wishlistId).transact(xa)
      result     <- storage.remove(id)
      sample     <- storage.get(id)
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
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      id         <- insertWish(wishlistId).transact(xa)
      sample     <- storage.get(id)
    } yield sample should matchPattern {
      case Right(Wish(_, _, "present", Some("some link"), _, Some("comment"), WishStatus.Free, _)) =>
    }
  }

  "updateWish" should "return Wish with modified parameters if update successful" in resetStorage {
    case (storage, xa) =>
      for {
        userId     <- insertUser().transact(xa)
        wishlistId <- insertWishlist(userId = userId).transact(xa)
        id         <- insertWish(wishlistId).transact(xa)
        result     <- storage.update(id, exampleWishOption)
      } yield result should matchPattern {
        case Right(Wish(_, _, "new present", Some("new link"), _, Some("modified comment"), WishStatus.Free, _)) =>
      }
  }

  it should "return Wish with modified comment if update successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      id         <- insertWish(wishlistId).transact(xa)
      result     <- storage.update(id, WishUpdate(None, None, None, Some("modified comment")))
    } yield result should matchPattern {
      case Right(Wish(_, _, "present", Some("some link"), _, Some("modified comment"), WishStatus.Free, _)) =>
    }
  }

  "getAllByWishlistId" should "return list of wishes with matching wishlist_id" in resetStorage { case (storage, xa) =>
    for {
      userId      <- insertUser().transact(xa)
      wishlist1Id <- insertWishlist(userId = userId).transact(xa)
      _           <- insertWish(wishlist1Id, "present", "some link", BigDecimal(123.45)).transact(xa)
      _           <- insertWish(wishlist1Id, "present2", "some link", BigDecimal(123.45)).transact(xa)
      wishlist2Id <- insertWishlist(userId = userId).transact(xa)
      _           <- insertWish(wishlist2Id, "present3", "some link", BigDecimal(123.45)).transact(xa)
      result      <- storage.getAllByWishlistId(wishlist1Id)
    } yield result should matchPattern {
      case Right(
            List(
              Wish(_, _, "present", Some("some link"), _, Some("comment"), WishStatus.Free, _),
              Wish(_, _, "present2", Some("some link"), _, Some("comment"), WishStatus.Free, _),
            ),
          ) =>
    }
  }

  it should "return empty list if non wishes matching wishlist_id" in resetStorage { case (storage, xa) =>
    for {
      userId      <- insertUser().transact(xa)
      wishlist1Id <- insertWishlist(userId = userId).transact(xa)
      _           <- insertWish(wishlist1Id, "present", "some link", BigDecimal(123.45)).transact(xa)
      _           <- insertWish(wishlist1Id, "present2", "some link", BigDecimal(123.45)).transact(xa)
      wishlist2Id <- insertWishlist(userId = userId).transact(xa)
      result      <- storage.getAllByWishlistId(wishlist2Id)
    } yield result should matchPattern { case Right(List()) => }
  }
}
