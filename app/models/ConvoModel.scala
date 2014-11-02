package models

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import com.github.nscala_time.time.Imports._

import utils.Flyweight

case class Convo(
        id: String,
        goal: Goal,
        expiry: DateTime) {
    def isExpired = DateTime.now > expiry

    def delete() = {
        DB.withSession { implicit session =>
            TableQuery[ConvoModel].filter(_.id === id).delete
        }
    }
}

object Convo extends Flyweight {
    type T = Convo
    type Key = String
    private val Table = TableQuery[ConvoModel]

    def getKey(goal: Goal, recipient: String) = {
        goal.id.toString + ":" + recipient
    }

    def create(_1: Goal, recipient: String, _2: DateTime) = {
        val key = getKey(_1, recipient)
        DB.withSession { implicit session =>
            Table += new Convo(key, _1, _2)
        }

        getById(key).get
    }

    def rawGet(id: String): Option[Convo] = {
        DB.withSession { implicit session =>
            Table.filter(_.id === id).firstOption
        }
    }

    object unmanaged {
        def getByGoal(goal: Goal) = {
            DB.withSession { implicit session =>
                Table.filter(_.goal === goal).list
            }
        }
    }

    def deleteExpired() = {
        val now = DateTime.now

        val expiredIds = DB.withSession { implicit session =>
            Table.list
        }.filter(_.expiry <= now).map { convo =>
            convo.delete()
            convo.id
        }

        expire(expiredIds)
    }
}


class ConvoModel(tag: Tag) extends Table[Convo](tag, "Convo") {
    import utils.DateConversions._

    def id = column[String]("id", O.PrimaryKey)
    def goal = column[Goal]("goal")
    def expiry = column[DateTime]("expiry")

    def convo = Convo.apply _
    def * = (
        id,
        goal,
        expiry
    ) <> (convo.tupled, Convo.unapply _)
}

