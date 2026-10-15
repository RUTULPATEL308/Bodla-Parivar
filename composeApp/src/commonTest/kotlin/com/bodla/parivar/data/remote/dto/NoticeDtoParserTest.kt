package com.bodla.parivar.data.remote.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NoticeDtoParserTest {
    @Test
    fun decodesPublishedNoticeWithAdditionalDatabaseColumns() {
        val response = """[
          {
            "id":"186f863b-a637-4754-9555-571a5cab5e92",
            "category_id":null,
            "published_by":null,
            "title_gu":"બોડલા લાઇવ ગરબા",
            "title_en":"BODLA LIVE GARBA",
            "description_gu":"https://www.youtube.com/live/example",
            "description_en":"https://www.youtube.com/live/example",
            "image_url":null,
            "attachment_url":null,
            "status":"PUBLISHED",
            "is_pinned":false,
            "publish_at":"2026-10-14T16:48:15.012+00:00",
            "expires_at":null,
            "created_at":"2026-10-14T16:48:15.362336+00:00",
            "updated_at":"2026-10-14T16:48:15.362336+00:00",
            "category":null
          }
        ]"""

        val notices = decodeNoticeDtos(response)

        assertEquals(1, notices.size)
        assertEquals("BODLA LIVE GARBA", notices.single().titleEn)
        assertEquals("PUBLISHED", notices.single().status)
        assertNull(notices.single().category)
    }
}
