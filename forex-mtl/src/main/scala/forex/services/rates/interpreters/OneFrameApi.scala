package forex.services.rates.interpreters

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.either._
import cats.Applicative
import forex.config.ApplicationConfig
import forex.domain.Rate
import forex.services.cache.Cache
import forex.services.rates.errors.Error
import forex.services.rates.{ Algebra, OneFrameProtocol }
import forex.services.rates.OneFrameProtocol.OneFrameResponse
import forex.services.rates.RateLimiter
import io.circe.parser.decode
import sttp.client3._
import scala.concurrent.duration._

class OneFrameApi[F[_]: Sync](
    config: ApplicationConfig,
    cache: Cache[F, String, String],
    rateLimiter: RateLimiter[F]
) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] = 
    rateLimiter.isRequestAllowed().flatMap {
      case false => Applicative[F].pure(Error.OneFrameLookupFailed("Rate limit exceeded").asLeft[Rate])
      case true => fetchRateWithHandling(pair)
    }

  private def fetchRateWithHandling(pair: Rate.Pair): F[Error Either Rate] =
    Sync[F].attempt(fetchRate(pair)).map {
      case Right(rate) => rate
      case Left(e) =>
        println(s"Error fetching rate: ${e.getMessage}")
        Error.OneFrameLookupFailed("Failed to fetch rate from API").asLeft[Rate]
    }

  private def fetchRate(pair: Rate.Pair): F[Error Either Rate] = {
    val cacheKey = s"CURR_${pair.from}${pair.to}"
    cache.get(cacheKey).flatMap {
      case Some(json) => decodeCachedRate(json)
      case None       => fetchFromApi(pair, cacheKey)
    }
  }

  private def decodeCachedRate(json: String): F[Error Either Rate] = {
    println("CACHE HIT")
    OneFrameProtocol.jsonToRate(json) match {
      case Right(rate) => Applicative[F].pure(rate.asRight[Error])
      case Left(error) =>
        println(s"Failed to decode cached JSON: $error")
        Applicative[F].pure(Error.OneFrameLookupFailed("Failed to decode cached JSON").asLeft[Rate])
    }
  }

  private def fetchFromApi(pair: Rate.Pair, cacheKey: String): F[Error Either Rate] = {
    println("CACHE MISS")
    val ratesEndpoint = config.oneframe.url
    val token = config.oneframe.token
    val backend = HttpClientSyncBackend()
    val ratesURI = uri"$ratesEndpoint?pair=${pair.from}${pair.to}"

    println(s"Sending Request to $ratesURI")
    val response = basicRequest
      .get(ratesURI)
      .header("token", token)
      .send(backend)

    processApiResponse(response.body, cacheKey)
  }

  private def processApiResponse(responseBody: Either[String, String], cacheKey: String): F[Error Either Rate] = {
    responseBody match {
      case Right(jsonString) => decodeApiResponse(jsonString, cacheKey)
      case Left(error)       =>
        println(s"API request failed: $error")
        Applicative[F].pure(Error.OneFrameLookupFailed(s"Request error: $error").asLeft[Rate])
    }
  }

  private def decodeApiResponse(jsonString: String, cacheKey: String): F[Error Either Rate] = {
    decode[List[OneFrameResponse]](jsonString) match {
      case Right(List(oneFrameResponse)) =>
        println(s"API response parsed successfully: $oneFrameResponse")
        val rate = OneFrameProtocol.toRate(oneFrameResponse)
        cacheAndReturnRate(rate, cacheKey)
      case Right(_) =>
        println("Unexpected response structure from API")
        Applicative[F].pure(Error.OneFrameLookupFailed("Unexpected response structure").asLeft[Rate])
      case Left(error) =>
        println(s"Error parsing API response: $error")
        Applicative[F].pure(Error.OneFrameLookupFailed(s"Parsing error: $error").asLeft[Rate])
    }
  }

  private def cacheAndReturnRate(rate: Rate, cacheKey: String): F[Error Either Rate] = {
    val rateJson = OneFrameProtocol.rateToJson(rate)
    cache.set(cacheKey, rateJson, Some(config.cache.currency.rateExpiryInSeconds.seconds)).map(_ => rate.asRight[Error])
  }
}
