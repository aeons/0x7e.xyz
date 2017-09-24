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

package xyz._0x7e

import cats.effect.{Effect, IO}
import cats.implicits._
import doobie._
import doobie.implicits._
import fs2._
import org.http4s.MaybeResponse.http4sMonoidForFMaybeResponse
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.staticcontent
import org.http4s.server.staticcontent.{ResourceService, WebjarService}
import org.http4s.util.StreamApp
import xyz._0x7e.conf.Config
import xyz._0x7e.db.Urls
import xyz._0x7e.service.v1.ShortenUrl
import xyz._0x7e.service.{ServerInfoService, WebsiteService}

object WebServerApp extends WebServer[IO]

/**
  * The main entry point of the application
  * - Connects to the database
  * - Creates the database tables, if they don't exist
  * - Starts the webserver
  */
class WebServer[F[_]: Effect] extends StreamApp[F] {

  // The database transactor
  private val xa = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    s"jdbc:postgresql://${Config.db.host}:${Config.db.port}/${Config.db.name}",
    Config.db.user,
    Config.db.password
  )

  // Routes for server info
  private val serverInfo = new ServerInfoService[F](List("127.0.0.1"))

  // Routes for the website
  private val websiteRoutes = new WebsiteService[F](xa)

  // Routes for assets
  val resourceService = staticcontent.resourceService[F](ResourceService.Config[F](basePath = "/assets"))
  val webjarService = staticcontent.webjarService[F](WebjarService.Config())
  val assets = resourceService |+| webjarService

  // Routes for the API
  private val apiRoutes = new ShortenUrl(
    Config.http.shortHostName,
    Config.http.longHostName,
    xa
  )

  def stream(args: List[String], requestShutdown: F[Unit]) =
    Stream.eval(Urls.createSchema.transact(xa)) >>
      BlazeBuilder[F]
        .bindHttp(Config.http.listenPort, Config.http.listenHost)
        .mountService(serverInfo.service, "/")
        .mountService(websiteRoutes.service, "/")
        .mountService(assets, "/assets")
        .mountService(apiRoutes.service, "/api/v1")
        .serve

}

