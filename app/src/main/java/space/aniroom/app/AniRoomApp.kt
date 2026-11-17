package space.aniroom.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import space.aniroom.app.data.Api
import space.aniroom.app.data.Session
import space.aniroom.app.data.UserHub
import java.io.File
import java.util.concurrent.TimeUnit

class AniRoomApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Session.init(this)
        http = OkHttpClient.Builder()
            .cache(Cache(File(cacheDir, "http"), 30L * 1024 * 1024))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                val token = Session.token.value
                val sameHost = request.url.host == apiHost
                if (token != null && sameHost && request.header("Authorization") == null) {
                    chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
                } else chain.proceed(request)
            })
            .build()
        Api.init(http)
        createChannels()
        space.aniroom.app.util.Sounds.init(this)
        UserHub.start(this)
    }

    private val apiHost: String by lazy { java.net.URI(Api.baseUrl).host }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SOCIAL, getString(R.string.channel_social), NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Приглашения в комнаты, друзья и новые серии"
            },
        )
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { http }
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(120L * 1024 * 1024).build() }
        .components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            // Video avatars / stickers (MP4, WebM) show their first frame wherever they can't play.
            add(coil.decode.VideoFrameDecoder.Factory())
        }
        .crossfade(220)
        .respectCacheHeaders(false)
        .build()

    companion object {
        const val CHANNEL_SOCIAL = "social"
        lateinit var instance: AniRoomApp
            private set
        lateinit var http: OkHttpClient
            private set
    }
}
