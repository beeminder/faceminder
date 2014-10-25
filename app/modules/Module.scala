package modules

import play.api.Logger
import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

object Module {
    private val available = Seq(
        ChatModule
    )

    private val lookup = available.map { module =>
        module.manifest.id -> module
    }.toMap

    def getById(id: String) = lookup(id)

    implicit def implicitModuleColumnMapper = MappedColumnType.base[Module, String](
        m => m.manifest.id,
        s => Module.getById(s)
    )
}

case class ModuleManifest(
        id: String,
        name: String,
        description: String,
        permissions: Seq[String])

trait Module {
    def manifest: ModuleManifest
}

