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

package xyz._0x7e.service.v1

import cats.effect.Sync
import cats.implicits._
import doobie._
import doobie.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString
import xyz._0x7e.db.Urls
import xyz._0x7e.qr.QRCode
import xyz._0x7e.service.v1.protocol.{ShortenError, ShortenRequest, ShortenResult}

class ShortenUrl[F[_]: Sync](val shortHostName: String, val fullHostName: String, xa: Transactor[F]) extends Http4sDsl[F] {

  val service = HttpService[F] {

    case GET -> Root /  key =>
      Urls
        .findUrlByKey(key)
        .transact(xa)
        .flatMap { oKey =>
          oKey.map { key =>
            Ok(result(key).asJson)
          } getOrElse {
            NotFound(ShortenError(404, "Not found").asJson)
          }
        }

    case httpRequest @ POST -> Root / "shorten" =>
      for {
        shortenRequest <- httpRequest.decodeJson[ShortenRequest]
        response       <- validateUrl(shortenRequest.url)(shortenUri)
      } yield response

    case GET -> Root / "qr" / key =>
      Ok(QRCode.fromString[F](full(key)))

  }

  private def validateUrl(url: String)(success: (Uri) => F[Response[F]]): F[Response[F]] =
    Uri.fromString(url) match {

      case Right(uri)
        if uri.scheme.exists(isValidScheme) && uri.host.isDefined =>
        success(uri)

      case _ =>
        BadRequest(ShortenError(400, s"The url '$url' could not be parsed.").asJson)

    }

  private def isValidScheme(scheme: CaseInsensitiveString): Boolean =
    List("http".ci, "https".ci).contains(scheme)

  private def shortenUri(uri: Uri): F[Response[F]] =
    for {
      shortenResult <- create(uri.toString())
      response      <- Ok(shortenResult.asJson)
    } yield response

  private def create(url: String): F[ShortenResult] =
    Urls.create(url).transact(xa).map(result)

  private def result(key: String): ShortenResult =
    ShortenResult(full(key), short(key), qr(key))

  private def short(key: String): String =
    shortHostName + "/" + key

  private def full(key: String): String =
    fullHostName + "/" + key

  private def qr(key: String): String =
    fullHostName + "/api/v1/qr/" + key

}
