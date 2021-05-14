package ru.dins.scalaschool.wishlist.controller

import ru.dins.scalaschool.wishlist.models._
import ru.dins.scalaschool.wishlist.models.Models._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.generic.auto._
import sttp.tapir.codec.newtype._

object Endpoints {

  import Examples._

  private val errorOut =
    endpoint
      .errorOut(
        oneOf[ApiError](
          oneOfMapping(
            StatusCode.BadRequest,
            jsonBody[BadRequest].description("Bad request").example(ApiError.badRequest),
          ),
          oneOfMapping(
            StatusCode.NotFound,
            jsonBody[NotFound].description("Not found").example(ApiError.notFound(exampleWishlistId)),
          ),
          oneOfMapping(
            StatusCode.UnprocessableEntity,
            jsonBody[LogicError].description("Logic error").example(ApiError.usernameAlreadyTaken("username")),
          ),
          oneOfMapping(
            StatusCode.InternalServerError,
            jsonBody[UnexpectedError].description("Unexpected error").example(ApiError.unexpectedError),
          ),
        ),
      )

  val registerUser: Endpoint[NewUser, ApiError, User, Any] =
    errorOut.post
      .in("user")
      .in(
        jsonBody[NewUser]
          .description("User to create")
          .example(NewUser("username", Some("address@mail.com"), Some("@telegramId"))),
      )
      .out(jsonBody[User].description("New user").example(exampleUser))

  val loginUser: Endpoint[String, ApiError, User, Any] =
    errorOut.get
      .in("user" / path[String].description("User's username").example("nickname"))
      .out(jsonBody[User].description("New user").example(exampleUser))

  private val base =
    errorOut
      .in(path[UserId].description("User's id").example(exampleUserID))
      .in("wishlist")

  private val baseWithPathId =
    base
      .in(path[WishlistId].description("Wishlist's id").example(exampleWishlistId))

  val createWishlist: Endpoint[(UserId, NewWishlist), ApiError, Wishlist, Any] =
    base.post
      .description("Create new wishlist")
      .in(
        jsonBody[NewWishlist]
          .description("Wishlist to create")
          .example(NewWishlist("My wishlist", Some(Access.Public), Some("For my birthday"))),
      )
      .out(jsonBody[Wishlist].description("New wishlist").example(exampleWishlist))

  val removeWishlist: Endpoint[(UserId, WishlistId), ApiError, Unit, Any] =
    baseWithPathId.delete.description("Delete wishlist by id").out(jsonBody[Unit].description("Empty json body"))

  val addWishToList: Endpoint[(UserId, WishlistId, NewWish), ApiError, Wish, Any] =
    baseWithPathId.post
      .description("Add wish to list")
      .in("wish")
      .in(
        jsonBody[NewWish]
          .description("Wish to add")
          .example(
            NewWish("Present", Some("http:\\\\www.com\\present"), Some(100.0), Some("dream about it")),
          ),
      )
      .out(jsonBody[Wish].description("New wish").example(exampleWish))

  val removeWishFromList: Endpoint[(UserId, WishlistId, Long), ApiError, Unit, Any] =
    baseWithPathId.delete
      .description("Remove wish from list")
      .in(path[Long].description("Wish's id to remove from wishlist").example(1))
      .out(jsonBody[Unit].description("Empty json body"))

  val clearWishlist: Endpoint[(UserId, WishlistId), ApiError, Wishlist, Any] =
    baseWithPathId.delete
      .description("Clear wishlist")
      .in("wishes")
      .out(jsonBody[Wishlist].description("Empty wishlist").example(exampleWishlist))

  val getWishlist: Endpoint[(UserId, WishlistId), ApiError, Wishlist, Any] =
    baseWithPathId.get
      .description("Get wishlist")
      .out(jsonBody[Wishlist].description("Wishlist").example(exampleWishlist))

  val getWishlists: Endpoint[(UserId, FilterList), ApiError, List[WishlistSaved], Any] =
    base.get
      .description("Get wishlists")
      .in("list")
      .in(
        query[Option[String]]("username")
          .description("filter by username")
          .and(query[Option[String]]("name").description("filter by wishlist's name"))
          .and(query[Option[WishlistOrder]]("orderBy").description("order by, default by createdAt"))
          .and(query[Option[OrderDir]]("orderDir").description("order dir, default ASC"))
          .mapTo(FilterList),
      )
      .out(
        jsonBody[List[WishlistSaved]]
          .description("List of wishlists with matching filter")
          .example(List(exampleWishlistSaved)),
      )

  val modifyWishlist: Endpoint[(UserId, WishlistId, WishlistUpdate), ApiError, Wishlist, Any] =
    baseWithPathId.patch
      .description("Modify wishlist")
      .in(
        jsonBody[WishlistUpdate]
          .description("Wishlist's fields to modify")
          .example(WishlistUpdate(Some("My new wishlist"), Some("New year"))),
      )
      .out(jsonBody[Wishlist].description("Wishlist with modified parameters").example(exampleModifiedWishlist))

  val modifyWish: Endpoint[(UserId, Long, WishUpdate), ApiError, Wish, Any] =
    base.patch
      .description("Modify wish")
      .in("wish" / path[Long].description("Wish's id").example(1))
      .in(
        jsonBody[WishUpdate]
          .description("Wish's fields to modify")
          .example(WishUpdate(Some("Phone"), Some("www.com"), Some(100.00), Some("Samsung"))),
      )
      .out(jsonBody[Wish].description("Wish with modified parameters").example(exampleModifiedWish))

  val modifyAccess: Endpoint[(UserId, WishlistId, Access), ApiError, Wishlist, Any] =
    baseWithPathId.post
      .description("Change wishlist's access")
      .in(query[Access]("access").description("Access value: private, public"))
      .out(jsonBody[Wishlist].description("Wishlist with modified access").example(exampleWishlist))

  val getWishes: Endpoint[(UserId, WishlistId), ApiError, List[Wish], Any] =
    baseWithPathId.get
      .description("Get all wishes in wishlist")
      .in("wishes")
      .out(jsonBody[List[Wish]].description("List of wishes").example(List(exampleWish)))

  val updateWishStatus: Endpoint[(UserId, WishlistId, Long, WishStatus), ApiError, Wish, Any] =
    baseWithPathId.patch
      .description("Update wish's status")
      .in("wish" / path[Long].description("Wish's id").example(1))
      .in(query[WishStatus]("status").description("New wish's status").example(WishStatus.Booked))
      .out(jsonBody[Wish].description("Wish with updated status").example(exampleWish))

  val provideAccess: Endpoint[(UserId, WishlistId, UserId), ApiError, Unit, Any] =
    baseWithPathId.put
      .description("Provide access to wishlist for user")
      .in("access")
      .in(query[UserId]("userId").description("User's id to provide access"))
      .out(jsonBody[Unit].description("Empty json body"))

  val forbidAccess: Endpoint[(UserId, WishlistId, UserId), ApiError, Unit, Any] =
    baseWithPathId.delete
      .description("Forbid access to wishlist for user")
      .in("access")
      .in(query[UserId]("userId").description("User's id to forbid access"))
      .out(jsonBody[Unit].description("Empty json body"))
}
