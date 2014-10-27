package modules

import play.api.Logger
import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.RequestHeader

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
}

