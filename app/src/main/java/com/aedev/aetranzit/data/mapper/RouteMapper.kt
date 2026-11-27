package com.aedev.aetranzit.data.mapper

import com.aedev.aetranzit.data.local.entity.RouteEntity
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop

fun RouteEntity.toDomain(): Route = Route(
    id = id,
    origin = origin,
    destination = destination,
    baseFare = baseFare,
    stops = RouteStopsJsonCodec.decode(stopsJson),
    companyId = companyId,
    isSynced = isSynced,
    updatedAt = updatedAt
)

fun Route.toEntity(): RouteEntity = RouteEntity(
    id = id,
    origin = origin,
    destination = destination,
    baseFare = baseFare,
    stopsJson = RouteStopsJsonCodec.encode(stops),
    companyId = companyId,
    isSynced = isSynced,
    updatedAt = updatedAt
)

/**
 * Zero-dependency RFC-8259 JSON codec for `RouteEntity.stopsJson` <-> `List<RouteStop>`.
 * Avoids Android `org.json` JVM unit-test stub failures while keeping clean JSON storage.
 */
object RouteStopsJsonCodec {

    fun encode(stops: List<RouteStop>): String {
        if (stops.isEmpty()) return "[]"
        return buildString {
            append('[')
            stops.forEachIndexed { index, stop ->
                if (index > 0) append(',')
                append("{\"name\":\"")
                appendJsonEscaped(stop.name)
                append("\",\"fare\":")
                append(stop.fare)
                append('}')
            }
            append(']')
        }
    }

    fun decode(json: String): List<RouteStop> {
        val trimmed = json.trim()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()
        return runCatching { JsonReader(trimmed).readStopsArray() }.getOrDefault(emptyList())
    }

    private fun StringBuilder.appendJsonEscaped(value: String) {
        for (ch in value) {
            when (ch) {
                '\"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        append("\\u")
                        append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        append(ch)
                    }
                }
            }
        }
    }

    private class JsonReader(private val src: String) {
        private var pos = 0

        fun readStopsArray(): List<RouteStop> {
            skipWhitespace()
            expect('[')
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return emptyList()
            }
            val result = mutableListOf<RouteStop>()
            while (true) {
                skipWhitespace()
                val stop = readStopObject()
                if (stop != null) {
                    result.add(stop)
                }
                skipWhitespace()
                when (val ch = peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        break
                    }
                    else -> throw IllegalArgumentException("Unexpected char '$ch' at $pos")
                }
            }
            return result
        }

        private fun readStopObject(): RouteStop? {
            expect('{')
            skipWhitespace()
            if (peek() == '}') {
                pos++
                return null
            }
            var name: String? = null
            var fare: Double? = null
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                when (key) {
                    "name" -> name = readString()
                    "fare" -> fare = readNumber()
                    else -> skipValue()
                }
                skipWhitespace()
                when (val ch = peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        break
                    }
                    else -> throw IllegalArgumentException("Unexpected char '$ch' at $pos")
                }
            }
            val validName = name?.trim().orEmpty()
            val validFare = fare ?: return null
            if (validName.isEmpty()) return null
            return RouteStop(name = validName, fare = validFare)
        }

        private fun readString(): String {
            expect('\"')
            val sb = StringBuilder()
            while (pos < src.length) {
                when (val ch = src[pos++]) {
                    '\"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= src.length) throw IllegalArgumentException("Unterminated escape")
                        when (val esc = src[pos++]) {
                            '\"' -> sb.append('\"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > src.length) {
                                    throw IllegalArgumentException("Invalid unicode escape")
                                }
                                val hex = src.substring(pos, pos + 4)
                                pos += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                            else -> sb.append(esc)
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        private fun readNumber(): Double {
            val start = pos
            while (pos < src.length && src[pos] in "0123456789+-eE.") {
                pos++
            }
            if (start == pos) throw IllegalArgumentException("Expected number at $pos")
            return src.substring(start, pos).toDouble()
        }

        private fun skipValue() {
            skipWhitespace()
            when (peek()) {
                '\"' -> readString()
                '{' -> {
                    pos++
                    var depth = 1
                    while (pos < src.length && depth > 0) {
                        when (src[pos]) {
                            '\"' -> readString()
                            '{' -> {
                                depth++
                                pos++
                            }
                            '}' -> {
                                depth--
                                pos++
                            }
                            else -> pos++
                        }
                    }
                }
                '[' -> {
                    pos++
                    var depth = 1
                    while (pos < src.length && depth > 0) {
                        when (src[pos]) {
                            '\"' -> readString()
                            '[' -> {
                                depth++
                                pos++
                            }
                            ']' -> {
                                depth--
                                pos++
                            }
                            else -> pos++
                        }
                    }
                }
                else -> {
                    while (pos < src.length && src[pos] !in ",}] \t\r\n") {
                        pos++
                    }
                }
            }
        }

        private fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) {
                pos++
            }
        }

        private fun peek(): Char? = if (pos < src.length) src[pos] else null

        private fun expect(expected: Char) {
            if (pos >= src.length || src[pos] != expected) {
                throw IllegalArgumentException("Expected '$expected' at $pos")
            }
            pos++
        }
    }
}
