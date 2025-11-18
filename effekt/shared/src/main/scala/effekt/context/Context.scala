package effekt
package context

import effekt.core.BindingDB
import effekt.namer.NamerOps
import effekt.source.Tree
import effekt.symbols.Module
import effekt.typer.Unification
import effekt.util.{Timed, TimerOps}
import effekt.util.messages.{EffektMessages, ErrorReporter}

/**
 * Phases like Typer can add operations to the context by extending this trait
 *
 * For example, see TyperOps
 */
trait ContextOps
    extends ErrorReporter
    with TreeAnnotations
    with SourceAnnotations 
    with SymbolAnnotations
    with Unification { self: Context => }

/**
 * The compiler context consists of
 * - configuration (immutable)
 * - symbols (mutable database)
 * - types (mutable database)
 * - error reporting (mutable focus)
 */
abstract class Context
    extends ModuleDB
    with ContextOps {

  // bring the context itself in scope
  implicit val context: Context = this

  val timeDB: TimeDB = new TimeDB
  var bindingDB: BindingDB = new BindingDB
  var annotationDB: AnnotationDB = new AnnotationDB
  val scopeDB: ScopeDB = new ScopeDB

  // the currently processed module
  var module: Module = _

  // the currently processed node
  var focus: Tree = _

  var _config: EffektConfig = _
  def config = _config

  // cache used by tasks to save their results (in addition to information in the AnnotationsDB)
  var cache: util.Task.Cache = util.Task.emptyCache

  // We assume the backend never changes
  lazy val backend = config.backend()
  lazy val compiler = backend.compiler
  lazy val runner = backend.runner

  /**
   * Clear current context to start processing a fresh unit
   */
  def setup(cfg: EffektConfig): Unit = {
    messaging.clear()
    // No timings are captured in server mode to keep the memory footprint small. Since the server is run continuously,
    // the memory claimed by the timing information would increase continuously.
    TimerOps.clearTimers(cfg.timed())(using this)
    _config = cfg
  }

  def using[T](module: Module = module, focus: Tree = focus)(block: => T): T = this in {
    this.module = module
    this.focus = focus
    block
  }

  /**
   * Time the execution of `f` and save the result in the times database under the "category" `timerName`
   * and the event `id`.
   */
  def timed[A](timerName: String, id: String)(f: => A): A = {
    if (!timeDB.timersActive) return f
    val (res, duration) = timed(f)
    timeDB.update(timerName, Timed(id, duration))
    res
  }

  /**
   * Convenience function for timing the execution of a given function.
   */
  private def timed[A](f: => A): (A, Double) = {
    val start = System.nanoTime()
    val res = f
    val duration = (System.nanoTime() - start) * 1e-6
    (res, duration)
  }


  /**
   *
   * Used throughout the compiler to create a new "scope"
   *
   * Each XOps slice can define what a new "scope" means and
   * backup and restore state accordingly. Overriding definitions
   * should call `super.in(block)`.
   *
   * This is useful to write code like: reporter in { ... implicitly uses reporter ... }
   */
  def in[T](block: => T): T = {
    val focusBefore = focus
    val moduleBefore = module
    val scopeBefore = scopeDB.scope
    val result = block

    // we purposefully do not include the reset into `finally` to preserve the
    // state at the error position
    focus = focusBefore
    module = moduleBefore
    scopeDB.scope = scopeBefore
    result
  }

  // temporarily switches the message buffer to collect messages
  def withMessages[T](block: => T): (EffektMessages, T) = {
    val bufferBefore = messaging.buffer

    messaging.clear()

    val res = block
    val msgs = messaging.buffer
    messaging.buffer = bufferBefore
    (msgs, res)
  }

  /**
   * The compiler state
   */
  case class State(annotations: DB, cache: util.Task.Cache)

  /**
   * Export the compiler state
   */
  def backup: State = State(this.db, this.cache)

  /**
   * Restores the compiler state from a backup
   */
  def restore(s: State) = {
    db = s.annotations
    cache = s.cache
  }
}

/**
 * Helper method to find the currently implicit context
 */
def Context(using C: Context): C.type = C
