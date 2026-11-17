package space.aniroom.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import space.aniroom.app.BuildConfig
import java.io.IOException

val AppJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    isLenient = true
    encodeDefaults = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

interface AniApi {
    @POST("auth/login") suspend fun login(@Body body: JsonObject): AuthResponse
    @POST("auth/register") suspend fun register(@Body body: JsonObject): AuthResponse
    @GET("auth/me") suspend fun me(): User
    @POST("auth/password") suspend fun changePassword(@Body body: JsonObject): AuthResponse
    @POST("auth/logout-all") suspend fun logoutAll(): AuthResponse

    @Multipart
    @POST("auth/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): User

    @PATCH("users/me/preferences") suspend fun updatePreferences(@Body body: JsonObject): User
    @PATCH("users/me/profile") suspend fun updateProfile(@Body body: JsonObject): User

    @GET("game/me") suspend fun gameMe(): GameMe
    @GET("game/leaderboard") suspend fun leaderboard(): Leaderboard

    @GET("users/{id}") suspend fun publicProfile(@Path("id") id: Int): PublicProfile
    @GET("users/{id}/stats") suspend fun profileStats(@Path("id") id: Int): ProfileStats
    @GET("users/{id}/badges") suspend fun userBadges(@Path("id") id: Int): List<Badge>
    @GET("users/{id}/list") suspend fun userList(
        @Path("id") id: Int,
        @Query("status") status: String?,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 60,
    ): AnimeListPage
    @PUT("users/me/list/{sid}") suspend fun setListEntry(@Path("sid") shikimoriId: String, @Body body: JsonObject): AnimeListEntry
    @GET("users/me/progress") suspend fun myProgress(): List<Progress>

    @GET("users/friends") suspend fun friends(): List<Friend>
    @GET("users/friend-requests") suspend fun friendRequests(@Query("direction") direction: String = "incoming"): List<FriendRequestItem>
    @POST("users/friend-requests") suspend fun addFriend(@Body body: JsonObject): JsonObject
    @POST("users/friend-requests/{id}/accept") suspend fun acceptFriend(@Path("id") id: Int): JsonObject
    @DELETE("users/friend-requests/{id}") suspend fun rejectFriend(@Path("id") id: Int)
    @DELETE("users/friends/{id}") suspend fun removeFriend(@Path("id") id: Int)
    @POST("users/blocks/{id}") suspend fun blockUser(@Path("id") id: Int)
    @DELETE("users/blocks/{id}") suspend fun unblockUser(@Path("id") id: Int)

    @GET("users/notifications") suspend fun notifications(): List<AppNotification>
    @POST("users/notifications/read-all") suspend fun markAllRead()
    @POST("users/notifications/{id}/read") suspend fun markRead(@Path("id") id: Long)

    @POST("rooms") suspend fun createRoom(@Body body: JsonObject): Room
    @POST("rooms/join") suspend fun joinRoom(@Body body: JsonObject): Room
    @GET("rooms/{code}") suspend fun getRoom(@Path("code") code: String): Room
    @PATCH("rooms/{code}") suspend fun updateRoom(@Path("code") code: String, @Body body: JsonObject): Room
    @DELETE("rooms/{code}") suspend fun deleteRoom(@Path("code") code: String)
    @GET("rooms/{code}/messages") suspend fun messages(@Path("code") code: String): List<Message>
    @GET("rooms/{code}/ratings") suspend fun roomRatings(@Path("code") code: String): RoomRatings
    @GET("rooms/{code}/progress") suspend fun roomProgress(@Path("code") code: String): RoomProgress
    @PUT("rooms/{code}/progress") suspend fun saveProgress(@Path("code") code: String, @Body body: JsonObject): JsonObject
    @POST("rooms/{code}/invite") suspend fun inviteFriend(@Path("code") code: String, @Body body: JsonObject): JsonObject
    @POST("rooms/{code}/lock") suspend fun setLock(@Path("code") code: String, @Body body: JsonObject): Room
    @POST("rooms/{code}/transfer") suspend fun transferOwner(@Path("code") code: String, @Body body: JsonObject): Room
    @DELETE("rooms/{code}/participants/{uid}") suspend fun kick(@Path("code") code: String, @Path("uid") userId: Int): Room
    @GET("rooms/history") suspend fun history(): List<Room>
    @GET("rooms/public") suspend fun publicRooms(): List<Room>

    @GET("stickers") suspend fun stickers(): List<Sticker>
    @Multipart
    @POST("stickers")
    suspend fun uploadSticker(@Part file: MultipartBody.Part, @Part("name") name: RequestBody): Sticker
    @DELETE("stickers/{id}") suspend fun deleteSticker(@Path("id") id: Int)

    @GET("anime/search") suspend fun animeSearch(@Query("q") q: String, @Query("limit") limit: Int = 20): AnimeSearchWrapper
    @GET("anime/title/{sid}") suspend fun animeTitle(@Path("sid") id: String): AnimeFullPage
    @GET("anime/title/{sid}/translations") suspend fun animeTranslations(@Path("sid") id: String): AnimeTranslations
    @GET("anime/title/{sid}/link") suspend fun animeLink(
        @Path("sid") id: String,
        @Query("translation_id") translationId: String,
        @Query("episode") episode: Int,
    ): AnimeLink
}

@kotlinx.serialization.Serializable
data class AnimeSearchWrapper(val results: List<AnimeSearchResult> = emptyList())

class ApiException(message: String, val code: Int = 0) : Exception(message)

object Api {
    val baseUrl: String = BuildConfig.API_URL.trimEnd('/')

    lateinit var service: AniApi
        private set

    fun init(client: OkHttpClient) {
        service = Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(client)
            .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AniApi::class.java)
    }

    /** Absolute URL for media the API returns as a path. */
    fun media(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return when {
            url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "$baseUrl$url"
            else -> url
        }
    }
}

fun obj(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
    for ((key, value) in pairs) {
        when (value) {
            null -> put(key, kotlinx.serialization.json.JsonNull)
            is String -> put(key, value)
            is Number -> put(key, value)
            is Boolean -> put(key, value)
            is kotlinx.serialization.json.JsonElement -> put(key, value)
            else -> put(key, value.toString())
        }
    }
}

/** Runs an API call and turns transport / HTTP failures into readable messages. */
suspend fun <T> api(block: suspend AniApi.() -> T): Result<T> = try {
    Result.success(Api.service.block())
} catch (e: HttpException) {
    val detail = runCatching {
        val raw = e.response()?.errorBody()?.string().orEmpty()
        val element = AppJson.parseToJsonElement(raw).jsonObject["detail"]
        when (element) {
            is JsonPrimitive -> element.contentOrNull
            is JsonArray -> element.mapNotNull { (it as? JsonObject)?.get("msg")?.let { m -> (m as? JsonPrimitive)?.contentOrNull } }.joinToString("; ")
            else -> null
        }
    }.getOrNull()
    Result.failure(ApiException(detail?.takeIf { it.isNotBlank() } ?: httpMessage(e.code()), e.code()))
} catch (e: IOException) {
    Result.failure(ApiException("Нет соединения с сервером"))
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(ApiException(e.message ?: "Что-то пошло не так"))
}

private fun httpMessage(code: Int) = when (code) {
    401 -> "Сессия истекла, войди снова"
    403 -> "Нет доступа"
    404 -> "Не найдено"
    429 -> "Слишком часто, подожди немного"
    in 500..599 -> "Сервер временно недоступен"
    else -> "Ошибка $code"
}
