package plugins

import play.api.Logger
import play.api.Play.current
import play.api.mvc._
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

import models.Goal

object Plugin {
    val Available = Seq(
        ChatPlugin
    )

    private val lookup = Available.map { plugin =>
        plugin.manifest.id -> plugin
    }.toMap

    def getById(id: String) = lookup.get(id)

    implicit def implicitpluginColumnMapper = MappedColumnType.base[Plugin, String](
        m => m.manifest.id,
        s => Plugin.getById(s).get
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

case class Manifest(
        id: String,
        name: String,
        description: String,
        permissions: Seq[String],
        goalType: GoalType.GoalType)

case class GoalSettings(
        requestParams: Map[String, String],
        options: String = "")

trait Plugin {
    def manifest: Manifest

    // HTML for additional options to display on the goal creation page
    def renderOptions: Option[Option[Goal] => play.api.templates.Html]

    // Custom handler for the additional options.
    // Returns: a map of parameters to override in goal creation
    def handleRequest(queryString: Map[String, String]): GoalSettings

    // The number of data points to return to beeminder
    def update(goal: Goal): Float

    def beforeUpdate(): Unit = { }
    def afterUpdate(): Unit = { }
}

