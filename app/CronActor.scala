package actors

import play.api._
import play.api.Logger
import play.api.Play.current
import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import com.github.nscala_time.time.Imports._

import plugins.Plugin
import models._

object Jobs {
    case object RefreshServices
    case object UpdateGoals
}

class CronActor extends Actor {
    val log = Logging(context.system, this)
    def receive = {
        case Jobs.RefreshServices => refreshServices
        case Jobs.UpdateGoals => updateGoals
        case _ => throw new NoSuchMethodException
    }

    def refreshServices = {
        // Start refreshing tokens 5x before they expire
        val duration = (Global.refreshFrequency * 5).seconds
        val nextRefresh = DateTime.now + duration

        Logger.info("refreshing services")
        Service.unmanaged.getByProvider("facebook").map { provider =>
            if (nextRefresh > provider.expiry.get) {
                provider.refresh()
            }
        }

        Service.inMemory.map(_.reload())
    }

    def updateGoals = {
        Logger.info("updating goals")

        Plugin.Available.map(_.beforeUpdate())
        Goal.unmanaged.getAll().map(_.update())
        Plugin.Available.map(_.afterUpdate())

        // Reclaim all of our flyweights to keep memory usage low
        User.reclaim(DateTime.now)
        Service.reclaim(DateTime.now)
        Goal.reclaim(DateTime.now)
    }
}

