package effekt.core

import effekt.context.Context

import scala.collection.mutable.ListBuffer

final class BindingDB {
  var bindings: ListBuffer[Binding] = ListBuffer()
}
