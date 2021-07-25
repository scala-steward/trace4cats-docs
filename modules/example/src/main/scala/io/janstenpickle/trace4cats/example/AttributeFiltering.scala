package io.janstenpickle.trace4cats.example

import cats.data.{NonEmptyMap, NonEmptySet}
import cats.effect.{IO, IOApp}
import cats.syntax.semigroup._
import fs2.Chunk
import io.janstenpickle.trace4cats.Span
import io.janstenpickle.trace4cats.`export`.{CompleterConfig, QueuedSpanCompleter}
import io.janstenpickle.trace4cats.avro.AvroSpanExporter
import io.janstenpickle.trace4cats.filtering.AttributeFilter._
import io.janstenpickle.trace4cats.filtering.{AttributeFilter, AttributeFilteringExporter}
import io.janstenpickle.trace4cats.kernel.SpanSampler
import io.janstenpickle.trace4cats.model.{SpanKind, SpanStatus, TraceProcess}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object AttributeFiltering extends IOApp.Simple {
  override def run: IO[Unit] = Slf4jLogger.create[IO].flatMap { implicit logger: Logger[IO] =>
    (for {
      exporter <- AvroSpanExporter.udp[IO, Chunk]()

      nameFilter = AttributeFilter.names(NonEmptySet.of("some.attribute.name", "some.other.name"))
      valueFilter = AttributeFilter.values(NonEmptySet.of("protected.value", "sensitive.info"))
      nameValueFilter = AttributeFilter.nameValues(NonEmptyMap.of("some.attribute.name" -> "protected-value"))

      combinedFilter =
        nameFilter |+| valueFilter |+| nameValueFilter // AttributeFilter.combined may also be used to combine two filters

      filteringExporter = AttributeFilteringExporter(combinedFilter, exporter)

      completer <- QueuedSpanCompleter[IO](TraceProcess("trace4cats"), filteringExporter, config = CompleterConfig())

      root <- Span.root[IO]("root", SpanKind.Client, SpanSampler.always, completer)
      child <- root.child("child", SpanKind.Server)
    } yield child).use(_.setStatus(SpanStatus.Internal("Error")))
  }
}
