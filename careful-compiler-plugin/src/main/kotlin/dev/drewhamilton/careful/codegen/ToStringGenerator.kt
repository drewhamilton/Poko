package dev.drewhamilton.careful.codegen

import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

internal class ToStringGenerator(
    declaration: KtClassOrObject,
    classDescriptor: ClassDescriptor,
    classAsmType: Type,
    fieldOwnerContext: FieldOwnerContext<*>,
    v: ClassBuilder,
    generationState: GenerationState
) : FunctionGenerator(declaration, classDescriptor, classAsmType, fieldOwnerContext, v, generationState) {

    override val methodDesc: String
        get() = "($firstParameterDesc)Ljava/lang/String;"

    override fun generateAnnotations(methodVisitor: MethodVisitor) {
        // Bytecode:
        //  @Lorg/jetbrains/annotations/NotNull;()
        visitEndForAnnotationVisitor(
            methodVisitor.visitAnnotation(Type.getDescriptor(NotNull::class.java), false)
        )
    }

    override fun generateBytecode(
        instructionAdapter: InstructionAdapter,
        function: FunctionDescriptor,
        properties: List<PropertyDescriptor>,
        context: MethodContext,
        methodName: String
    ) {
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
                    // Bytecode: Resolve special toString for arrays
                    //  INVOKESTATIC java/util/Arrays.toString ([Ljava/lang/Object;)Ljava/lang/String;
                    instructionAdapter.invokestatic(
                        "java/util/Arrays", "toString",
                        "([Ljava/lang/Object;)Ljava/lang/String;",
                        false
                    )
                    asmType = AsmTypes.JAVA_STRING_TYPE
                    kotlinType = function.builtIns.stringType
                } else if (elementType.sort != Type.CHAR) {
                    // Bytecode: Resolve special toString for arrays
                    //  INVOKESTATIC java/util/Arrays.toString ([<type>)Ljava/lang/String;
                    instructionAdapter.invokestatic(
                        "java/util/Arrays", "toString",
                        "(${asmType.descriptor})Ljava/lang/String;",
                        false
                    )
                    asmType = AsmTypes.JAVA_STRING_TYPE
                    kotlinType = function.builtIns.stringType
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
    }
}
