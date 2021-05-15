package ru.dins.scalaschool.wishlist.service

import ru.dins.scalaschool.wishlist.models.Models._
import ru.dins.scalaschool.wishlist.models._

trait Service[F[_]] {

  def registerUser(user: NewUser): F[Either[ApiError, User]]

  def login(username: String): F[Either[ApiError, User]]

  def save(userId: UserId, wishlist: NewWishlist): F[Either[ApiError, Wishlist]]

  def remove(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Unit]]

  def addWish(userId: UserId, wishlistId: WishlistId, wish: NewWish): F[Either[ApiError, Wish]]

  def removeWish(userId: UserId, wishlistId: WishlistId, wishId: Long): F[Either[ApiError, Unit]]

  def clear(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Wishlist]]

  def get(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Wishlist]]

  def list(
      userId: UserId,
      filter: FilterList,
  ): F[Either[ApiError, List[WishlistSaved]]]

  def modify(userId: UserId, wishlistId: WishlistId, wishlist: WishlistUpdate): F[Either[ApiError, Wishlist]]

  def modifyWish(userId: UserId, wishId: Long, wish: WishUpdate): F[Either[ApiError, Wish]]

  def modifyAccess(userId: UserId, wishlistId: WishlistId, access: Access): F[Either[ApiError, Wishlist]]

  def getWishes(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, List[Wish]]]

  def updateWishStatus(
      userId: UserId,
      wishlistId: WishlistId,
      wishId: Long,
      wishStatus: WishStatus,
  ): F[Either[ApiError, Wish]]

  def provideAccess(userOwnerId: UserId, wishlistId: WishlistId, userId: UserId): F[Either[ApiError, Unit]]

  def forbidAccess(userOwnerId: UserId, wishlistId: WishlistId, userId: UserId): F[Either[ApiError, Unit]]

  def addUserToShareWish(userId: UserId, wishlistId: WishlistId, wishId: Long): F[Either[ApiError, Unit]]

  def removeUserToShareWish(userId: UserId, wishlistId: WishlistId, wishId: Long): F[Either[ApiError, Unit]]
}
