package models

import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

object ServiceProvider extends Enumeration {
    type ServiceProvider = Value
    val Beeminder = Value("Beeminder")
    val Facebook = Value("Facebook")

    implicit def implicitProviderColumnMapper = MappedColumnType.base[ServiceProvider, String](
        sp => sp.toString,
        s => ServiceProvider.withName(s)
    )
}

case class Service(
        var id: Option[Int] = None,
        owner: User,
        provider: ServiceProvider.ServiceProvider,
        token: String,
        secret: String) {
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
}

object Service {
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
    import ServiceProvider._

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def owner = column[User]("owner")
    def provider = column[ServiceProvider]("provider")
    def token = column[String]("token")
    def secret = column[String]("secret")

    def service = Service.apply _
    def * = (id.?, owner, provider, token, secret) <> (service.tupled, Service.unapply _)
}

