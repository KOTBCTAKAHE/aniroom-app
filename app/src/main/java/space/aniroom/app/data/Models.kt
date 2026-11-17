package space.aniroom.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class GameCosmetics(
    val level: Int = 1,
    val title: String? = null,
    val frame: String = "none",
    val bubble: String = "default",
    val nickFx: String = "none",
)

@Serializable
data class User(
    val id: Int,
    val username: String,
    val email: String = "",
    val avatar: String? = null,
    val nicknameColor: String? = null,
    val specialTitle: String? = null,
    val game: GameCosmetics? = null,
    val statsPublic: Boolean = true,
    val dataSaver: Boolean = false,
    val qualityMode: String = "auto",
    val doNotDisturb: Boolean = false,
    val watchSeconds: Double = 0.0,
    val createdAt: String = "",
    val bio: String? = null,
    val isAdmin: Boolean = false,
    val tzOffset: Int? = null,
    val profileCover: String? = null,
    val favoriteTitles: List<String> = emptyList(),
    val usernameChangedAt: String? = null,
    val listVisibility: String? = null,
    val statusVisibility: String? = null,
    val allowInvites: Boolean = true,
    val allowFriendRequests: Boolean = true,
    val notifyFriendWatching: Boolean = true,
    val notifyNewEpisodes: Boolean = true,
    val achievementsPublic: Boolean = true,
    val dndFrom: String? = null,
    val dndTo: String? = null,
    val clientPrefs: JsonObject? = null,
)

@Serializable
data class AuthResponse(val accessToken: String, val tokenType: String = "bearer", val user: User)

@Serializable
data class Participant(
    val id: Int,
    val username: String = "",
    val avatar: String? = null,
    val nicknameColor: String? = null,
    val specialTitle: String? = null,
    val game: GameCosmetics? = null,
    val isOwner: Boolean = false,
    val isOnline: Boolean = false,
    val sid: String? = null,
    val buffering: Boolean = false,
    val pingMs: Double? = null,
    val driftMs: Double? = null,
) {
    val online: Boolean get() = isOnline || sid != null
}

@Serializable
data class PlaybackState(
    val playing: Boolean = false,
    val currentTime: Double = 0.0,
    val playbackRate: Double = 1.0,
    val serverTime: Double = 0.0,
    val lastStateUpdateAt: String = "",
    val reason: String? = null,
)

@Serializable
data class AnimeState(
    val shikimoriId: String? = null,
    val title: String? = null,
    val poster: String? = null,
    val translationId: String? = null,
    val translationName: String? = null,
    val quality: Int? = null,
    val episode: Int = 0,
    val episodesTotal: Int = 0,
    val skipSegments: List<List<Double>> = emptyList(),
)

@Serializable
data class Room(
    val id: Int = 0,
    val roomCode: String,
    val ownerId: Int = 0,
    val title: String = "",
    val createdAt: String = "",
    val lastViewedAt: String? = null,
    val liveCount: Int = 0,
    val isPublic: Boolean = false,
    val isLocked: Boolean = false,
    val videoUrl: String? = null,
    val anime: AnimeState? = null,
    val playback: PlaybackState = PlaybackState(),
    val participants: List<Participant> = emptyList(),
)

@Serializable
data class SyncState(
    val roomCode: String = "",
    val videoUrl: String? = null,
    val playing: Boolean = false,
    val currentTime: Double = 0.0,
    val playbackRate: Double = 1.0,
    val serverTime: Double = 0.0,
    val lastStateUpdateAt: String = "",
    val anime: AnimeState? = null,
    val reason: String = "",
)

@Serializable
data class Telemetry(
    val participants: List<Participant> = emptyList(),
    val serverTime: Double = 0.0,
    val authoritativeTime: Double = 0.0,
)

@Serializable
data class MessageReaction(val reaction: String, val count: Int = 0, val reactedByMe: Boolean = false)

@Serializable
data class MessageReply(
    val id: Long = 0,
    val userId: Int = 0,
    val username: String = "",
    val nicknameColor: String? = null,
    val text: String = "",
    val createdAt: String = "",
)

@Serializable
data class Message(
    val id: Long,
    val roomId: Int = 0,
    val userId: Int = 0,
    val username: String = "",
    val nicknameColor: String? = null,
    val specialTitle: String? = null,
    val game: GameCosmetics? = null,
    val avatar: String? = null,
    val replyToId: Long? = null,
    val replyTo: MessageReply? = null,
    val text: String = "",
    val stickerUrl: String? = null,
    val createdAt: String = "",
    val reactions: List<MessageReaction> = emptyList(),
    val deliveryState: String? = null,
    val clientMessageId: String? = null,
)

@Serializable
data class Sticker(val id: Int, val name: String = "", val image: String = "", val createdAt: String = "")

@Serializable
data class TypingUser(val id: Int, val username: String = "", val avatar: String? = null)

@Serializable
data class AnimeSearchResult(
    val shikimoriId: String,
    val title: String = "",
    val titleOrig: String? = null,
    val year: Int? = null,
    val poster: String? = null,
    val screenshots: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val score: JsonElement? = null,
    val kind: String? = null,
    val description: String? = null,
) {
    val scoreText: String?
        get() = (score as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
}

@Serializable
data class AnimeTranslation(val id: String, val type: String? = null, val name: String? = null, val seriesRange: String? = null)

@Serializable
data class AnimeTranslations(val translations: List<AnimeTranslation> = emptyList(), val seriesCount: Int = 0)

@Serializable
data class AnimeLink(
    val link: String = "",
    val maxQuality: Int = 720,
    val qualities: List<Int> = emptyList(),
    val skipSegments: List<List<Double>> = emptyList(),
)

@Serializable
data class AnimeFullPage(
    val shikimoriId: String = "",
    val title: String? = null,
    val originalTitle: String? = null,
    val description: String? = null,
    val picture: String? = null,
    val score: String? = null,
    val status: String? = null,
    val studio: String? = null,
    val type: String? = null,
    val episodes: String? = null,
    val genres: List<String> = emptyList(),
    val screenshots: List<String> = emptyList(),
)

@Serializable
data class Progress(
    val shikimoriId: String? = null,
    val translationId: String? = null,
    val episode: Int = 0,
    val positionSec: Double = 0.0,
    val durationSec: Double = 0.0,
    val quality: Int? = null,
    val updatedAt: String = "",
    val roomCode: String? = null,
)

@Serializable
data class EpisodeProgress(val episode: Int, val positionSec: Double = 0.0, val durationSec: Double = 0.0, val watched: Boolean = false)

@Serializable
data class RoomProgress(val progress: Progress? = null, val episodes: List<EpisodeProgress> = emptyList())

@Serializable
data class AppNotification(
    val id: Long,
    val kind: String = "",
    val payload: JsonObject = JsonObject(emptyMap()),
    val isRead: Boolean = false,
    val createdAt: String = "",
)

@Serializable
data class Friendship(
    val level: Int = 0,
    val name: String = "",
    val hours: Double = 0.0,
    val nextHours: Double? = null,
    val nextName: String? = null,
    val progress: Double = 0.0,
)

@Serializable
data class Friend(
    val id: Int,
    val username: String = "",
    val avatar: String? = null,
    val nicknameColor: String? = null,
    val online: Boolean = false,
    val roomCode: String? = null,
    val watchSeconds: Double? = null,
    val friendship: Friendship? = null,
    val game: GameCosmetics? = null,
)

@Serializable
data class FriendUser(val id: Int, val username: String = "", val avatar: String? = null, val nicknameColor: String? = null)

@Serializable
data class FriendRequestItem(val id: Int, val user: FriendUser, val createdAt: String = "")

@Serializable
data class FavoriteTitle(val shikimoriId: String = "", val title: String = "", val poster: String? = null, val status: String? = null)

@Serializable
data class PublicProfile(
    val id: Int,
    val username: String = "",
    val avatar: String? = null,
    val nicknameColor: String? = null,
    val specialTitle: String? = null,
    val game: GameCosmetics? = null,
    val statsPublic: Boolean = true,
    val watchSeconds: Double = 0.0,
    val createdAt: String = "",
    val isBlocked: Boolean = false,
    val isFriend: Boolean = false,
    val bio: String? = null,
    val profileCover: String? = null,
    val favorites: List<FavoriteTitle> = emptyList(),
    val listVisible: Boolean = true,
    val canInvite: Boolean = false,
    val acceptsFriendRequests: Boolean = true,
)

@Serializable
data class ActivityDay(val date: String = "", val watchSeconds: Double = 0.0)

@Serializable
data class TopAnime(val title: String = "", val watchSeconds: Double = 0.0, val episodesCount: Int = 0, val poster: String? = null)

@Serializable
data class ProfileStats(
    @SerialName("private") val isPrivate: Boolean = false,
    val watchSeconds: Double? = null,
    val episodesWatched: Int? = null,
    val roomsWatched: Int? = null,
    val friendsCount: Int? = null,
    val streakDays: Int? = null,
    val activity: List<ActivityDay> = emptyList(),
    val topAnime: List<TopAnime> = emptyList(),
)

@Serializable
data class Badge(
    val key: String,
    val title: String = "",
    val description: String = "",
    val earned: Boolean = false,
    val earnedAt: String? = null,
    val featured: Boolean = false,
    val earnedPercent: Double? = null,
    val progress: Double = 0.0,
    val progressValue: Double? = null,
    val progressTarget: Double? = null,
    val rarity: String = "",
    val icon: String? = null,
    val isSpecial: Boolean = false,
    val secret: Boolean = false,
    val xp: Int? = null,
)

@Serializable
data class AnimeListEntry(
    val shikimoriId: String,
    val title: String = "",
    val poster: String? = null,
    val status: String = "planned",
    val episodesWatched: Int = 0,
    val episodesTotal: Int = 0,
    val score: Double = 0.0,
    val source: String = "",
    val updatedAt: String? = null,
)

@Serializable
data class AnimeListPage(
    val hidden: Boolean = false,
    val entries: List<AnimeListEntry> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val total: Int = 0,
)

@Serializable
data class StreakInfo(val days: Int = 0, val watchedToday: Boolean = false, val atRisk: Boolean = false, val best: Int = 0)

@Serializable
data class QuestItem(
    val key: String,
    val title: String = "",
    val icon: String = "",
    val xp: Int = 0,
    val value: Double = 0.0,
    val target: Double = 1.0,
    val done: Boolean = false,
)

@Serializable
data class QuestBoard(val week: String = "", val resetsAt: String = "", val items: List<QuestItem> = emptyList())

@Serializable
data class GameMe(
    val level: Int = 1,
    val xp: Int = 0,
    val levelStart: Int = 0,
    val levelEnd: Int = 0,
    val intoLevel: Int = 0,
    val levelNeed: Int = 1,
    val toNext: Int = 0,
    val progress: Double = 0.0,
    val streak: StreakInfo? = null,
    val quests: QuestBoard? = null,
    val weekXp: Int = 0,
    val cosmetics: GameCosmetics? = null,
    val titleKey: String? = null,
    val equipped: Equipped = Equipped(),
    val catalog: Catalog = Catalog(),
)

@Serializable
data class Equipped(val titleKey: String? = null, val frame: String = "none", val bubble: String = "default", val nickFx: String = "none")

@Serializable
data class CatalogItem(val key: String, val name: String = "", val level: Int = 0, val unlocked: Boolean = false, val fromBadge: Boolean = false, val rarity: String? = null)

@Serializable
data class Catalog(
    val frames: List<CatalogItem> = emptyList(),
    val bubbles: List<CatalogItem> = emptyList(),
    val nickFx: List<CatalogItem> = emptyList(),
    val titles: List<CatalogItem> = emptyList(),
)

@Serializable
data class BlockedUser(val id: Int, val username: String = "", val avatar: String? = null, val nicknameColor: String? = null, val blockedAt: String = "")

@Serializable
data class ShikimoriImportResult(val added: Int = 0, val updated: Int = 0, val unchanged: Int = 0, val total: Int = 0, val nickname: String = "")

@Serializable
data class GithubAsset(val name: String = "", val size: Long = 0, val browserDownloadUrl: String = "")

@Serializable
data class GithubRelease(val tagName: String = "", val name: String? = null, val body: String? = null, val publishedAt: String? = null, val assets: List<GithubAsset> = emptyList())

@Serializable
data class LeaderboardRow(
    val userId: Int,
    val username: String = "",
    val avatar: String? = null,
    val nicknameColor: String? = null,
    val level: Int = 1,
    val weekXp: Int = 0,
    val weekEpisodes: Int = 0,
    val isMe: Boolean = false,
    val rank: Int = 0,
)

@Serializable
data class Leaderboard(val week: String = "", val resetsAt: String = "", val rows: List<LeaderboardRow> = emptyList())

@Serializable
data class LiveReaction(
    val id: String = "",
    val kind: String = "fire",
    val userId: Int = 0,
    val username: String = "",
    val nicknameColor: String? = null,
)

@Serializable
data class EpisodeVoteState(
    val roomCode: String = "",
    val active: Boolean = false,
    val episode: Int? = null,
    val nextEpisode: Int? = null,
    val endsAt: Double? = null,
    val serverNow: Double? = null,
    val duration: Double? = null,
    val yes: List<Int> = emptyList(),
    val no: List<Int> = emptyList(),
    val voters: Int = 0,
)

@Serializable
data class BestMoment(val at: Double = 0.0, val count: Int = 0, val kind: String = "", val emoji: String = "")

@Serializable
data class EpisodeVoteResult(
    val roomCode: String = "",
    val result: String = "next",
    val yes: Int = 0,
    val no: Int = 0,
    val episode: Int = 0,
    val nextEpisode: Int = 0,
    val decidedBy: String? = null,
    val bestMoment: BestMoment? = null,
)

@Serializable
data class RatingSummary(val avg: Double? = null, val count: Int = 0)

@Serializable
data class EpisodeRatingSummary(val episode: Int, val avg: Double? = null, val count: Int = 0, val mine: Int? = null)

@Serializable
data class RoomRatings(
    val shikimoriId: String? = null,
    val title: RatingSummary = RatingSummary(),
    val episodes: List<EpisodeRatingSummary> = emptyList(),
)

@Serializable
data class RatingsUpdate(
    val roomCode: String = "",
    val shikimoriId: String? = null,
    val title: RatingSummary = RatingSummary(),
    val episode: EpisodeRatingSummary? = null,
)

@Serializable
data class XpReason(val label: String = "", val xp: Int = 0, val quiet: Boolean = false)

@Serializable
data class XpEvent(val gained: Int = 0, val level: Int = 1, val levelUp: Boolean = false, val reasons: List<XpReason> = emptyList())

@Serializable
data class LevelUpEvent(val level: Int = 1, val fromLevel: Int = 0)

@Serializable
data class QuestEvent(val key: String = "", val title: String = "", val xp: Int = 0, val icon: String = "")

@Serializable
data class StreakEvent(val days: Int = 0, val bonus: Int = 0)

@Serializable
data class AchievementPayload(val id: String = "", val title: String = "", val description: String = "", val rarity: String = "common")

@Serializable
data class AnimeLoadingPayload(val reason: String? = null, val episode: Int? = null, val title: String? = null)

@Serializable
data class ProfileUpdate(val userId: Int, val username: String = "", val avatar: String? = null, val nicknameColor: String? = null)

@Serializable
data class SystemNoticePayload(val text: String = "", val createdAt: String? = null, val at: String? = null)

@Serializable
data class ReactionsPayload(val messageId: Long, val reactions: List<MessageReaction> = emptyList())

@Serializable
data class MessageDeleted(val messageId: Long)

@Serializable
data class ChatAck(val ok: Boolean = false, val message: Message? = null, val error: String? = null)

@Serializable
data class SimpleAck(val ok: Boolean = false, val error: String? = null)

@Serializable
data class RoomBan(val id: Int, val username: String = "", val avatar: String? = null, val nicknameColor: String? = null)

/** Body for creating a room / setting an anime. */
@Serializable
data class AnimeChoice(
    val shikimoriId: String,
    val title: String,
    val poster: String?,
    val translationId: String,
    val translationName: String,
    val quality: Int,
    val episode: Int,
    val episodesTotal: Int,
    val skipSegments: List<List<Double>> = emptyList(),
)

/** Locally generated lines in the room feed. */
data class SystemNotice(val id: String, val text: String, val createdAt: Long = System.currentTimeMillis())
