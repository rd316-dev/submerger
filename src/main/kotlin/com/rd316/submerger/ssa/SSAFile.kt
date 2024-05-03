package com.rd316.submerger.ssa

data class SSAFile(
    val scriptInfo  : Map<String, String>,
    val styleFormat : List<String>,
    val eventFormat : List<String>,
    val styles      : List<SSAStyle>,
    val events      : List<SSAEvent>
)