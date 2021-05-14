package ru.dins.scalaschool.wishlist.models

import enumeratum._
import enumeratum.EnumEntry.Lowercase
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum

sealed trait OrderDir extends EnumEntry with Lowercase with TapirCodecEnumeratum

case object OrderDir extends Enum[OrderDir] with CirceEnum[OrderDir] {

  case object Asc  extends OrderDir
  case object Desc extends OrderDir

  val values: IndexedSeq[OrderDir] = findValues
}
