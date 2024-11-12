package forex.services.rates

import forex.config.ApplicationConfig
import forex.services.cache.Cache
import cats.Monad
import cats.syntax.functor._

class RateLimiter[F[_]: Monad](config: ApplicationConfig, cache: Cache[F, String, String]) {

  // Checks if the request is within the allowed token limit
  def isRequestAllowed(): F[Boolean] = {
    for {
      // Increment the count for the token limit; initialize with 1 if not present
      tokenCount <- cache.increment("TOKEN_LIMIT")
    } yield tokenCount <= config.oneframe.limit
  }
}
