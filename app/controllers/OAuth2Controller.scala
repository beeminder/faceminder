package controllers

import play.api._
import play.api.mvc._

import oauth2._

import models._

object OAuth2Controller extends Controller {
    def authenticate(provider: String) = Action { implicit request =>
        val callback_uri = "http://" + request.host + "/auth/" + provider + "/callback"

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
                val username = Service.beeminder.getResource("/me.json", token)
                // TODO(sandy): this is where we left off -- doing proper user auth
                Logger.info(username.get)
                Ok.withSession(session + ("user_id" -> "1"))
            }
        }
    }
}
