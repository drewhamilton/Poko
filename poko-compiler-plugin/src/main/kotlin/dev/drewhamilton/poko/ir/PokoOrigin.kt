package dev.drewhamilton.poko.ir

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

internal object PokoOrigin : IrDeclarationOrigin, ReadOnlyProperty<Any?, PokoOrigin> {
    override val name: String = "GENERATED_POKO_CLASS_MEMBER"

    // TODO: Remove when support for Kotlin 1.9 is dropped
    override val isSynthetic: Boolean
        get() = false

    override fun toString(): String = name

    override fun getValue(thisRef: Any?, property: KProperty<*>): PokoOrigin = this
}
