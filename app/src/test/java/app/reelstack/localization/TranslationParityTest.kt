package app.reelstack.localization

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Keeps Nynorsk and English from drifting apart.
 *
 * Roadmap 0.18 asks for automatic checks of missing translations, format arguments and plural
 * resources. Two of those fail silently in a way no screen test catches: a string added to one
 * locale and forgotten in the other simply falls back, and a mismatched format argument throws
 * `IllegalFormatException` at runtime — in the locale the developer was not looking at.
 *
 * The check reads the resource XML directly rather than the generated `R` class, because the point
 * is the source files being in step.
 */
class TranslationParityTest {

    private val defaultDir = resolve("src/main/res/values")
    private val nynorskDir = resolve("src/main/res/values-b+nn")

    /** Values that are deliberately English-only: colours, the app name, and other non-prose. */
    private val notTranslated = setOf("app_name")

    @Test
    fun `every English string has a Nynorsk counterpart`() {
        assumeTrue("Resource folders not reachable from the test working directory", defaultDir != null)
        val missing = (strings(defaultDir!!).keys - strings(nynorskDir!!).keys) - notTranslated
        assertTrue(
            "Desse strengane manglar nynorsk: ${missing.sorted()}",
            missing.isEmpty(),
        )
    }

    @Test
    fun `no Nynorsk string is left without an English original`() {
        assumeTrue("Resource folders not reachable", defaultDir != null)
        val orphans = strings(nynorskDir!!).keys - strings(defaultDir!!).keys
        assertTrue(
            "Desse finst berre på nynorsk: ${orphans.sorted()}",
            orphans.isEmpty(),
        )
    }

    /**
     * `%1$s` in one locale and `%s` — or nothing — in the other is a crash, not a typo. Comparing
     * the multiset of specifiers catches a dropped argument and a reordered one alike.
     */
    @Test
    fun `format arguments match between the two locales`() {
        assumeTrue("Resource folders not reachable", defaultDir != null)
        val english = strings(defaultDir!!)
        val nynorsk = strings(nynorskDir!!)
        val mismatched = english.keys.intersect(nynorsk.keys).filter { key ->
            specifiers(english.getValue(key)) != specifiers(nynorsk.getValue(key))
        }
        assertTrue(
            "Ulike formatparameter: " + mismatched.sorted().joinToString {
                "$it (${specifiers(english.getValue(it))} mot ${specifiers(nynorsk.getValue(it))})"
            },
            mismatched.isEmpty(),
        )
    }

    @Test
    fun `every plural resource exists in both locales with the same quantities`() {
        assumeTrue("Resource folders not reachable", defaultDir != null)
        val english = plurals(defaultDir!!)
        val nynorsk = plurals(nynorskDir!!)
        val missing = english.keys - nynorsk.keys
        assertTrue("Desse fleirtalsressursane manglar nynorsk: ${missing.sorted()}", missing.isEmpty())

        val differing = english.keys.intersect(nynorsk.keys)
            .filter { english.getValue(it) != nynorsk.getValue(it) }
        assertTrue(
            "Ulike mengdeformer: " + differing.sorted().joinToString {
                "$it (${english.getValue(it)} mot ${nynorsk.getValue(it)})"
            },
            differing.isEmpty(),
        )
    }

    // ── Lesing ───────────────────────────────────────────────────────────────

    private fun resolve(relative: String): File? =
        listOf(File(relative), File("app/$relative"), File("../app/$relative"))
            .firstOrNull { it.isDirectory }

    private fun strings(dir: File): Map<String, String> =
        dir.listFiles { file -> file.extension == "xml" }.orEmpty().flatMap { file ->
            STRING.findAll(file.readText()).map { it.groupValues[1] to it.groupValues[2] }
        }.toMap()

    /** Resource name to the set of quantities it declares. */
    private fun plurals(dir: File): Map<String, Set<String>> =
        dir.listFiles { file -> file.extension == "xml" }.orEmpty().flatMap { file ->
            PLURAL.findAll(file.readText()).map { match ->
                match.groupValues[1] to ITEM.findAll(match.groupValues[2])
                    .map { it.groupValues[1] }.toSet()
            }
        }.toMap()

    private fun specifiers(value: String): List<String> =
        FORMAT.findAll(value).map { it.value }.sorted().toList()

    private companion object {
        val STRING = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        val PLURAL = Regex("""<plurals name="([^"]+)"[^>]*>(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
        val ITEM = Regex("""<item quantity="([^"]+)"""")
        val FORMAT = Regex("""%(\d+\$)?[sdf]""")
    }
}
