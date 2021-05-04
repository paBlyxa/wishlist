package ru.dins.scalaschool.wishlist.db

import cats.effect.Sync
import cats.implicits.toFunctorOps
import doobie.implicits.toSqlInterpolator
import doobie.util.transactor.Transactor.Aux
import doobie.util.update.Update0
import doobie.implicits._

object Migrations {

  private val migration: Update0 =
    sql"""create table if not exists users(
         |id UUID,
         |username TEXT UNIQUE NOT NULL,
         |email TEXT,
         |telegram_id TEXT,
         |PRIMARY KEY(id)
         |);
         |create type access_type as enum ('public', 'private');
         |create table if not exists wishlist(
         |id UUID,
         |user_id UUID REFERENCES users(id),
         |name TEXT NOT NULL,
         |access access_type NOT NULL,
         |comment TEXT,
         |created_at TIMESTAMP DEFAULT current_timestamp,
         |PRIMARY KEY (id)
         |);
         |create table if not exists wish(
         |id SERIAL,
         |wishlist_id UUID REFERENCES wishlist(id),
         |name TEXT NOT NULL,
         |link TEXT,
         |price DECIMAL(10, 2),
         |comment TEXT,
         |created_at TIMESTAMP DEFAULT current_timestamp,
         |PRIMARY KEY(id)
         |);
         |""".stripMargin.update

  def migrate[F[_]: Sync](xa: Aux[F, Unit]): F[Unit] = migration.run.void.transact(xa)

}
