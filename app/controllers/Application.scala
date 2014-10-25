package controllers

import play.api._
import play.api.mvc._

import actions._

import modules._

object Application extends Controller {

  def index = Authenticated {
    Ok(views.html.newgoal(Module.Available))
  }

}
