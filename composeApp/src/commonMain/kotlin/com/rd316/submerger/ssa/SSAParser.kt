package com.rd316.submerger.ssa

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

class SSAParser private constructor() {
    companion object {
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm:ss.SS")

        private fun parseStyle(line: String, format: List<String>): SSAStyle {
            val map = HashMap<String, String>()

            val fields = line.split(",")
            val fieldIterator = fields.iterator()

            for (fieldName in format) {
                if (!fieldIterator.hasNext())
                    throw IllegalArgumentException("Style field $fieldName not found")

                map[fieldName] = fieldIterator.next()
            }

            return SSAStyle(map)
        }

        private fun parseEvent(line: String, format: List<String>): SSAEvent {
            val map = HashMap<String, String>()

            val fields = line.split(",")
            val fieldIterator = fields.iterator()

            for (fieldName in format) {
                if (!fieldIterator.hasNext())
                    throw IllegalArgumentException("Event field $fieldName not found")

                map[fieldName] = fieldIterator.next()
            }

            return SSAEvent(
                Integer.parseInt(map["Layer"] ?: "0"),
                LocalTime.parse(map["Start"] ?: throw IllegalArgumentException("Event doesn't have Start field"), FORMATTER),
                LocalTime.parse(map["End"] ?: throw IllegalArgumentException("Event doesn't have End field"), FORMATTER),
                map["Style"] ?: "",
                map["Name"] ?: "",
                Integer.parseInt(map["MarginL"] ?: "0"),
                Integer.parseInt(map["MarginR"] ?: "0"),
                Integer.parseInt(map["MarginV"] ?: "0"),
                map["Effect"] ?: "",
                map["Text"] ?: throw IllegalArgumentException("Event doesn't have Text field"),
            )
        }

        fun parse(data: String): SSAFile {
            var section = ""
            var lineNumber = 0

            val scriptInfo = mutableListOf<Pair<String, String>>()
            val styleFormat = mutableListOf<String>()
            val eventFormat = mutableListOf<String>()
            val styles = mutableListOf<SSAStyle>()
            val events = mutableListOf<SSAEvent>()

            for (l in data.removePrefix("\uFEFF").lines()) {
                lineNumber++

                if (l.isBlank() || l.startsWith(";")) continue
                if (l.startsWith("[")) {
                    section = l.removePrefix("[").removeSuffix("]")
                    continue
                }

                val firstColon = l.indexOfFirst { c -> c == ':' }

                if (firstColon == -1) {
                    Logger.getGlobal().info("Unable to find descriptor at line $lineNumber. Skipping")
                    continue
                }

                val descriptor = l.substring(0, firstColon)
                val value = l.substring(firstColon + 1, l.length).trim()

                when (section.lowercase()) {
                    "script info" -> {
                        scriptInfo.add(Pair(descriptor, value))
                    }

                    "v4 styles", "v4+ styles" -> {
                        when (descriptor.lowercase()) {
                            "format" -> styleFormat.addAll(value.split(",").map { f -> f.trim() })
                            "style" -> styles.add(parseStyle(value, styleFormat))
                            else -> throw IllegalArgumentException("[$lineNumber]: Invalid descriptor in $section section: $descriptor")
                        }
                    }

                    "events" -> {
                        when (descriptor.lowercase()) {
                            "format" -> eventFormat.addAll(value.split(",").map { f -> f.trim() })
                            "dialogue" -> events.add(parseEvent(value, eventFormat))
                        }
                    }
                }
            }

            return SSAFile(
                scriptInfo = scriptInfo,
                styleFormat = styleFormat,
                eventFormat = eventFormat,
                styles = styles,
                events = events
            )
        }
    }

}