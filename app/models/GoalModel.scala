package models

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import com.github.nscala_time.time.Imports._

import modules._
import utils.Flyweight

case class Goal(
        id: Int,
        module: Module,
        ownerId: Int,
        slug: String,
        title: String,
        var lastUpdated: DateTime) {
    private val Table = TableQuery[GoalModel]

    def save() = {
        DB.withSession { implicit session =>
            Table.filter(_.id === id).update(this)
        }
    }

    def update() = {
        val points = module.update(this)

        Service.beeminder.post(
            "/users/" + owner.username + "/goals/" + slug + "/datapoints.json",
            owner.bee_service.token,
            Map(
                "value" -> points.toString,
                "comment" -> "automatic datapoint from Faceminder"
            )
        ) match {
            case Some(_) => {
                lastUpdated = DateTime.now
                save()
            }

            case None => // do nothing
        }
    }

    lazy val owner = User.getById(ownerId).get
}

object Goal extends Flyweight {
    type T = Goal
    type Key = Int
    private val Table = TableQuery[GoalModel]

    def create(_1: Module, owner: User, _3: String, _4: String) = {
        val goal = getById(
            DB.withSession { implicit session =>
                (Table returning Table.map(_.id)) +=
                    new Goal(0, _1, owner.id, _3, _4, DateTime.now)
            }
        ).get

        // Update the owner's goals collection
        owner.goals = owner.goals :+ goal
        owner.save()

        goal
    }

    def rawGet(id: Int): Option[Goal] = {
        DB.withSession { implicit session =>
            Table.filter(_.id === id).firstOption
        }
    }

    object unmanaged {
        def getAll(): Seq[Goal] = {
            DB.withSession { implicit session =>
                Table.list
            }
        }
    }

    implicit def implicitGoalColumnMapper = MappedColumnType.base[Seq[Goal], String](
        sg => sg.map { g => g.id.toString }.mkString(","),
        s => s match {
            case "" => Seq()
            case _ => s.split(",").map(i => Goal.getById(i.toInt).get)
        }
    )
}

class GoalModel(tag: Tag) extends Table[Goal](tag, "Goal") {
    import utils.DateConversions._

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def module = column[Module]("module")
    def ownerId = column[Int]("owner")
    def slug = column[String]("slug")
    def title = column[String]("title")
    def lastUpdated = column[DateTime]("lastUpdated")

    val goal = Goal.apply _
    def * = (id, module, ownerId, slug, title, lastUpdated) <> (goal.tupled, Goal.unapply _)
}

