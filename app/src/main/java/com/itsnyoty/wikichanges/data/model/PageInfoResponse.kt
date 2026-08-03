package com.itsnyoty.wikichanges.data.model

import com.google.gson.annotations.SerializedName

data class PageInfoResponse(
    val query: PageInfoQuery?
)

data class PageInfoQuery(
    val pages: List<PageDetail>?
)

data class PageDetail(
    val pageid: Long?,
    val ns: Int?,
    val title: String?,
    val revisions: List<Revision>?
)

data class Revision(
    val revid: Long?,
    val parentid: Long?,
    val slots: Map<String, Slot>?
)

data class Slot(
    val contentmodel: String?,
    @SerializedName("*") val content: String?
)
