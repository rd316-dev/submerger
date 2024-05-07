package com.rd316.submerger.ssa

import java.time.LocalTime

data class SSAEvent(
    val layer   : Int       = 0,
    val start   : LocalTime,
    val end     : LocalTime,
    val style   : String    = "",
    val name    : String    = "",
    val marginL : Int       = 0,
    val marginR : Int       = 0,
    val marginV : Int       = 0,
    val effect  : String    = "",
    val text    : String
)
