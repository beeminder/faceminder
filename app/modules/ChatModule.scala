package modules

import play.api.Logger
import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._

import models._

object ChatModule extends Module {
    def manifest = new ModuleManifest(
        "chat",
        "Stay in touch",
        "Initiate chats with Facebook friends",
        Seq("read_mailbox"),
        GoalType.DoMore
    )

    def renderOptions = Some(views.html.goalOptions())

    def handleRequest(postData: Map[String, String]) = {
        case class OptionsForm(
            allowSamePerson: String
        )

        // TODO(sandy): i can't get this to bind a boolean
        val optionsForm = Form(mapping(
            "allow_same_person" -> text
        )(OptionsForm.apply)(OptionsForm.unapply)).bind(postData).get

        Map()
    }

    def update(goal: Goal): Float = {
        val limit = 5
        val since = (goal.lastUpdated.getMillis / 1000).toString

        Service.facebook.getResource(
            "/me/inbox",
            goal.owner.fb_service.get.token,
            Map(
                "fields" -> ("comments.limit(" +
                    limit.toString + ").since(" + since +
                    "){from,created_time},updated_time"),
                "since" -> since
            )
        ) match {
            case Some(payload) => {
                var points = 0

                // Not the nicest parser in the world, but it works
                // so that's good enough for me
                val data = (payload \ "data").as[JsArray].value
                for (threadVal <- data) {
                    val thread = threadVal.as[JsObject]
                    val comments = (thread \ "comments" \ "data").as[JsArray]

                    if (comments.value.length == limit) {
                        points += 1
                    } else {
                        val participants = (comments \\ "name").toSet.size
                        Logger.info(participants.toString)
                        if (participants > 1) {
                            points += 1
                        }
                    }
                }

                Logger.info("number of points obtained:")
                Logger.info(points.toString)

                points
            }

            case None => 0
        }
    }
}

