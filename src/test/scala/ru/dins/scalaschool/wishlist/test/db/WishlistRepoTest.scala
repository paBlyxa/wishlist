package ru.dins.scalaschool.wishlist.test.db

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.scalatest.Assertion
import ru.dins.scalaschool.wishlist.db.{WishlistRepo, WishlistRepoImpl}
import ru.dins.scalaschool.wishlist.models.{Access, ApiError, FilterList, OrderDir, WishlistOrder}
import ru.dins.scalaschool.wishlist.models.Models._
import ru.dins.scalaschool.wishlist.test.TestExamples._

import java.time.LocalDate

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
      case Right(WishlistSaved(_, _, "My wishlist", Access.Public, Some("For my birthday"), _, _)) =>
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
    storage.remove(exampleWishlistId).map(_ shouldBe Right(()))
  }

  "get" should "return Left(Not found) if storage is empty" in resetStorage { case (storage, _) =>
    storage.get(exampleWishlistId).map(_ shouldBe Left(ApiError.notFound(exampleWishlistId)))
  }

  it should "return Right(Wishlist) if storage return something" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      sample     <- storage.get(wishlistId)
    } yield sample should matchPattern {
      case Right(WishlistSaved(_, _, "wishlist", Access.Public, Some("comment"), _, _)) =>
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
    storage.clear(exampleWishlistId).map(_ shouldBe Right(()))
  }

  "findAll" should "return list of wishlists if storage return something" in resetStorage { case (storage, xa) =>
    for {
      userId <- insertUser().transact(xa)
      _      <- insertWishlist(userId = userId).transact(xa)
      result <- storage.findAll(userId, filterEmpty)
    } yield result should matchPattern {
      case Right(List(WishlistWeb(_, _, "wishlist", Access.Public, Some("comment"), _))) =>
    }
  }

  it should "return empty list if storage empty" in resetStorage { case (storage, _) =>
    storage.findAll(exampleUserId, filterEmpty).map(_ shouldBe Right(List()))
  }

  it should "return list of wishlists with matching filter and order by name" in resetStorage { case (storage, xa) =>
    for {
      userId        <- insertUser().transact(xa)
      anotherUserId <- insertUser(username = "anotherUser").transact(xa)
      _             <- insertWishlist(userId = userId, name = "birthday").transact(xa)
      _             <- insertWishlist(userId = anotherUserId, name = "Zero wishlist").transact(xa)
      _             <- insertWishlist(userId = anotherUserId, name = "Private wishlist", access = Access.Private).transact(xa)
      wishlistId <- insertWishlist(userId = anotherUserId, name = "Private wishlist shared", access = Access.Private)
        .transact(xa)
      _ <- sql"""insert into users_access (user_id, wishlist_id) values ($userId, $wishlistId)""".update.run
        .transact(xa)
      result <- storage.findAll(
        userId,
        FilterList(Some("user"), Some("wish"), Some(WishlistOrder.Name), Some(OrderDir.Desc)),
      )
    } yield result should matchPattern {
      case Right(
            List(
              WishlistWeb(_, _, "Zero wishlist", Access.Public, Some("comment"), _),
              WishlistWeb(_, _, "Private wishlist shared", Access.Private, Some("comment"), _),
            ),
          ) =>
    }
  }

  it should "return list of wishlists with matching filter and order by username" in resetStorage {
    case (storage, xa) =>
      for {
        userId         <- insertUser(username = "myname").transact(xa)
        anotherUser1Id <- insertUser(username = "anotherUser1").transact(xa)
        anotherUser2Id <- insertUser(username = "anotherUser2").transact(xa)
        _              <- insertWishlist(userId = userId, name = "birthday").transact(xa)
        _              <- insertWishlist(userId = anotherUser1Id, name = "Zero wishlist").transact(xa)
        _              <- insertWishlist(userId = anotherUser2Id, name = "Private wishlist", access = Access.Private).transact(xa)
        wishlistId <- insertWishlist(userId = anotherUser2Id, name = "Private wishlist shared", access = Access.Private)
          .transact(xa)
        _ <- sql"""insert into users_access (user_id, wishlist_id) values ($userId, $wishlistId)""".update.run
          .transact(xa)
        result <- storage.findAll(
          userId,
          FilterList(Some("another"), None, Some(WishlistOrder.Username), None),
        )
      } yield result should matchPattern {
        case Right(
              List(
                WishlistWeb(_, _, "Zero wishlist", Access.Public, Some("comment"), _),
                WishlistWeb(_, _, "Private wishlist shared", Access.Private, Some("comment"), _),
              ),
            ) =>
      }
  }

  "update" should "return Wishlist with modified parameters if update successful" in resetStorage {
    case (storage, xa) =>
      for {
        userId     <- insertUser().transact(xa)
        wishlistId <- insertWishlist(userId = userId).transact(xa)
        result     <- storage.update(wishlistId, exampleWishlistOption)
      } yield result should matchPattern {
        case Right(WishlistSaved(_, _, "new name", Access.Private, Some("new comment"), _, _)) =>
      }
  }

  it should "return Wishlist with modified name if update successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      result     <- storage.update(wishlistId, WishlistUpdate(Some("new name"), None, None, None))
    } yield result should matchPattern {
      case Right(WishlistSaved(_, _, "new name", Access.Public, Some("comment"), _, _)) =>
    }
  }

  it should "return Wishlist with modified access if update successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      result     <- storage.update(wishlistId, WishlistUpdate(None, Some(Access.Private), None, None))
    } yield result should matchPattern {
      case Right(WishlistSaved(_, _, "wishlist", Access.Private, Some("comment"), _, _)) =>
    }
  }

  it should "return Wishlist with modified eventDate if update successful" in resetStorage { case (storage, xa) =>
    for {
      userId     <- insertUser().transact(xa)
      wishlistId <- insertWishlist(userId = userId).transact(xa)
      result     <- storage.update(wishlistId, WishlistUpdate(None, None, None, Some(LocalDate.MIN)))
    } yield result should matchPattern {
      case Right(WishlistSaved(_, _, "wishlist", Access.Public, Some("comment"), _, Some(LocalDate.MIN))) =>
    }
  }

  "updateAccess" should "return Wishlist with modified access if update successful" in resetStorage {
    case (storage, xa) =>
      for {
        userId     <- insertUser().transact(xa)
        wishlistId <- insertWishlist(userId = userId).transact(xa)
        result     <- storage.updateAccess(wishlistId, Access.Private)
      } yield result should matchPattern {
        case Right(WishlistSaved(_, _, "wishlist", Access.Private, Some("comment"), _, _)) =>
      }
  }

  it should "return Left(Not found) if storage is empty" in resetStorage { case (storage, _) =>
    storage.updateAccess(exampleWishlistId, Access.Private).map(_ shouldBe Left(ApiError.notFound(exampleWishlistId)))
  }
}
