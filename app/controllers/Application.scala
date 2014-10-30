package controllers

import play.api._
import play.api.mvc._

import actions._

import plugins._

object Application extends Controller {
    def index = UserAware { implicit request =>
        if (request.user.isReal) {
            Ok(views.html.goalList(request.user, Plugin.Available))
        } else {
            Ok("you should login (but we don't have a login yet)")
        }
    }
}

