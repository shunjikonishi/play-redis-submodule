package flect.redis

import com.redis.serialization.Format
import com.redis.serialization.Parse

trait Operations {
	self: RedisService =>

	//StringOperations
  def set(key: Any, value: Any)(implicit format: Format): Boolean = withClient(_.set(key, value))
  def set(key: Any, value: Any, nxxx: Any, expx: Any, time: Long): Boolean = withClient(_.set(key, value, nxxx, expx, time))
  def get[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[A] = withClient(_.get(key))
  def getset[A](key: Any, value: Any)(implicit format: Format, parse: Parse[A]): Option[A] = withClient(_.getset(key, value))
  def setnx(key: Any, value: Any)(implicit format: Format): Boolean = withClient(_.setnx(key, value))
  def setex(key: Any, expiry: Int, value: Any)(implicit format: Format): Boolean = withClient(_.setex(key, expiry, value))
  def incr(key: Any)(implicit format: Format): Option[Long] = withClient(_.incr(key))
  def incrby(key: Any, increment: Int)(implicit format: Format): Option[Long] = withClient(_.incrby(key, increment))
  def decr(key: Any)(implicit format: Format): Option[Long] = withClient(_.decr(key))
  def decrby(key: Any, increment: Int)(implicit format: Format): Option[Long] = withClient(_.decrby(key, increment))
  def mget[A](key: Any, keys: Any*)(implicit format: Format, parse: Parse[A]): Option[List[Option[A]]] = withClient(_.mget(key, keys))
  def mset(kvs: (Any, Any)*)(implicit format: Format) = withClient(_.mset(kvs:_*))
  def msetnx(kvs: (Any, Any)*)(implicit format: Format) = withClient(_.msetnx(kvs:_*))
  def setrange(key: Any, offset: Int, value: Any)(implicit format: Format): Option[Long] = withClient(_.setrange(key, offset, value))
  def getrange[A](key: Any, start: Int, end: Int)(implicit format: Format, parse: Parse[A]): Option[A] = withClient(_.getrange(key, start, end))
  def strlen(key: Any)(implicit format: Format): Option[Long] = withClient(_.strlen(key))
  def append(key: Any, value: Any)(implicit format: Format): Option[Long] = withClient(_.append(key, value))
  def getbit(key: Any, offset: Int)(implicit format: Format): Option[Int] = withClient(_.getbit(key, offset))
  def setbit(key: Any, offset: Int, value: Any)(implicit format: Format): Option[Int] = withClient(_.setbit(key, offset, value))
  def bitop(op: String, destKey: Any, srcKeys: Any*)(implicit format: Format): Option[Int] = withClient(_.bitop(op, destKey, srcKeys))
  def bitcount(key: Any, range: Option[(Int, Int)] = None)(implicit format: Format): Option[Int] = withClient(_.bitcount(key, range))

  //HashOperations
  def hset(key: Any, field: Any, value: Any)(implicit format: Format): Boolean = withClient(_.hset(key, field, value))
  def hsetnx(key: Any, field: Any, value: Any)(implicit format: Format): Boolean = withClient(_.hsetnx(key, field, value))
  def hget[A](key: Any, field: Any)(implicit format: Format, parse: Parse[A]): Option[A] = withClient(_.hget(key, field))
  def hmset(key: Any, map: Iterable[Product2[Any,Any]])(implicit format: Format): Boolean = withClient(_.hmset(key, map))
  def hmget[K,V](key: Any, fields: K*)(implicit format: Format, parseV: Parse[V]): Option[Map[K,V]] = withClient(_.hmget(key, fields:_*))
  def hincrby(key: Any, field: Any, value: Int)(implicit format: Format): Option[Long] = withClient(_.hincrby(key, field, value))
  def hexists(key: Any, field: Any)(implicit format: Format): Boolean = withClient(_.hexists(key, field))
  def hdel(key: Any, field: Any, fields: Any*)(implicit format: Format): Option[Long] = withClient(_.hdel(key, field, fields))
  def hlen(key: Any)(implicit format: Format): Option[Long] = withClient(_.hlen(key))
  def hkeys[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[List[A]] = withClient(_.hkeys(key))
  def hvals[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[List[A]] = withClient(_.hvals(key))
  def hgetall[K,V](key: Any)(implicit format: Format, parseK: Parse[K], parseV: Parse[V]): Option[Map[K,V]] = withClient(_.hgetall[K, V](key))
}