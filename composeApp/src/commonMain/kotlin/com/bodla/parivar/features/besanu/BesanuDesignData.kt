package com.bodla.parivar.features.besanu

data class BesanuDesignData(
    val name: String = "",
    val dateOfDeath: String = "",
    val day: String = "",
    val besanuDate: String = "",
    val besanuStartTime: String = "",
    val besanuEndTime: String = "",
    val address: String = "",
    val mobileNumber: String = "",
    val familyMembers: List<String> = List(5) { "" },
    val photoBytes: ByteArray? = null
) {
    fun toNoticeText(): String = buildString {
        appendLine("ૐ શાંતિઃ")
        appendLine()
        appendLine("બેસણું")
        appendLine()
        appendLine("સ્વ. ${name.trim()}")
        val deathLine = listOfNotNull(
            dateOfDeath.takeIf { it.isNotBlank() }?.let { "તા. $it" },
            day.takeIf { it.isNotBlank() }
        ).joinToString(", ")
        if (deathLine.isNotBlank()) appendLine(deathLine)
        appendLine()
        val deathPart = if (dateOfDeath.isNotBlank()) "નું તા. ${dateOfDeath.trim()} ના રોજ" else "ના"
        appendLine("દિલગીરી સાથે જણાવવાનું કે અમારા સ્વ. ${name.trim()} $deathPart અક્ષરવાસ થયેલ છે.")
        appendLine("પરમકૃપાળુ પરમાત્મા સદ્દગત દિવ્ય આત્માને શાંતિ પ્રદાન કરે એવી પ્રભુના ચરણોમાં પ્રાર્થના.")
        appendLine()
        appendLine("બેસણું")
        val besanuLine = buildString {
            if (besanuDate.isNotBlank()) append("તા. ${besanuDate.trim()} ના રોજ")
            val timeSpan = formatTimeSpan(besanuStartTime, besanuEndTime)
            if (timeSpan.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append("$timeSpan કલાક સુધી રાખેલ છે.")
            } else if (isNotEmpty()) {
                append(" રાખેલ છે.")
            }
        }
        if (besanuLine.isNotBlank()) appendLine(besanuLine)
        if (address.isNotBlank()) {
            appendLine()
            appendLine("સ્થળ:")
            appendLine(address.trim())
        }
        val members = familyMembers.map(String::trim).filter(String::isNotEmpty)
        if (members.isNotEmpty()) {
            appendLine()
            appendLine("લિ.")
            members.forEach { appendLine(it) }
        }
        if (mobileNumber.isNotBlank()) appendLine("મો. ${mobileNumber.trim()}")
    }.trim()

    companion object {
        fun formatSingleTime(timeStr: String): String {
            if (timeStr.isBlank()) return ""
            val parts = timeStr.trim().split(":")
            val hour24 = parts.getOrNull(0)?.toIntOrNull() ?: return ""
            val minute = parts.getOrNull(1) ?: "00"
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            val hourStr = hour12.toString().padStart(2, '0')
            val period = if (hour24 < 12) "સવારે" else "સાંજે"
            return "$period $hourStr:$minute"
        }

        fun formatTimeSpan(start: String, end: String): String {
            val sFormatted = formatSingleTime(start)
            val eFormatted = formatSingleTime(end)
            return when {
                sFormatted.isNotBlank() && eFormatted.isNotBlank() -> "$sFormatted થી $eFormatted"
                sFormatted.isNotBlank() -> sFormatted
                eFormatted.isNotBlank() -> eFormatted
                else -> ""
            }
        }
    }
}
