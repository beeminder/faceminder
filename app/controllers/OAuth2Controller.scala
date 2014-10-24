package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import oauth2._
import actions._
import models._

object OAuth2Controller extends Controller {
    def authenticate(provider: String) = Authenticated { implicit request =>
        val callback_uri = routes.OAuth2Controller.callback(provider).absoluteURL()

        provider match {
            case "beeminder" =>
                if (!request.user.isReal) {
                    TemporaryRedirect(Service.beeminder.getAuthURI(callback_uri))
                } else {
                    Ok
                }

            case "facebook" =>
                if (request.user.isReal) {
                    if (!request.user.fb_service.isDefined) {
                        TemporaryRedirect(Service.facebook.getAuthURI(callback_uri, List("read_mailbox")))
                    } else {
                        Ok
                    }
                } else {
                    Forbidden
                }

            case _ => NotFound
        }
    }

    def callback(provider: String) = Authenticated { implicit request =>
        provider match {
            case "facebook" => {
                val callback_uri = "http://" + request.host + "/auth/" + provider + "/callback"

                request.getQueryString("code") match {
                    case Some(code) => {
                        val token = Service.facebook.exchangeCodeForToken(code, callback_uri).get
                        val fb = new Service(None, "facebook", token.token, Some(token.expiry))
                        fb.insert()

                        request.user.fb_service = Some(fb)
                        request.user.save()
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
