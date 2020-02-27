package dev.drewhamilton.safe.gradle

import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor

interface VariantFilter {
    /**
     * Overrides whether or not to enable this particular variant. Default is whatever is declared in
     * the extension.
     */
    fun overrideEnabled(enabled: Boolean)

    /**
     * Returns the Build Type.
     */
    val buildType: BuildType

    /**
     * Returns the list of flavors, or an empty list.
     */
    val flavors: List<ProductFlavor>

    /**
     * Returns the unique variant name.
     */
    val name: String?
}
