package dev.drewhamilton.poko

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class PokoCompilerPluginTest {
    @OptIn(RedactSupport::class)
    @Test fun `plugin default mask matches Redact annotation default`() {
        assertThat(Poko.Redact().mask).isEqualTo(PokoAnnotationDefaults.REDACT_MASK)
    }

    @Test fun `fir IDE mode option parses all values`() {
        val commandLineProcessor = PokoCommandLineProcessor()
        val option = commandLineProcessor.pluginOptions.single {
            it.optionName == CompilerOptions.FIR_IDE_MODE.toString()
        }

        FirIdeMode.entries.forEach { firIdeMode ->
            val configuration = createCompilerConfiguration()
            commandLineProcessor.processOption(option, firIdeMode.name, configuration)

            assertThat(configuration.get(CompilerOptions.FIR_IDE_MODE)).isEqualTo(firIdeMode)
        }
    }
}
