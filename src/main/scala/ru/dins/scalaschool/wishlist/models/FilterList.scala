package ru.dins.scalaschool.wishlist.models

case class FilterList(
    username: Option[String],
    name: Option[String],
    orderBy: Option[WishlistOrder],
    orderDir: Option[OrderDir],
)
