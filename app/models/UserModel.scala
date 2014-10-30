package models

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

import utils.Flyweight

case class User(
        id: Int,
        username: String,
        var goals: Seq[Goal],
        var bee_service: Service,
        var fb_service: Option[Service]) {
    private val Table = TableQuery[UserModel]

    def save() = {
        id match {
            case -1 => throw new InstantiationException
            case _ => // do nothing
        }

        DB.withSession { implicit session =>
            Table.filter(_.id === id).update(this)
        }
    }

    def permissions: Set[String] = {
        goals.flatMap { goal =>
            goal.plugin.manifest.permissions
        }.toSet
    }

    val isReal = id != -1
}

object User extends Flyweight {
    type T = User
    type Key = Int

    def create(_1: String, _2: Seq[Goal], _3: Service, _4: Option[Service]) = {
        getById(
            DB.withSession { implicit session =>
                (Table returning Table.map(_.id)) +=
                    new User(0, _1, _2, _3, _4)
            }
        ).get
    }

    private val Table = TableQuery[UserModel]
    def rawGet(id: Int): Option[User] = {
        DB.withSession { implicit session =>
            Table.filter(_.id === id).firstOption
        }
    }

    def getByUsername(username: String): Option[User] = {
        DB.withSession { implicit session =>
            Table.filter(_.username === username).firstOption
        }.flatMap { user =>
            User.getById(user.id)
        }
    }

    implicit def implicitUserColumnMapper = MappedColumnType.base[User, Int](
        u => u.id,
        i => User.getById(i).get
    )

    val Guest = new User(
        -1,
        "Guest",
        Seq(),
        new Service(0, "beeminder", "", "", None),
        None)
}

class UserModel(tag: Tag) extends Table[User](tag, "User") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def goals = column[Seq[Goal]]("goals")
    def bee_service = column[Service]("bee_service")
    def fb_service = column[Option[Service]]("fb_service")

    val user = User.apply _
    def * = (id, username, goals, bee_service, fb_service) <> (user.tupled, User.unapply _)
}

