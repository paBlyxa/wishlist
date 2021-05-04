package ru.dins.scalaschool.wishlist.models

import doobie.Meta
import doobie.postgres.implicits.pgEnumString
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import ru.dins.scalaschool.wishlist.models.Models.Access.Access

import java.time.LocalDateTime
import java.util.UUID
import scala.util.{Failure, Success, Try}

object Models {

  case class User(id: UUID, username: String, email: Option[String], telegramId: Option[String])
  case class NewUser(username: String, email: Option[String], telegramId: Option[String])
  case class UserOption(username: Option[String], email: Option[String], telegramId: Option[String])

  case class Wish(
      id: Long,
      wishlistId: UUID,
      name: String,
      link: Option[String],
      price: Option[BigDecimal],
      comment: Option[String],
      createdAt: LocalDateTime,
  )

  case class NewWish(
      name: String,
      link: Option[String],
      price: Option[BigDecimal],
      comment: Option[String],
  )

  case class Wishlist(
      id: UUID,
      userId: UUID,
      name: String,
      access: Access,
      comment: Option[String],
      createdAt: LocalDateTime,
  )

  case class NewWishlist(userId: UUID, name: String, access: Option[Access], comment: Option[String])
  case class WishlistOption(name: Option[String], comment: Option[String]) {
    lazy val isEmpty: Boolean = name.isEmpty && comment.isEmpty
  }
  case class WishOption(
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
  implicit val codecWishlist: Codec[Wishlist]             = deriveCodec
  implicit val codecNewWishlist: Codec[NewWishlist]       = deriveCodec
  implicit val codecWishlistOption: Codec[WishlistOption] = deriveCodec
  implicit val codecWishOption: Codec[WishOption]         = deriveCodec

  object Access extends Enumeration with EnumHelper {
    type Access = Value
    val privat: Value = Value("private")
    val public: Value = Value("public")

    implicit val accessMeta: Meta[Access] = pgEnumString("access_type", Access.withName, access => access.toString)
  }

  trait EnumHelper { e: Enumeration =>
    import io.circe._
    import sttp.tapir._
    import sttp.tapir.CodecFormat.TextPlain

    implicit val enumDecoder: Decoder[e.Value] = Decoder.decodeEnumeration(e)
    implicit val enumEncoder: Encoder[e.Value] = Encoder.encodeEnumeration(e)

    implicit def schemaForEnum: Schema[e.Value] =
      Schema.string.validate(Validator.enum(e.values.toList, v => Option(v)))

    implicit val enumCodec: sttp.tapir.Codec[String, e.Value, TextPlain] =
      sttp.tapir.Codec.string.mapDecode(decode)(encode)

    def parse(id: String): Try[e.Value] =
      Success(e.withName(id))

    def decode(s: String): DecodeResult[e.Value] = parse(s) match {
      case Success(v) => DecodeResult.Value(v)
      case Failure(f) => DecodeResult.Error(s, f)
    }
    def encode(id: e.Value): String = id.toString
  }
}
