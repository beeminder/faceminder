package actors

import play.api._
import play.api.Logger
import play.api.Play.current
import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import com.github.nscala_time.time.Imports._
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
        Service.getByProvider("facebook").map { provider =>
            if (nextRefresh > provider.expiry.get) {
                provider.refresh()
            }
        }
    }

    def updateGoals = {
        Logger.info("updating goals")
        Goal.getAll().map(_.update())
    }
}

