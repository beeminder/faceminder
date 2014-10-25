package modules

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

object ChatModule extends Module {
    def manifest = new ModuleManifest(
        "chat",
        "Stay in touch",
        "Initiate chats with Facebook friends",
        Seq("read_mailbox")
    )
}

