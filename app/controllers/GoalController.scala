package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
import org.scala_tools.time.Imports._

import oauth2.ServiceProvider

import actions._
import models._
import plugins._

object GoalController extends Controller {
    def update(slug: String) = Authenticated { implicit request =>
        Goal.getBySlug(request.user, slug) match {
            case Some(goal) => {
                Ok(views.html.newGoal(request.user, goal.plugin, Some(goal)))
            }

            case None => NotFound
        }
    }

    def change(slug: String) = Authenticated { implicit request =>
        Goal.getBySlug(request.user, slug) match {
            case Some(goal) => {
                val goalSettings = getGoalSettings(
                    goal.plugin,
                    request.body.asFormUrlEncoded.get
                )

                goal.rawOptions = goalSettings.options
                goal.save()

                Redirect(routes.Application.index.absoluteURL())
            }

            case None => NotFound
        }
    }

    def obtain = Authenticated { implicit request =>
        case class GoalForm(
            pluginId: String
        )

        val goalForm = Form(mapping(
            "pluginId" -> text
        )(GoalForm.apply)(GoalForm.unapply)).bindFromRequest.get

        val plugin = Plugin.getById(goalForm.pluginId).get

        val nextPage = routes.GoalController.setup(goalForm.pluginId).absoluteURL()
        val toGrant = plugin.manifest.permissions.toSet - request.user.permissions

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
    }

    def setup(pluginId: String) = Authenticated { implicit request =>
        Plugin.getById(pluginId) match {
            case Some(plugin) => Ok(views.html.newGoal(request.user, plugin, None))
            case None => BadRequest
        }
    }

    def getGoalSettings(plugin: plugins.Plugin, formUrlEncoded: Map[String, Seq[String]]) = {
        plugin.handleRequest(
            formUrlEncoded.map { case (k, v) =>
                k -> v.mkString(",")
            }
        )
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

        val user = request.user
        val goalSettings = getGoalSettings(plugin, request.body.asFormUrlEncoded.get)

        // Try contacting Beeminder 3 times
        (1 to 3).find { _ =>
            ServiceProvider.beeminder.post(
                "/users/" + user.username + "/goals.json",
                user.bee_service.token,
                defaultParams ++ goalSettings.requestParams
            ).isDefined
        } match {
            case Some(_) => {
                Goal.create(plugin, user, goalForm.slug, goalForm.title, goalSettings.options)
                Redirect(routes.Application.index.absoluteURL())
            }

            // TODO(sandy): ideally show a template if this happens
            case None => InternalServerError("Unable to contact beeminder. Maybe their servers are down?")
        }
    }
}

