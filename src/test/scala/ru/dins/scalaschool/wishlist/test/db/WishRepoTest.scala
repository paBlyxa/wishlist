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

  "save" should "return Right(Wish) if save successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      sample     <- storage.save(wishlistId, exampleNewWish)
    } yield sample should matchPattern {
      case Right(Wish(_, _, "present", Some("some link"), _, Some("comment"), WishStatus.Free, _)) =>
    }
  }

  it should "return Left(NotFound) if wishlist with id not found" in resetStorage { case (storage, _) =>
    storage.save(exampleWishlistId, exampleNewWish).map(_ shouldBe Left(ApiError.notFound(exampleWishlistId)))
  }

  "remove" should "return Unit if note deleted successful" in resetStorage { case (storage, xa) =>
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

  "get" should "return Left(Not found) if storage is empty" in resetStorage { case (storage, _) =>
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

  "update" should "return Wish with modified parameters if update successful" in resetStorage { case (storage, xa) =>
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

  "updateStatus" should "return Wish with updated status 'booked' if update successful" in resetStorage {
    case (storage, xa) =>
      for {
        userId     <- insertUser().transact(xa)
        wishlistId <- insertWishlist(userId = userId).transact(xa)
        wishId     <- insertWish(wishlistId, "present", "some link", BigDecimal(123.45)).transact(xa)
        wish       <- storage.updateStatus(userId, wishId, WishStatus.Booked)
        users <- sql"select user_id from users_wish where wish_id = $wishId"
          .query[UserId]
          .stream
          .compile
          .toList
          .transact(xa)
      } yield (wish, users) should matchPattern {
        case (Right(Wish(_, _, "present", Some("some link"), _, _, WishStatus.Booked, _)), List(_)) =>
      }
  }
  it should "return Wish with updated status 'free' if update successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      wishId     <- insertWish(wishlistId, "present", "some link", BigDecimal(123.45)).transact(xa)
      _          <- storage.updateStatus(userId, wishId, WishStatus.Booked)
      wish       <- storage.updateStatus(userId, wishId, WishStatus.Free)
      users <- sql"select user_id from users_wish where wish_id = $wishId"
        .query[UserId]
        .stream
        .compile
        .toList
        .transact(xa)
    } yield (wish, users) should matchPattern {
      case (Right(Wish(_, _, "present", Some("some link"), _, _, WishStatus.Free, _)), List()) =>
    }
  }
  it should "return Wish with updated status 'got' if update successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      wishId     <- insertWish(wishlistId, "present", "some link", BigDecimal(123.45)).transact(xa)
      _          <- storage.updateStatus(userId, wishId, WishStatus.Booked)
      wish       <- storage.updateStatus(userId, wishId, WishStatus.Got)
      users <- sql"select user_id from users_wish where wish_id = $wishId"
        .query[UserId]
        .stream
        .compile
        .toList
        .transact(xa)
    } yield (wish, users) should matchPattern {
      case (Right(Wish(_, _, "present", Some("some link"), _, _, WishStatus.Got, _)), List(_)) =>
    }
  }

  "addUserToShare" should "return Unit if adding user successful" in resetStorage { case (storage, xa) =>
    for {
      userId         <- insertUser().transact(xa)
      anotherUser1Id <- insertUser(username = "user1").transact(xa)
      anotherUser2Id <- insertUser(username = "user2").transact(xa)
      wishlistId     <- insertWishlist(userId = userId).transact(xa)
      wishId         <- insertWish(wishlistId, "present", "some link", BigDecimal(123.45)).transact(xa)
      _              <- storage.updateStatus(anotherUser1Id, wishId, WishStatus.Shared)
      result         <- storage.addUserToShare(anotherUser2Id, wishId)
      list           <- storage.getUsersBooked(wishId)
    } yield (result, list) should matchPattern { case (Right(()), List(_, _)) =>
    }
  }

  "removeUserToShare" should "return Unit if adding user successful" in resetStorage { case (storage, xa) =>
    for {
      userId         <- insertUser().transact(xa)
      anotherUser1Id <- insertUser(username = "user1").transact(xa)
      anotherUser2Id <- insertUser(username = "user2").transact(xa)
      wishlistId     <- insertWishlist(userId = userId).transact(xa)
      wishId         <- insertWish(wishlistId, "present", "some link", BigDecimal(123.45)).transact(xa)
      _              <- storage.updateStatus(anotherUser1Id, wishId, WishStatus.Shared)
      _              <- storage.addUserToShare(anotherUser2Id, wishId)
      result         <- storage.removeUserToShare(anotherUser1Id, wishId)
      list           <- storage.getUsersBooked(wishId)
    } yield (result, list) should matchPattern { case (Right(()), List(_)) =>
    }
  }
  it should "change wish's status to Free if no more users share wish" in resetStorage { case (storage, xa) =>
    for {
      userId         <- insertUser().transact(xa)
      anotherUser1Id <- insertUser(username = "user1").transact(xa)
      wishlistId     <- insertWishlist(userId = userId).transact(xa)
      wishId         <- insertWish(wishlistId, "present", "some link", BigDecimal(123.45)).transact(xa)
      _              <- storage.updateStatus(anotherUser1Id, wishId, WishStatus.Shared)
      result         <- storage.removeUserToShare(anotherUser1Id, wishId)
      list           <- storage.getUsersBooked(wishId)
      wish           <- storage.get(wishId)
    } yield (result, list, wish) should matchPattern {
      case (Right(()), List(), Right(Wish(_, _, _, _, _, _, WishStatus.Free, _))) =>
    }
  }
}
