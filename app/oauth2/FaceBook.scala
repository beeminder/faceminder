package oauth2

import java.security.MessageDigest
import org.scala_tools.time.Imports._

/**
 * User     : Anh T. Nguyen
 * Date     : 12/24/12
 * Time     : 9:03 PM
 * GitHub   : http://github.com/iizmoo
 * Use as your own risk..
 * Facebook OAuth2 wrapper class
 */
case class FaceBook (
	applicationID: String,
	secretKey: String
) extends OAuth2
{
	def getAuthURI (
		redirectURI: String,
		scope: List[String],
		state: String = MessageDigest.getInstance("SHA1").digest("bob".getBytes).map(_ & 0xFF).map(_.toHexString).mkString
	) : String =
	{
		getOAuth2AuthorizationURI("https://www.facebook.com/dialog/oauth", Map[String, String](
			"client_id" -> applicationID,
			"redirect_uri" -> redirectURI,
			"scope" -> scope.mkString(","),
			"response_type" -> "code",
			"state" -> state
		))
	}

	def exchangeCodeForToken (code: String, redirectURI: String) : Option[AuthToken] = {
		getOAuth2Request("https://graph.facebook.com/oauth/access_token", Map[String, String](
			"client_id" -> applicationID,
			"client_secret" -> secretKey,
			"redirect_uri" -> redirectURI,
			"code" -> code
		)).map(parseQueryString)
	}

	def refreshToken(oldToken: String) : Option[AuthToken] = {
		getOAuth2Request("https://graph.facebook.com/oauth/access_token", Map[String, String](
            "grant_type" -> "fb_exchange_token",
			"client_id" -> applicationID,
			"client_secret" -> secretKey,
			"fb_exchange_token" -> oldToken
		)).map(parseQueryString)
	}

    def parseQueryString(query: String): AuthToken = {
        val data = query.split("&").map{ param =>
            val split = param.split("=")
            split(0) -> split(1)
        }.toMap

        new AuthToken(data("access_token"), DateTime.now + data("expires").toInt.seconds)
    }
}
