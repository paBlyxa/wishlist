package ru.dins.scalaschool.wishlist.service

import cats.effect.Sync
import ru.dins.scalaschool.wishlist.models._
import ru.dins.scalaschool.wishlist.models.Models._
import cats.syntax.functor._
import cats.syntax.flatMap._
import ru.dins.scalaschool.wishlist.db.{UserRepo, WishRepo, WishlistRepo}
import scala.language.implicitConversions

case class ServiceImpl[F[_]: Sync](userRepo: UserRepo[F], wishlistRepo: WishlistRepo[F], wishRepo: WishRepo[F])
    extends Service[F] {

  private val F: Sync[F] = Sync[F]

  override def registerUser(user: NewUser): F[Either[ApiError, User]] = userRepo.save(user)

  override def login(username: String): F[Either[ApiError, User]] = userRepo.getByUsername(username)

  override def save(userId: UserId, wishlist: NewWishlist): F[Either[ApiError, Wishlist]] =
    wishlistRepo.save(userId, wishlist)

  override def remove(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Unit]] =
    wishlistRepo.remove(wishlistId)

  override def addWish(userId: UserId, wishlistId: WishlistId, wish: NewWish): F[Either[ApiError, Wish]] =
    wishRepo.save(wishlistId, wish)

  override def removeWish(userId: UserId, wishlistId: WishlistId, wishId: Long): F[Either[ApiError, Unit]] =
    wishRepo.remove(wishId)

  override def clear(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Wishlist]] =
    wishlistRepo.clear(wishlistId).flatMap {
      case Left(err) => F.pure(Left(err))
      case _         => wishlistRepo.get(wishlistId).map(_.map(Wishlist(_, List.empty)))
    }

  override def get(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Wishlist]] =
    wishlistRepo
      .get(wishlistId)

  override def list: F[Either[ApiError, List[WishlistSaved]]] = wishlistRepo.findAll

  override def modify(userId: UserId, wishlistId: WishlistId, wishlist: WishlistUpdate): F[Either[ApiError, Wishlist]] =
    if (wishlist.isEmpty) {
      wishlistRepo
        .get(wishlistId)
    } else {
      wishlistRepo
        .update(wishlistId, wishlist)
    }

  override def modifyWish(userId: UserId, wishId: Long, wish: WishUpdate): F[Either[ApiError, Wish]] =
    if (wish.isEmpty) {
      wishRepo.get(wishId)
    } else {
      wishRepo.update(wishId, wish)
    }

  override def modifyAccess(userId: UserId, wishlistId: WishlistId, access: Access): F[Either[ApiError, Wishlist]] =
    wishlistRepo
      .update(wishlistId, access)

  override def getWishes(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, List[Wish]]] =
    wishRepo.getAllByWishlistId(wishlistId)

  implicit def injectWishes(f: F[Either[ApiError, WishlistSaved]]): F[Either[ApiError, Wishlist]] =
    f.flatMap(
      _.fold(
        err => F.pure(Left(err)),
        wishlistSaved =>
          wishRepo
            .getAllByWishlistId(wishlistSaved.id)
            .map(_.fold(left => Left(left), wishes => Right(Wishlist(wishlistSaved, wishes)))),
      ),
    )

}
