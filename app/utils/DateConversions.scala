package utils

import com.github.nscala_time.time.Imports._
import play.api.db.slick.Config.driver.simple._

object DateConversions {
    implicit def dateTimeSlick  =
      MappedColumnType.base[DateTime, Long](
        dt => dt.getMillis,
        ts => new DateTime(ts)
    )
}
