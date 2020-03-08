package dev.drewhamilton.careful.codegen

import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

internal class EqualsGenerator(
    declaration: KtClassOrObject,
    classDescriptor: ClassDescriptor,
    classAsmType: Type,
    fieldOwnerContext: FieldOwnerContext<*>,
    v: ClassBuilder,
    generationState: GenerationState
) : FunctionGenerator(declaration, classDescriptor, classAsmType, fieldOwnerContext, v, generationState) {

    override val methodDesc: String
        get() = "(${firstParameterDesc}Ljava/lang/Object;)Z"

    override fun generateAnnotations(methodVisitor: MethodVisitor) {
        // Bytecode:
        //  @Lorg/jetbrains/annotations/Nullable;()
        visitEndForAnnotationVisitor(
            methodVisitor.visitAnnotation(Type.getDescriptor(Nullable::class.java), false)
        )
    }

    override fun generateBytecode(
        instructionAdapter: InstructionAdapter,
        function: FunctionDescriptor,
        properties: List<PropertyDescriptor>,
        context: MethodContext,
        methodName: String
    ) {
        if (!generationState.classBuilderMode.generateBodies) {
            FunctionCodegen.endVisit(instructionAdapter, methodName, declaration)
            return
        }

        val l0 = Label()
        val l1 = Label()

        // Bytecode: Load the receiver and the argument onto the stack
        //  ALOAD 0
        //  ALOAD 1
        instructionAdapter.load(0, classAsmType)
        instructionAdapter.load(1, classAsmType)

        // Bytecode: jump to L0 if the references are the same
        //  IF_ACMPEQ L0
        instructionAdapter.ifacmpeq(l0)

        // Bytecode: Load the argument back onto the stack
        //  ALOAD 1
        instructionAdapter.load(1, classAsmType)

        // Bytecode: Check that the argument is an instance of the same class
        //  INSTANCEOF <path/ClassName>
        instructionAdapter.instanceOf(classAsmType)

        // Bytecode: jump to L1 if the argument is the wrong class
        //  IFEQ L1
        instructionAdapter.ifeq(l1)

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
            val leftType = genOrLoadOnStack(instructionAdapter, context, property, 0)
            val rightType = genOrLoadOnStack(instructionAdapter, context, property, 2)

            if (AsmUtil.isPrimitive(leftType.type) && leftType.type == rightType.type) {
                // Bytecode: If ints are not equals, branch to L1
                //  IF_ICMPNE L1
                instructionAdapter.ificmpne(l1)
            } else {
                // Bytecode: If objects are not equals, branch to L1
                //  INVOKESTATIC kotlin/jvm/internal/Intrinsics.areEqual (Ljava/lang/Object;Ljava/lang/Object;)Z
                //  IFEQ L1
                AsmUtil.genAreEqualCall(instructionAdapter)
                instructionAdapter.ifeq(l1)
            }
        }

        // Bytecode: L0 (return true)
        //  ICONST_1
        //  IRETURN
        instructionAdapter.visitLabel(l0)
        instructionAdapter.iconst(1)
        // TODO: IRETURN
        instructionAdapter.areturn(Type.INT_TYPE)

        // Bytecode: L1 (return false)
        //  ICONST_0
        //  IRETURN
        instructionAdapter.visitLabel(l1)
        instructionAdapter.iconst(0)
        // TODO: IRETURN
        instructionAdapter.areturn(Type.INT_TYPE)
    }
}
