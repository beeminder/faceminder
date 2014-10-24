package models

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory

case class Service(
        var id: Option[Int] = None,
        provider: String,
        var token: String,
        var expiry: Option[DateTime]) {

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
            expiry = Some(authToken.expiry)
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

    def getByProvider(provider: String): Seq[Service] = {
        DB.withSession { implicit session =>
            Table.filter(_.provider === provider).list
        }
    }

    implicit def implicitServiceColumnMapper = MappedColumnType.base[Service, Int](
        s => s.id.get,
        i => Service.getById(i).get
    )
}


class ServiceModel(tag: Tag) extends Table[Service](tag, "Service") {
    import utils.DateConversions._

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def provider = column[String]("provider")
    def token = column[String]("token")
    def expiry = column[Option[DateTime]]("expiry")

    def service = Service.apply _
    def * = (
        id.?,
        provider,
        token,
        expiry
    ) <> (service.tupled, Service.unapply _)
}

