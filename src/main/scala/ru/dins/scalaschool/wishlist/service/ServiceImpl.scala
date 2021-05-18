package ru.dins.scalaschool.wishlist.service

import cats.effect.Sync
import cats.implicits.catsSyntaxEitherId
import ru.dins.scalaschool.wishlist.models._
import ru.dins.scalaschool.wishlist.models.Models._
import cats.syntax.functor._
import cats.syntax.flatMap._
import ru.dins.scalaschool.wishlist.db.{UserRepo, WishRepo, WishlistRepo}

import scala.language.implicitConversions

case class ServiceImpl[F[_]: Sync](
    userRepo: UserRepo[F],
    wishlistRepo: WishlistRepo[F],
    wishRepo: WishRepo[F],
) extends Service[F] {

  private val F: Sync[F] = Sync[F]

  override def registerUser(user: NewUser): F[Either[ApiError, User]] = userRepo.save(user)

  override def login(username: String): F[Either[ApiError, User]] = userRepo.getByUsername(username)

  override def save(userId: UserId, wishlist: NewWishlist): F[Either[ApiError, Wishlist]] =
    wishlistRepo.save(userId, wishlist)

  override def remove(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Unit]] =
    wishlistRepo.remove(wishlistId).withWriteAccess(userId, wishlistId)

  override def addWish(userId: UserId, wishlistId: WishlistId, wish: NewWish): F[Either[ApiError, Wish]] =
    wishRepo.save(wishlistId, wish).withWriteAccess(userId, wishlistId)

  override def getWish(userId: UserId, wishlistId: WishlistId, wishId: Long): F[Either[ApiError, Wish]] =
    wishRepo.get(wishId).withReadAccess(userId, wishlistId)

  override def removeWish(userId: UserId, wishlistId: WishlistId, wishId: Long): F[Either[ApiError, Unit]] =
    wishRepo.remove(wishId).withWriteAccess(userId, wishlistId)

  override def clear(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Wishlist]] =
    wishlistRepo
      .clear(wishlistId)
      .withWriteAccess(userId, wishlistId)
      .flatMap {
        case Left(err) => F.pure(Left(err))
        case _         => wishlistRepo.get(wishlistId).map(_.map(Wishlist(_, List.empty)))
      }

  override def get(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, Wishlist]] =
    wishlistRepo.get(wishlistId).flatMap {
      case Left(err)    => F.pure(err.asLeft)
      case Right(value) => isWishlistShared(userId, value)
    }

  override def list(
      userId: UserId,
      filter: FilterList,
  ): F[Either[ApiError, List[WishlistWeb]]] = wishlistRepo.findAll(userId, filter)

  override def modify(userId: UserId, wishlistId: WishlistId, wishlist: WishlistUpdate): F[Either[ApiError, Wishlist]] =
    if (wishlist.isEmpty) {
      get(userId, wishlistId)
    } else
      wishlistRepo.update(wishlistId, wishlist).withWriteAccess(userId, wishlistId)

  override def modifyWish(userId: UserId, wishId: Long, wish: WishUpdate): F[Either[ApiError, Wish]] =
    (if (wish.isEmpty) {
       wishRepo.get(wishId)
     } else
       wishRepo
         .update(wishId, wish))
      .withModifyAccess(userId, wishId)

  override def modifyAccess(userId: UserId, wishlistId: WishlistId, access: Access): F[Either[ApiError, Wishlist]] =
    wishlistRepo.updateAccess(wishlistId, access).withWriteAccess(userId, wishlistId)

  override def getWishes(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, List[Wish]]] =
    wishRepo.getAllByWishlistId(wishlistId).withReadAccess(userId, wishlistId)

  override def updateWishStatus(
      userId: UserId,
      wishlistId: WishlistId,
      wishId: Long,
      wishStatus: WishStatus,
  ): F[Either[ApiError, Wish]] =
    wishRepo
      .get(wishId)
      .withReadAccess(userId, wishlistId)
      .flatMap {
        case e @ Left(_) => F.pure(e)
        case Right(wish) =>
          wish.status match {
            case WishStatus.Free => wishRepo.updateStatus(userId, wishId, wishStatus)
            case WishStatus.Shared =>
              wishRepo
                .getUsersBooked(wishId)
                .map(list => list.contains(userId) && list.tail.isEmpty)
                .ifM(wishRepo.updateStatus(userId, wishId, wishStatus), F.pure(Left(ApiError.forbidden)))
            case WishStatus.Booked | WishStatus.Got =>
              wishRepo
                .getUsersBooked(wishId)
                .map(_.contains(userId))
                .ifM(wishRepo.updateStatus(userId, wishId, wishStatus), F.pure(Left(ApiError.forbidden)))
          }
      }

  override def provideAccess(userOwnerId: UserId, wishlistId: WishlistId, username: String): F[Either[ApiError, Unit]] =
    userRepo.saveUserAccess(username, wishlistId).withWriteAccess(userOwnerId, wishlistId)

  override def forbidAccess(userOwnerId: UserId, wishlistId: WishlistId, username: String): F[Either[ApiError, Unit]] =
    userRepo.removeUserAccess(username, wishlistId).withWriteAccess(userOwnerId, wishlistId)

  override def getSubscribers(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, List[NewUser]]] =
    userRepo.getSubscribers(wishlistId).withReadAccess(userId, wishlistId)

  override def addUserToShareWish(userId: UserId, wishlistId: WishlistId, wishId: Long): F[Either[ApiError, Unit]] =
    wishRepo
      .get(wishId)
      .withReadAccess(userId, wishlistId)
      .flatMap {
        case Left(err) => F.pure(Left(err))
        case Right(wish) =>
          if (wish.status == WishStatus.Shared) wishRepo.addUserToShare(userId, wishId)
          else F.pure(Left(ApiError.forbidden))
      }

  override def removeUserToShareWish(userId: UserId, wishlistId: WishlistId, wishId: Long): F[Either[ApiError, Unit]] =
    wishRepo.removeUserToShare(userId, wishId).withReadAccess(userId, wishlistId)

  implicit def injectWishes(f: F[Either[ApiError, WishlistSaved]]): F[Either[ApiError, Wishlist]] =
    f.flatMap(
      _.fold(
        err => F.pure(Left(err)),
        wishlistSaved =>
          wishRepo
            .getAllByWishlistId(wishlistSaved.id)
            .map(_.fold(left => Left(left), wishes => Right(Wishlist(wishlistSaved, wishes)))),
      ),
    )

  implicit class SecurityOps[T](f: => F[Either[ApiError, T]]) {
    def withWriteAccess(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, T]] =
      wishlistRepo
        .get(wishlistId)
        .map(_.filterOrElse(_.userId == userId, ApiError.forbidden))
        .flatMap(
          _.fold(
            err => F.pure(Left(err)),
            _ => f,
          ),
        )

    def withModifyAccess(userId: UserId, wishId: Long): F[Either[ApiError, T]] =
      wishRepo.get(wishId).flatMap {
        case Left(err)    => F.pure(Left(err))
        case Right(value) => withWriteAccess(userId, value.wishlistId)
      }

    def withReadAccess(userId: UserId, wishlistId: WishlistId): F[Either[ApiError, T]] =
      wishlistRepo.get(wishlistId).flatMap {
        case Left(err)    => F.pure(Left(err))
        case Right(value) => isWishlistShared(userId, value).flatMap(_.fold(err => F.pure(Left(err)), _ => f))
      }
  }

  private def isWishlistShared(userId: UserId, wishlist: WishlistSaved): F[Either[ApiError, WishlistSaved]] =
    F.pure(wishlist.access == Access.Public || wishlist.userId == userId)
      .ifM(
        F.pure(Right(wishlist)),
        userRepo.hasUserAccess(userId, wishlist.id).map(_.toRight(ApiError.forbidden).map(_ => wishlist)),
      )
}
