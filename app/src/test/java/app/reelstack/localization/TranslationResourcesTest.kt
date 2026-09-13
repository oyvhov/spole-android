package app.reelstack.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class TranslationResourcesTest {
    private fun resources(directory: String): Map<String, Element> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        return File("src/main/res/$directory").listFiles().orEmpty().filter { it.extension == "xml" }
            .flatMap { file ->
                val root = factory.newDocumentBuilder().parse(file).documentElement
                (0 until root.childNodes.length).mapNotNull { root.childNodes.item(it) as? Element }
                    .filter { it.tagName in setOf("string", "plurals", "string-array") && it.getAttribute("translatable") != "false" }
            }.associateBy { it.getAttribute("name") }
    }

    @Test fun everyMigratedResourceHasBothLanguages() {
        AppLanguage.entries.filter { it != AppLanguage.SYSTEM && it != AppLanguage.ENGLISH }.forEach {
            assertEquals(it.tag, resources("values").keys, resources("values-b+${it.tag.replace('-', '+')}").keys)
        }
    }

    @Test fun translationsPreservePositionalArgumentsAndPluralForms() {
        val source = resources("values")
        val parameters = Regex("%[0-9]+\\$[sdf]")
        AppLanguage.entries.filter { it != AppLanguage.SYSTEM && it != AppLanguage.ENGLISH }.forEach { lang ->
            val translatedRes = resources("values-b+${lang.tag.replace('-', '+')}")
            source.forEach { (key, english) ->
                val translated = translatedRes.getValue(key)
                assertEquals("$key in ${lang.tag}", english.tagName, translated.tagName)
                assertEquals("$key parameters in ${lang.tag}",
                    parameters.findAll(english.textContent).map { it.value }.sorted().toList(),
                    parameters.findAll(translated.textContent).map { it.value }.sorted().toList())
                if (english.tagName == "plurals") {
                    fun quantities(element: Element): List<String> = (0 until element.childNodes.length)
                        .mapNotNull { element.childNodes.item(it) as? Element }.map { it.getAttribute("quantity") }.sorted()
                    assertEquals("$key quantities in ${lang.tag}", quantities(english), quantities(translated))
                }
                assertTrue("Blank translation: $key in ${lang.tag}", translated.textContent.isNotBlank())
            }
        }
    }
}
