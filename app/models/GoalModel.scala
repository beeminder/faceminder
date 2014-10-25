package models

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

import modules._

case class Goal(
        var id: Option[Int] = None,
        module: Module,
        owner: User) {
    private val Table = TableQuery[GoalModel]
    def insert() = {
        // Ensure this Event hasn't already been put into the database
        id match {
            case Some(_) => throw new CloneNotSupportedException
            case None => // do nothing
        }

        DB.withSession { implicit session =>
            id = Some((Table returning Table.map(_.id)) += this)
        }
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

    def create() = {
    }

    def update() = {
    }
}

object Goal {
    private val Table = TableQuery[GoalModel]
    def getById(id: Int): Option[Goal] = {
        DB.withSession { implicit session =>
            Table.filter(_.id === id).firstOption
        }
    }

    def getAll(): Seq[Goal] = {
        DB.withSession { implicit session =>
            Table.list
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
    def owner = column[User]("owner")

    val goal = Goal.apply _
    def * = (id.?, module, owner) <> (goal.tupled, Goal.unapply _)
}

