package com.rd316

class SSAParser {

    fun parseStyle(line: String, format: List<String>) : SSAStyle {
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

    fun parseEvent(line: String, format: List<String>) : SSAEvent {
        val map = HashMap<String, String>()

        val fields = line.split(",")
        val fieldIterator = fields.iterator()

        for (fieldName in format) {
            if (!fieldIterator.hasNext())
                throw IllegalArgumentException("Event field $fieldName not found")

            map[fieldName] = fieldIterator.next()
        }

        return SSAEvent(map)
    }

    fun parse(data: String): SSAFile {
        val scriptInfo = HashMap<String, String>()

        val styleFormat = ArrayList<String>()
        val eventFormat = ArrayList<String>()

        val styles = ArrayList<SSAStyle>()
        val events = ArrayList<SSAEvent>()

        var section = ""
        var lineNumber = 0
        for (l in data.lines()) {
            lineNumber++

            if (l.isBlank() || l.startsWith(";")) continue
            if (l.startsWith("[")) {
                section = l.removePrefix("[").removeSuffix("]")
                continue
            }

            val firstColon = l.indexOfFirst { c -> c == ':' }

            val descriptor = l.substring(0, firstColon)
            val value = l.substring(firstColon + 1, l.length).trim()

            when (section.lowercase()) {
                "script info" -> {
                    scriptInfo[descriptor] = value
                } "v4 styles", "v4+ styles" -> {
                    when (descriptor.lowercase()) {
                        "format" -> styleFormat.addAll(value.split(",").map { f -> f.trim() })
                        "style" -> styles.add(parseStyle(value, styleFormat))
                        else -> throw IllegalArgumentException("[$lineNumber]: Invalid descriptor in $section section: $descriptor")
                    }
                } "events" -> {
                    when (descriptor.lowercase()) {
                        "format" -> eventFormat.addAll(value.split(",").map { f -> f.trim() })
                        "dialogue" -> events.add(parseEvent(value, eventFormat))
                    }
                }
            }
        }

        return SSAFile(scriptInfo, styleFormat, eventFormat, styles, events)
    }

}