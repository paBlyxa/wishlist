package ru.dins.scalaschool.wishlist.service

import cats.effect.Sync
import ru.dins.scalaschool.wishlist.models._
import ru.dins.scalaschool.wishlist.models.Models._
import ru.dins.scalaschool.wishlist.models.Models.Access.Access
import cats.syntax.flatMap._
import ru.dins.scalaschool.wishlist.db.{UserRepo, WishRepo, WishlistRepo}

import java.util.UUID

case class ServiceImpl[F[_]: Sync](userRepo: UserRepo[F], wishlistRepo: WishlistRepo[F], wishRepo: WishRepo[F]) extends Service[F] {

  private val F: Sync[F] = Sync[F]

  override def registerUser(user: NewUser): F[Either[ApiError, User]] = userRepo.save(user)

  override def login(username: String): F[Either[ApiError, User]] = userRepo.getByUsername(username)

  override def save(wishlist: NewWishlist): F[Either[ApiError, Wishlist]] = wishlistRepo.save(wishlist)

  override def remove(uuid: UUID): F[Either[ApiError, Unit]] = wishlistRepo.remove(uuid)

  override def addWish(uuid: UUID, wish: NewWish): F[Either[ApiError, Wish]] = wishRepo.save(uuid, wish)

  override def removeWish(uuid: UUID, wishId: Long): F[Either[ApiError, Unit]] = wishRepo.remove(wishId)

  override def clear(uuid: UUID): F[Either[ApiError, Wishlist]] =
    wishlistRepo.clear(uuid).flatMap {
      case Left(err) => F.pure(Left(err))
      case _         => wishlistRepo.get(uuid)
    }

  override def get(uuid: UUID): F[Either[ApiError, Wishlist]] = wishlistRepo.get(uuid)

  override def list: F[Either[ApiError, List[Wishlist]]] = wishlistRepo.findAll

  override def modify(uuid: UUID, wishlist: WishlistOption): F[Either[ApiError, Wishlist]] =
    if (wishlist.isEmpty) {
      wishlistRepo.get(uuid)
    } else {
      wishlistRepo.update(uuid, wishlist)
    }

  override def modifyWish(wishId: Long, wish: WishOption): F[Either[ApiError, Wish]] =
    if (wish.isEmpty) {
      wishRepo.get(wishId)
    } else {
      wishRepo.update(wishId, wish)
    }

  override def modifyAccess(uuid: UUID, access: Access): F[Either[ApiError, Wishlist]] =
    wishlistRepo.update(uuid, access)

}
