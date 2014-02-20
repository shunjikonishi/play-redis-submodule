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
  def mget[A](key: Any, keys: Any*)(implicit format: Format, parse: Parse[A]): Option[List[Option[A]]] = withClient(_.mget(key, keys:_*))
  def mset(kvs: (Any, Any)*)(implicit format: Format) = withClient(_.mset(kvs:_*))
  def msetnx(kvs: (Any, Any)*)(implicit format: Format) = withClient(_.msetnx(kvs:_*))
  def setrange(key: Any, offset: Int, value: Any)(implicit format: Format): Option[Long] = withClient(_.setrange(key, offset, value))
  def getrange[A](key: Any, start: Int, end: Int)(implicit format: Format, parse: Parse[A]): Option[A] = withClient(_.getrange(key, start, end))
  def strlen(key: Any)(implicit format: Format): Option[Long] = withClient(_.strlen(key))
  def append(key: Any, value: Any)(implicit format: Format): Option[Long] = withClient(_.append(key, value))
  def getbit(key: Any, offset: Int)(implicit format: Format): Option[Int] = withClient(_.getbit(key, offset))
  def setbit(key: Any, offset: Int, value: Any)(implicit format: Format): Option[Int] = withClient(_.setbit(key, offset, value))
  def bitop(op: String, destKey: Any, srcKeys: Any*)(implicit format: Format): Option[Int] = withClient(_.bitop(op, destKey, srcKeys:_*))
  def bitcount(key: Any, range: Option[(Int, Int)] = None)(implicit format: Format): Option[Int] = withClient(_.bitcount(key, range))

  //HashOperations
  def hset(key: Any, field: Any, value: Any)(implicit format: Format): Boolean = withClient(_.hset(key, field, value))
  def hsetnx(key: Any, field: Any, value: Any)(implicit format: Format): Boolean = withClient(_.hsetnx(key, field, value))
  def hget[A](key: Any, field: Any)(implicit format: Format, parse: Parse[A]): Option[A] = withClient(_.hget(key, field))
  def hmset(key: Any, map: Iterable[Product2[Any,Any]])(implicit format: Format): Boolean = withClient(_.hmset(key, map))
  def hmget[K,V](key: Any, fields: K*)(implicit format: Format, parseV: Parse[V]): Option[Map[K,V]] = withClient(_.hmget(key, fields:_*))
  def hincrby(key: Any, field: Any, value: Int)(implicit format: Format): Option[Long] = withClient(_.hincrby(key, field, value))
  def hexists(key: Any, field: Any)(implicit format: Format): Boolean = withClient(_.hexists(key, field))
  def hdel(key: Any, field: Any, fields: Any*)(implicit format: Format): Option[Long] = withClient(_.hdel(key, field, fields:_*))
  def hlen(key: Any)(implicit format: Format): Option[Long] = withClient(_.hlen(key))
  def hkeys[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[List[A]] = withClient(_.hkeys(key))
  def hvals[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[List[A]] = withClient(_.hvals(key))
  def hgetall[K,V](key: Any)(implicit format: Format, parseK: Parse[K], parseV: Parse[V]): Option[Map[K,V]] = withClient(_.hgetall[K, V](key))

  //Operations
  def sort[A](key:String, 
              limit:Option[Pair[Int, Int]] = None, 
              desc:Boolean = false, 
              alpha:Boolean = false, 
              by:Option[String] = None, 
              get:List[String] = Nil)(implicit format:Format, parse:Parse[A]):Option[List[Option[A]]] =
    withClient(_.sort[A](key, limit, desc, alpha, by, get))
  def sortNStore[A](key:String, 
              limit:Option[Pair[Int, Int]] = None, 
              desc:Boolean = false, 
              alpha:Boolean = false, 
              by:Option[String] = None, 
              get:List[String] = Nil,
              storeAt: String)(implicit format:Format, parse:Parse[A]):Option[Long] = 
    withClient(_.sortNStore[A](key, limit, desc, alpha, by, get, storeAt))
  def keys[A](pattern: Any = "*")(implicit format: Format, parse: Parse[A]): Option[List[Option[A]]] = withClient(_.keys[A](pattern))
  def randomkey[A](implicit parse: Parse[A]): Option[A] = withClient(_.randomkey[A])
  def rename(oldkey: Any, newkey: Any)(implicit format: Format): Boolean = withClient(_.rename(oldkey, newkey))
  def renamenx(oldkey: Any, newkey: Any)(implicit format: Format): Boolean = withClient(_.renamenx(oldkey, newkey))
  def dbsize: Option[Long] = withClient(_.dbsize)
  def exists(key: Any)(implicit format: Format): Boolean = withClient(_.exists(key))
  def del(key: Any, keys: Any*)(implicit format: Format): Option[Long] = withClient(_.del(key, keys:_*))
  def getType(key: Any)(implicit format: Format): Option[String] = withClient(_.getType(key))
  def expire(key: Any, ttl: Int)(implicit format: Format): Boolean = withClient(_.expire(key, ttl))
  def pexpire(key: Any, ttlInMillis: Int)(implicit format: Format): Boolean = withClient(_.pexpire(key, ttlInMillis))
  def expireat(key: Any, timestamp: Long)(implicit format: Format): Boolean = withClient(_.expireat(key, timestamp))
  def pexpireat(key: Any, timestampInMillis: Long)(implicit format: Format): Boolean = withClient(_.pexpireat(key, timestampInMillis))
  def ttl(key: Any)(implicit format: Format): Option[Long] = withClient(_.ttl(key))
  def pttl(key: Any)(implicit format: Format): Option[Long] = withClient(_.pttl(key))
  def select(index: Int): Boolean = withClient(_.select(index))
  def flushdb: Boolean = withClient(_.flushdb)
  def flushall: Boolean = withClient(_.flushall)
  def move(key: Any, db: Int)(implicit format: Format): Boolean = withClient(_.move(key, db))
  def quit: Boolean = withClient(_.quit)
  def auth(secret: Any)(implicit format: Format): Boolean = withClient(_.auth(secret))
  def persist(key: Any)(implicit format: Format): Boolean = withClient(_.persist(key))
}