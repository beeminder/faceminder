package models

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

case class User(
        var id: Option[Int] = None,
        username: String,
        var goals: Seq[Goal],
        var bee_service: Service,
        var fb_service: Option[Service]) {
    private val Table = TableQuery[UserModel]
    def insert() = {
        // Ensure this Event hasn't already been put into the database
        id match {
            case Some(-1) => throw new InstantiationException
            case Some(_) => throw new CloneNotSupportedException
            case None => // do nothing
        }

        DB.withSession { implicit session =>
            id = Some((Table returning Table.map(_.id)) += this)
        }
    }

    def save() = {
        id match {
            case Some(-1) => throw new InstantiationException
            case Some(_) => // do nothing
            case None => throw new NullPointerException
        }

        DB.withSession { implicit session =>
            Table.filter(_.id === id.get).update(this)
        }
    }

    def permissions: Set[String] = {
        goals.flatMap { goal =>
            goal.module.manifest.permissions
        }.toSet
    }

    val isReal = id != Some(-1)
}

object User {
    private val Table = TableQuery[UserModel]
    def getById(id: Int): Option[User] = {
        DB.withSession { implicit session =>
            Table.filter(_.id === id).firstOption
        }
    }

    def getByUsername(username: String): Option[User] = {
        DB.withSession { implicit session =>
            Table.filter(_.username === username).firstOption
        }
    }

    implicit def implicitUserColumnMapper = MappedColumnType.base[User, Int](
        u => u.id.get,
        i => User.getById(i).get
    )

    val Guest = new User(
        Some(-1),
        "Guest",
        Seq(),
        new Service(None, "beeminder", "", None),
        None)
}

class UserModel(tag: Tag) extends Table[User](tag, "User") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def goals = column[Seq[Goal]]("goals")
    def bee_service = column[Service]("bee_service")
    def fb_service = column[Option[Service]]("fb_service")

    val user = User.apply _
    def * = (id.?, username, goals, bee_service, fb_service) <> (user.tupled, User.unapply _)
}

