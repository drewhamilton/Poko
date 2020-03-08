package dev.drewhamilton.careful.codegen

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.JvmKotlinType
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.substitutedUnderlyingType
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

abstract class FunctionGenerator(
    protected val declaration: KtClassOrObject,
    protected val classDescriptor: ClassDescriptor,
    protected val classAsmType: Type,
    protected val fieldOwnerContext: FieldOwnerContext<*>,
    private val v: ClassBuilder,
    protected val generationState: GenerationState
) {
    protected val typeMapper: KotlinTypeMapper = generationState.typeMapper
    protected val underlyingType: JvmKotlinType

    protected abstract val methodDesc: String

    private val access: Int
        get() {
            var access = Opcodes.ACC_PUBLIC
            if (fieldOwnerContext.contextKind == OwnerKind.ERASED_INLINE_CLASS) {
                access = access or Opcodes.ACC_STATIC
            }

            return access
        }

    init {
        underlyingType = JvmKotlinType(
            typeMapper.mapType(classDescriptor),
            classDescriptor.defaultType.substitutedUnderlyingType()
        )
    }

    fun generate(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
        val context = fieldOwnerContext.intoFunction(function)
        val methodOrigin = OtherOrigin(function)
        val methodName = mapFunctionName(function)
        val methodVisitor = v.newMethod(methodOrigin, access, methodName, methodDesc, null, null)

        if (fieldOwnerContext.contextKind != OwnerKind.ERASED_INLINE_CLASS && classDescriptor.isInline) {
            FunctionCodegen.generateMethodInsideInlineClassWrapper(
                methodOrigin,
                function,
                classDescriptor,
                methodVisitor,
                typeMapper
            )
        } else {
            generateBytecode(function, properties, context, methodName, methodVisitor)
        }
    }

    protected abstract fun generateBytecode(
        function: FunctionDescriptor,
        properties: List<PropertyDescriptor>,
        context: MethodContext,
        methodName: String,
        methodVisitor: MethodVisitor
    )

    private fun mapFunctionName(functionDescriptor: FunctionDescriptor): String {
        return typeMapper.mapFunctionName(functionDescriptor, fieldOwnerContext.contextKind)
    }

    protected fun visitEndForAnnotationVisitor(annotation: AnnotationVisitor?) {
        annotation?.visitEnd()
    }

    protected fun genOrLoadOnStack(
        iv: InstructionAdapter,
        context: MethodContext,
        propertyDescriptor: PropertyDescriptor,
        index: Int
    ): JvmKotlinType {
        return if (fieldOwnerContext.contextKind == OwnerKind.ERASED_INLINE_CLASS) {
            iv.load(index, underlyingType.type)
            underlyingType
        } else {
            ImplementationBodyCodegen.genPropertyOnStack(
                iv,
                context,
                propertyDescriptor,
                classAsmType,
                index,
                generationState
            )
        }
    }
}
