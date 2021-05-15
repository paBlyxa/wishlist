package ru.dins.scalaschool.wishlist.test

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.dins.scalaschool.wishlist.db.{UserRepo, WishRepo, WishlistRepo}
import ru.dins.scalaschool.wishlist.models.Models.{WishUpdate, WishlistUpdate}
import ru.dins.scalaschool.wishlist.models.{Access, ApiError, UserId, WishStatus}
import ru.dins.scalaschool.wishlist.service.ServiceImpl
import ru.dins.scalaschool.wishlist.test.TestExamples._

class ServiceTest extends AnyFlatSpec with Matchers with MockFactory {

  private val userRepo     = mock[UserRepo[IO]]
  private val wishlistRepo = mock[WishlistRepo[IO]]
  private val wishRepo     = mock[WishRepo[IO]]

  private val service = ServiceImpl(userRepo, wishlistRepo, wishRepo)

  "registerUser" should "return User if storage save successful" in {
    (userRepo.save _).expects(exampleNewUser).returns(IO.pure(Right(exampleUser)))

    service.registerUser(exampleNewUser).unsafeRunSync() shouldBe Right(exampleUser)
  }
  it should "return properly filled error model if username already taken" in {
    (userRepo.save _)
      .expects(exampleNewUser)
      .returns(IO.pure(Left(ApiError.usernameAlreadyTaken("username"))))

    service.registerUser(exampleNewUser).unsafeRunSync() shouldBe Left(ApiError.usernameAlreadyTaken("username"))
  }

  "login" should "return User if storage return something" in {
    (userRepo.getByUsername _).expects("username").returns(IO.pure(Right(exampleUser)))

    service.login("username").unsafeRunSync() shouldBe Right(exampleUser)
  }
  it should "return UserNotFound if storage hasn't user with that username" in {
    (userRepo.getByUsername _).expects("username").returns(IO.pure(ApiError.userNotFound("username").asLeft))

    service.login("username").unsafeRunSync() shouldBe Left(ApiError.userNotFound("username"))
  }

  "save" should "return Wishlist if storage save successful" in {
    (wishlistRepo.save _).expects(exampleUserId, exampleNewWishlist).returns(IO.pure(Right(exampleWishlistSaved)))
    (wishRepo.getAllByWishlistId _).expects(exampleWishlistId).returns(IO.pure(Right(List())))

    service.save(exampleUserId, exampleNewWishlist).unsafeRunSync() shouldBe Right(exampleWishlistEmpty)
  }
  it should "return UserNotFound if storage hasn't user with that userId" in {
    (wishlistRepo.save _)
      .expects(exampleUserId, exampleNewWishlist)
      .returns(IO.pure(ApiError.userNotFound(exampleUserId).asLeft))

    service.save(exampleUserId, exampleNewWishlist).unsafeRunSync() shouldBe Left(ApiError.userNotFound(exampleUserId))
  }

  "remove" should "return Unit if storage remove successful" in {
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(exampleWishlistSaved.asRight))
      (wishlistRepo.remove _).expects(exampleWishlistId).returns(IO.pure(Right(())))
    }

    service.remove(exampleUserId, exampleWishlistId).unsafeRunSync() shouldBe Right(())
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(exampleWishlistSaved.asRight))

    service.remove(anotherUserId, exampleWishlistId).unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return NotFound if storage hasn't wishlist with that id" in {
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(ApiError.notFound(exampleWishlistId).asLeft))

    service.remove(exampleUserId, exampleWishlistId).unsafeRunSync() shouldBe ApiError
      .notFound(exampleWishlistId)
      .asLeft
  }

  "addWish" should "return Wish if storage add new wish successful" in {
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(exampleWishlistSaved.asRight))
      (wishRepo.save _).expects(exampleWishlistId, exampleNewWish).returns(IO.pure(exampleWish.asRight))
    }

    service.addWish(exampleUserId, exampleWishlistId, exampleNewWish).unsafeRunSync() shouldBe exampleWish.asRight
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(exampleWishlistSaved.asRight))

    service.addWish(anotherUserId, exampleWishlistId, exampleNewWish).unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return NotFound if storage hasn't wishlist with that id" in {
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(ApiError.notFound(exampleWishlistId).asLeft))

    service.addWish(exampleUserId, exampleWishlistId, exampleNewWish).unsafeRunSync() shouldBe ApiError
      .notFound(exampleWishlistId)
      .asLeft
  }

  "removeWish" should "return Unit if storage remove wish successful" in {
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(exampleWishlistSaved.asRight))
      (wishRepo.remove _).expects(1).returns(IO.pure(Right(())))
    }

    service.removeWish(exampleUserId, exampleWishlistId, 1).unsafeRunSync() shouldBe ().asRight
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(exampleWishlistSaved.asRight))

    service.removeWish(anotherUserId, exampleWishlistId, 1).unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return NotFound if storage hasn't wishlist with that id" in {
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(ApiError.notFound(exampleWishlistId).asLeft))

    service.removeWish(exampleUserId, exampleWishlistId, 1).unsafeRunSync() shouldBe ApiError
      .notFound(exampleWishlistId)
      .asLeft
  }

  "clear" should "return empty Wishlist if storage delete all wishes from list successful" in {
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(exampleWishlistSaved.asRight))
      (wishlistRepo.clear _).expects(exampleWishlistId).returns(IO.pure(Right(())))
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(exampleWishlistSaved.asRight))
    }

    service.clear(exampleUserId, exampleWishlistId).unsafeRunSync() shouldBe Right(exampleWishlistEmpty)
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(exampleWishlistSaved.asRight))

    service.clear(anotherUserId, exampleWishlistId).unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return NotFound if storage hasn't wishlist with that id" in {
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(ApiError.notFound(exampleWishlistId).asLeft))

    service.clear(exampleUserId, exampleWishlistId).unsafeRunSync() shouldBe ApiError
      .notFound(exampleWishlistId)
      .asLeft
  }

  "get" should "return Wishlist with wishes if storage return something" in {
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.getAllByWishlistId _).expects(exampleWishlistId).returns(IO.pure(Right(List(exampleWish))))
    }
    service.get(exampleUserId, exampleWishlistId).unsafeRunSync() shouldBe Right(exampleWishlist)
  }
  it should "return Wishlist with empty wishes if storage return empty wishes" in {
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
    (wishRepo.getAllByWishlistId _).expects(exampleWishlistId).returns(IO.pure(Right(List())))

    service.get(exampleUserId, exampleWishlistId).unsafeRunSync() shouldBe Right(exampleWishlistEmpty)
  }
  it should "return Wishlist with wishes if Access.Public" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.getAllByWishlistId _).expects(exampleWishlistId).returns(IO.pure(Right(List(exampleWish))))
    }

    service.get(anotherUserId, exampleWishlistId).unsafeRunSync() shouldBe Right(exampleWishlist)
  }
  it should "return Wishlist with wishes if Access.Private and user in access list" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _)
        .expects(exampleWishlistId)
        .returns(IO.pure(Right(exampleWishlistSaved.copy(access = Access.Private))))
      (userRepo.hasUserAccess _).expects(anotherUserId, exampleWishlistId).returns(IO.pure(Some(1)))
      (wishRepo.getAllByWishlistId _).expects(exampleWishlistId).returns(IO.pure(Right(List(exampleWish))))
    }

    service.get(anotherUserId, exampleWishlistId).unsafeRunSync() shouldBe Right(
      exampleWishlist.copy(access = Access.Private),
    )
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _)
        .expects(exampleWishlistId)
        .returns(IO.pure(Right(exampleWishlistSaved.copy(access = Access.Private))))
      (userRepo.hasUserAccess _).expects(anotherUserId, exampleWishlistId).returns(IO.pure(None))
    }

    service.get(anotherUserId, exampleWishlistId).unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return NotFound if storage hasn't wishlist with that id" in {
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(ApiError.notFound(exampleWishlistId).asLeft))

    service.get(exampleUserId, exampleWishlistId).unsafeRunSync() shouldBe ApiError
      .notFound(exampleWishlistId)
      .asLeft
  }

  "modify" should "return Wishlist if storage update successful" in {
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(exampleWishlistSaved.asRight))
      (wishlistRepo.update _)
        .expects(exampleWishlistId, exampleWishlistOption)
        .returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.getAllByWishlistId _).expects(exampleWishlistId).returns(IO.pure(Right(List(exampleWish))))
    }

    service
      .modify(exampleUserId, exampleWishlistId, exampleWishlistOption)
      .unsafeRunSync() shouldBe exampleWishlist.asRight
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))

    service
      .modify(anotherUserId, exampleWishlistId, exampleWishlistOption)
      .unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return Forbidden if WishlistUpdate empty and user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _)
        .expects(exampleWishlistId)
        .returns(IO.pure(Right(exampleWishlistSaved.copy(access = Access.Private))))
      (userRepo.hasUserAccess _).expects(anotherUserId, exampleWishlistId).returns(IO.pure(None))
    }
    service
      .modify(anotherUserId, exampleWishlistId, WishlistUpdate(None, None))
      .unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return NotFound if storage hasn't wishlist with that id" in {
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(ApiError.notFound(exampleWishlistId).asLeft))

    service
      .modify(exampleUserId, exampleWishlistId, exampleWishlistOption)
      .unsafeRunSync() shouldBe ApiError.notFound(exampleWishlistId).asLeft
  }

  "modifyWish" should "return updated Wish if storage update successful" in {
    inSequence {
      (wishRepo.get _).expects(1).returns(IO.pure(Right(exampleWish)))
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.update _).expects(1, exampleWishOption).returns(IO.pure(exampleWish.asRight))
    }

    service.modifyWish(exampleUserId, 1, exampleWishOption).unsafeRunSync() shouldBe exampleWish.asRight
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishRepo.get _).expects(1).returns(IO.pure(Right(exampleWish)))
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
    }

    service
      .modifyWish(anotherUserId, 1, exampleWishOption)
      .unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return Forbidden if user hasn't access to that wishlist and WishUpdate empty" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishRepo.get _).expects(1).returns(IO.pure(Right(exampleWish)))
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
    }

    service
      .modifyWish(anotherUserId, 1, WishUpdate(None, None, None, None))
      .unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return NotFound if storage hasn't wish with that id" in {
    (wishRepo.get _).expects(1).returns(IO.pure(ApiError.wishNotFound(1).asLeft))

    service
      .modifyWish(exampleUserId, 1, exampleWishOption)
      .unsafeRunSync() shouldBe ApiError.wishNotFound(1).asLeft
  }

  "modifyAccess" should "return Wishlist if storage update successful" in {
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishlistRepo.updateAccess _)
        .expects(exampleWishlistId, Access.Private)
        .returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.getAllByWishlistId _).expects(exampleWishlistId).returns(IO.pure(Right(List(exampleWish))))
    }

    service
      .modifyAccess(exampleUserId, exampleWishlistId, Access.Private)
      .unsafeRunSync() shouldBe exampleWishlist.asRight
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))

    service
      .modifyAccess(anotherUserId, exampleWishlistId, Access.Private)
      .unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return NotFound if storage hasn't wishlist with that id" in {
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(ApiError.notFound(exampleWishlistId).asLeft))

    service
      .modifyAccess(exampleUserId, exampleWishlistId, Access.Private)
      .unsafeRunSync() shouldBe ApiError.notFound(exampleWishlistId).asLeft
  }

  "getWishes" should "return list of wish if storage return something" in {
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.getAllByWishlistId _).expects(exampleWishlistId).returns(IO.pure(Right(List(exampleWish))))
    }
    service.getWishes(exampleUserId, exampleWishlistId).unsafeRunSync() shouldBe List(exampleWish).asRight
  }
  it should "return list of wish if wishlist's access = public" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.getAllByWishlistId _).expects(exampleWishlistId).returns(IO.pure(Right(List(exampleWish))))
    }
    service.getWishes(anotherUserId, exampleWishlistId).unsafeRunSync() shouldBe List(exampleWish).asRight
  }
  it should "return list of wish if wishlist's access = private and user has access" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _)
        .expects(exampleWishlistId)
        .returns(IO.pure(Right(exampleWishlistSaved.copy(access = Access.Private))))
      (userRepo.hasUserAccess _).expects(anotherUserId, exampleWishlistId).returns(IO.pure(Some(1)))
      (wishRepo.getAllByWishlistId _).expects(exampleWishlistId).returns(IO.pure(Right(List(exampleWish))))
    }
    service.getWishes(anotherUserId, exampleWishlistId).unsafeRunSync() shouldBe List(exampleWish).asRight
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _)
        .expects(exampleWishlistId)
        .returns(IO.pure(Right(exampleWishlistSaved.copy(access = Access.Private))))
      (userRepo.hasUserAccess _).expects(anotherUserId, exampleWishlistId).returns(IO.pure(None))
    }
    service.getWishes(anotherUserId, exampleWishlistId).unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return NotFound if storage hasn't wishlist with that id" in {
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(ApiError.notFound(exampleWishlistId).asLeft))

    service.getWishes(exampleUserId, exampleWishlistId).unsafeRunSync() shouldBe ApiError
      .notFound(exampleWishlistId)
      .asLeft
  }

  "updateWishStatus" should "return Wish if storage update status successful" in {
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.get _).expects(1).returns(IO.pure(exampleWish.asRight))
      (wishRepo.updateStatus _).expects(exampleUserId, 1, WishStatus.Booked).returns(IO.pure(exampleWish.asRight))
    }
    service
      .updateWishStatus(exampleUserId, exampleWishlistId, 1, WishStatus.Booked)
      .unsafeRunSync() shouldBe exampleWish.asRight
  }
  it should "return wish if wishlist's access = public" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.get _).expects(1).returns(IO.pure(exampleWish.asRight))
      (wishRepo.updateStatus _).expects(anotherUserId, 1, WishStatus.Booked).returns(IO.pure(exampleWish.asRight))
    }
    service
      .updateWishStatus(anotherUserId, exampleWishlistId, 1, WishStatus.Booked)
      .unsafeRunSync() shouldBe exampleWish.asRight
  }
  it should "return wish if wishlist's access = private and user has access" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _)
        .expects(exampleWishlistId)
        .returns(IO.pure(Right(exampleWishlistSaved.copy(access = Access.Private))))
      (userRepo.hasUserAccess _).expects(anotherUserId, exampleWishlistId).returns(IO.pure(Some(1)))
      (wishRepo.get _).expects(1).returns(IO.pure(exampleWish.asRight))
      (wishRepo.updateStatus _).expects(anotherUserId, 1, WishStatus.Booked).returns(IO.pure(exampleWish.asRight))
    }
    service
      .updateWishStatus(anotherUserId, exampleWishlistId, 1, WishStatus.Booked)
      .unsafeRunSync() shouldBe exampleWish.asRight
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _)
        .expects(exampleWishlistId)
        .returns(IO.pure(Right(exampleWishlistSaved.copy(access = Access.Private))))
      (userRepo.hasUserAccess _).expects(anotherUserId, exampleWishlistId).returns(IO.pure(None))
    }
    service
      .updateWishStatus(anotherUserId, exampleWishlistId, 1, WishStatus.Booked)
      .unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return Forbidden if wish already booked by other user" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.get _).expects(1).returns(IO.pure(exampleWish.copy(status = WishStatus.Booked).asRight))
      (wishRepo.getUsersBooked _).expects(1).returns(IO.pure(List(exampleUserId)))
    }
    service
      .updateWishStatus(anotherUserId, exampleWishlistId, 1, WishStatus.Got)
      .unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return wish if wish already booked by this user" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.get _).expects(1).returns(IO.pure(exampleWish.copy(status = WishStatus.Booked).asRight))
      (wishRepo.getUsersBooked _).expects(1).returns(IO.pure(List(anotherUserId)))
      (wishRepo.updateStatus _).expects(anotherUserId, 1, WishStatus.Got).returns(IO.pure(exampleWish.asRight))
    }
    service
      .updateWishStatus(anotherUserId, exampleWishlistId, 1, WishStatus.Got)
      .unsafeRunSync() shouldBe exampleWish.asRight
  }
  it should "return NotFound if storage hasn't wishlist with that id" in {
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(ApiError.notFound(exampleWishlistId).asLeft))

    service
      .updateWishStatus(exampleUserId, exampleWishlistId, 1, WishStatus.Booked)
      .unsafeRunSync() shouldBe ApiError
      .notFound(exampleWishlistId)
      .asLeft
  }
  it should "return Forbidden if wish already shared by many users" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.get _).expects(1).returns(IO.pure(exampleWish.copy(status = WishStatus.Shared).asRight))
      (wishRepo.getUsersBooked _).expects(1).returns(IO.pure(List(exampleUserId, anotherUserId)))
    }
    service
      .updateWishStatus(anotherUserId, exampleWishlistId, 1, WishStatus.Free)
      .unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }

  "provideAccess" should "return Unit if storage save users_access successful" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (userRepo.saveUserAccess _).expects(anotherUserId, exampleWishlistId).returns(IO.pure(Right(())))
    }
    service.provideAccess(exampleUserId, exampleWishlistId, anotherUserId).unsafeRunSync() shouldBe Right(())
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))

    service
      .provideAccess(anotherUserId, exampleWishlistId, exampleUserId)
      .unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }

  "forbidAccess" should "return Unit if storage remove users_access successful" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (userRepo.removeUserAccess _).expects(anotherUserId, exampleWishlistId).returns(IO.pure(Right(())))
    }
    service.forbidAccess(exampleUserId, exampleWishlistId, anotherUserId).unsafeRunSync() shouldBe Right(())
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))

    service
      .forbidAccess(anotherUserId, exampleWishlistId, exampleUserId)
      .unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }

  "addUserToShareWish" should "return Unit if storage add user to share wish successful" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.get _).expects(1).returns(IO.pure(Right(exampleWish.copy(status = WishStatus.Shared))))
      (wishRepo.addUserToShare _).expects(anotherUserId, 1).returns(IO.pure(Right(())))
    }

    service.addUserToShareWish(anotherUserId, exampleWishlistId, 1).unsafeRunSync() shouldBe Right(())
  }
  it should "return Forbidden if wish's status not Share" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.get _).expects(1).returns(IO.pure(Right(exampleWish.copy(status = WishStatus.Booked))))
    }

    service.addUserToShareWish(anotherUserId, exampleWishlistId, 1).unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _)
        .expects(exampleWishlistId)
        .returns(IO.pure(Right(exampleWishlistSaved.copy(access = Access.Private))))
      (userRepo.hasUserAccess _).expects(anotherUserId, exampleWishlistId).returns(IO.pure(None))
    }
    service.addUserToShareWish(anotherUserId, exampleWishlistId, 1).unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }

  "removeUserToShareWish" should "return Unit if storage remove user from share wish successful" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _).expects(exampleWishlistId).returns(IO.pure(Right(exampleWishlistSaved)))
      (wishRepo.removeUserToShare _).expects(anotherUserId, 1).returns(IO.pure(Right(())))
    }

    service.removeUserToShareWish(anotherUserId, exampleWishlistId, 1).unsafeRunSync() shouldBe Right(())
  }
  it should "return Forbidden if user hasn't access to that wishlist" in {
    val anotherUserId = UserId("00000000-0000-0000-0000-000000000001")
    inSequence {
      (wishlistRepo.get _)
        .expects(exampleWishlistId)
        .returns(IO.pure(Right(exampleWishlistSaved.copy(access = Access.Private))))
      (userRepo.hasUserAccess _).expects(anotherUserId, exampleWishlistId).returns(IO.pure(None))
    }
    service
      .removeUserToShareWish(anotherUserId, exampleWishlistId, 1)
      .unsafeRunSync() shouldBe ApiError.forbidden.asLeft
  }
}
