package modules

import play.api.Logger
import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

object Module {
    val Available = Seq(
        ChatModule
    )

    private val lookup = Available.map { module =>
        module.manifest.id -> module
    }.toMap

    def getById(id: String) = lookup(id)

    implicit def implicitModuleColumnMapper = MappedColumnType.base[Module, String](
        m => m.manifest.id,
        s => Module.getById(s)
    )
}

object GoalType extends Enumeration {
    type GoalType = Value
    val DoMore = Value("hustler")
    val Odometer = Value("biker")
    val LoseWeight = Value("fatloser")
    val GainWeight = Value("gainer")
    val InboxFewer = Value("inboxer")
    val DoLess = Value("drinker")
}

case class ModuleManifest(
        id: String,
        name: String,
        description: String,
        permissions: Seq[String],
        goal_type: GoalType.GoalType)

trait Module {
    def manifest: ModuleManifest

    // TODO(sandy): it probably doesn't make sense for the module
    // to have SUPREME ULTIMATE POWER over setup rendering. maybe
    // it just gets an extra panel or something?
    // also: this is a shit name for a method
    def renderNew: play.api.templates.Html
}

