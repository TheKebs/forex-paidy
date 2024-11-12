package forex.services.cache

import scala.concurrent.duration.FiniteDuration

trait Cache[F[_], K, V] {
  def get(key: K): F[Option[V]]
  def set(key: K, value: V, expiration: Option[FiniteDuration] = None): F[Unit]
  def increment(key: K, expiration: Option[FiniteDuration] = None): F[Long]
}