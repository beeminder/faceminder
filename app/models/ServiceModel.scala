package models

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory

case class Service(
        var id: Option[Int] = None,
        owner: User,
        provider: String,
        var token: String,
        var expiry: DateTime) {

    // make sure we have a valid provider
    // TODO(sandy): this should be smarter than it is.
    provider match {
        case "facebook" => // do nothing
        case "beeminder" => // do nothing
        case _ => throw new IllegalArgumentException
    }

    private val Table = TableQuery[ServiceModel]
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

    def refresh() = {
        if (provider == "facebook") {
           // TODO(sandy): make this invalidate the token if it fails
            val authToken = Service.facebook.refreshToken(token).get
            token = authToken.token
            expiry = authToken.expiry
            save()
        }
    }
}

object Service {
    val conf = ConfigFactory.load

    val facebook = new oauth2.FaceBook(
        conf.getString("facebook.appID"),
        conf.getString("facebook.secret")
    )

    val beeminder = new oauth2.Beeminder(
        conf.getString("beeminder.appID"),
        conf.getString("beeminder.secret")
    )

    private val Table = TableQuery[ServiceModel]
    def getById(id: Int): Option[Service] = {
        DB.withSession { implicit session =>
            Table.filter(_.id === id).firstOption
        }
    }

    implicit def implicitServiceColumnMapper = MappedColumnType.base[Option[Service], Int](
        os => os match {
            case Some(s) => s.id.get
            case None => -1
        },

        i => i match {
            case -1 => None
            case id => Some(Service.getById(id).get)
        }
    )
}


class ServiceModel(tag: Tag) extends Table[Service](tag, "Service") {
    import utils.DateConversions._

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def owner = column[User]("owner")
    def provider = column[String]("provider")
    def token = column[String]("token")
    def expiry = column[DateTime]("expiry")

    def service = Service.apply _
    def * = (
        id.?,
        owner,
        provider,
        token,
        expiry
    ) <> (service.tupled, Service.unapply _)
}

