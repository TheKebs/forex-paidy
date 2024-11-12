package forex.services.rates

import forex.domain._
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration
import io.circe.syntax._

object OneFrameProtocol {

  // Circe configuration for handling snake_case or other naming conventions
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  // Case class representing the OneFrame API response
  final case class OneFrameResponse(
    from: Currency,
    to: Currency,
    bid: BigDecimal,
    ask: BigDecimal,
    price: BigDecimal,
    timeStamp: String // The timestamp is a string in the response
  )

  // Custom decoder for Currency
  implicit val currencyDecoder: Decoder[Currency] = Decoder.decodeString.emap { s =>
    try {
      Right(Currency.fromString(s))
    } catch {
      case _: NoSuchElementException => Left(s"Invalid currency code: $s")
    }
  }
  implicit val currencyEncoder: Encoder[Currency] = Encoder.encodeString.contramap(_.toString)

  // Price decoder and encoder
  implicit val priceDecoder: Decoder[Price] = Decoder.decodeBigDecimal.map(Price(_))
  implicit val priceEncoder: Encoder[Price] = Encoder.encodeBigDecimal.contramap(_.value)

  implicit val timestampDecoder: Decoder[Timestamp] = Decoder.decodeString.map(Timestamp.parse)
  implicit val timestampEncoder: Encoder[Timestamp] = Encoder.encodeString.contramap(_.value.toString)

  // Encoder and Decoder for Rate.Pair
  implicit val ratePairDecoder: Decoder[Rate.Pair] = deriveConfiguredDecoder
  implicit val ratePairEncoder: Encoder[Rate.Pair] = deriveConfiguredEncoder

  // Encoder and Decoder for Rate
  implicit val rateDecoder: Decoder[Rate] = deriveConfiguredDecoder
  implicit val rateEncoder: Encoder[Rate] = deriveConfiguredEncoder

  // Encoder and Decoder for OneFrameResponse
  implicit val oneFrameResponseDecoder: Decoder[OneFrameResponse] = deriveConfiguredDecoder[OneFrameResponse]

  // Convert OneFrameResponse to Rate
  def toRate(response: OneFrameResponse): Rate = Rate(
      Rate.Pair(response.from, response.to),
      Price(response.price),
      Timestamp.parse(response.timeStamp) // Use the `parse` method to convert the timestamp
    )

  // Methods to convert Rate to JSON and back
  def rateToJson(rate: Rate): String = rate.asJson.noSpaces
  def jsonToRate(json: String): Either[Error, Rate] = parser.decode[Rate](json)
}
