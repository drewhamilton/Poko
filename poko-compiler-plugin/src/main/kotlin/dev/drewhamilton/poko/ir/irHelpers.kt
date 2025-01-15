package dev.drewhamilton.poko.ir

import dev.drewhamilton.poko.PokoAnnotationNames
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.source.getPsi

@UnsafeDuringIrConstructionAPI
internal fun IrClass.pokoProperties(
    pokoAnnotation: ClassId,
): List<IrProperty> {
    return properties
        .toList()
        .filter {
            val metadata = it.metadata
            if (metadata is FirMetadataSource.Property) {
                // Using K2:
                metadata.fir.source?.kind is KtFakeSourceElementKind.PropertyFromParameter
            } else {
                // Not using K2:
                @OptIn(ObsoleteDescriptorBasedAPI::class)
                it.symbol.descriptor.source.getPsi() is KtParameter
            }
        }
        .filter {
            !it.hasAnnotation(
                classId = pokoAnnotation.createNestedClassId(PokoAnnotationNames.Skip),
            )
        }
}
