package main.scala.test


/**
{
	def facebook()
	{
		println("Now starting Facebook OAUTH 2.0 Example")
		val conf = ConfigFactory.load
		val appID = conf.getString("test.facebook.appID")
		val secret = conf.getString("test.facebook.secret")
		val redirectURI = conf.getString("test.facebook.redirectURI")
		val scope = conf.getStringList("test.facebook.scope").toList
		val access_token = conf.getString("test.facebook.access_token")


		val facebook = new FaceBook(appID, secret)
		println("FaceBook Authorization URL:\n" + facebook.getFBAuthorizationURI(redirectURI, scope))

		if ( !access_token.isEmpty )
		{
			//  Set a new instance and initialize with a valid access token and it TTL.
			val facebook = new FaceBook(appID, secret, access_token, 3600)

			val response = facebook.getFBResource("https://graph.facebook.com/me")

			if ( !response.isEmpty )
			{
				println("Response: " + response.toString())
			}
			else
			{
				println("Got Error \"" + facebook.error + "\"")
			}
		}
	}
}


*/

