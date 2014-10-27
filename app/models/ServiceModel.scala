package models

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory

import utils.Flyweight

// You should never call this constructor by hand; instead call Service.create
case class Service(
        id: Int,
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

    def save() = {
        DB.withSession { implicit session =>
            TableQuery[ServiceModel].filter(_.id === id).update(this)
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

    def reload() = {
        // TODO(sandy): this can fail if the object is deleted
        val reloaded = Service.rawGet(id).get
        token = reloaded.token
        expiry = reloaded.expiry
    }
}

object Service extends Flyweight {
    type T = Service
    type Key = Int
    private val Table = TableQuery[ServiceModel]

    def create(_1: String, _2: String, _3: Option[DateTime]) = {
        getById(
            DB.withSession { implicit session =>
                (Table returning Table.map(_.id)) +=
                    new Service(0, _1, _2, _3)
            }
        ).get
    }

    // TODO(sandy): these don't really belong here
    private val conf = ConfigFactory.load
    val facebook = new oauth2.FaceBook(
        conf.getString("facebook.appID"),
        conf.getString("facebook.secret")
    )

    val beeminder = new oauth2.Beeminder(
        conf.getString("beeminder.appID"),
        conf.getString("beeminder.secret")
    )


    def rawGet(id: Int): Option[Service] = {
        DB.withSession { implicit session =>
            Table.filter(_.id === id).firstOption
        }
    }


    object unmanaged {
        def getByProvider(provider: String): Seq[Service] = {
            DB.withSession { implicit session =>
                Table.filter(_.provider === provider).list
            }
        }
    }


    implicit def implicitServiceColumnMapper = MappedColumnType.base[Service, Int](
        s => s.id,
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
        id,
        provider,
        token,
        expiry
    ) <> (service.tupled, Service.unapply _)
}

