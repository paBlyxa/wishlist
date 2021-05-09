package ru.dins.scalaschool.wishlist.controller

import ru.dins.scalaschool.wishlist.models.Models._
import ru.dins.scalaschool.wishlist.models.{Access, UserId, WishStatus, WishlistId}

import java.time.LocalDateTime

object Examples {

  val exampleWishlistId: WishlistId = WishlistId("00000000-0000-0000-0000-000000000000")
  val exampleUserID: UserId         = UserId("12345678-0000-0000-0000-000000000000")
  val exampleWish: Wish =
    Wish(
      1,
      exampleWishlistId,
      "Present",
      Some("some link"),
      Some(12.34),
      Some("comment"),
      WishStatus.Free,
      LocalDateTime.now(),
    )
  val exampleWishlist: Wishlist =
    Wishlist(
      exampleWishlistId,
      exampleUserID,
      "My wishlist",
      Access.Public,
      Some("For my birthday"),
      LocalDateTime.now(),
      List(exampleWish),
    )
  val exampleModifiedWishlist: Wishlist =
    Wishlist(
      exampleWishlistId,
      exampleUserID,
      "My new wishlist",
      Access.Public,
      Some("New year"),
      LocalDateTime.now(),
      List(exampleWish),
    )
  val exampleWishlistSaved: WishlistSaved =
    WishlistSaved(
      exampleWishlistId,
      exampleUserID,
      "My wishlist",
      Access.Public,
      Some("For my birthday"),
      LocalDateTime.now(),
    )
  val exampleModifiedWish: Wish =
    Wish(
      1,
      exampleWishlistId,
      "Phone",
      Some("www.com"),
      Some(100.00),
      Some("Samsung"),
      WishStatus.Free,
      LocalDateTime.now(),
    )

  val exampleUser: User =
    User(exampleUserID, "username", Some("address@mail.com"), Some("@telegramId"))
}
