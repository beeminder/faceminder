package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import oauth2.ServiceProvider
import actions._
import models._

object AuthController extends Controller {
    def authenticate(provider: String) = UserAware { implicit request =>
        val callback_uri = routes.AuthController.callback(provider).absoluteURL()

        provider match {
            case "beeminder" =>
                if (!request.user.isReal) {
                    Redirect(ServiceProvider.beeminder.getAuthURI(callback_uri))
                } else {
                    Redirect(routes.Application.index.absoluteURL())
                }

            case "facebook" =>
                if (request.user.isReal) {
                    if (!request.user.fb_service.isDefined) {
                        Redirect(ServiceProvider.facebook.getAuthURI(callback_uri, List()))
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
                   ServiceProvider.facebook.getAuthURI(callback_uri, permissions)
               ).withSession(session - "obtain_permissions")
            }

            case None => Ok
        }
    }

    private def exchangeFacebookCodeForToken(
            callback_uri: String,
            queryString: Map[String, Seq[String]],
            user: User): Either[String, Unit] = {
        queryString.get("code") match {
            case Some(code) => {
                try {
                    val token = ServiceProvider.facebook.exchangeCodeForToken(
                        code.toList(0), callback_uri).get

                    if (user.fb_service.isDefined) {
                        // Update the old service
                        val fb = user.fb_service.get
                        fb.token = token.token
                        fb.expiry = Some(token.expiry)
                        fb.save()
                    } else {
                        val idPayload = ServiceProvider.facebook.getResource(
                            "/me",
                            token.token,
                            Map("fields" -> "id")
                        ).get

                        Logger.info(idPayload.toString)

                        val username = (idPayload \ "id").as[String]

                        // Create a new one, because it doesn't exist yet
                        val fb = Service.create(
                            "facebook",
                            user.id,
                            username,
                            token.token,
                            Some(token.expiry)
                        )

                        user.fb_service = Some(fb)
                        user.save()
                    }

                    Right()
                } catch {
                    // TODO(sandy): in an ideal world, we would try again
                    case e: NoSuchElementException => Left("Unable to connect to Facebook")
                }
            }

            case None => {
                Left("Unable to connect to Facebook")
            }
        }
    }

    def callback(provider: String) = UserAware { implicit request =>
        provider match {
            case "facebook" => {
                exchangeFacebookCodeForToken(
                    routes.AuthController.callback("facebook").absoluteURL(),
                    request.queryString,
                    request.user
                ) match {
                    // TODO: make this show an error page?
                    case Left(error) => InternalServerError(error)
                    case Right(_) => Redirect(
                        request.session("redirect_to")
                    ).withSession(session - "redirect_to")
                }
            }

            case "beeminder" => {
                val token = request.getQueryString("access_token").get
                val payload = ServiceProvider.beeminder.getResource("/me.json", token).get
                val username = (payload \ "username").as[String]

                val user = User.getByUsername(username) match {
                    case Some(user) => {
                        user.bee_service.token = token
                        user.bee_service.save()
                        user
                    }
                    case None => {
                        User.create(
                            username,
                            Seq(),
                            // TOTAL HACK(sandy): this is bad bad bad bad
                            // because we don't know what their uid is! Let's hope
                            // nobody ever does anything with this (since we don't
                            // need it for beeminder services)
                            Service.create("beeminder", 0, username, token, None),
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
