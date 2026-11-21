package illegal

import dev.drewhamilton.poko.Poko

@Suppress("unused")
@Poko class <!PrimaryConstructorPropertiesRequired, PrimaryConstructorRequired!>NoPrimaryConstructor<!> {
    @Suppress("ConvertSecondaryConstructorToPrimary", "UNUSED_PARAMETER")
    constructor(string: String)
}
