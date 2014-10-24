package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import oauth2._

import models._

object OAuth2Controller extends Controller {
    def authenticate(provider: String) = Action { implicit request =>
        val callback_uri = routes.OAuth2Controller.callback(provider).absoluteURL()

        TemporaryRedirect(
            provider match {
                case "facebook" => Service.facebook.getAuthURI(callback_uri, List("read_mailbox"))
                case "beeminder" => Service.beeminder.getAuthURI(callback_uri)
                case _ => throw new IllegalArgumentException
            }
        )
    }

    def callback(provider: String) = Action { implicit request =>
        provider match {
            case "facebook" => {
                val callback_uri = "http://" + request.host + "/auth/" + provider + "/callback"

                request.getQueryString("code") match {
                    case Some(code) => {
                        val token = Service.facebook.exchangeCodeForToken(code, callback_uri).get
                        Logger.info(token.token)
                        Logger.info(token.expiry.toString)
                    }

                    case None => {
                        // TODO(sandy): what happens here?
                    }
                }

                Ok
            }

            case "beeminder" => {
                val token = request.getQueryString("access_token").get
                val payload = Service.beeminder.getResource("/me.json", token).get
                val username = (payload \ "username").as[String]

                val user = User.getByUsername(username) match {
                    case Some(u) => u
                    case None => {
                        val bee = new Service(None, "beeminder", token, None)
                        bee.insert()

                        val u = new User(None, username, Seq(), bee, None)
                        u.insert()

                        u
                    }
                }

                Ok.withSession(session +
                    ("user_id" -> user.id.get.toString)
                )
            }
        }
    }
}
