package app.reelstack.update

import org.junit.Assert.*
import org.junit.Test

class ReleaseNotesTest {
    @Test fun headingsBulletsAndEmphasisAreReadableWithoutMarkdownSyntax() {
        val blocks = releaseNoteBlocks("# Nytt\n\n- **Betre** avspeling\n- `AC3` og *tekst*\n\nVanleg tekst\npå to linjer.")
        assertEquals(listOf(ReleaseNoteBlock("Nytt", heading = true),
            ReleaseNoteBlock("Betre avspeling", bullet = true), ReleaseNoteBlock("AC3 og tekst", bullet = true),
            ReleaseNoteBlock("Vanleg tekst på to linjer.")), blocks)
    }
    @Test fun linksAndCodeKeepTheirContentsAndBlankNotesStayEmpty() {
        assertEquals("Kjelde (https://example.com)", releaseNoteBlocks("[Kjelde](https://example.com)").single().text)
        assertEquals("file_name **literal**", releaseNoteBlocks("```\nfile_name **literal**\n```").single().text)
        assertTrue(releaseNoteBlocks("\n\n---").isEmpty())
        assertEquals("file_name", releaseNoteBlocks("file_name").single().text)
    }
}
