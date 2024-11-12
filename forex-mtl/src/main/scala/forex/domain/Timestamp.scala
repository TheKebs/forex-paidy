package forex.domain

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  // Parse a string into a Timestamp
  def parse(timestamp: String): Timestamp = {
    val parsedDateTime = OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    Timestamp(parsedDateTime)
  }
}
