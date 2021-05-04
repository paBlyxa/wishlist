package ru.dins.scalaschool.wishlist.service

import ru.dins.scalaschool.wishlist.models.Models.Access.Access
import ru.dins.scalaschool.wishlist.models.Models._
import ru.dins.scalaschool.wishlist.models._

import java.util.UUID

trait Service[F[_]] {

  def registerUser(user: NewUser): F[Either[ApiError, User]]

  def login(username: String): F[Either[ApiError, User]]

  def save(wishlist: NewWishlist): F[Either[ApiError, Wishlist]]

  def remove(uuid: UUID): F[Either[ApiError, Unit]]

  def addWish(uuid: UUID, wish: NewWish): F[Either[ApiError, Wish]]

  def removeWish(uuid: UUID, wishId: Long): F[Either[ApiError, Unit]]

  def clear(uuid: UUID): F[Either[ApiError, Wishlist]]

  def get(uuid: UUID): F[Either[ApiError, Wishlist]]

  def list: F[Either[ApiError, List[Wishlist]]]

  def modify(uuid: UUID, wishlist: WishlistOption): F[Either[ApiError, Wishlist]]

  def modifyWish(wishId: Long, wish: WishOption): F[Either[ApiError, Wish]]

  def modifyAccess(uuid: UUID, access: Access): F[Either[ApiError, Wishlist]]
}
