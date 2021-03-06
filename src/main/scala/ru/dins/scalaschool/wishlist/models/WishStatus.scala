package ru.dins.scalaschool.wishlist.models

import enumeratum.EnumEntry.Lowercase
import enumeratum._
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum

sealed trait WishStatus extends EnumEntry with Lowercase with TapirCodecEnumeratum

case object WishStatus extends Enum[WishStatus] with CirceEnum[WishStatus] with DoobieEnum[WishStatus] {

  case object Free   extends WishStatus
  case object Shared extends WishStatus
  case object Booked extends WishStatus
  case object Got    extends WishStatus

  val values: IndexedSeq[WishStatus] = findValues
}
