package com.bodla.parivar.features.design

data class NoticeDesignData(
    val name: String = "",
    val dateOfDeath: String = "",
    val day: String = "",
    val antimYatraDate: String = "",
    val antimYatraTime: String = "",
    val address: String = "",
    val smashanGhat: String = "",
    val mobileNumber: String = "",
    val familyMembers: List<String> = List(5) { "" },
    val photoBytes: ByteArray? = null
) {
    fun toNoticeText(): String = buildString {
        appendLine("ૐ શાંતિઃ")
        appendLine()
        appendLine("ભાવભીની શ્રદ્ધાંજલિ")
        appendLine(name.trim())
        appendLine()
        appendLine("દિલગીરી સાથે જણાવવાનું કે અમારા વ્હાલા ${name.trim()} નું તા. ${dateOfDeath.trim()} ના રોજ અવસાન થયેલ છે.")
        appendLine("ભગવાન એમના દિવ્ય આત્માને શાંતિ અર્પે અને પરિવારજનોને આ દુઃખ સહન કરવાની શક્તિ આપે એ જ અમારી પ્રાર્થના.")
        appendLine("સ્મશાન યાત્રા / અંતિમ યાત્રા")
        
        val timeFormatted = if (antimYatraTime.isNotBlank()) {
            val parts = antimYatraTime.trim().split(":")
            val hour24 = parts.getOrNull(0)?.toIntOrNull() ?: 16
            val minute = parts.getOrNull(1) ?: "00"
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            val hourStr = hour12.toString().padStart(2, '0')
            val period = if (hour24 < 12) "સવારે" else "સાંજે"
            " ના $period $hourStr:$minute વાગ્યે"
        } else ""

        val addressPart = if (address.isNotBlank()) " અમારા નિવાસસ્થાન ${address.trim()} થી નીકળીને" else ""
        val ghatPart = if (smashanGhat.isNotBlank()) " ${smashanGhat.trim()} જશે." else " જશે."
        appendLine("અંતિમ યાત્રા તા. ${antimYatraDate.trim()}$timeFormatted$addressPart$ghatPart")
        val members = familyMembers.map(String::trim).filter(String::isNotEmpty)
        if (members.isNotEmpty()) appendLine("શોકાકુલ પરિવાર: ${members.joinToString(", ")}")
        if (mobileNumber.isNotBlank()) appendLine("મો. ${mobileNumber.trim()}")
    }.trim()
}
