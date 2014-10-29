package plugins

import play.api.Logger
import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import org.scala_tools.time.Imports._

import models._

object ChatPlugin extends Plugin {
    def manifest = new Manifest(
        "chat",
        "Stay in touch",
        "Initiate chats with Facebook friends",
        Seq("read_mailbox"),
        GoalType.DoMore
    )

    def renderOptions = Some(views.html.goalOptions())

    def handleRequest(postData: Map[String, String]) = {
        case class OptionsForm(
            samePersonMinTime: Int
        )

        // TODO(sandy): i can't get this to bind a boolean
        val form = Form(mapping(
            "same_person_min_time" -> number
        )(OptionsForm.apply)(OptionsForm.unapply)).bind(postData).get

        new GoalSettings(
            Map(),

            // It would probably be nice to make a constructor for this?
            (form.samePersonMinTime * 86400).toString + ";"
        )
    }

    override def beforeUpdate(): Unit = {
        Convo.deleteExpired()
    }

    def update(goal: Goal): Float = {
        val limit = 5
        val since = (goal.lastUpdated.getMillis / 1000).toString

        // Expensive, so cache it
        val options = goal.options

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


                Convo.preload(Convo.unmanaged.getByGoal(goal))

                // Not the nicest parser in the world, but it works
                // so that's good enough for me
                val data = (payload \ "data").as[JsArray].value

                for (threadVal <- data) {
                    val thread = threadVal.as[JsObject]
                    val recipient = (thread \ "id").as[JsString].value

                    // If the convo is loaded, it has yet to expire
                    if (!Convo.isLoaded(Convo.getKey(goal, recipient))) {
                        val comments = (thread \ "comments" \ "data").as[JsArray]

                        val participants = (comments \\ "name").toSet.size
                        if (comments.value.length == limit || participants > 1) {
                            points += 1

                            Convo.create(
                                goal,
                                recipient,
                                DateTime.now + options.samePersonMinTime.seconds
                            )
                        }
                    }
                }

                Convo.reclaim(DateTime.now)

                Logger.info(goal.slug + ": " + points.toString)
                points
            }

            case None => 0
        }
    }

    case class GoalChatView(underlying: Goal) {
        case class Options(
                samePersonMinTime: Int,
                friendLists: Seq[String]) {
            val useFriendLists = friendLists.length > 0
        }

        val options = {
            val bits = underlying.rawOptions.split(";")
            new Options(
                bits(0).toInt,

                // TODO(sandy): this should depend on bits haha
                Seq()
            )
        }
    }

    implicit def implRichChatGoal(underlying: Goal): GoalChatView = new GoalChatView(underlying)
}

