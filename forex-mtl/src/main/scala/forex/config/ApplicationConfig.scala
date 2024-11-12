package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    oneframe: OneframeConfig,
    cache: CacheConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class OneframeConfig(
    url: String,
    token: String,
    limit: Int
)

case class CacheConfig(
  url: String,
  pass: String,
  port: Int,
  currency: CurrencyCacheConfig // Nested cache configuration for currency
)

case class CurrencyCacheConfig(
  rateExpiryInSeconds: Int  // Cache expiry in seconds
)
