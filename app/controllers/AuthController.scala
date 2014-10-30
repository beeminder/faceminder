package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import oauth2._
import actions._
import models._

object AuthController extends Controller {
    def authenticate(provider: String) = Authenticated { implicit request =>
        val callback_uri = routes.AuthController.callback(provider).absoluteURL()

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
                        TemporaryRedirect(Service.facebook.getAuthURI(callback_uri, List()))
                    } else {
                        Ok
                    }
                } else {
                    Forbidden
                }

            case _ => NotFound
        }
    }

    // Grant additional permissions to a facebook token
    def obtainPermission = Authenticated { implicit request =>
        val callback_uri = routes.AuthController.callback("facebook").absoluteURL()

        request.session.get("obtain_permissions") match {
            case Some(permStr) => {
                if (request.user.isReal) {
                    val permissions = permStr.split(",") ++ request.user.permissions
                    Logger.info("requesting " + permissions.mkString(","))
                    TemporaryRedirect(
                        Service.facebook.getAuthURI(callback_uri, permissions)
                    ).withSession(session - "obtain_permissions")
                } else {
                    Forbidden
                }
            }

            case None => Ok
        }
    }

    def callback(provider: String) = Authenticated { implicit request =>
        // TODO(sandy): this is super ugly, clean it up
        provider match {
            case "facebook" => {
                val callback_uri = "http://" + request.host + "/auth/" + provider + "/callback"

                request.getQueryString("code") match {
                    case Some(code) => {
                        val token = Service.facebook.exchangeCodeForToken(code, callback_uri).get

                        if (request.user.fb_service.isDefined) {
                            // Update the old service
                            val fb = request.user.fb_service.get
                            fb.token = token.token
                            fb.expiry = Some(token.expiry)
                            fb.save()
                        } else {
                            // TODO(sandy): this can fail
                            val idPayload = Service.facebook.getResource(
                                "/me",
                                token.token,
                                Map("fields" -> "id")
                            ).get

                            Logger.info(idPayload.toString)

                            val username = (idPayload \ "id").as[String]

                            // Create a new one, because it doesn't exist yet
                            val fb = Service.create(
                                "facebook",
                                username,
                                token.token,
                                Some(token.expiry)
                            )

                            request.user.fb_service = Some(fb)
                            request.user.save()
                        }

                        // TODO(sandy): should be able to redirect here back to goal creation
                    }

                    case None => {
                        // TODO(sandy): what happens here?
                    }
                }

                session.get("redirect_to") match {
                    case Some(url) => Redirect(url).withSession(session - "redirect_to")
                    case None => Ok
                }
            }

            case "beeminder" => {
                val token = request.getQueryString("access_token").get
                val payload = Service.beeminder.getResource("/me.json", token).get
                val username = (payload \ "username").as[String]

                val user = User.getByUsername(username) match {
                    case Some(u) => u
                    case None => {
                        User.create(
                            username,
                            Seq(),
                            Service.create("beeminder", username, token, None),
                            None
                        )
                    }
                }

                Ok.withSession(session +
                    ("user_id" -> user.id.toString)
                )
            }
        }
    }
}