package com.itsnyoty.wikichanges.data.api

import com.itsnyoty.wikichanges.data.model.*
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded

interface WikipediaApiService {
    // Get recent changes
    @GET
    suspend fun getRecentChanges(
        @Url url: String,
        @Query("action") action: String = "query",
        @Query("list") list: String = "recentchanges",
        @Query("rcprop") rcprop: String = "user|title|timestamp|flags|comment|sizes|ids|parsedcomment|patrolled",
        @Query("rclimit") rclimit: Int = 50,
        @Query("rcnamespace") rcnamespace: String = "0",
        @Query("rcstart") rcstart: String? = null,
        @Query("rcdir") rcdir: String = "older",
        @Query("rctype") rctype: String = "edit|new",
        @Query("rcshow") rcshow: String? = null,
        @Query("format") format: String = "json"
    ): RecentChangesResponse

    // Get tokens
    @GET
    suspend fun getTokens(
        @Url url: String,
        @Query("action") action: String = "query",
        @Query("meta") meta: String = "tokens",
        @Query("type") type: String = "rollback|block|thank|patrol|csrf",
        @Query("format") format: String = "json"
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
        @Field("summary") summary: String? = null
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
        @Field("blockemail") blockEmail: Boolean = false
    ): BlockResponse

    // Thank user
    @FormUrlEncoded
    @POST
    suspend fun thankUser(
        @Url url: String,
        @Field("action") action: String = "thanks",
        @Field("id") id: Long,
        @Field("token") token: String
    ): ThankResponse

    // Patrol
    @FormUrlEncoded
    @POST
    suspend fun patrol(
        @Url url: String,
        @Field("action") action: String = "patrol",
        @Field("rcid") rcid: Long,
        @Field("token") token: String
    ): Unit

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
        @Field("bot") bot: String = "true"
    ): Unit

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
        @Query("action") action: String = "query",
        @Query("meta") meta: String = "tokens",
        @Query("type") type: String = "login",
        @Query("format") format: String = "json"
    ): TokensResponse

    // Get page info for base revision
    @GET
    suspend fun getPageInfo(
        @Url url: String,
        @Query("action") action: String = "query",
        @Query("prop") prop: String = "revisions",
        @Query("titles") titles: String,
        @Query("rvslots") rvslots: String = "main",
        @Query("rvlimit") rvlimit: Int = 1,
        @Query("format") format: String = "json"
    ): PageInfoResponse

    // Get current user's rights/groups on a specific wiki
    @GET
    suspend fun getUserRights(
        @Url url: String,
        @Query("action") action: String = "query",
        @Query("meta") meta: String = "userinfo",
        @Query("uiprop") uiprop: String = "rights|groups",
        @Query("format") format: String = "json"
    ): UserInfoResponse

    // Compare two revisions and return HTML diff
    @GET
    suspend fun compareRevisions(
        @Url url: String,
        @Query("action") action: String = "compare",
        @Query("fromrev") fromRev: Long,
        @Query("torev") toRev: Long,
        @Query("prop") prop: String = "diff|title|ids",
        @Query("format") format: String = "json"
    ): CompareResponse
}
