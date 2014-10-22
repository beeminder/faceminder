package controllers

import play.api._
import play.api.mvc._

import oauth2._

import com.typesafe.config.ConfigFactory

object OAuth2Controller extends Controller {
    val conf = ConfigFactory.load
    val facebook = new FaceBook(
        conf.getString("facebook.appID"),
        conf.getString("facebook.secret")
    )

    def authenticate(provider: String) = Action { implicit request =>
        val callback_uri = "http://" + request.host + "/auth/" + provider + "/callback"

        TemporaryRedirect(facebook.getFBAuthorizationURI(callback_uri, List("read_mailbox")))
    }

    def callback(provider: String) = Action { implicit request =>
        val callback_uri = "http://" + request.host + "/auth/" + provider + "/callback"

        request.getQueryString("code") match {
            case Some(code) => {
                Logger.info(facebook.exchangeFBCodeForAccessToken(code, callback_uri).get)
            }

            case None => {
            }
        }


        Ok
    }
}
