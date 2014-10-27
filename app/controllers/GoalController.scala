package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
import org.scala_tools.time.Imports._

import oauth2._
import actions._
import models._
import modules._

object GoalController extends Controller {
    def obtain = Authenticated { implicit request =>
        case class GoalForm(
            moduleId: String
        )

        val goalForm = Form(mapping(
            "moduleId" -> text
        )(GoalForm.apply)(GoalForm.unapply)).bindFromRequest.get

        val module = Module.getById(goalForm.moduleId)

        val user = request.user
        if (user.isReal) {
            val nextPage = routes.GoalController.setup(goalForm.moduleId).absoluteURL()
            val toGrant = module.manifest.permissions.toSet - user.permissions

            if (toGrant.nonEmpty) {
                Redirect(
                    routes.OAuth2Controller.obtainPermission.absoluteURL()
                ).withSession(session +
                    ("obtain_permissions" -> toGrant.mkString(",")) +
                    ("redirect_to" -> nextPage)
                )
            } else {
                Redirect(nextPage)
            }
        } else {
            Forbidden
        }
    }

    def setup(moduleId: String) = Authenticated { implicit request =>
        if (request.user.isReal) {
            // TODO(sandy): this lookup can fail
            Ok(Module.getById(moduleId).renderNew)
        } else {
            Forbidden
        }
    }

    def create = Authenticated { implicit request =>
        case class GoalForm(
            moduleId: String,
            slug: String,
            title: String,
            perWeek: Int
        )

        val goalForm = Form(mapping(
            "moduleId" -> text,
            "slug" -> text,
            "title" -> text,
            "perWeek" -> number
        )(GoalForm.apply)(GoalForm.unapply)).bindFromRequest.get

        val module = Module.getById(goalForm.moduleId)

        val user = request.user
        val result = Service.beeminder.post(
            "/users/" + user.username + "/goals.json",
            user.bee_service.token,
            Map(
                "slug" -> goalForm.slug,
                "title" -> goalForm.title,
                "goal_type" -> module.manifest.goalType.toString,
                "goaldate" -> ((DateTime.now + 52.weeks).getMillis / 1000).toString,
                "goalval" -> (52 * goalForm.perWeek).toString,
                "autodata" -> ("Faceminder: " + module.manifest.name),
                "dryrun" -> "true"
            ))

        if (result.isDefined) {
            Goal.create(module, user, goalForm.slug, goalForm.title)
            Ok("cool!")
        } else {
            // TODO(sandy): do something smart here
            InternalServerError
        }
    }
}

