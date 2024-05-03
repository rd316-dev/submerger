package com.rd316.submerger.srt

class SubRipParser {

    @OptIn(ExperimentalStdlibApi::class)
    fun parse(data: String): SubRipFile {
        val events = ArrayList<SubRipEvent>()

        var state = 0

        var eventNumber = 0
        var eventStart = ""
        var eventEnd = ""
        var text = ""

        var lineNumber = 0

        for (l in data.removePrefix("\uFEFF").lines()) {
            lineNumber++
            if (l.isBlank()) {
                if (state == 2) {
                    events.add(SubRipEvent(eventNumber, eventStart, eventEnd, text.trim()))

                    eventNumber = 0
                    eventStart = ""
                    eventEnd = ""
                    text = ""
                }

                state = 0
                continue
            }

            when (state) {
                0 -> {
                    eventNumber = l.toIntOrNull() ?: throw IllegalArgumentException("[$lineNumber]: \"$l\" is not a number")
                    state++
                } 1 -> {
                    val tokens = l.split("-->")
                    eventStart = tokens[0].trim()
                    eventEnd = tokens[1].trim()
                    state++
                } 2 -> {
                    if (text.isNotBlank())
                        text += '\n'
                    text += l
                }
            }
        }

        return SubRipFile(events)
    }

}