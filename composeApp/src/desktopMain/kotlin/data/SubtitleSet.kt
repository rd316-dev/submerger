package data

data class SubtitleSet(
    val files: List<String?> = emptyList(),
    val offset: Int = 0,
    val style: String? = null
)