package ru.dins.scalaschool.wishlist.models

import enumeratum._
import enumeratum.EnumEntry.Lowercase
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum

sealed trait Access extends EnumEntry with Lowercase with TapirCodecEnumeratum

case object Access extends Enum[Access] with CirceEnum[Access] with DoobieEnum[Access] {

  case object Private extends Access
  case object Public  extends Access

  val values: IndexedSeq[Access] = findValues
}
