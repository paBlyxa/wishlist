package ru.dins.scalaschool.wishlist.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import java.time.LocalDateTime
import java.util.UUID

object Models {

  case class User(id: UUID, username: String, email: Option[String], telegramId: Option[String])
  case class NewUser(username: String, email: Option[String], telegramId: Option[String])
  case class UserUpdate(username: Option[String], email: Option[String], telegramId: Option[String])

  case class Wish(
      id: Long,
      wishlistId: UUID,
      name: String,
      link: Option[String],
      price: Option[BigDecimal],
      comment: Option[String],
      status: WishStatus,
      createdAt: LocalDateTime,
  )

  case class NewWish(
      name: String,
      link: Option[String],
      price: Option[BigDecimal],
      comment: Option[String],
  )

  case class WishlistSaved(
      id: UUID,
      userId: UUID,
      name: String,
      access: Access,
      comment: Option[String],
      createdAt: LocalDateTime,
  )

  case class Wishlist(
      id: UUID,
      userId: UUID,
      name: String,
      access: Access,
      comment: Option[String],
      createdAt: LocalDateTime,
      wishes: List[Wish],
  )

  object Wishlist {
    def apply(wishlistSaved: WishlistSaved, wishes: List[Wish]): Wishlist = {
      import wishlistSaved._
      Wishlist(id, userId, name, access, comment, createdAt, wishes)
    }
  }

  case class NewWishlist(name: String, access: Option[Access], comment: Option[String])
  case class WishlistUpdate(name: Option[String], comment: Option[String]) {
    lazy val isEmpty: Boolean = name.isEmpty && comment.isEmpty
  }
  case class WishUpdate(
      name: Option[String],
      link: Option[String],
      price: Option[BigDecimal],
      comment: Option[String],
  ) {
    lazy val isEmpty: Boolean = name.isEmpty && link.isEmpty && price.isEmpty && comment.isEmpty
  }

  implicit val codecUser: Codec[User]       = deriveCodec
  implicit val codecNewUser: Codec[NewUser] = deriveCodec
  implicit val codecWish: Codec[Wish]       = deriveCodec
  implicit val codecNewWish: Codec[NewWish] = deriveCodec
  implicit val codecWishlistSaved: Codec[WishlistSaved]   = deriveCodec
  implicit val codecWishlist: Codec[Wishlist]             = deriveCodec
  implicit val codecNewWishlist: Codec[NewWishlist]       = deriveCodec
  implicit val codecWishlistUpdate: Codec[WishlistUpdate] = deriveCodec
  implicit val codecWishUpdate: Codec[WishUpdate]         = deriveCodec
}
