package com.rd316.submerger.ssa

class SSAFile {
    val scriptInfo  = ArrayList<Pair<String, String>>()
    val styleFormat = ArrayList<String>()
    val eventFormat = ArrayList<String>()
    val styles      = ArrayList<SSAStyle>()
    val events      = ArrayList<SSAEvent>()
}