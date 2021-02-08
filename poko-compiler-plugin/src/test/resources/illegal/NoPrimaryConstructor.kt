package illegal

import dev.drewhamilton.poko.Poko

@Suppress("unused")
@Poko class NoPrimaryConstructor {
    @Suppress("ConvertSecondaryConstructorToPrimary", "UNUSED_PARAMETER")
    constructor(string: String)
}
