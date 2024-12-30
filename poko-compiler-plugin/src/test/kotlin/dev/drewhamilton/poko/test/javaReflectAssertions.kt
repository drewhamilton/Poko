package dev.drewhamilton.poko.test

import assertk.Assert
import assertk.assertions.prop
import java.lang.reflect.Method
import java.lang.reflect.Parameter

internal fun Assert<Class<*>>.name() = prop("name") { it.name }

internal fun Assert<Method>.isSynthetic() = prop("isSynthetic") { it.isSynthetic }

internal fun Assert<Method>.parameters() = prop("parameters") { it.parameters }

internal fun Assert<Method>.returnType() = prop("returnType") { it.returnType }

internal fun Assert<Parameter>.type() = prop("type") { it.type }
