package com.itsnyoty.wikichanges.data.api

import com.itsnyoty.wikichanges.data.model.PageInfoResponse
import com.itsnyoty.wikichanges.data.model.TokensResponse
import retrofit2.http.*

interface OAuthAuthenticatedApiService {
    // For OAuth authenticated requests
    @GET
    suspend fun getTokens(
        @Url url: String,
        @Query("action") action: String = "query",
        @Query("meta") meta: String = "tokens",
        @Query("type") type: String = "rollback|block|thanks|patrol|csrf",
        @Query("format") format: String = "json"
    ): TokensResponse

    @FormUrlEncoded
    @POST
    suspend fun rollback(
        @Url url: String,
        @Field("action") action: String = "rollback",
        @Field("title") title: String,
        @Field("user") user: String,
        @Field("token") token: String,
        @Field("summary") summary: String? = null,
        @Field("markbot") markbot: Int? = null
    )

    @FormUrlEncoded
    @POST
    suspend fun block(
        @Url url: String,
        @Field("action") action: String = "block",
        @Field("user") user: String,
        @Field("expiry") expiry: String,
        @Field("reason") reason: String,
        @Field("token") token: String,
        @Field("anononly") anononly: Int = 0,
        @Field("createaccount") createaccount: Int = 1,
        @Field("enableautoblock") enableautoblock: Int = 1
    )

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
        @Field("bot") bot: Int = 1
    )

    @FormUrlEncoded
    @POST
    suspend fun patrol(
        @Url url: String,
        @Field("action") action: String = "patrol",
        @Field("rcid") rcid: Long,
        @Field("token") token: String
    )

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
}
