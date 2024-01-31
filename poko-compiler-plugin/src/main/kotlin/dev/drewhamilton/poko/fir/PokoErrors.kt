package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0

// TODO: Custom errors https://youtrack.jetbrains.com/issue/KT-53510
internal object PokoErrors {
  val POKO_ON_NON_CLASS_ERROR by error0<PsiElement>(
    positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
  )

  val POKO_ON_DATA_CLASS_ERROR by error0<PsiElement>(
    positioningStrategy = SourceElementPositioningStrategies.DATA_MODIFIER,
  )

  val POKO_ON_VALUE_CLASS_ERROR by error0<PsiElement>(
    positioningStrategy = SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER,
  )

  val POKO_ON_INNER_CLASS_ERROR by error0<PsiElement>(
    positioningStrategy = SourceElementPositioningStrategies.INNER_MODIFIER,
  )

  val POKO_REQUIRES_PRIMARY_CONSTRUCTOR_ERROR by error0<PsiElement>(
    positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
  )

  val POKO_REQUIRES_PRIMARY_CONSTRUCTOR_PROPERTIES_ERROR by error0<PsiElement>(
    positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
  )
}
