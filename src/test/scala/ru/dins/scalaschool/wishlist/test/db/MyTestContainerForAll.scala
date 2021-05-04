package ru.dins.scalaschool.wishlist.test.db

import cats.effect.{ContextShift, IO}
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import doobie.Transactor
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.dins.scalaschool.wishlist.db.Migrations

import scala.concurrent.ExecutionContext

trait MyTestContainerForAll extends AnyFlatSpec with Matchers with TestContainerForAll with MockFactory {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def()

  def createTransactor(container: Containers) =
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
}
