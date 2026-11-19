package com.example.qrvault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardTest {

    @Test
    fun buildThenParse_roundTripsAllFields() {
        val text = VCard.build("Asha Rao", listOf("+91 98765 43210", "080-2345"), listOf("asha@example.com"))
        val parsed = VCard.parse(text)!!
        assertEquals("Asha Rao", parsed.name)
        assertEquals(listOf("+91 98765 43210", "080-2345"), parsed.phones)
        assertEquals(listOf("asha@example.com"), parsed.emails)
    }

    @Test
    fun specialCharacters_areEscapedAndRestored() {
        val name = "Rao, Asha; \"Dev\\Ops\""
        val text = VCard.build(name, emptyList(), emptyList())
        assertTrue(text.contains("FN:Rao\\, Asha\\; \"Dev\\\\Ops\""))
        assertEquals(name, VCard.parse(text)!!.name)
    }

    @Test
    fun parse_readsThirdPartyVCardWithFoldingAndGroups() {
        val text = "BEGIN:VCARD\r\nVERSION:3.0\r\nN:Doe;John;;;\r\n" +
            "item1.TEL;type=CELL:+1 555\r\n 0100\r\nEMAIL;TYPE=WORK:j@d.com\r\nEND:VCARD"
        val parsed = VCard.parse(text)!!
        assertEquals("John Doe", parsed.name)          // no FN, falls back to N (given + family)
        assertEquals(listOf("+1 5550100"), parsed.phones)
        assertEquals(listOf("j@d.com"), parsed.emails)
    }

    @Test
    fun parse_rejectsNonVCardText() {
        assertNull(VCard.parse("https://example.com"))
        assertNull(VCard.parse("BEGIN:VCARD\nVERSION:3.0\nEND:VCARD"))
    }
}
