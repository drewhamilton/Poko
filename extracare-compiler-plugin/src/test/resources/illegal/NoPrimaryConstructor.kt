package illegal

import dev.drewhamilton.extracare.DataApi

@Suppress("unused")
@DataApi class NoPrimaryConstructor {
    @Suppress("ConvertSecondaryConstructorToPrimary", "UNUSED_PARAMETER")
    constructor(string: String)
}
