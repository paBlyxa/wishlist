package ru.dins.scalaschool.wishlist

import doobie.Meta
import io.circe.{Decoder, Encoder, Json}

import java.util.UUID
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops.toCoercibleIdOps

import scala.util.Try
import scala.language.implicitConversions
import doobie.postgres.implicits._

package object models {
  @newtype case class UserId(uuid: UUID)
  object UserId {

    def apply(id: String): UserId = UserId(UUID.fromString(id))

    implicit val metaUserId: Meta[UserId]      = implicitly[Meta[UUID]].coerce
    implicit val encodeUserId: Encoder[UserId] = (a: UserId) => Json.fromString(a.toString)
    implicit val decodeUserId: Decoder[UserId] = Decoder.decodeString.emapTry { str =>
      Try(UserId(str))
    }
  }

  @newtype case class WishlistId(uuid: UUID)
  object WishlistId {

    def apply(id: String): WishlistId = WishlistId(UUID.fromString(id))

    implicit val metaWishlistId: Meta[WishlistId]      = implicitly[Meta[UUID]].coerce
    implicit val encodeWishlistId: Encoder[WishlistId] = (a: WishlistId) => Json.fromString(a.toString)
    implicit val decodeWishlistId: Decoder[WishlistId] = Decoder.decodeString.emapTry { str =>
      Try(WishlistId(str))
    }
  }
}
