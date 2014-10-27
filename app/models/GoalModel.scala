package models

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

import modules._
import utils.Flyweight

case class Goal(
        var id: Option[Int] = None,
        module: Module,
        ownerId: Int,
        slug: String,
        title: String) {
    private val Table = TableQuery[GoalModel]

    private var hasBeenInserted = false
    def insert() = {
        if (id.isDefined || hasBeenInserted) {
            throw new CloneNotSupportedException
        }

        val newId = DB.withSession { implicit session =>
            (Table returning Table.map(_.id)) += this
        }

        val goal = Goal.getById(newId).get

        owner.goals = owner.goals :+ goal
        owner.save()

        goal
    }

    def save() = {
        id match {
            case Some(_) => // do nothing
            case None => throw new NullPointerException
        }

        DB.withSession { implicit session =>
            Table.filter(_.id === id.get).update(this)
        }
    }

    def update() = {
    }

    lazy val owner = User.getById(ownerId).get
}

object Goal extends Flyweight {
    type T = Goal
    type Key = Int

    private val Table = TableQuery[GoalModel]

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
        sg => sg.map { g => g.id.get.toString }.mkString(";"),
        s => s match {
            case "" => Seq()
            case _ => s.split(";").map(i => Goal.getById(i.toInt).get)
        }
    )
}

class GoalModel(tag: Tag) extends Table[Goal](tag, "Goal") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def module = column[Module]("module")
    def ownerId = column[Int]("owner")
    def slug = column[String]("slug")
    def title = column[String]("title")

    val goal = Goal.apply _
    def * = (id.?, module, ownerId, slug, title) <> (goal.tupled, Goal.unapply _)
}

