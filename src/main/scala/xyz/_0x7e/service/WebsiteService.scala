/*
 * Copyright 2016 Timo Schmid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz._0x7e.service

import cats.Monad
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.http4s._
import org.http4s.dsl._
import org.http4s.twirl._
import xyz._0x7e.db.Urls
import xyz._0x7e.dsl._

/**
  * Routes that display the website
  *
  * @param xa The database transactor
  */
class WebsiteService[F[_]: Monad](xa: Transactor[F]) extends Http4sDsl[F] {

  val service = HttpService[F] {

    // route for the index page
    case GET -> Root =>
      Ok(html.index.apply())

    // route for the redirects
    case request @ GET -> Root / key if isValidKey(key) =>
      routeRedirect(key, request)

  }

  // routes a redirect
  private def routeRedirect(key: String, request: Request[F]): F[Response[F]] =
    for {
      oUrl      <- Urls.findUrlByKey(key).transact(xa)
      response  <- redirectUrl(key, oUrl, request)
    } yield response


  // redirects to an url
  private def redirectUrl(key: String, oUrl: Option[String], request: Request[F]): F[Response[F]] =
    oUrl match {
      case Some(url) =>
        for {
          _         <- trackClick(key, request)
          redirect  <- PermanentRedirect(Uri.unsafeFromString(url))
        } yield redirect

      case None =>
        NotFound()
    }

  // tracks a click
  private def trackClick(key: String, request: Request[F]): F[Int] =
    Urls.trackClick(
      key,
      request.remoteAddr,
      request.header("User-Agent"),
      request.header("Referrer")
    ).transact(xa)

}
