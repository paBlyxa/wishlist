package ru.dins.scalaschool.wishlist.test

import cats.effect.IO
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.dins.scalaschool.wishlist.db.{UserRepo, WishRepo, WishlistRepo}
import ru.dins.scalaschool.wishlist.service.ServiceImpl
import ru.dins.scalaschool.wishlist.test.TestExamples._

class ServiceTest extends AnyFlatSpec with Matchers with MockFactory {

  private val userRepo     = mock[UserRepo[IO]]
  private val wishlistRepo = mock[WishlistRepo[IO]]
  private val wishRepo     = mock[WishRepo[IO]]

  private val service = ServiceImpl(userRepo, wishlistRepo, wishRepo)

  "clear" should "return empty Wishlist if storage return something" in {
    (wishlistRepo.clear _).expects(exampleUUID).returns(IO.pure(Right(())))
    (wishlistRepo.get _).expects(exampleUUID).returns(IO.pure(Right(exampleWishlistSaved)))

    service.clear(exampleUserId, exampleUUID).unsafeRunSync() shouldBe Right(exampleWishlistEmpty)
  }

//  it should "return ApiError.UnexpectedError if unexpected error occurred" in {
//    (wishlistRepo.clear _).expects(exampleUUID).returns(IO.raiseError(new RuntimeException("Ooops")))
//
//    service.clear(exampleUUID).unsafeRunSync() shouldBe Left(ApiError.unexpectedError)
//  }

  "get" should "return Wishlist with wishes if storage return something" in {
    (wishlistRepo.get _).expects(exampleUUID).returns(IO.pure(Right(exampleWishlistSaved)))
    (wishRepo.getAllByWishlistId _).expects(exampleUUID).returns(IO.pure(Right(List(exampleWish))))

    service.get(exampleUserId, exampleUUID).unsafeRunSync() shouldBe Right(exampleWishlist)
  }

  it should "return Wishlist with empty wishes if storage return empty wishes" in {
    (wishlistRepo.get _).expects(exampleUUID).returns(IO.pure(Right(exampleWishlistSaved)))
    (wishRepo.getAllByWishlistId _).expects(exampleUUID).returns(IO.pure(Right(List())))

    service.get(exampleUserId, exampleUUID).unsafeRunSync() shouldBe Right(exampleWishlistEmpty)
  }
}
