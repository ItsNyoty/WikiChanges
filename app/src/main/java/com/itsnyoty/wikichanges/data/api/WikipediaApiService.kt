package com.itsnyoty.wikichanges.data.api

import com.itsnyoty.wikichanges.data.model.*
import retrofit2.http.*
import retrofit2.http.Query as RetrofitQuery

interface WikipediaApiService {
    // Get recent changes
    @Headers("Cache-Control: no-cache")
    @GET
    suspend fun getRecentChanges(
        @Url url: String,
        @RetrofitQuery("action") action: String = "query",
        @RetrofitQuery("list") list: String = "recentchanges",
        @RetrofitQuery("rcprop") rcprop: String = "user|title|timestamp|flags|comment|sizes|ids|parsedcomment|patrolled",
        @RetrofitQuery("rclimit") rclimit: Int = 50,
        @RetrofitQuery("rcnamespace") rcnamespace: String? = null,
        @RetrofitQuery("rcstart") rcstart: String? = null,
        @RetrofitQuery("rcdir") rcdir: String = "older",
        @RetrofitQuery("rctype") rctype: String = "edit|new",
        @RetrofitQuery("rcshow") rcshow: String? = null,
        @RetrofitQuery("curtimestamp") curtimestamp: String = "1",
        @RetrofitQuery("format") format: String = "json"
    ): RecentChangesResponse

    // Get tokens
    @GET
    suspend fun getTokens(
        @Url url: String,
        @RetrofitQuery("action") action: String = "query",
        @RetrofitQuery("meta") meta: String = "tokens",
        @RetrofitQuery("type") type: String = "rollback|patrol|csrf",
        @RetrofitQuery("format") format: String = "json"
    ): TokensResponse

    // Rollback
    @FormUrlEncoded
    @POST
    suspend fun rollback(
        @Url url: String,
        @Field("action") action: String = "rollback",
        @Field("title") title: String,
        @Field("user") user: String,
        @Field("id") id: Long,
        @Field("token") token: String,
        @Field("summary") summary: String? = null,
        @Field("format") format: String = "json"
    ): RollbackResponse

    // Block user
    @FormUrlEncoded
    @POST
    suspend fun blockUser(
        @Url url: String,
        @Field("action") action: String = "block",
        @Field("user") user: String,
        @Field("expiry") expiry: String,
        @Field("reason") reason: String,
        @Field("token") token: String,
        @Field("anonly") anonOnly: Boolean = false,
        @Field("createaccount") createAccount: Boolean = true,
        @Field("enableautoblock") enableAutoBlock: Boolean = true,
        @Field("hideuser") hideUser: Boolean = false,
        @Field("noresetlog") noResetLog: Boolean = false,
        @Field("allowusertalk") allowUserTalk: Boolean = true,
        @Field("blockemail") blockEmail: Boolean = false,
        @Field("format") format: String = "json"
    ): BlockResponse

    // Thank user
    @FormUrlEncoded
    @POST
    suspend fun thankUser(
        @Url url: String,
        @Field("action") action: String = "thanks",
        @Field("id") id: Long,
        @Field("token") token: String,
        @Field("format") format: String = "json"
    ): ThankResponse

    // Patrol
    @FormUrlEncoded
    @POST
    suspend fun patrol(
        @Url url: String,
        @Field("action") action: String = "patrol",
        @Field("rcid") rcid: Long,
        @Field("token") token: String,
        @Field("format") format: String = "json"
    ): okhttp3.ResponseBody

    // Warn user (via API edit to user talk page as new section)
    @FormUrlEncoded
    @POST
    suspend fun warnUser(
        @Url url: String,
        @Field("action") action: String = "edit",
        @Field("title") title: String,
        @Field("section") section: String = "new",
        @Field("sectiontitle") sectionTitle: String,
        @Field("text") text: String,
        @Field("summary") summary: String,
        @Field("token") token: String,
        @Field("bot") bot: String = "true",
        @Field("format") format: String = "json"
    ): okhttp3.ResponseBody

    // Delete page
    @FormUrlEncoded
    @POST
    suspend fun deletePage(
        @Url url: String,
        @Field("action") action: String = "delete",
        @Field("title") title: String,
        @Field("reason") reason: String,
        @Field("token") token: String,
        @Field("format") format: String = "json"
    ): DeleteResponse

    // Login
    @FormUrlEncoded
    @POST
    suspend fun login(
        @Url url: String,
        @Field("action") action: String = "login",
        @Field("lgname") username: String,
        @Field("lgpassword") password: String,
        @Field("lgtoken") loginToken: String,
        @Field("format") format: String = "json"
    ): LoginResponse

    // Get login token
    @GET
    suspend fun getLoginToken(
        @Url url: String,
        @RetrofitQuery("action") action: String = "query",
        @RetrofitQuery("meta") meta: String = "tokens",
        @RetrofitQuery("type") type: String = "login",
        @RetrofitQuery("format") format: String = "json"
    ): TokensResponse

    // Get page info for base revision
    @GET
    suspend fun getPageInfo(
        @Url url: String,
        @RetrofitQuery("action") action: String = "query",
        @RetrofitQuery("prop") prop: String = "revisions",
        @RetrofitQuery("titles") titles: String,
        @RetrofitQuery("rvslots") rvslots: String = "main",
        @RetrofitQuery("rvlimit") rvlimit: Int = 1,
        @RetrofitQuery("format") format: String = "json"
    ): PageInfoResponse

    // Get current user's rights/groups on a specific wiki
    @GET
    suspend fun getUserRights(
        @Url url: String,
        @RetrofitQuery("action") action: String = "query",
        @RetrofitQuery("meta") meta: String = "userinfo",
        @RetrofitQuery("uiprop") uiprop: String = "rights|groups",
        @RetrofitQuery("format") format: String = "json"
    ): UserInfoResponse

    // Compare two revisions and return HTML diff
    @GET
    suspend fun compareRevisions(
        @Url url: String,
        @RetrofitQuery("action") action: String = "compare",
        @RetrofitQuery("fromrev") fromRev: Long? = null,
        @RetrofitQuery("torev") toRev: Long,
        @RetrofitQuery("prop") prop: String = "diff|title|ids",
        @RetrofitQuery("format") format: String = "json"
    ): CompareResponse

    // Get revision content
    @GET
    suspend fun getRevisionContent(
        @Url url: String,
        @RetrofitQuery("action") action: String = "query",
        @RetrofitQuery("prop") prop: String = "revisions",
        @RetrofitQuery("revids") revIds: Long,
        @RetrofitQuery("rvslots") rvSlots: String = "main",
        @RetrofitQuery("rvprop") rvProp: String = "content",
        @RetrofitQuery("format") format: String = "json"
    ): PageInfoResponse

    // Get user groups for a list of users
    @GET
    suspend fun getUsersGroups(
        @Url url: String,
        @RetrofitQuery("action") action: String = "query",
        @RetrofitQuery("list") list: String = "users",
        @RetrofitQuery("ususers") users: String,
        @RetrofitQuery("usprop") prop: String = "groups",
        @RetrofitQuery("format") format: String = "json"
    ): UsersGroupsResponse
}
