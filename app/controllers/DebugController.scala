package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import com.github.nscala_time.time.Imports._

import actions._
import models._

object DebugController extends Controller {
    def grantPermission(permission: String) = Authenticated { implicit request =>
        Redirect(
            routes.AuthController.obtainPermission.absoluteURL()
        ).withSession(session + ("obtain_permissions" -> permission))
    }

    def destroyService(service: String) = Action { implicit request =>
        DB.withSession { implicit session =>
            TableQuery[ServiceModel].filter(_.provider === service).delete
        }
        Service.reclaim(DateTime.now)

        if (service == "facebook") {
           User.inMemory.map { user =>
                user.fb_service = None
                user.save()
            }
        }

        User.reclaim(DateTime.now)

        Ok("eat it!")
    }
}

