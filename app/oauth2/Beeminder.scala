package oauth2

import java.security.MessageDigest
import dispatch._
import java.util.concurrent.ExecutionException
import play.api.Logger
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import org.scala_tools.time.Imports.DateTime
import play.api.libs.json.{Json, JsValue}

case class Beeminder (
    applicationID: String,
    secretKey: String
) extends OAuth2
{
    def getAuthURI (
        redirectURI: String
    ) : String =
    {
        getOAuth2AuthorizationURI("https://www.beeminder.com/apps/authorize", Map[String, String](
            "client_id" -> applicationID,
            "redirect_uri" -> redirectURI,
            "response_type" -> "token"
        ))
    }

    def getResource(
            endpoint: String,
            token: String,
            params: Map[String, String] = Map()): Option[JsValue] = {
        request = url("https://www.beeminder.com/api/v1" + endpoint).GET <<?
            (params + ("access_token" -> token))
        response = ""

        Logger.info(request.url)

        try
        {
            response = Await.result(browser(request OK as.String), 10 seconds)
            return Some(Json.parse(response))
        }
        catch
        {
            case ex: ExecutionException => this.error = "HTTP Error: " + ex.getMessage
            case e: Exception => this.error = "Generic Error " + e.getMessage
        }

        None
    }

    def post(
            endpoint: String,
            token: String,
            params: Map[String, String] = Map()): Option[JsValue] = {
        request = url("https://www.beeminder.com/api/v1" + endpoint).POST <<?
            (params + ("access_token" -> token))
        response = ""

        Logger.info(request.url)

        try
        {
            response = Await.result(browser(request OK as.String), 10 seconds)
            return Some(Json.parse(response))
        }
        catch
        {
            case ex: ExecutionException => this.error = "HTTP Error: " + ex.getMessage
            case e: Exception => this.error = "Generic Error " + e.getMessage
        }

        Logger.info(this.error)

        None
    }
}

