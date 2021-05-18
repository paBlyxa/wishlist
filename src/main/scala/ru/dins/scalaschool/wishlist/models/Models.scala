package ru.dins.scalaschool.wishlist.models

import io.circe.Codec
import io.circe.generic.semiauto._

import java.time.LocalDateTime

object Models {

  case class User(id: UserId, username: String, email: Option[String], telegramId: Option[String])
  case class NewUser(username: String, email: Option[String], telegramId: Option[String])
  case class UserUpdate(username: Option[String], email: Option[String], telegramId: Option[String])

  case class Wish(
      id: Long,
      wishlistId: WishlistId,
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
      id: WishlistId,
      userId: UserId,
      name: String,
      access: Access,
      comment: Option[String],
      createdAt: LocalDateTime,
  )

  case class Wishlist(
      id: WishlistId,
      userId: UserId,
      name: String,
      access: Access,
      comment: Option[String],
      createdAt: LocalDateTime,
      wishes: List[Wish],
  )

  case class WishlistWeb(
      id: WishlistId,
      username: String,
      name: String,
      access: Access,
      comment: Option[String],
  )

  object Wishlist {
    def apply(wishlistSaved: WishlistSaved, wishes: List[Wish]): Wishlist = {
      import wishlistSaved._
      Wishlist(id, userId, name, access, comment, createdAt, wishes)
    }
  }

  case class NewWishlist(name: String, access: Option[Access] = Some(Access.Public), comment: Option[String])
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

  implicit val codecUser: Codec[User]                     = deriveCodec
  implicit val codecNewUser: Codec[NewUser]               = deriveCodec
  implicit val codecWish: Codec[Wish]                     = deriveCodec
  implicit val codecNewWish: Codec[NewWish]               = deriveCodec
  implicit val codecWishlistSaved: Codec[WishlistSaved]   = deriveCodec
  implicit val codecWishlist: Codec[Wishlist]             = deriveCodec
  implicit val codecNewWishlist: Codec[NewWishlist]       = deriveCodec
  implicit val codecWishlistUpdate: Codec[WishlistUpdate] = deriveCodec
  implicit val codecWishUpdate: Codec[WishUpdate]         = deriveCodec
  implicit val codecWishlistWeb: Codec[WishlistWeb]       = deriveCodec

}
