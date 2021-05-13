package ru.dins.scalaschool.wishlist.models

import io.circe.{Codec, Encoder}
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.EncoderOps

sealed trait ApiError
object ApiError {

  def unexpectedError: UnexpectedError                   = UnexpectedError(500, "Unexpected error")
  def notFound(id: WishlistId): NotFound                 = NotFound(404, s"Wishlist with id=$id not found")
  def userNotFound(id: UserId): NotFound                 = NotFound(404, s"User with id=$id not found")
  def userNotFound(username: String): NotFound           = NotFound(404, s"User with username=$username not found")
  def wishNotFound(id: Long): NotFound                   = NotFound(404, s"Wish with id=$id not found")
  def notFound: ApiError                                 = NotFound(404, s"Not found")
  def badRequest: BadRequest                             = BadRequest(400, "Malformed message body: Invalid JSON")
  def usernameAlreadyTaken(username: String): LogicError = LogicError(422, s"Username '$username' already taken")
  def forbidden: Forbidden                               = Forbidden(403, s"Forbidden")

  implicit val codecNotFound: Codec[NotFound]               = deriveCodec
  implicit val codecLogicError: Codec[LogicError]           = deriveCodec
  implicit val codecBadRequest: Codec[BadRequest]           = deriveCodec
  implicit val codecUnexpectedError: Codec[UnexpectedError] = deriveCodec
  implicit val codecForbidden: Codec[Forbidden]             = deriveCodec
  implicit val encodeApiError: Encoder[ApiError] = Encoder.instance {
    case err @ UnexpectedError(_, _) => err.asJson
    case err @ NotFound(_, _)        => err.asJson
    case err @ LogicError(_, _)      => err.asJson
    case err @ BadRequest(_, _)      => err.asJson
    case err @ Forbidden(_, _)       => err.asJson
  }
}

case class UnexpectedError(code: Int, message: String) extends ApiError
case class NotFound(code: Int, message: String)        extends ApiError
case class LogicError(code: Int, message: String)      extends ApiError
case class BadRequest(code: Int, message: String)      extends ApiError
case class Forbidden(code: Int, message: String)       extends ApiError
