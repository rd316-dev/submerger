package com.rd316.submerger.ssa

data class SSAFile (
    val scriptInfo: List<Pair<String, String>>,
    val styleFormat: List<String>,
    val eventFormat: List<String>,
    val styles: List<SSAStyle>,
    val events: List<SSAEvent>
)