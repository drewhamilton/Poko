package dev.drewhamilton.poko.ir

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

internal object PokoOrigin : IrDeclarationOrigin, ReadOnlyProperty<Any?, PokoOrigin> {
    override val name: String = "GENERATED_POKO_CLASS_MEMBER"

    override fun toString(): String = name

    override fun getValue(thisRef: Any?, property: KProperty<*>): PokoOrigin = this
}
