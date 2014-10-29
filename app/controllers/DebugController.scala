package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import actions._
import models._

object DebugController extends Controller {
    def grantPermission(permission: String) = Authenticated { implicit request =>
        Redirect(
            routes.AuthController.obtainPermission.absoluteURL()
        ).withSession(session + ("obtain_permissions" -> permission))
    }
}

