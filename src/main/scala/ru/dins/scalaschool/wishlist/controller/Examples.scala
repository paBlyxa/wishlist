package ru.dins.scalaschool.wishlist.controller

import ru.dins.scalaschool.wishlist.models.Models.{Access, User, Wish, Wishlist}

import java.time.LocalDateTime
import java.util.UUID

object Examples {

  val exampleUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
  val exampleWish: Wish =
    Wish(1, exampleUUID, "Present", Some("some link"), Some(12.34), Some("comment"), LocalDateTime.now())
  val exampleWishlist: Wishlist =
    Wishlist(
      exampleUUID,
      UUID.fromString("12345678-0000-0000-0000-000000000000"),
      "My wishlist",
      Access.public,
      Some("For my birthday"),
      LocalDateTime.now(),
    )
  val exampleModifiedWishlist: Wishlist =
    Wishlist(
      exampleUUID,
      UUID.fromString("12345678-0000-0000-0000-000000000000"),
      "My new wishlist",
      Access.public,
      Some("New year"),
      LocalDateTime.now(),
    )
  val exampleModifiedWish: Wish =
    Wish(1, exampleUUID, "Phone", Some("www.com"), Some(100.00), Some("Samsung"), LocalDateTime.now())

  val exampleUser: User =
    User(exampleUUID, "username", Some("address@mail.com"), Some("@telegramId"))
}
