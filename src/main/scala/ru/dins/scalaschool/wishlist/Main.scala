package ru.dins.scalaschool.wishlist

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import doobie.Transactor
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import ru.dins.scalaschool.wishlist.controller.Controller
import ru.dins.scalaschool.wishlist.db.{Migrations, UserRepoImpl, WishRepoImpl, WishlistRepoImpl}
import ru.dins.scalaschool.wishlist.service.ServiceImpl
import sttp.tapir.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.swagger.http4s.SwaggerHttp4s

import java.util.concurrent.Executors

object Main extends IOApp {

  private def createTransactor() = {
    val host = sys.env.getOrElse("POSTGRES_HOST", "db")
    val port = sys.env.getOrElse("POSTGRES_PORT", "5432")
    val user = sys.env.getOrElse("POSTGRES_USER", "postgres")
    val pass = sys.env.getOrElse("POSTGRES_PASSWORD", "postgres")
    val db   = sys.env.getOrElse("POSTGRES_DB", "wishlist")
    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = s"jdbc:postgresql://$host:$port/$db",
      user = user,
      pass = pass,
    )
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val xa           = createTransactor()
    val userRepo     = UserRepoImpl(xa)
    val wishlistRepo = WishlistRepoImpl(xa)
    val wishRepo     = WishRepoImpl(xa)
    val service      = ServiceImpl(userRepo, wishlistRepo, wishRepo)
    val controller   = Controller(service)
    val swagger      = new SwaggerHttp4s(controller.docs.toYaml)
    val blockingPool = Executors.newFixedThreadPool(4)
    val blocker      = Blocker.liftExecutorService(blockingPool)
    val staticRoutes = HttpRoutes.of[IO] {
      case request @ GET -> Root / "index.html" =>
        StaticFile.fromResource("/static/index.html", blocker, Some(request)).getOrElseF(NotFound())
      case request @ GET -> Root / "script.js" =>
        StaticFile.fromResource("/static/script.js", blocker, Some(request)).getOrElseF(NotFound())
    }
    val httpApp = Router(
      "/"        -> controller.routes,
      "/swagger" -> swagger.routes,
      "/"        -> staticRoutes,
    ).orNotFound
    val server = BlazeServerBuilder[IO](executionContext)
      .withHttpApp(httpApp)
      .bindHttp(8080, "0.0.0.0")
      .resource
    for {
      _ <- Migrations.migrate(xa)
      _ <- server.use(_ => IO.never)
    } yield ExitCode.Success
  }
}
