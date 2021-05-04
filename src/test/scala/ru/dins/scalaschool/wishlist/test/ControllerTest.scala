package ru.dins.scalaschool.wishlist.test

import cats.effect.{ContextShift, IO, Timer}
import io.circe.Json
import io.circe.literal.JsonStringContext
import org.http4s.circe.{jsonDecoder, jsonEncoder}
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.{Method, Request, Status, Uri}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.dins.scalaschool.wishlist.controller.Controller
import ru.dins.scalaschool.wishlist.models.Models._
import ru.dins.scalaschool.wishlist.models._
import ru.dins.scalaschool.wishlist.service.Service
import ru.dins.scalaschool.wishlist.test.TestExamples._
import scala.concurrent.ExecutionContext

class ControllerTest extends AnyFlatSpec with Matchers with MockFactory {

  implicit val ec: ExecutionContext           = scala.concurrent.ExecutionContext.Implicits.global
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO]               = IO.timer(ec)

  private val mockService = mock[Service[IO]]
  private val controller  = Controller(mockService)

  def run(
      method: Method,
      path: String,
      body: Option[Json] = None,
      query: Option[Map[String, String]] = None,
  ): (Status, Json) = {
    val uri =
      if (query.isDefined) Uri(path = path).withQueryParams(query.get) else Uri(path = path)
    val base = Request[IO](method, uri)
    val request = body match {
      case Some(value) => base.withEntity(value)
      case None        => base
    }
    controller.routes.orNotFound
      .run(request)
      .flatMap { response =>
        response
          .as[Json]
          .map { json =>
            (response.status, json)
          }
          .handleErrorWith(_ => IO.delay(response.status, json""" {} """))
      }
      .unsafeRunSync()
  }

  // Success case in POST /user
  "POST /user" should "return JSON user if service save successful" in {
    (mockService.registerUser _).expects(exampleNewUser).returns(IO.pure(Right(exampleUser)))

    val (status, body) = run(Method.POST, "/user", Some(jsonNewUser))

    status shouldBe Status.Ok
    body shouldBe jsonUser
  }
  // Bad request in POST /user
  it should "return properly filled error model and HTTP code 400 if body can't be parsed" in {

    val (status, body) = run(Method.POST, "/user")

    status shouldBe Status.BadRequest
    body shouldBe jsonBadRequest
  }
  // Username already taken in POST /user
  it should "return properly filled error model and HTTP code 422 if username already taken" in {
    (mockService.registerUser _).expects(exampleNewUser).returns(IO.pure(Left(ApiError.usernameAlreadyTaken("username"))))

    val (status, body) = run(Method.POST, "/user", Some(jsonNewUser))

    status shouldBe Status.UnprocessableEntity
    body shouldBe jsonUsernameTaken
  }
  // Unexpected error in POST /user
  it should "return properly filled error model and HTTP code 500 if unexpected error occurred" in {
    (mockService.registerUser _).expects(exampleNewUser).returns(IO.raiseError(new RuntimeException("Ooops")))

    val (status, body) = run(Method.POST, "/user", Some(jsonNewUser))

    status shouldBe Status.InternalServerError
    body shouldBe jsonUnexpectedError
  }

  // Success case in GET /user/{username}
  "GET /user/{username}" should "return JSON user if storage return something" in {
    (mockService.login _).expects("username").returns(IO.pure(Right(exampleUser)))

    val (status, body) = run(Method.GET, "/user/username")

    status shouldBe Status.Ok
    body shouldBe jsonUser
  }
  // Not found in GET /user/{username}
  it should "return properly filled error model and HTTP code 404 if user not found" in {
    (mockService.login _).expects("username").returns(IO.pure(Left(ApiError.userNotFound("username"))))

    val (status, body) = run(Method.GET, "/user/username")

    status shouldBe Status.NotFound
    body shouldBe json""" { "code": 404, "message": "User with username=username not found" } """
  }

  // Success case in POST /wishlist
  "POST /wishlist" should "return JSON wishlist if service return something" in {
    (mockService.save _).expects(exampleNewWishlist).returns(IO.pure(Right(exampleWishlist)))

    val (status, body) = run(Method.POST, "/wishlist", Some(jsonNewWishlist))

    status shouldBe Status.Ok
    body shouldBe jsonWishlist
  }
  // Bad request in POST /wishlist
  it should "return properly filled error model and HTTP code 400 if body can't be parsed" in {

    val (status, body) = run(Method.POST, "/wishlist")

    status shouldBe Status.BadRequest
    body shouldBe jsonBadRequest
  }
  // User with id not found in POST /wishlist
  it should "return properly filled error model and HTTP code 404 if user not found" in {
    (mockService.save _).expects(exampleNewWishlist).returns(IO.pure(Left(ApiError.userNotFound(exampleUserId))))

    val (status, body) = run(Method.POST, "/wishlist", Some(jsonNewWishlist))

    status shouldBe Status.NotFound
    body shouldBe jsonUserNotFound
  }
  // Unexpected error in POST /wishlist
  it should "return properly filled error model and HTTP code 500 if unexpected error occurred" in {
    (mockService.save _).expects(exampleNewWishlist).returns(IO.raiseError(new RuntimeException("Ooops")))

    val (status, body) = run(Method.POST, "/wishlist", Some(jsonNewWishlist))

    status shouldBe Status.InternalServerError
    body shouldBe jsonUnexpectedError
  }

  // Success case in DELETE /wishlist/{uuid}
  "DELETE /wishlist/{uuid}" should "return empty json if delete successful" in {
    (mockService.remove _).expects(exampleUUID).returns(IO.pure(Right(())))

    val (status, body) = run(Method.DELETE, s"/wishlist/$exampleStrUUID")

    status shouldBe Status.Ok
    body shouldBe jsonEmpty
  }
  // Unexpected error in DELETE /wishlist/{uuid}
  it should "return properly filled error model and HTTP code 500 if unexpected error occurred" in {
    (mockService.remove _).expects(exampleUUID).returns(IO.raiseError(new RuntimeException("Ooops")))

    val (status, body) = run(Method.DELETE, s"/wishlist/$exampleStrUUID")

    status shouldBe Status.InternalServerError
    body shouldBe jsonUnexpectedError
  }

  // Success case in POST /wishlist/{uuid}/add
  "POST /wishlist/{uuid}/add" should "return JSON wishlist if adding wish successful" in {
    (mockService.addWish _).expects(exampleUUID, exampleNewWish).returns(IO.pure(Right(exampleWish)))

    val (status, body) = run(Method.POST, s"/wishlist/$exampleStrUUID/add", Some(jsonNewWish))

    status shouldBe Status.Ok
    body shouldBe jsonWish
  }
  // Bad request in POST /wishlist/{uuid}/add
  it should "return properly filled error model and HTTP code 400 if body can't be parsed" in {

    val (status, body) = run(Method.POST, s"/wishlist/$exampleStrUUID/add")

    status shouldBe Status.BadRequest
    body shouldBe jsonBadRequest
  }
  // User with id not found in POST /wishlist/{uuid}/add
  it should "return properly filled error model and HTTP code 404 if user not found" in {
    (mockService.addWish _)
      .expects(exampleUUID, exampleNewWish)
      .returns(IO.pure(Left(ApiError.userNotFound(exampleUserId))))

    val (status, body) = run(Method.POST, s"/wishlist/$exampleStrUUID/add", Some(jsonNewWish))

    status shouldBe Status.NotFound
    body shouldBe jsonUserNotFound
  }
  // Wishlist with id not found in POST /wishlist/{uuid}/add
  it should "return properly filled error model and HTTP code 404 if wishlist not found" in {
    (mockService.addWish _)
      .expects(exampleUUID, exampleNewWish)
      .returns(IO.pure(Left(ApiError.notFound(exampleUUID))))

    val (status, body) = run(Method.POST, s"/wishlist/$exampleStrUUID/add", Some(jsonNewWish))

    status shouldBe Status.NotFound
    body shouldBe jsonWishlistNotFound
  }
  // Unexpected error in POST /wishlist/{uuid}/add
  it should "return properly filled error model and HTTP code 500 if unexpected error occurred" in {
    (mockService.addWish _)
      .expects(exampleUUID, exampleNewWish)
      .returns(IO.raiseError(new RuntimeException("Ooops")))

    val (status, body) = run(Method.POST, s"/wishlist/$exampleStrUUID/add", Some(jsonNewWish))

    status shouldBe Status.InternalServerError
    body shouldBe jsonUnexpectedError
  }

  // Success case in DELETE /wishlist/{uuid}/{wishId}
  "DELETE /wishlist/{uuid}/{wishId}" should "return empty json if delete successful" in {
    (mockService.removeWish _).expects(exampleUUID, 1).returns(IO.pure(Right(())))

    val (status, body) = run(Method.DELETE, s"/wishlist/$exampleStrUUID/1")

    status shouldBe Status.Ok
    body shouldBe jsonEmpty
  }
  // Wishlist with id not found in DELETE /wishlist/{uuid}/{wishId}
  it should "return properly filled error model and HTTP code 404 if wishlist not found" in {
    (mockService.removeWish _)
      .expects(exampleUUID, 1)
      .returns(IO.pure(Left(ApiError.notFound(exampleUUID))))

    val (status, body) = run(Method.DELETE, s"/wishlist/$exampleStrUUID/1")

    status shouldBe Status.NotFound
    body shouldBe jsonWishlistNotFound
  }
  // Unexpected error in DELETE /wishlist/{uuid}/{wishId}
  it should "return properly filled error model and HTTP code 500 if unexpected error occurred" in {
    (mockService.removeWish _).expects(exampleUUID, 1).returns(IO.raiseError(new RuntimeException("Ooops")))

    val (status, body) = run(Method.DELETE, s"/wishlist/$exampleStrUUID/1")

    status shouldBe Status.InternalServerError
    body shouldBe jsonUnexpectedError
  }

  // Success case in PATCH /wishlist/{uuid}/clear
  "PATCH /wishlist/{uuid}/clear" should "return JSON empty wishlist" in {
    (mockService.clear _).expects(exampleUUID).returns(IO.pure(Right(exampleWishlist)))

    val (status, body) = run(Method.PATCH, s"/wishlist/$exampleStrUUID/clear")

    status shouldBe Status.Ok
    body shouldBe jsonWishlist
  }
  // Wishlist with id not found in PATCH /wishlist/{uuid}/clear
  it should "return properly filled error model and HTTP code 404 if wishlist not found" in {
    (mockService.clear _).expects(exampleUUID).returns(IO.pure(Left(ApiError.notFound(exampleUUID))))

    val (status, body) = run(Method.PATCH, s"/wishlist/$exampleStrUUID/clear")

    status shouldBe Status.NotFound
    body shouldBe jsonWishlistNotFound
  }
  // Unexpected error in PATCH /wishlist/{uuid}/clear
  it should "return properly filled error model and HTTP code 500 if unexpected error occurred" in {
    (mockService.clear _).expects(exampleUUID).returns(IO.raiseError(new RuntimeException("Ooops")))

    val (status, body) = run(Method.PATCH, s"/wishlist/$exampleStrUUID/clear")

    status shouldBe Status.InternalServerError
    body shouldBe jsonUnexpectedError
  }

  // Success case in GET /wishlist/{uuid}
  "GET /wishlist/{uuid}" should "return JSON wishlist if storage return something" in {
    (mockService.get _).expects(exampleUUID).returns(IO.pure(Right(exampleWishlist)))

    val (status, body) = run(Method.GET, s"/wishlist/$exampleStrUUID")

    status shouldBe Status.Ok
    body shouldBe jsonWishlist
  }
  // Wishlist with id not found in GET /wishlist/{uuid}
  it should "return properly filled error model and HTTP code 404 if wishlist not found" in {
    (mockService.get _).expects(exampleUUID).returns(IO.pure(Left(ApiError.notFound(exampleUUID))))

    val (status, body) = run(Method.GET, s"/wishlist/$exampleStrUUID")

    status shouldBe Status.NotFound
    body shouldBe jsonWishlistNotFound
  }
  // Unexpected error in GET /wishlist/{uuid}
  it should "return properly filled error model and HTTP code 500 if unexpected error occurred" in {
    (mockService.get _).expects(exampleUUID).returns(IO.raiseError(new RuntimeException("Ooops")))

    val (status, body) = run(Method.GET, s"/wishlist/$exampleStrUUID")

    status shouldBe Status.InternalServerError
    body shouldBe jsonUnexpectedError
  }

  // Success case in GET /wishlist/list
  "GET /wishlist/list" should "return JSON wishlist's list if storage return something" in {
    (() => mockService.list).expects().returns(IO.pure(Right(List(exampleWishlist))))

    val (status, body) = run(Method.GET, s"/wishlist/list")

    status shouldBe Status.Ok
    body shouldBe jsonListOfWishlist
  }
  // Unexpected error in GET /wishlist/{uuid}
  it should "return properly filled error model and HTTP code 500 if unexpected error occurred" in {
    (() => mockService.list).expects().returns(IO.raiseError(new RuntimeException("Ooops")))

    val (status, body) = run(Method.GET, s"/wishlist/list")

    status shouldBe Status.InternalServerError
    body shouldBe jsonUnexpectedError
  }

  // Success case in PATCH /wishlist/{uuid}
  "PATCH /wishlist/{uuid}" should "return JSON wishlist with modified parameters" in {
    (mockService.modify _).expects(exampleUUID, exampleWishlistOption).returns(IO.pure(Right(exampleWishlist)))

    val (status, body) = run(Method.PATCH, s"/wishlist/$exampleStrUUID", Some(jsonWishlistOption))

    status shouldBe Status.Ok
    body shouldBe jsonWishlist
  }
  // Success case in PATCH /wishlist/{uuid}
  it should "return JSON wishlist with modified name" in {
    (mockService.modify _)
      .expects(exampleUUID, WishlistOption(Some("new name"), None))
      .returns(IO.pure(Right(exampleWishlist)))

    val (status, body) = run(Method.PATCH, s"/wishlist/$exampleStrUUID", Some(json""" { "name": "new name" } """))

    status shouldBe Status.Ok
    body shouldBe jsonWishlist
  }
  // Success case in PATCH /wishlist/{uuid}
  it should "return JSON wishlist with modified comment" in {
    (mockService.modify _)
      .expects(exampleUUID, WishlistOption(None, Some("new comment")))
      .returns(IO.pure(Right(exampleWishlist)))

    val (status, body) = run(Method.PATCH, s"/wishlist/$exampleStrUUID", Some(json""" { "comment": "new comment" } """))

    status shouldBe Status.Ok
    body shouldBe jsonWishlist
  }
  // Bad request in PATCH /wishlist/{uuid}
  it should "return properly filled error model and HTTP code 400 if body can't be parsed" in {

    val (status, body) = run(Method.PATCH, s"/wishlist/$exampleStrUUID")

    status shouldBe Status.BadRequest
    body shouldBe jsonBadRequest
  }
  // Wishlist with id not found in PATCH /wishlist/{uuid}
  it should "return properly filled error model and HTTP code 404 if wishlist not found" in {
    (mockService.modify _)
      .expects(exampleUUID, exampleWishlistOption)
      .returns(IO.pure(Left(ApiError.notFound(exampleUUID))))

    val (status, body) = run(Method.PATCH, s"/wishlist/$exampleStrUUID", Some(jsonWishlistOption))

    status shouldBe Status.NotFound
    body shouldBe jsonWishlistNotFound
  }
  // Unexpected error in PATCH /wishlist/{uuid}
  it should "return properly filled error model and HTTP code 500 if unexpected error occurred" in {
    (mockService.modify _)
      .expects(exampleUUID, exampleWishlistOption)
      .returns(IO.raiseError(new RuntimeException("Ooops")))

    val (status, body) = run(Method.PATCH, s"/wishlist/$exampleStrUUID", Some(jsonWishlistOption))

    status shouldBe Status.InternalServerError
    body shouldBe jsonUnexpectedError
  }

  // Success case in PATCH /wishlist/wish/{wishId}
  "PATCH /wishlist/wish/{wishId}" should "return JSON wish with modified parameters" in {
    (mockService.modifyWish _).expects(1, exampleWishOption).returns(IO.pure(Right(exampleWish)))

    val (status, body) = run(Method.PATCH, s"/wishlist/wish/1", Some(jsonWishOption))

    status shouldBe Status.Ok
    body shouldBe jsonWish
  }
  // Success case in PATCH /wishlist/wish/{wishId}
  it should "return JSON wish with modified comment" in {
    (mockService.modifyWish _)
      .expects(1, WishOption(None, None, None, Some("new comment")))
      .returns(IO.pure(Right(exampleWish)))

    val (status, body) = run(Method.PATCH, s"/wishlist/wish/1", Some(json""" { "comment": "new comment" } """))

    status shouldBe Status.Ok
    body shouldBe jsonWish
  }
  // Bad request in PATCH /wishlist/wish/{wishId}
  it should "return properly filled error model and HTTP code 400 if body can't be parsed" in {

    val (status, body) = run(Method.PATCH, s"/wishlist/wish/1")

    status shouldBe Status.BadRequest
    body shouldBe jsonBadRequest
  }
  // Wish with id not found in PATCH /wishlist/wish/{wishId}
  it should "return properly filled error model and HTTP code 404 if wish not found" in {
    (mockService.modifyWish _).expects(99, exampleWishOption).returns(IO.pure(Left(ApiError.wishNotFound(99))))

    val (status, body) = run(Method.PATCH, s"/wishlist/wish/99", Some(jsonWishOption))

    status shouldBe Status.NotFound
    body shouldBe jsonWishNotFound
  }
  // Unexpected error in PATCH /wishlist/wish/{wishId}
  it should "return properly filled error model and HTTP code 500 if unexpected error occurred" in {
    (mockService.modifyWish _).expects(99, exampleWishOption).returns(IO.raiseError(new RuntimeException("Ooops")))

    val (status, body) = run(Method.PATCH, s"/wishlist/wish/99", Some(jsonWishOption))

    status shouldBe Status.InternalServerError
    body shouldBe jsonUnexpectedError
  }

  // Success case in POST /wishlist/{uuid}?access=<Access>
  "POST /wishlist/{uuid}?access=<Access>" should "return wishlist with modified access" in {
    (mockService.modifyAccess _).expects(exampleUUID, Access.public).returns(IO.pure(Right(exampleWishlist)))

    val (status, body) = run(Method.POST, s"/wishlist/$exampleStrUUID", None, Some(Map("access" -> "public")))

    status shouldBe Status.Ok
    body shouldBe jsonWishlist
  }
  // Bad request in PATCH /wishlist/{uuid}?access=<Access>
  it should "return properly filled error model and HTTP code 400 if body can't be parsed" in {

    val (status, body) = run(Method.POST, s"/wishlist/$exampleStrUUID", None, Some(Map("access" -> "publ")))

    status shouldBe Status.BadRequest
    body shouldBe jsonBadRequest
  }
  // Wishlist with id not found in PATCH /wishlist/{uuid}?access=<Access>
  it should "return properly filled error model and HTTP code 404 if wishlist not found" in {
    (mockService.modifyAccess _)
      .expects(exampleUUID, Access.public)
      .returns(IO.pure(Left(ApiError.notFound(exampleUUID))))

    val (status, body) = run(Method.POST, s"/wishlist/$exampleStrUUID", None, Some(Map("access" -> "public")))

    status shouldBe Status.NotFound
    body shouldBe jsonWishlistNotFound
  }
  // Unexpected error in PATCH /wishlist/{uuid}?access=<Access>
  it should "return properly filled error model and HTTP code 500 if unexpected error occurred" in {
    (mockService.modifyAccess _)
      .expects(exampleUUID, Access.public)
      .returns(IO.raiseError(new RuntimeException("Ooops")))

    val (status, body) = run(Method.POST, s"/wishlist/$exampleStrUUID", None, Some(Map("access" -> "public")))

    status shouldBe Status.InternalServerError
    body shouldBe jsonUnexpectedError
  }
}
