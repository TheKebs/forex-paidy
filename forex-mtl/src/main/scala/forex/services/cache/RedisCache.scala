package forex.services.cache

import scala.concurrent.duration.FiniteDuration
import redis.clients.jedis.{ Jedis, JedisPool }
import cats.effect.{ Resource, Sync }
import cats.syntax.functor._
import redis.clients.jedis.JedisPoolConfig

class RedisCache[F[_]: Sync, K, V](
  redisHost: String,
  redisPort: Int,
  password: String
) extends Cache[F, K, V] {

  private val jedisPoolResource: Resource[F, JedisPool] =
    Resource.make(Sync[F].delay {
      val poolConfig = new JedisPoolConfig()
      
      new JedisPool(poolConfig, redisHost, redisPort, 2000, password)
    })(pool => Sync[F].delay(pool.close()))

  // Execute a command within a Jedis context
  private def withJedis[T](operation: Jedis => T): F[T] =
    jedisPoolResource.use { pool =>
      Sync[F].delay {
        val jedis = pool.getResource
        try operation(jedis)
        finally jedis.close()
      }
    }

  override def get(key: K): F[Option[V]] =
    withJedis { jedis =>
      Option(jedis.get(key.toString)).map(_.asInstanceOf[V])
    }

  override def set(key: K, value: V, expiration: Option[FiniteDuration] = None): F[Unit] =
    withJedis { jedis =>
      expiration match {
        case Some(exp) => jedis.setex(key.toString, exp.toSeconds.toInt, value.toString)
        case None      => jedis.set(key.toString, value.toString)
      }
    }.void

  override def increment(key: K, expiration: Option[FiniteDuration] = None): F[Long] = 
    withJedis { jedis =>
      val count = jedis.incr(key.toString)
      // Set expiration only if this is the first increment and expiration is defined
      if (count == 1L) {
        expiration match {
          case Some(exp) => jedis.expire(key.toString, exp.toSeconds.toInt)
          case None      => () // Do nothing if there's no expiration
        }
      }
      count
    }
}
