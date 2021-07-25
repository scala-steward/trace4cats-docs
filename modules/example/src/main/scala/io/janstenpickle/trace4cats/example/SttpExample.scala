package io.janstenpickle.trace4cats.example

import cats.effect.{IO, IOApp, Resource}
import io.janstenpickle.trace4cats.sttp.client3.syntax._
import org.http4s.blaze.client.BlazeClientBuilder
import sttp.client3.http4s.Http4sBackend

object SttpExample extends IOApp.Simple {
  override def run: IO[Unit] =
    (for {
      ec <- Resource.eval(IO.executionContext)
      client <- BlazeClientBuilder[IO](ec).resource
      sttpBackend = Http4sBackend.usingClient(client)
      tracedBackend = sttpBackend.liftTrace()
    } yield tracedBackend).use_
}
