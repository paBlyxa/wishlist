package ru.dins.scalaschool.wishlist.test

import io.circe.literal.JsonStringContext
import ru.dins.scalaschool.wishlist.models.Models._
import ru.dins.scalaschool.wishlist.models.{Access, FilterList, UserId, WishStatus, WishlistId}

import java.time.LocalDateTime

object TestExamples {
  val exampleStrUUID                = "00000000-0000-0000-0000-000000000000"
  val exampleStrUserId              = "11111111-2222-3333-4444-000000000000"
  val exampleWishlistId: WishlistId = WishlistId(exampleStrUUID)
  val exampleUserId: UserId         = UserId(exampleStrUserId)
  val exampleNewUser: NewUser       = NewUser("username", Some("email"), Some("@username"))
  val exampleUser: User             = User(exampleUserId, "username", Some("email"), Some("@username"))
  val exampleLDT: LocalDateTime     = LocalDateTime.MIN
  val exampleNewWishlist: NewWishlist =
    NewWishlist("My wishlist", Some(Access.Public), Some("For my birthday"))

  val exampleNewWish: NewWish = NewWish("present", Some("some link"), Some(12.34), Some("comment"))
  val exampleWish: Wish =
    Wish(1, exampleWishlistId, "present", Some("some link"), Some(12.34), Some("comment"), WishStatus.Free, exampleLDT)
  val exampleWishlist: Wishlist =
    Wishlist(
      exampleWishlistId,
      exampleUserId,
      "My wishlist",
      Access.Public,
      Some("For my birthday"),
      exampleLDT,
      List(exampleWish),
    )
  val exampleWishlistEmpty: Wishlist =
    Wishlist(
      exampleWishlistId,
      exampleUserId,
      "My wishlist",
      Access.Public,
      Some("For my birthday"),
      exampleLDT,
      List(),
    )
  val exampleWishlistSaved: WishlistSaved =
    WishlistSaved(
      exampleWishlistId,
      exampleUserId,
      "My wishlist",
      Access.Public,
      Some("For my birthday"),
      exampleLDT,
    )
  val exampleWishlistWeb: WishlistWeb =
    WishlistWeb(
      exampleWishlistId,
      "username",
      "My wishlist",
      Access.Public,
      Some("For my birthday"),
    )
  val exampleWishlistOption: WishlistUpdate = WishlistUpdate(Some("new name"), Some("new comment"))
  val exampleWishOption: WishUpdate =
    WishUpdate(Some("new present"), Some("new link"), Some(1000.00), Some("modified comment"))

  val jsonNewUser =
    json""" { "username": "username", "email": "email", "telegramId": "@username" } """
  val jsonUser =
    json""" { "id": $exampleUserId, "username": "username", "email": "email", "telegramId": "@username" } """
  val jsonNewWishlist =
    json""" { "name": "My wishlist", "access": "public", "comment": "For my birthday" } """
  val jsonNewWish        = json""" { "name": "present", "link": "some link", "price": "12.34", "comment": "comment"} """
  val jsonWish           = json""" { 
          "id": 1, 
          "wishlistId": $exampleWishlistId,
          "name": "present", 
          "link": "some link", 
          "price": 12.34,
          "comment": "comment",
          "status": "free",
          "createdAt": $exampleLDT
      }"""
  val jsonWishes         = json""" [$jsonWish] """
  val jsonWishlist       = json""" { 
          "id": $exampleWishlistId, 
          "userId": $exampleUserId, 
          "name": "My wishlist", 
          "access": "public", 
          "comment": "For my birthday", 
          "createdAt": $exampleLDT,
          "wishes" : [$jsonWish]
          } """
  val jsonWishlistWeb    = json""" { 
          "id": $exampleWishlistId, 
          "username": "username", 
          "name": "My wishlist", 
          "access": "public", 
          "comment": "For my birthday"
          } """
  val jsonListOfWishlist = json""" [$jsonWishlistWeb]"""
  val jsonWishlistOption = json"""{ "name": "new name", "comment": "new comment" }"""
  val jsonWishOption =
    json""" { "name": "new present", "link": "new link", "price": 1000.00, "comment": "modified comment" } """

  val jsonUsernameTaken       = json""" { "code": 422, "message": "Username 'username' already taken" } """
  val jsonBadRequest          = json""" { "code": 400, "message": "Malformed message body: Invalid JSON" }"""
  val messageWishlistNotFound = s"Wishlist with id=$exampleStrUUID not found"
  val jsonWishlistNotFound    = json""" { "code": 404, "message": $messageWishlistNotFound } """
  val messageUserNotFound     = s"User with id=$exampleStrUserId not found"
  val jsonUserNotFound        = json""" { "code": 404, "message": $messageUserNotFound } """
  val messageWishNotFound     = s"Wish with id=99 not found"
  val jsonWishNotFound        = json""" { "code": 404, "message": $messageWishNotFound } """
  val jsonUnexpectedError     = json""" { "code": 500, "message": "Unexpected error" }"""
  val jsonEmpty               = json""" {} """

  val filterEmpty: FilterList = FilterList(None, None, None, None)
}
