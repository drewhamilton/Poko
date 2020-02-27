package dev.drewhamilton.safe.gradle

import org.gradle.api.Action

open class SafePluginExtension {
    var enabled: Boolean = true
    internal var variantFilter: Action<VariantFilter>? = null

    /**
     * Applies a variant filter for Android.
     *
     * @param action the configure action for the [VariantFilter]
     */
    fun androidVariantFilter(action: Action<VariantFilter>) {
        this.variantFilter = action
    }
}
