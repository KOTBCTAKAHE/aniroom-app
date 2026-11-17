package space.aniroom.app.data

import android.os.Handler
import android.os.Looper
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import kotlin.coroutines.resume

enum class LinkStatus { CONNECTING, CONNECTED, RECONNECTING, OFFLINE, FAILED }

/**
 * Thin wrapper around the socket.io client. All callbacks are delivered on the main thread
 * so view models can touch ExoPlayer and state directly.
 */
class SocketClient(token: String, client: OkHttpClient) {
    private val main = Handler(Looper.getMainLooper())
    private val socket: Socket

    init {
        val options = IO.Options().apply {
            path = "/socket.io"
            auth = mapOf("token" to token)
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 1_000
            reconnectionDelayMax = 15_000
            randomizationFactor = 0.3
            timeout = 20_000
            callFactory = client
            webSocketFactory = client
        }
        socket = IO.socket(URI.create(Api.baseUrl), options)
    }

    val connected: Boolean get() = socket.connected()

    /** Registers a handler that receives the first argument as raw JSON text. */
    fun on(event: String, handler: (String?) -> Unit) {
        socket.on(event) { args ->
            val raw = args.firstOrNull()?.let { argToString(it) }
            main.post { handler(raw) }
        }
    }

    inline fun <reified T> onJson(event: String, crossinline handler: (T) -> Unit) {
        on(event) { raw ->
            if (raw == null) return@on
            val value = runCatching { AppJson.decodeFromString<T>(raw) }.getOrNull() ?: return@on
            handler(value)
        }
    }

    fun connect() { socket.connect() }

    fun disconnect() {
        socket.off()
        socket.disconnect()
        socket.close()
    }

    fun emit(event: String, payload: JsonObject) {
        if (socket.connected()) socket.emit(event, toJson(payload))
    }

    /** Emits with an acknowledgement; returns the raw ack JSON or null on timeout / offline. */
    suspend fun emitAck(event: String, payload: JsonObject, timeoutMs: Long = 8_000): String? {
        if (!socket.connected()) return null
        return suspendCancellableCoroutine { cont ->
            var done = false
            val timeout = Runnable {
                if (!done) { done = true; if (cont.isActive) cont.resume(null) }
            }
            main.postDelayed(timeout, timeoutMs)
            socket.emit(event, arrayOf<Any>(toJson(payload)), Ack { args ->
                main.post {
                    if (!done) {
                        done = true
                        main.removeCallbacks(timeout)
                        if (cont.isActive) cont.resume(args.firstOrNull()?.let { argToString(it) } ?: "{}")
                    }
                }
            })
        }
    }

    private fun argToString(arg: Any): String = when (arg) {
        is JSONObject, is JSONArray -> arg.toString()
        is String -> JSONObject.quote(arg)
        else -> arg.toString()
    }

    companion object {
        fun toJson(element: JsonObject): JSONObject {
            val result = JSONObject()
            for ((key, value) in element) result.put(key, convert(value))
            return result
        }

        private fun convert(element: JsonElement): Any = when (element) {
            is JsonNull -> JSONObject.NULL
            is JsonObject -> toJson(element)
            is JsonArray -> JSONArray().also { array -> element.forEach { array.put(convert(it)) } }
            is JsonPrimitive -> when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.booleanOrNull!!
                element.longOrNull != null -> element.longOrNull!!
                element.doubleOrNull != null -> element.doubleOrNull!!
                else -> element.content
            }
        }
    }
}
