package effekt.util

import effekt.context.Context

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.AnsiColor.*
case class Timed(name: String, time: Double)

/**
 * Trait for timing events. Using the `timed` function, a new event can be timed.
 * The result is saved in map under a specified category with a unique identifier.
 */
object TimerOps {

  private def totalTime(using Context): Double = {
    val times = Context.timeDB.times
    times.get("total").map(_.head.time).getOrElse {
      times.foldLeft(0d) { (acc, values) =>
        acc + values._2.foldLeft(0d)((acc, timed) => acc + timed.time)
      }
    }
  }

  def clearTimers(active: Boolean)(using Context): Unit = {
    Context.timeDB.times.clear()
    Context.timeDB.timersActive = active
  }

  def timersActive(using Context): Boolean = Context.timeDB.timersActive


  def timesToString()(using Context): String = {
    val times = Context.timeDB.times

    val spacetab = " ".repeat(4)
    times.zipWithIndex.map { case ((name, ts), i) =>
      val totalsubtime = ts.foldLeft(0d)((acc, timed) => acc + timed.time)
      val subs = ts.map { case Timed(subname, time) =>
        val subname1 = if (subname.isEmpty) "<repl>" else subname
        f"$subname1: ${time}%.2f ms"
      }.mkString(spacetab, s",\n$spacetab", "")
      f"""$UNDERLINED$BOLD${i + 1}. $name$RESET:
         |$subs
         |$spacetab${UNDERLINED}Total$RESET: $totalsubtime%.2f ms
         |$spacetab${UNDERLINED}Percentage$RESET: ${(totalsubtime / totalTime) * 100}%.2f %%
         |""".stripMargin
    }.mkString("")
  }

  private def withENLocale[T](p: => T): T = {
    val locale = java.util.Locale.getDefault
    java.util.Locale.setDefault(java.util.Locale.US)
    val result = p
    java.util.Locale.setDefault(locale)
    result
  }

  def timesToJSON()(using Context): String = withENLocale {
    val times = Context.timeDB.times
    val spacetab = " ".repeat(4)
    val entries = times.map { (name, ts) =>
      val subs = ts.map { case Timed(subname, time) =>
        val subname1 = if (subname.isEmpty) "<repl>" else subname
        f"\"$subname1\": $time%.2f"
      }.mkString(spacetab.repeat(2), s",\n${spacetab.repeat(2)}", "")
      s"$spacetab\"$name\": {\n$subs\n$spacetab}"
    }.mkString(",\n")
    s"{\n$entries\n}\n"
  }
}
