package utils

import com.github.nscala_time.time.Imports._

trait Flyweight {
    type T
    type Key

    def rawGet(key: Key): Option[T]

    private case class Access(obj: T, var lastTouched: DateTime)

    private val cached = scala.collection.mutable.Map[Key, Access]()
    def inMemory: Seq[T] = cached.toMap.map(_._2.obj).toSeq

    def getById(key: Key): Option[T] = {
        cached.get(key) match {
            case Some(access) => {
                access.lastTouched = DateTime.now
                Some(access.obj)
            }

            case None => {
                rawGet(key).map { obj =>
                    cached.synchronized {
                        cached += key -> new Access(obj, DateTime.now)
                    }
                    obj
                }
            }
        }
    }

    def reclaim(beforeWhen: DateTime): Unit = {
        cached.synchronized {
            cached --= cached.filter { case (key, access) =>
                access.lastTouched < beforeWhen
            }.toList.map(_._1)
        }
    }
}

