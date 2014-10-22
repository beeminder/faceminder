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
class FaceBook (
	applicationID: String,
	secretKey: String
) extends OAuth2
{
	/**
	 * Constructor including accessToken
	 * @param applicationID Facebook Application ID
	 * @param secretKey Application Secret Key
	 * @param accessToken Access Token if one is already generated
	 * @param expiresIn TTL remaining where the token is valid
	 *
	 */
	def this(applicationID: String, secretKey: String, accessToken: String, expiresIn: Int ) =
	{
		this(applicationID, secretKey)
		this.accessToken = accessToken
		this.expiresIn = expiresIn
		this.expirationTime += expiresIn.seconds
	}

	var expiresIn: Int = 0
	var expirationTime: DateTime = DateTime.now

	/**
	 * Get FB Application Authorization URI for user to authorize our app
	 * @param redirectURI URI to redirect user after authorization
	 * @param scope Authorization scopes
	 * @param state Application state that will be resent to us
	 * @return
	 */
	def getFBAuthorizationURI (
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

	/**
	 * Exchange FB Code for an access token and redirect
	 * @param code Authorization Code
	 * @param redirectURI URI to redirect user
	 * @return response from server
	 */
	def exchangeFBCodeForAccessToken (code : String, redirectURI: String) : Option[String] = {
		getOAuth2Request("https://graph.facebook.com/oauth/access_token", Map[String, String](
			"client_id" -> applicationID,
			"client_secret" -> secretKey,
			"redirect_uri" -> redirectURI,
			"code" -> code
		)).map { str =>
            str.substring("access_token=".length)
        }
	}

	/**
	 * Get Facebook Access Token
	 * @param code  Access code returned by FB
	 * @return Access token after exchange
	 */
	def getFBAccessToken ( code: String ) : Option[String] =
	{
		postOAuth2Request("https://graph.facebook.com/oauth/access_token", Map[String, String](
			"client_id" -> applicationID,
			"client_secret" -> secretKey,
			"grant_type" -> "client_credentials"
		))
	}

	/**
	 * Get Facebook API Resource
	 * @param resourceURI URI of resource to request
	 * @param params extra querystring parameters
	 * @return response from server
	 */
	def getFBResource ( resourceURI: String, params: Map[String, String] = Map()) : Option[String] =
	{
		if ( DateTime.now < expirationTime )
		{
			getOAuth2Resource( resourceURI, params )
		}
		else
		{
			this.error = "Token Has Expired!"
			None
		}
	}
}
