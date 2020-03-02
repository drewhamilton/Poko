package dev.drewhamilton.careful.codegen

import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.JvmKotlinType
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.substitutedUnderlyingType
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

internal class EqualsGenerator(
    private val declaration: KtClassOrObject,
    private val classDescriptor: ClassDescriptor,
    private val classAsmType: Type,
    private val fieldOwnerContext: FieldOwnerContext<*>,
    private val v: ClassBuilder,
    private val generationState: GenerationState
) {
    private val typeMapper: KotlinTypeMapper = generationState.typeMapper
    private val underlyingType: JvmKotlinType

    // TODO: Not sure about this?
    private val equalsDesc: String
        get() = "(${firstParameterDesc}Ljava/lang/Object;)Z"

    private val firstParameterDesc: String
        get() {
            return if (fieldOwnerContext.contextKind == OwnerKind.ERASED_INLINE_CLASS)
                "${underlyingType.type.descriptor}, "
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

    fun generateEqualsMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
        val context = fieldOwnerContext.intoFunction(function)
        val methodOrigin = OtherOrigin(function)
        val equalsMethodName = mapFunctionName(function)
        val methodVisitor = v.newMethod(methodOrigin, access, equalsMethodName, equalsDesc, null, null)

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

        // Bytecode:
        //  @Lorg/jetbrains/annotations/Nullable;()
        visitEndForAnnotationVisitor(
            methodVisitor.visitAnnotation(Type.getDescriptor(Nullable::class.java), false)
        )

        if (!generationState.classBuilderMode.generateBodies) {
            FunctionCodegen.endVisit(methodVisitor, equalsMethodName, declaration)
            return
        }

        val instructionAdapter = InstructionAdapter(methodVisitor)
        methodVisitor.visitCode()

        // TODO: I probably need to use context more?

        // Bytecode: Load the receiver and the argument onto the stack
        //  ALOAD 0
        //  ALOAD 1
        instructionAdapter.load(0, classAsmType)
        instructionAdapter.load(1, classAsmType)

        // Bytecode: jump to L0 if the references are the same
        //  IF_ACMPEQ L0
        // FIXME: Need to do the label properly
        instructionAdapter.ifacmpeq(Label())

        // Bytecode: Load the argument back onto the stack
        //  ALOAD 1
        instructionAdapter.load(1, classAsmType)

        // Bytecode: Check that the argument is an instance of the same class
        //  INSTANCEOF <path/ClassName>
        instructionAdapter.instanceOf(classAsmType)

        // Bytecode: jump to L1 if the argument is the wrong class
        //  IFEQ L1
        // FIXME: Need to do the label properly
        instructionAdapter.ifeq(Label())

        // Bytecode: Load the argument back onto the stack
        //  ALOAD 1
        instructionAdapter.load(1, classAsmType)

        // Bytecode: Cast the argument to the known class and clear the stack
        //  CHECKCAST dev/drewhamilton/careful/sample/alt/DataSimple
        //  ASTORE 2
        instructionAdapter.checkcast(classAsmType)
        instructionAdapter.store(2, classAsmType)

        for (property in properties) {
            // Load the property from the receiver and the argument
            //  ALOAD 0
            //  GETFIELD path/Class.property : <type>
            //  ALOAD 2
            //  GETFIELD path/Class.property : <type>
            val type = genOrLoadOnStack(instructionAdapter, context, property, 0)
            genOrLoadOnStack(instructionAdapter, context, property, 2)

            // TODO: Now I need to do IF branches to L1 based on the type of the property
            AsmUtil.genEqualsForExpressionsOnStack(TODO(), TODO(), TODO())
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

        FunctionCodegen.endVisit(methodVisitor, equalsMethodName, declaration)
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
