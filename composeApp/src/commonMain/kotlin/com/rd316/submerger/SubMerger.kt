package com.rd316.submerger

import com.rd316.submerger.srt.SubRipParser
import com.rd316.submerger.ssa.SSAEvent
import com.rd316.submerger.ssa.SSAFile
import com.rd316.submerger.ssa.SSAParser
import java.io.FileReader
import java.io.FileWriter
import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.HashSet

class SubMerger {

    fun merge(formatFilename: String, outputFilename: String, syncThresholdMs: Long = 0L, inputFiles: Iterable<SubInfo>) {
        val format: SSAFile = FileReader(formatFilename).use { file ->
            val contents = file.readText()
            SSAParser.parse(contents)
        }

        val ssaOverrideRegex = Regex("\\{[^}]*}|\\{.*")

        val maxSyncDelta = Duration.of(syncThresholdMs, ChronoUnit.MILLIS)

        val syncOrigin = inputFiles.firstOrNull { f -> f.syncOrigin }
        val startPositions = HashSet<LocalTime>()
        val endPositions = HashSet<LocalTime>()

        val events = ArrayList<SSAEvent>()

        // sort the files so that the first file is the one with synchronization points
        for (f in inputFiles.sortedByDescending { f -> f.syncOrigin }) {
            // check if the style requested by file exists in the format file
            format.styles.find { s -> s.fields["Name"] == f.appliedStyle }
                ?: throw IllegalArgumentException("${f.appliedStyle} is not present in format file")

            val fileEvents = ArrayList<SSAEvent>()
            FileReader(f.filename).use { file ->
                val contents = file.readText()

                if (f.filename.endsWith(".ass") or f.filename.endsWith(".ssa")) {
                    val parsed = SSAParser.parse(contents)
                    fileEvents.addAll(parsed.events.map { e ->
                        e.copy(
                            text = e.text.replace(ssaOverrideRegex, "")
                        )
                    }.filter { e -> e.text.isNotBlank() })
                } else if (f.filename.endsWith(".srt")) {
                    // convert SubRip events to SSA events for easier processing
                    val parsed = SubRipParser.parse(contents)
                    fileEvents.addAll(parsed.events.map { e ->
                        SSAEvent(
                            start = e.start,
                            end = e.end,
                            text = e.text.replace("\n", "\\n")
                        )
                    })
                } else throw IllegalArgumentException("Unknown subtitles format: ${f.filename}")
            }

            val unusedStartPositions = HashSet<LocalTime>(startPositions)
            val unusedEndPositions = HashSet<LocalTime>(endPositions)

            for (e in fileEvents) {
                var event = e.copy(
                    start = e.start.plus(f.offsetMs, ChronoUnit.MILLIS),
                    end   = e.end.plus(f.offsetMs, ChronoUnit.MILLIS),
                    style = f.appliedStyle
                )

                if (syncOrigin == f) {
                    startPositions.add(event.start)
                    endPositions.add(event.end)

                    events.add(event)
                    continue
                }

                // synchronize the start and end points to the closest origin ones if possible
                if (unusedStartPositions.isNotEmpty()) {
                    val closestStart = unusedStartPositions.sortedBy { p ->
                        Duration.between(event.start, p).abs()
                    }.first()

                    val delta = Duration.between(event.start, closestStart).abs()

                    if (delta <= maxSyncDelta) {
                        event = event.copy(start = closestStart)

                        // delete the point to prevent overlaps
                        unusedStartPositions.remove(closestStart)
                    }
                }

                if (unusedEndPositions.isNotEmpty()) {
                    val closestEnd = unusedEndPositions.sortedBy { p ->
                        Duration.between(event.end, p).abs()
                    }.first()

                    val delta = Duration.between(event.end, closestEnd).abs()

                    kotlin.time.Duration
                    if (delta <= maxSyncDelta) {
                        event = event.copy(end = closestEnd)
                        unusedEndPositions.remove(closestEnd)
                    }
                }

                events.add(event)
            }
        }

        events.sortBy { e -> e.start }

        FileWriter(outputFilename).use { file ->
            file.appendLine("[Script Info]")
            format.scriptInfo.forEach { f -> file.appendLine("${f.first}: ${f.second}") }

            file.appendLine()
                .appendLine("[V4+ Styles]")
                .append("Format: ")
                .appendLine(format.styleFormat.joinToString())

            format.styles.forEach { s ->
                file.append("Style: ")

                val it = format.styleFormat.iterator()
                while (it.hasNext()) {
                    file.append(s.fields[it.next()])

                    if (it.hasNext())
                        file.append(",")
                    else
                        file.appendLine()
                }
            }

            file.appendLine()
                .append("[Events]")
                .appendLine("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text")
            events.forEach { e -> file.appendLine("Dialogue: ${e.layer}," +
                    "${e.start.format(SSAParser.FORMATTER)},${e.end.format(SSAParser.FORMATTER)}," +
                    "${e.style},${e.name},${e.marginL},${e.marginR},${e.marginV},${e.effect},${e.text}") }
            file.appendLine()
        }
    }

}