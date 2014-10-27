package modules

import play.api.Logger
import play.api.Play.current
import play.api.mvc._
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

import models._

object Module {
    val Available = Seq(
        ChatModule
    )

    private val lookup = Available.map { module =>
        module.manifest.id -> module
    }.toMap

    def getById(id: String) = lookup.get(id)

    implicit def implicitModuleColumnMapper = MappedColumnType.base[Module, String](
        m => m.manifest.id,
        s => Module.getById(s).get
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
        goalType: GoalType.GoalType)

trait Module {
    def manifest: ModuleManifest

    // HTML for additional options to display on the goal creation page
    def renderOptions: Option[play.api.templates.Html]

    // Custom handler for the additional options.
    // Returns: a map of parameters to override in goal creation
    def handleRequest(queryString: Map[String, String]): Map[String, String]

    // The number of data points to return to beeminder
    def update(goal: Goal): Float
}

