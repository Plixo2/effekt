package effekt

import effekt.context.Annotations
import effekt.typer.{CapabilityScope, GlobalCapabilityScope}

/**
 * Local annotations database, only used by Typer
 *
 * It is used to (1) model the typing context, (2) collect information
 * used for elaboration (i.e., capabilities), and (3) gather inferred
 * types for LSP support.
 *
 * (1) "Typing Context"
 * --------------------
 * Since symbols are unique, we can use mutable state instead of reader.
 * Typer uses local annotations that are immutable and can be backtracked.
 *
 * The "Typing Context" consists of:
 * - typing context for value types [[Annotations.ValueType]]
 * - typing context for block types [[Annotations.BlockType]]
 * - modalities on typing context for block symbol [[Annotations.Captures]]
 *
 * (2) Elaboration Info
 * --------------------
 * - [[Annotations.CapabilityReceiver]]
 * - [[Annotations.CapabilityArguments]]
 * - [[Annotations.BoundCapabilities]]
 * - [[Annotations.TypeArguments]]
 *
 * (3) Inferred Information for LSP
 * --------------------------------
 * We first store the inferred types here, before substituting and committing to the
 * global DB, later.
 * - [[Annotations.InferredValueType]]
 * - [[Annotations.InferredBlockType]]
 * - [[Annotations.InferredEffect]]
 */
final class AnnotationDB {
  var annotations: Annotations = Annotations.empty
  var capabilityScope: CapabilityScope = GlobalCapabilityScope
//


}
