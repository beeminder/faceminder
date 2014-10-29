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
import plugins._

object GoalController extends Controller {
    def obtain = Authenticated { implicit request =>
        case class GoalForm(
            pluginId: String
        )

        val goalForm = Form(mapping(
            "pluginId" -> text
        )(GoalForm.apply)(GoalForm.unapply)).bindFromRequest.get

        val plugin = Plugin.getById(goalForm.pluginId).get

        val user = request.user
        if (user.isReal) {
            val nextPage = routes.GoalController.setup(goalForm.pluginId).absoluteURL()
            val toGrant = plugin.manifest.permissions.toSet - user.permissions

            if (toGrant.nonEmpty) {
                Redirect(
                    routes.AuthController.obtainPermission.absoluteURL()
                ).withSession(session +
                    ("obtain_permissions" -> toGrant.mkString(",")) +
                    ("redirect_to" -> nextPage)
                )
            } else {
                Redirect(nextPage)
            }
        } else {
            // TODO(sandy): this isn't right, we need to redirect back to the proper place
            // it would be nice to have a redirect framework handler thing
            Redirect(routes.AuthController.authenticate("beeminder").absoluteURL())
        }
    }

    def setup(pluginId: String) = Authenticated { implicit request =>
        if (request.user.isReal) {
            Plugin.getById(pluginId) match {
                case Some(plugin) => Ok(views.html.newGoal(plugin.renderOptions))
                case None => BadRequest
            }
        } else {
            Forbidden
        }
    }

    def create = Authenticated { implicit request =>
        case class GoalForm(
            pluginId: String,
            slug: String,
            title: String,
            perWeek: Int
        )

        val goalForm = Form(mapping(
            "pluginId" -> text,
            "slug" -> text,
            "title" -> text,
            "perWeek" -> number
        )(GoalForm.apply)(GoalForm.unapply)).bindFromRequest.get

        val plugin = Plugin.getById(goalForm.pluginId).get

        val defaultParams = Map(
            "slug" -> goalForm.slug,
            "title" -> goalForm.title,
            "goal_type" -> plugin.manifest.goalType.toString,
            "goaldate" -> ((DateTime.now + 52.weeks).getMillis / 1000).toString,
            "goalval" -> (52 * goalForm.perWeek).toString
        )

        val postData = request.body.asFormUrlEncoded.get.map { case (k, v) =>
            k -> v.mkString(",")
        }

        val goalSettings = plugin.handleRequest(postData)

        val user = request.user
        val result = Service.beeminder.post(
            "/users/" + user.username + "/goals.json",
            user.bee_service.token,
            defaultParams ++ goalSettings.requestParams
        )

        if (result.isDefined) {
            Goal.create(plugin, user, goalForm.slug, goalForm.title, goalSettings.options)
            Ok("cool!")
        } else {
            // TODO(sandy): do something smart here
            InternalServerError
        }
    }
}

