package ru.dins.scalaschool.wishlist.controller

import ru.dins.scalaschool.wishlist.models._
import ru.dins.scalaschool.wishlist.models.Models.Access.Access
import ru.dins.scalaschool.wishlist.models.Models._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.generic.auto._

import java.util.UUID

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
            jsonBody[NotFound].description("Not found").example(ApiError.notFound(exampleUUID)),
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
      .in("wishlist")

  private val baseWithPathId =
    base
      .in(path[UUID].description("Wishlist's id").example(exampleUUID))

  val createWishlist: Endpoint[NewWishlist, ApiError, Wishlist, Any] =
    base.post
      .description("Create new wishlist")
      .in(
        jsonBody[NewWishlist]
          .description("Wishlist to create")
          .example(NewWishlist(exampleUUID, "My wishlist", Some(Access.public), Some("For my birthday"))),
      )
      .out(jsonBody[Wishlist].description("New wishlist").example(exampleWishlist))

  val removeWishlist: Endpoint[UUID, ApiError, Unit, Any] =
    baseWithPathId.delete.description("Delete wishlist by id").out(jsonBody[Unit].description("Empty json body"))

  val addWishToList: Endpoint[(UUID, NewWish), ApiError, Wish, Any] =
    baseWithPathId.post
      .description("Add wish to list")
      .in("add")
      .in(
        jsonBody[NewWish]
          .description("Wish to add")
          .example(
            NewWish("Present", Some("http:\\\\www.com\\present"), Some(100.0), Some("dream about it")),
          ),
      )
      .out(jsonBody[Wish].description("New wish").example(exampleWish))

  val removeWishFromList: Endpoint[(UUID, Long), ApiError, Unit, Any] =
    baseWithPathId.delete
      .description("Remove wish from list")
      .in(path[Long].description("Wish's id to remove from wishlist").example(1))
      .out(jsonBody[Unit].description("Empty json body"))

  val clearWishlist: Endpoint[UUID, ApiError, Wishlist, Any] =
    baseWithPathId.patch
      .description("Clear wishlist")
      .in("clear")
      .out(jsonBody[Wishlist].description("Empty wishlist").example(exampleWishlist))

  val getWishlist: Endpoint[UUID, ApiError, Wishlist, Any] =
    baseWithPathId.get
      .description("Get wishlist")
      .out(jsonBody[Wishlist].description("Wishlist").example(exampleWishlist))

  //TODO: add filter and order
  val getWishlists: Endpoint[Unit, ApiError, List[Wishlist], Any] =
    base.get
      .description("Get wishlists")
      .in("list")
      .out(
        jsonBody[List[Wishlist]].description("List of wishlists with matching filter").example(List(exampleWishlist)),
      )

  val modifyWishlist: Endpoint[(UUID, WishlistOption), ApiError, Wishlist, Any] =
    baseWithPathId.patch
      .description("Modify wishlist")
      .in(
        jsonBody[WishlistOption]
          .description("Wishlist's fields to modify")
          .example(WishlistOption(Some("My new wishlist"), Some("New year"))),
      )
      .out(jsonBody[Wishlist].description("Wishlist with modified parameters").example(exampleModifiedWishlist))

  val modifyWish: Endpoint[(Long, WishOption), ApiError, Wish, Any] =
    base.patch
      .description("Modify wish")
      .in("wish" / path[Long].description("Wish's id").example(1))
      .in(
        jsonBody[WishOption]
          .description("Wish's fields to modify")
          .example(WishOption(Some("Phone"), Some("www.com"), Some(100.00), Some("Samsung"))),
      )
      .out(jsonBody[Wish].description("Wish with modified parameters").example(exampleModifiedWish))

  val modifyAccess: Endpoint[(UUID, Access), ApiError, Wishlist, Any] =
    baseWithPathId.post
      .description("Change wishlist's access")
      .in(query[Access]("access").description("Access value: private, public"))
      .out(jsonBody[Wishlist].description("Wishlist with modified access").example(exampleWishlist))

}
