package forex.services.rates

import forex.config.ApplicationConfig
import forex.services.cache.Cache
import forex.services.rates.Algebra
import forex.services.rates.interpreters.OneFrameApi
import cats.effect.Sync

object Interpreters {
  def api[F[_]: Sync](
    config: ApplicationConfig,
    cache: Cache[F, String, String],
    rateLimiter: RateLimiter[F]
  ): Algebra[F] = new OneFrameApi[F](config, cache, rateLimiter)
}
