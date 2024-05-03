package com.rd316.submerger.srt

import java.time.LocalTime

data class SubRipEvent(
    val number: Int,
    val start: LocalTime,
    val end: LocalTime,
    val text: String
)
