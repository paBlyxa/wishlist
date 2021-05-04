package ru.dins.scalaschool.wishlist.controller

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import ru.dins.scalaschool.wishlist.models._
import ru.dins.scalaschool.wishlist.service.Service
import sttp.model.{Header, StatusCode}
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.interceptor.ValuedEndpointOutput
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler
import sttp.tapir.server.interceptor.exception.{ExceptionContext, ExceptionHandler}
import sttp.tapir.{headers, statusCode}

case class Controller[F[_]: Concurrent: ContextShift: Timer](service: Service[F]) {

  def myFailureResponse(c: StatusCode, hs: List[Header], m: String): ValuedEndpointOutput[_] =
    ValuedEndpointOutput(statusCode.and(headers).and(jsonBody[BadRequest]), (c, hs, ApiError.badRequest))

  object MyExceptionHandler extends ExceptionHandler {
    override def apply(ctx: ExceptionContext): Option[ValuedEndpointOutput[_]] =
      Some(
        ValuedEndpointOutput(
          statusCode.and(jsonBody[UnexpectedError]),
          (StatusCode.InternalServerError, ApiError.unexpectedError),
        ),
      )
  }

  implicit val customServerOptions: Http4sServerOptions[F, F] = Http4sServerOptions
    .customInterceptors[F, F](
      exceptionHandler = Some(MyExceptionHandler),
      serverLog = Some(Http4sServerOptions.Log.defaultServerLog),
      decodeFailureHandler = DefaultDecodeFailureHandler(
        DefaultDecodeFailureHandler
          .respond(
            _,
            badRequestOnPathErrorIfPathShapeMatches = false,
            badRequestOnPathInvalidIfPathShapeMatches = true,
          ),
        DefaultDecodeFailureHandler.FailureMessages.failureMessage,
        myFailureResponse,
      ),
    )

  import Endpoints._

  private val registerUserRoute = Http4sServerInterpreter.toRoutes(registerUser)(service.registerUser)

  private val loginUserRoute = Http4sServerInterpreter.toRoutes(loginUser)(service.login)

  private val createWishlistRoute = Http4sServerInterpreter.toRoutes(createWishlist)(service.save)

  private val removeWishlistRoute = Http4sServerInterpreter.toRoutes(removeWishlist)(service.remove)

  private val addWishToListRoute =
    Http4sServerInterpreter.toRoutes(addWishToList)((service.addWish _).tupled)

  private val removeWishFromListRoute =
    Http4sServerInterpreter.toRoutes(removeWishFromList)((service.removeWish _).tupled)

  private val clearWishlistRoute = Http4sServerInterpreter.toRoutes(clearWishlist)(service.clear)

  private val getWishlistRoute = Http4sServerInterpreter.toRoutes(getWishlist)(service.get)

  private val getWishlistsRoute = Http4sServerInterpreter.toRoutes(getWishlists)(_ => service.list)

  private val modifyWishlistRoute = Http4sServerInterpreter.toRoutes(modifyWishlist)((service.modify _).tupled)

  private val modifyWishRoute = Http4sServerInterpreter.toRoutes(modifyWish)((service.modifyWish _).tupled)

  private val modifyAccessRoute = Http4sServerInterpreter.toRoutes(modifyAccess)((service.modifyAccess _).tupled)

  def routes: HttpRoutes[F] =
    registerUserRoute <+>
      loginUserRoute <+>
      createWishlistRoute <+>
      removeWishlistRoute <+>
      addWishToListRoute <+>
      removeWishFromListRoute <+>
      clearWishlistRoute <+>
      getWishlistRoute <+>
      getWishlistsRoute <+>
      modifyWishlistRoute <+>
      modifyWishRoute <+>
      modifyAccessRoute

  val docs: OpenAPI = OpenAPIDocsInterpreter.toOpenAPI(
    List(
      registerUser,
      loginUser,
      createWishlist,
      removeWishlist,
      addWishToList,
      removeWishFromList,
      clearWishlist,
      getWishlist,
      getWishlists,
      modifyWishlist,
      modifyWish,
      modifyAccess,
    ),
    "Wishlist API",
    "1.0",
  )
}
