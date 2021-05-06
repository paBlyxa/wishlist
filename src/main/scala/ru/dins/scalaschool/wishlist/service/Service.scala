package ru.dins.scalaschool.wishlist.service

import ru.dins.scalaschool.wishlist.models.Models._
import ru.dins.scalaschool.wishlist.models._

import java.util.UUID

trait Service[F[_]] {

  def registerUser(user: NewUser): F[Either[ApiError, User]]

  def login(username: String): F[Either[ApiError, User]]

  def save(userId: UUID, wishlist: NewWishlist): F[Either[ApiError, Wishlist]]

  def remove(userId: UUID, wishlistId: UUID): F[Either[ApiError, Unit]]

  def addWish(userId: UUID, wishlistId: UUID, wish: NewWish): F[Either[ApiError, Wish]]

  def removeWish(userId: UUID, wishlistId: UUID, wishId: Long): F[Either[ApiError, Unit]]

  def clear(userId: UUID, wishlistId: UUID): F[Either[ApiError, Wishlist]]

  def get(userId: UUID, wishlistId: UUID): F[Either[ApiError, Wishlist]]

  def list: F[Either[ApiError, List[WishlistSaved]]]

  def modify(userId: UUID, wishlistId: UUID, wishlist: WishlistUpdate): F[Either[ApiError, Wishlist]]

  def modifyWish(userId: UUID, wishId: Long, wish: WishUpdate): F[Either[ApiError, Wish]]

  def modifyAccess(userId: UUID, wishlistId: UUID, access: Access): F[Either[ApiError, Wishlist]]

  def getWishes(userId: UUID, wishlistId: UUID): F[Either[ApiError, List[Wish]]]
}
