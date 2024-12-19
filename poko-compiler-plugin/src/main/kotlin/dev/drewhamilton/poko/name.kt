package dev.drewhamilton.poko

import org.jetbrains.kotlin.name.Name

internal fun Name.unSpecial(): Name {
    require(isSpecial) { "Can't un-special a name that is already not special" }
    return Name.identifier(asStringStripSpecialMarkers())
}
