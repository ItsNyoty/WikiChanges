package com.itsnyoty.wikichanges.data.model

import com.google.gson.annotations.SerializedName

data class RecentChange(
    @SerializedName("rcid") val id: Long = 0,
    @SerializedName("timestamp") val timestamp: String = "",
    @SerializedName("title") val title: String? = null,
    @SerializedName("ns") val namespace: Int? = null,
    @SerializedName("user") val user: String? = null,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("patrolled") val patrolled: String? = null,
    @SerializedName("unpatrolled") val unpatrolled: String? = null,
    @SerializedName("oldlen") val oldLength: Int? = null,
    @SerializedName("newlen") val newLength: Int? = null,
    @SerializedName("revid") val oldRevid: Long? = null,
    @SerializedName("old_revid") val lastRevid: Long? = null,
    @SerializedName("pageid") val pageId: Long = 0,
    @SerializedName("type") val type: String = "",
    @SerializedName("bot") val bot: String? = null,
    @SerializedName("minor") val minor: String? = null,
    @SerializedName("parsedcomment") val parsedComment: String? = null,
    @SerializedName("sizediff") val sizeDiff: Int? = null
)

data class CompareResponse(
    @SerializedName("compare") val compare: CompareResult?
)

data class CompareResult(
    @SerializedName("fromtitle") val fromTitle: String?,
    @SerializedName("totitle") val toTitle: String?,
    @SerializedName("fromrevid") val fromRevId: Long?,
    @SerializedName("torevid") val toRevId: Long?,
    @SerializedName("fromns") val fromNs: Int?,
    @SerializedName("tons") val toNs: Int?,
    @SerializedName("*") val body: String?
)

data class PageInfo(
    @SerializedName("pageid") val pageId: Long,
    @SerializedName("ns") val namespace: Int,
    @SerializedName("title") val title: String,
    @SerializedName("lastrevid") val lastRevid: Long?,
    @SerializedName("parentrevid") val parentRevid: Long?,
    @SerializedName("revid") val revid: Long?,
    @SerializedName("sha1") val sha1: String?,
    @SerializedName("length") val length: Int?,
    @SerializedName("touched") val touched: String?,
    @SerializedName("restricted") val restricted: List<String>?,
    @SerializedName("linksherecount") val linkshereCount: Int?,
    @SerializedName("counter") val counter: Int?,
    @SerializedName("watchers") val watchers: Int?,
    @SerializedName("watchlist") val watchlist: String?,
    @SerializedName("missing") val missing: String?,
    @SerializedName("descriptionurl") val descriptionUrl: String?,
    @SerializedName("page_language") val pageLanguage: String?,
    @SerializedName("page_iscontent") val pageIsContent: Boolean?,
    @SerializedName("page_isnew") val pageIsNew: Boolean?,
    @SerializedName("redirect") val redirect: String?
)

data class RecentChangesResponse(
    @SerializedName("query") val query: Query,
    @SerializedName("continue") val continueMap: Map<String, String>? = null,
    @SerializedName("warnings") val warnings: Any? = null
)

data class Query(
    @SerializedName("recentchanges") val recentChanges: List<RecentChange>
)

data class AbuseFilterResult(
    @SerializedName("filterid") val filterId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("sample") val sample: String,
    @SerializedName("actions") val actions: List<String>,
    @SerializedName("variables") val variables: Map<String, String>?,
    @SerializedName("hit") val hit: Boolean
)

data class AbuseFilterResponse(
    @SerializedName("abusefilter") val abuseFilter: List<AbuseFilterResult>
)

data class UserBlockResult(
    @SerializedName("blockid") val blockId: Int?,
    @SerializedName("user") val user: String,
    @SerializedName("by") val by: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("expiry") val expiry: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("anononly") val anonOnly: Boolean?,
    @SerializedName("autoblock") val autoBlock: Boolean?,
    @SerializedName("createaccount") val createAccount: Boolean?,
    @SerializedName("enableautoblock") val enableAutoBlock: Boolean?,
    @SerializedName("hideuser") val hideUser: Boolean?,
    @SerializedName("id") val id: Int?,
    @SerializedName("range") val range: String?,
    @SerializedName("noresetlog") val noResetLog: Boolean?,
    @SerializedName("allowusertalk") val allowUserTalk: Boolean?,
    @SerializedName("blockemail") val blockEmail: Boolean?
)

data class BlockResponse(
    @SerializedName("block") val block: UserBlockResult?,
    @SerializedName("error") val error: ApiError? = null
)

data class RollbackResult(
    @SerializedName("pageid") val pageId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("ns") val ns: Int,
    @SerializedName("contentmodel") val contentModel: String,
    @SerializedName("oldrevid") val oldRevid: Int,
    @SerializedName("newrevid") val newRevid: Int,
    @SerializedName("user") val user: String,
    @SerializedName("userid") val userId: Int,
    @SerializedName("summary") val summary: String,
    @SerializedName("userlastedit") val userLastEdit: String
)

data class RollbackResponse(
    @SerializedName("rollback") val rollback: RollbackResult?,
    @SerializedName("error") val error: ApiError? = null
)

data class ApiError(
    @SerializedName("code") val code: String?,
    @SerializedName("info") val info: String?
)

data class ThankResult(
    @SerializedName("id") val id: Int,
    @SerializedName("recipientid") val recipientId: Int,
    @SerializedName("recipientname") val recipientName: String,
    @SerializedName("revid") val revid: Int,
    @SerializedName("comment") val comment: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("userid") val userId: Int,
    @SerializedName("name") val name: String
)

data class ThankResponse(
    @SerializedName("thanks") val thanks: ThankResult?,
    @SerializedName("error") val error: ApiError? = null
)

data class WarningResult(
    @SerializedName("pageid") val pageId: Int,
    @SerializedName("ns") val ns: Int,
    @SerializedName("title") val title: String,
    @SerializedName("contentmodel") val contentModel: String,
    @SerializedName("oldrevid") val oldRevid: Int,
    @SerializedName("newrevid") val newRevid: Int,
    @SerializedName("user") val user: String,
    @SerializedName("userid") val userId: Int,
    @SerializedName("summary") val summary: String,
    @SerializedName("userlastedit") val userLastEdit: String
)

data class WarningResponse(
    @SerializedName("warning") val warning: WarningResult? = null,
    @SerializedName("error") val error: ApiError? = null
)

data class DeleteResult(
    @SerializedName("title") val title: String?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("logid") val logId: Long?
)

data class DeleteResponse(
    @SerializedName("delete") val delete: DeleteResult?,
    @SerializedName("error") val error: ApiError? = null
)

data class LoginResponse(
    @SerializedName("login") val login: LoginResult
)

data class LoginResult(
    @SerializedName("token") val token: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("lgusername") val lgUsername: String?,
    @SerializedName("result") val result: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("reason") val reason: String?
)

data class TokensResponse(
    @SerializedName("batchcomplete") val batchComplete: Boolean?,
    @SerializedName("query") val query: TokenQuery?
)

data class TokenQuery(
    @SerializedName("tokens") val tokens: Map<String, String>
)

data class UserInfoResponse(
    @SerializedName("batchcomplete") val batchComplete: Boolean?,
    @SerializedName("query") val query: UserInfoQuery?
)

data class UserInfoQuery(
    @SerializedName("userinfo") val userInfo: CurrentUserInfo?
)

data class CurrentUserInfo(
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?,
    @SerializedName("rights") val rights: List<String>?,
    @SerializedName("groups") val groups: List<String>?,
    @SerializedName("anon") val anon: String?,
    @SerializedName("blockid") val blockId: Long?,
    @SerializedName("blockedby") val blockedBy: String?,
    @SerializedName("blockedbyid") val blockedById: Long?,
    @SerializedName("blockreason") val blockReason: String?
)
