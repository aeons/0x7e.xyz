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
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import xyz._0x7e.BuildInfo

class ServerInfoService[F[_]: Monad](allowedIps: List[String]) extends Http4sDsl[F] {

  val service = HttpService[F] {
    case request @ GET -> Root / "server-info" if request.remoteAddr.exists(ip => allowedIps.exists(ip.equals)) =>
      Ok(
        ServerInfo(
          BuildInfo.organization,
          BuildInfo.name,
          BuildInfo.version,
          BuildInfo.scalaVersion,
          BuildInfo.sbtVersion
        ).asJson
      )
  }

}
