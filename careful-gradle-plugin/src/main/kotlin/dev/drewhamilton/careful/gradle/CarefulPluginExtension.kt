package dev.drewhamilton.careful.gradle

import org.gradle.api.Action

open class CarefulPluginExtension {
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
