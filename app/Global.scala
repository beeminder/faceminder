package models

import play.api._
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import play.api.Play.current
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

import actors._

object Global extends GlobalSettings {
    val conf = ConfigFactory.load
    val refreshFrequency = conf.getInt("faceminder.refreshFrequency")
    val updateFrequency = conf.getInt("faceminder.updateFrequency")

    val cronActor = Akka.system.actorOf(
        Props[CronActor],
        name = "cronMgr"
    )

    Akka.system.scheduler.schedule(0.seconds, refreshFrequency.seconds) {
        cronActor ! Jobs.RefreshServices
    }

    // TODO(sandy): do we want to schedule each goal individually? not a bad idea
    Akka.system.scheduler.schedule(60.seconds, updateFrequency.seconds) {
        cronActor ! Jobs.UpdateGoals
    }
}

