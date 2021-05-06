package ru.dins.scalaschool.wishlist.controller

import ru.dins.scalaschool.wishlist.models.Models.{User, Wish, Wishlist, WishlistSaved}
import ru.dins.scalaschool.wishlist.models.{Access, WishStatus}

import java.time.LocalDateTime
import java.util.UUID

object Examples {

  val exampleUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
  val exampleWish: Wish =
    Wish(1, exampleUUID, "Present", Some("some link"), Some(12.34), Some("comment"), WishStatus.Free, LocalDateTime.now())
  val exampleWishlist: Wishlist =
    Wishlist(
      exampleUUID,
      UUID.fromString("12345678-0000-0000-0000-000000000000"),
      "My wishlist",
      Access.Public,
      Some("For my birthday"),
      LocalDateTime.now(),
      List(exampleWish),
    )
  val exampleModifiedWishlist: Wishlist =
    Wishlist(
      exampleUUID,
      UUID.fromString("12345678-0000-0000-0000-000000000000"),
      "My new wishlist",
      Access.Public,
      Some("New year"),
      LocalDateTime.now(),
      List(exampleWish),
    )
  val exampleWishlistSaved: WishlistSaved =
    WishlistSaved(
      exampleUUID,
      UUID.fromString("12345678-0000-0000-0000-000000000000"),
      "My wishlist",
      Access.Public,
      Some("For my birthday"),
      LocalDateTime.now(),
    )
  val exampleModifiedWish: Wish =
    Wish(1, exampleUUID, "Phone", Some("www.com"), Some(100.00), Some("Samsung"), WishStatus.Free, LocalDateTime.now())

  val exampleUser: User =
    User(exampleUUID, "username", Some("address@mail.com"), Some("@telegramId"))
}
