/** The given JSON helper: parse text into Map, List, String, Long, Double, Boolean or null, and write it back. */
object Json {
    fun parse(text: String): Any? {
        val p = Parser(text)
        val value = p.value()
        p.skip()
        require(p.i == text.length) { "unexpected data after the JSON value" }
        return value
    }

    fun stringify(value: Any?): String = StringBuilder().also { write(it, value) }.toString()

    private fun write(out: StringBuilder, v: Any?) {
        when (v) {
            null -> out.append("null")
            is String -> quote(out, v)
            is Number, is Boolean -> out.append(v)
            is Map<*, *> -> {
                out.append('{')
                var first = true
                for ((k, x) in v) {
                    if (!first) out.append(',')
                    first = false
                    quote(out, k.toString())
                    out.append(':')
                    write(out, x)
                }
                out.append('}')
            }
            is List<*> -> {
                out.append('[')
                v.forEachIndexed { i, x ->
                    if (i > 0) out.append(',')
                    write(out, x)
                }
                out.append(']')
            }
            else -> throw IllegalArgumentException("not a JSON value: ${v::class}")
        }
    }

    private fun quote(out: StringBuilder, s: String) {
        out.append('"')
        for (c in s) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c < ' ' -> out.append("\\u%04x".format(c.code))
                else -> out.append(c)
            }
        }
        out.append('"')
    }

    private class Parser(val s: String) {
        var i = 0

        fun skip() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun peek(): Char {
            skip()
            require(i < s.length) { "unexpected end of JSON" }
            return s[i]
        }

        fun expect(c: Char) {
            require(peek() == c) { "expected '$c' at $i" }
            i++
        }

        fun value(): Any? {
            val c = peek()
            return when {
                c == '{' -> obj()
                c == '[' -> arr()
                c == '"' -> str()
                s.startsWith("true", i) -> { i += 4; true }
                s.startsWith("false", i) -> { i += 5; false }
                s.startsWith("null", i) -> { i += 4; null }
                else -> num()
            }
        }

        fun obj(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>()
            expect('{')
            if (peek() == '}') { i++; return m }
            while (true) {
                val key = str()
                expect(':')
                m[key] = value()
                if (peek() == ',') { i++; continue }
                expect('}')
                return m
            }
        }

        fun arr(): List<Any?> {
            val l = ArrayList<Any?>()
            expect('[')
            if (peek() == ']') { i++; return l }
            while (true) {
                l.add(value())
                if (peek() == ',') { i++; continue }
                expect(']')
                return l
            }
        }

        fun str(): String {
            expect('"')
            val b = StringBuilder()
            while (true) {
                require(i < s.length) { "unterminated string" }
                val c = s[i++]
                if (c == '"') return b.toString()
                if (c != '\\') { b.append(c); continue }
                when (val e = s[i++]) {
                    'n' -> b.append('\n')
                    'r' -> b.append('\r')
                    't' -> b.append('\t')
                    'b' -> b.append('\b')
                    'f' -> b.append('\u000C')
                    'u' -> { b.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                    else -> b.append(e)
                }
            }
        }

        fun num(): Any {
            val start = i
            while (i < s.length && s[i] in "+-0123456789.eE") i++
            require(start != i) { "unexpected character at $i" }
            val t = s.substring(start, i)
            return if ('.' in t || 'e' in t || 'E' in t) t.toDouble() else t.toLong()
        }
    }
}
