package models

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import com.github.nscala_time.time.Imports._

import oauth2.ServiceProvider

import utils.Flyweight

// You should never call this constructor by hand; instead call Service.create
case class Service(
        id: Int,
        provider: String,
        // NOTE(sandy): This ownerId will NEVER BE RIGHT for beeminder services,
        // mostly because I am a crap programmer. Don't call service.owner on
        // beeminder services or you will be sad.
        ownerId: Int,
        // NOTE: This is the service's identifier for this user, not anything
        // that might make sense for our purposes on Faceminder except when
        // calling external stuff.
        username: String,
        var token: String,
        var expiry: Option[DateTime]) {

    // Make sure we have a valid provider
    provider match {
        case "facebook" =>
        case "beeminder" =>
        case _ => throw new IllegalArgumentException
    }

    lazy val owner = User.getById(ownerId).get


    def save() = {
        DB.withSession { implicit session =>
            TableQuery[ServiceModel].filter(_.id === id).update(this)
        }
    }

    object unmanaged {
        def delete() = {
            DB.withSession { implicit session =>
                TableQuery[ServiceModel].filter(_.id === id).delete
            }
        }
    }

    def refresh(): Boolean = {
        if (provider == "facebook") {
            // Try connecting to Facebook 3 times
            (1 to 3).view
                    .map(_ => ServiceProvider.facebook.refreshToken(token))
                    .find(_.isDefined) match {
                case Some(Some(authToken)) => {
                    token = authToken.token
                    expiry = Some(authToken.expiry)
                    save()

                    true
                }

                case _ => false
            }
        } else true
    }

    def reload() = {
        val reloaded = Service.rawGet(id).get
        token = reloaded.token
        expiry = reloaded.expiry
    }
}

object Service extends Flyweight {
    type T = Service
    type Key = Int
    private val Table = TableQuery[ServiceModel]

    def create(_1: String, _2: Int, _3: String, _4: String, _5: Option[DateTime]) = {
        getById(
            DB.withSession { implicit session =>
                (Table returning Table.map(_.id)) +=
                    new Service(0, _1, _2, _3, _4, _5)
            }
        ).get
    }

    def rawGet(id: Int): Option[Service] = {
        DB.withSession { implicit session =>
            Table.filter(_.id === id).firstOption
        }
    }


    def getByProvider(provider: String): Seq[Service] = {
        DB.withSession { implicit session =>
            Table.filter(_.provider === provider).list
        } map { service =>
            getById(service.id).get
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
    def ownerId = column[Int]("owner")
    def username = column[String]("username")
    def token = column[String]("token")
    def expiry = column[Option[DateTime]]("expiry")

    def service = Service.apply _
    def * = (
        id,
        provider,
        ownerId,
        username,
        token,
        expiry
    ) <> (service.tupled, Service.unapply _)
}

