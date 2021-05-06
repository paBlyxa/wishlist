package ru.dins.scalaschool.wishlist.test.db

import cats.effect.{ContextShift, IO}
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import doobie.Transactor
import doobie.implicits.toSqlInterpolator
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor.Aux
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.dins.scalaschool.wishlist.db.Migrations
import ru.dins.scalaschool.wishlist.models.Access
import ru.dins.scalaschool.wishlist.test.TestExamples._

import java.util.UUID
import scala.concurrent.ExecutionContext

trait MyTestContainerForAll extends AnyFlatSpec with Matchers with TestContainerForAll with MockFactory {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def()

  def createTransactor(container: Containers): Aux[IO, Unit] =
    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = container.jdbcUrl,
      user = container.username,
      pass = container.password,
    )

  override def startContainers(): PostgreSQLContainer = {
    val container = super.startContainers()
    val xa        = createTransactor(container)
    Migrations.migrate(xa).unsafeRunSync()
    container
  }

  def insertUser(
      userId: UUID = UUID.randomUUID(),
      username: String = "username",
      email: String = "email",
      telegramId: String = "@username",
  ): doobie.ConnectionIO[UUID] =
    sql"insert into users values ($userId, $username, $email, $telegramId)".update.withUniqueGeneratedKeys[UUID]("id")

  def insertWishlist(
      wishlistId: UUID = UUID.randomUUID(),
      userId: UUID,
      name: String = "wishlist",
      access: Access = Access.Public,
      comment: String = "comment",
  ): doobie.ConnectionIO[UUID] =
    sql"insert into wishlist values ($wishlistId, $userId, $name, $access, $comment, $exampleLDT)".update
      .withUniqueGeneratedKeys[UUID]("id")

  def insertWish(
      wishlistId: UUID = exampleUUID,
      name: String = "present",
      link: String = "some link",
      price: BigDecimal = BigDecimal(123.45),
      comment: String = "comment",
  ): doobie.ConnectionIO[Long] =
    sql"insert into wish (wishlist_id, name, link, price, comment) values ($wishlistId, $name, $link, $price, $comment)".update
      .withUniqueGeneratedKeys[Long]("id")

  def genUUID: IO[UUID] = IO.delay(UUID.randomUUID())
}
