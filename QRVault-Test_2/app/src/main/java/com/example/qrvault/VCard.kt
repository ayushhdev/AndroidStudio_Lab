package com.example.qrvault

/**
 * Builds and parses the vCard 3.0 text that QRVault encodes into its QR codes.
 * Any phone camera or QR app recognises a vCard and offers to save it as a contact.
 */
object VCard {

    data class Parsed(val name: String, val phones: List<String>, val emails: List<String>)

    fun build(name: String, phones: List<String>, emails: List<String>): String = buildString {
        append("BEGIN:VCARD\n")
        append("VERSION:3.0\n")
        append("N:;").append(escape(name)).append(";;;\n")
        append("FN:").append(escape(name)).append('\n')
        phones.forEach { append("TEL;TYPE=CELL:").append(escape(it)).append('\n') }
        emails.forEach { append("EMAIL;TYPE=INTERNET:").append(escape(it)).append('\n') }
        append("END:VCARD")
    }

    /** Returns the contact in [text], or null if it is not a vCard (e.g. a URL). */
    fun parse(text: String): Parsed? {
        if (!text.trimStart().startsWith("BEGIN:VCARD", ignoreCase = true)) return null

        // Unfold continuation lines (RFC 6350 §3.2): a line starting with a space continues the previous one.
        val lines = mutableListOf<String>()
        text.split(Regex("\r?\n")).forEach { line ->
            if ((line.startsWith(" ") || line.startsWith("\t")) && lines.isNotEmpty()) {
                lines[lines.lastIndex] = lines.last() + line.substring(1)
            } else if (line.isNotBlank()) {
                lines += line
            }
        }

        var fullName = ""
        var structuredName = ""
        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        for (line in lines) {
            val colon = line.indexOf(':')
            if (colon <= 0) continue
            // "item1.TEL;TYPE=CELL" -> "TEL"
            val property = line.substring(0, colon).substringBefore(';').substringAfterLast('.').uppercase()
            val value = line.substring(colon + 1)
            when (property) {
                "FN" -> fullName = unescape(value).trim()
                "N" -> structuredName = splitUnescaped(value)
                    .let { parts -> listOf(parts.getOrNull(1), parts.getOrNull(0)) }
                    .filterNot { it.isNullOrBlank() }.joinToString(" ").trim()
                "TEL" -> unescape(value).trim().takeIf { it.isNotEmpty() }?.let(phones::add)
                "EMAIL" -> unescape(value).trim().takeIf { it.isNotEmpty() }?.let(emails::add)
            }
        }
        val name = fullName.ifEmpty { structuredName }
        if (name.isEmpty() && phones.isEmpty() && emails.isEmpty()) return null
        return Parsed(name, phones, emails)
    }

    // vCard text values escape backslash, comma, semicolon and newline.
    private fun escape(value: String) = value
        .replace("\\", "\\\\")
        .replace(",", "\\,")
        .replace(";", "\\;")
        .replace("\r\n", "\\n").replace("\n", "\\n")

    private fun unescape(value: String): String {
        val out = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                val next = value[i + 1]
                out.append(if (next == 'n' || next == 'N') '\n' else next)
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    /** Splits a structured value on unescaped ';' and unescapes each component. */
    private fun splitUnescaped(value: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            when {
                c == '\\' && i + 1 < value.length -> { current.append(c).append(value[i + 1]); i += 2; continue }
                c == ';' -> { parts += unescape(current.toString()); current.clear() }
                else -> current.append(c)
            }
            i++
        }
        parts += unescape(current.toString())
        return parts
    }
}
