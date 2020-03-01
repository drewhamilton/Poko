package dev.drewhamilton.careful.codegen

import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.codegen.AsmUtil
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
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.substitutedUnderlyingType
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

internal class ToStringGenerator(
    private val declaration: KtClassOrObject,
    private val classDescriptor: ClassDescriptor,
    private val classAsmType: Type,
    private val fieldOwnerContext: FieldOwnerContext<*>,
    private val v: ClassBuilder,
    private val generationState: GenerationState
) {
    private val typeMapper: KotlinTypeMapper = generationState.typeMapper
    private val underlyingType: JvmKotlinType

    private val toStringDesc: String
        get() = "($firstParameterDesc)Ljava/lang/String;"

    private val firstParameterDesc: String
        get() {
            return if (fieldOwnerContext.contextKind == OwnerKind.ERASED_INLINE_CLASS)
                underlyingType.type.descriptor
            else
                ""
        }

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

    fun generateToStringMethod(function: FunctionDescriptor?, properties: List<PropertyDescriptor>) {
        // TODO: Figure out null case, which is probably the typical case
        function!!

        val context = fieldOwnerContext.intoFunction(function)
        val methodOrigin = OtherOrigin(function)
        val toStringMethodName = mapFunctionName(function)
        val methodVisitor = v.newMethod(methodOrigin, access, toStringMethodName, toStringDesc, null, null)

        if (fieldOwnerContext.contextKind != OwnerKind.ERASED_INLINE_CLASS && classDescriptor.isInline) {
            FunctionCodegen.generateMethodInsideInlineClassWrapper(
                methodOrigin,
                function,
                classDescriptor,
                methodVisitor,
                typeMapper
            )
            return
        }

        visitEndForAnnotationVisitor(
            methodVisitor.visitAnnotation(Type.getDescriptor(NotNull::class.java), false)
        )

        if (!generationState.classBuilderMode.generateBodies) {
            FunctionCodegen.endVisit(methodVisitor, toStringMethodName, declaration)
            return
        }

        val instructionAdapter = InstructionAdapter(methodVisitor)
        methodVisitor.visitCode()

        // Bytecode: Create a StringBuilder to build the instance's string
        //  NEW java/lang/StringBuilder
        //  DUP
        //  INVOKESPECIAL java/lang/StringBuilder.<init> ()V
        AsmUtil.genStringBuilderConstructor(instructionAdapter)

        var first = true
        for (property in properties) {
            val propertyName = property.name.asString()
            val prefix = if (first) "${classDescriptor.name}(" else ", "
            // Bytecode: Create static text
            //  LDC "ClassName(property1="
            // or
            //  LDC ", property2="
            instructionAdapter.aconst("$prefix$propertyName=")
            first = false

            // Bytecode: Append previously created static text to the StringBuilder
            //  INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
            AsmUtil.genInvokeAppendMethod(instructionAdapter, AsmTypes.JAVA_STRING_TYPE, null)

            // Bytecode: Load the property's value
            //  ALOAD 0
            //  GETFIELD package/name/ClassName.propertyName : <type>
            val type = genOrLoadOnStack(instructionAdapter, context, property, 0)
            var asmType = type.type
            var kotlinType = type.kotlinType

            // Arrays require special handling
            if (asmType.sort == Type.ARRAY) {
                val elementType = AsmUtil.correctElementType(asmType)
                if (elementType.sort == Type.OBJECT || elementType.sort == Type.ARRAY) {
                    instructionAdapter.invokestatic(
                        "java/util/Arrays", "toString",
                        "([Ljava/lang/Object;)Ljava/lang/String;",
                        false
                    )
                    asmType = AsmTypes.JAVA_STRING_TYPE
                    kotlinType = function.builtIns.stringType
                } else if (elementType.sort != Type.CHAR) {
                    instructionAdapter.invokestatic(
                        "java/util/Arrays", "toString",
                        "(${asmType.descriptor})Ljava/lang/String;",
                        false
                    )
                    asmType = AsmTypes.JAVA_STRING_TYPE
                    kotlinType = function.builtIns
                        .stringType
                }
            }

            // Bytecode: Append the property's value to the StringBuilder
            //  INVOKEVIRTUAL java/lang/StringBuilder.append (<type>)Ljava/lang/StringBuilder;
            AsmUtil.genInvokeAppendMethod(instructionAdapter, asmType, kotlinType, typeMapper)
        }

        // Bytecode: Create static text (a single character in this case)
        //  BIPUSH 41
        instructionAdapter.aconst(")")
        // Bytecode: Append character to StringBuilder
        //  INVOKEVIRTUAL java/lang/StringBuilder.append (C)Ljava/lang/StringBuilder;
        AsmUtil.genInvokeAppendMethod(instructionAdapter, AsmTypes.JAVA_STRING_TYPE, null)

        // Bytecode: Build the string
        //  INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
        instructionAdapter.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        // Bytecode: return
        //  ARETURN
        instructionAdapter.areturn(AsmTypes.JAVA_STRING_TYPE)

        FunctionCodegen.endVisit(methodVisitor, toStringMethodName, declaration)
    }

    private fun mapFunctionName(functionDescriptor: FunctionDescriptor): String {
        return typeMapper.mapFunctionName(functionDescriptor, fieldOwnerContext.contextKind)
    }

    private fun visitEndForAnnotationVisitor(annotation: AnnotationVisitor?) {
        annotation?.visitEnd()
    }

    @Suppress("SameParameterValue")
    private fun genOrLoadOnStack(
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
