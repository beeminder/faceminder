package actions

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.mvc.Security._

import models._

object UserAware extends AuthenticatedBuilder({ request =>
    Some(request.session.get("user_id").flatMap { id =>
        User.getById(id.toInt)
    }.getOrElse(User.Guest))
})

object Authenticated extends AuthenticatedBuilder({ request =>
    request.session.get("user_id").flatMap { id =>
        User.getById(id.toInt)
    }
})

