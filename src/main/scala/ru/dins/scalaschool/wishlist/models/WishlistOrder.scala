package ru.dins.scalaschool.wishlist.models

import enumeratum.EnumEntry.Lowercase
import enumeratum._
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum

sealed trait WishlistOrder extends EnumEntry with Lowercase with TapirCodecEnumeratum

case object WishlistOrder extends Enum[WishlistOrder] with CirceEnum[WishlistOrder] {

  case object Username  extends WishlistOrder
  case object Name      extends WishlistOrder
  case object CreatedAt extends WishlistOrder

  val values: IndexedSeq[WishlistOrder] = findValues
}
