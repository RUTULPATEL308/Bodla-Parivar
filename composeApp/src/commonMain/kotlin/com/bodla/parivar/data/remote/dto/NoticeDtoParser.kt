package com.bodla.parivar.data.remote.dto

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal fun decodeNoticeDtos(response: String): List<NoticeDto> = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}.decodeFromString(response)
