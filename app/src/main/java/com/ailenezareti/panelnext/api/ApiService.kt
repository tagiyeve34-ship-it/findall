package com.ailenezareti.panelnext.api

import com.ailenezareti.panelnext.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("login.php") suspend fun login(@Body body: LoginRequest): Response<LoginResponse>
    @GET("children.php") suspend fun children(): Response<ChildrenResponse>
    @GET("locations.php") suspend fun locations(@Query("child_id") childId: Int, @Query("range") range: String, @Query("from") from: String? = null, @Query("to") to: String? = null): Response<LocationsResponse>
    @GET("calls.php") suspend fun calls(@Query("child_id") childId: Int, @Query("from") from: String? = null, @Query("to") to: String? = null, @Query("type") type: String = "all", @Query("search") search: String? = null, @Query("limit") limit: Int = 300, @Query("offset") offset: Int = 0): Response<CallsResponse>
    @GET("alerts.php") suspend fun alerts(@Query("child_id") childId: Int): Response<AlertsResponse>
    @PUT("alerts.php") suspend fun markRead(@Body body: MarkReadRequest): Response<SimpleStatus>
    @GET("zones.php") suspend fun zones(@Query("child_id") childId: Int): Response<ZonesResponse>
    @POST("zones.php") suspend fun createZone(@Body body: ZoneSaveRequest): Response<SimpleStatus>
    @HTTP(method="DELETE", path="zones.php", hasBody=true) suspend fun deleteZone(@Body body: ZoneDeleteRequest): Response<SimpleStatus>
}
