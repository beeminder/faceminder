package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import oauth2._
import actions._
import models._

object AuthController extends Controller {
    def authenticate(provider: String) = UserAware { implicit request =>
        val callback_uri = routes.AuthController.callback(provider).absoluteURL()

        provider match {
            case "beeminder" =>
                if (!request.user.isReal) {
                    Redirect(Service.beeminder.getAuthURI(callback_uri))
                } else {
                    Redirect(routes.Application.index.absoluteURL())
                }

            case "facebook" =>
                if (request.user.isReal) {
                    if (!request.user.fb_service.isDefined) {
                        Redirect(Service.facebook.getAuthURI(callback_uri, List()))
                    } else {
                        Redirect(routes.Application.index.absoluteURL())
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
               val permissions = (permStr.split(",") ++ request.user.permissions).distinct
               Logger.info("requesting " + permissions.mkString(","))

               Redirect(
                   Service.facebook.getAuthURI(callback_uri, permissions)
               ).withSession(session - "obtain_permissions")
            }

            case None => Ok
        }
    }

    def callback(provider: String) = UserAware { implicit request =>
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
                    case Some(u) => {
                        u.bee_service.token = token
                        u.bee_service.save()
                        u
                    }
                    case None => {
                        User.create(
                            username,
                            Seq(),
                            Service.create("beeminder", username, token, None),
                            None
                        )
                    }
                }

                Redirect(
                    routes.Application.index.absoluteURL()
                ).withSession(session +
                    ("user_id" -> user.id.toString)
                )
            }
        }
    }

    def logout = Action { implicit request =>
        Redirect(
            routes.Application.index.absoluteURL()
        ).withSession(session - "user_id")
    }
}
