package dev.drewhamilton.poko.ir

import dev.drewhamilton.poko.PokoFunction
import dev.drewhamilton.poko.fir.PokoKey
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.isEquals
import org.jetbrains.kotlin.ir.util.isHashCode
import org.jetbrains.kotlin.ir.util.isToString
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class PokoFunctionBodyFiller(
    private val pokoAnnotation: ClassId,
    private val context: IrPluginContext,
    private val messageCollector: MessageCollector,
) : IrElementVisitorVoid {

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        val origin = declaration.origin
        if (origin !is GeneratedByPlugin || !interestedIn(origin.pluginKey)) {
            return
        }

        require(declaration.body == null)

        val pokoFunction = when {
            declaration.isEquals() -> PokoFunction.Equals
            declaration.isHashCode() -> PokoFunction.HashCode
            declaration.isToString() -> PokoFunction.ToString
            else -> return
        }

        val pokoClass = declaration.parentAsClass
        val pokoProperties = pokoClass.pokoProperties(pokoAnnotation).also {
            if (it.isEmpty()) {
                messageCollector.log("No primary constructor properties")
                messageCollector.reportErrorOnClass(
                    irClass = pokoClass,
                    message = "Poko class primary constructor must have at least one not-skipped property",
                )
            }
        }

        declaration.body = DeclarationIrBuilder(
            generatorContext = context,
            symbol = declaration.symbol,
        ).irBlockBody {
            when (pokoFunction) {
                PokoFunction.Equals -> generateEqualsMethodBody(
                    pokoAnnotation = pokoAnnotation,
                    context = this@PokoFunctionBodyFiller.context,
                    irClass = pokoClass,
                    functionDeclaration = declaration,
                    classProperties = pokoProperties,
                    messageCollector = messageCollector,
                )

                PokoFunction.HashCode -> generateHashCodeMethodBody(
                    pokoAnnotation = pokoAnnotation,
                    context = this@PokoFunctionBodyFiller.context,
                    functionDeclaration = declaration,
                    classProperties = pokoProperties,
                    messageCollector = messageCollector,
                )

                PokoFunction.ToString -> generateToStringMethodBody(
                    pokoAnnotation = pokoAnnotation,
                    context = this@PokoFunctionBodyFiller.context,
                    irClass = pokoClass,
                    functionDeclaration = declaration,
                    classProperties = pokoProperties,
                    messageCollector = messageCollector,
                )
            }
        }
    }

    private fun interestedIn(
        key: GeneratedDeclarationKey?,
    ): Boolean {
        return key == PokoKey
    }

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration, is IrFile, is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> Unit
        }
    }
}
