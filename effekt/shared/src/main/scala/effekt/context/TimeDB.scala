package effekt.context

import effekt.util.Timed

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

final class TimeDB {
  val times: mutable.LinkedHashMap[String, mutable.ListBuffer[Timed]] = mutable.LinkedHashMap.empty
  var timersActive: Boolean = true

  def update(timerName: String, time: Timed): Unit = {
    times.update(timerName, times.getOrElse(timerName, mutable.ListBuffer.empty).prepend(time))
  }

}
