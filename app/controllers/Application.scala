package controllers

import play.api._
import play.api.mvc._

import actions._

object Application extends Controller {

  def index = Authenticated {
    Ok(views.html.index("Your new application is ready."))
  }

}
