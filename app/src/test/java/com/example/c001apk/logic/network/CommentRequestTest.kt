package com.example.c001apk.logic.network

import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CommentRequestTest {
    private val api = Retrofit.Builder().baseUrl("https://api.coolapk.com")
        .addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)

    @Test fun commentPagePreservesSortAuthorFilterAndCursors() {
        val url = api.getFeedContentReply("123", "dateline_desc", 2, "first", "last", 1,
            "feed", 0, 1).request().url
        assertEquals("/v6/feed/replyList", url.encodedPath)
        assertEquals("dateline_desc", url.queryParameter("listType"))
        assertEquals("1", url.queryParameter("fromFeedAuthor"))
        assertEquals("first", url.queryParameter("firstItem"))
        assertEquals("last", url.queryParameter("lastItem"))
        assertEquals("2", url.queryParameter("page"))
    }

    @Test fun subReplyPageUsesParentReplyIdAndReplyFeedType() {
        val url = api.getReply2Reply("parent", 2, "last-child").request().url
        assertEquals("parent", url.queryParameter("id"))
        assertEquals("feed_reply", url.queryParameter("feedType"))
        assertEquals("0", url.queryParameter("discussMode"))
        assertEquals("2", url.queryParameter("page"))
        assertEquals("last-child", url.queryParameter("lastItem"))
    }
}
