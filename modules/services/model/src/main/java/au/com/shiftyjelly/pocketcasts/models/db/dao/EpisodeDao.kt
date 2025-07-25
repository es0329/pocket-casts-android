package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.helper.QueryHelper
import au.com.shiftyjelly.pocketcasts.models.db.helper.UuidCount
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeDownloadFailureStatistics
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class EpisodeDao {

    @RawQuery(observedEntities = [PodcastEpisode::class, Podcast::class])
    abstract fun findEpisodesBlocking(query: SupportSQLiteQuery): List<PodcastEpisode>

    @RawQuery(observedEntities = [PodcastEpisode::class, Podcast::class])
    abstract fun findEpisodesRxFlowable(query: SupportSQLiteQuery): Flowable<List<PodcastEpisode>>

    @RawQuery(observedEntities = [PodcastEpisode::class, Podcast::class])
    abstract fun countRxFlowable(query: SupportSQLiteQuery): Flowable<Int>

    @Query("SELECT * FROM podcast_episodes WHERE uuid = :uuid")
    abstract suspend fun findByUuid(uuid: String): PodcastEpisode?

    @Query("SELECT * FROM podcast_episodes WHERE uuid IN (:episodeUuids)")
    abstract suspend fun findByUuids(episodeUuids: List<String>): List<PodcastEpisode>

    @Query("SELECT count(*) FROM podcast_episodes WHERE podcast_id = :podcastUuid AND played_up_to > (duration / 2)")
    abstract suspend fun countPlayedEpisodes(podcastUuid: String): Int

    @Query("SELECT count(*) FROM podcast_episodes WHERE podcast_id = :podcastUuid")
    abstract suspend fun countEpisodesByPodcast(podcastUuid: String): Int

    @Query("SELECT * FROM podcast_episodes WHERE uuid = :uuid")
    abstract fun findByUuidRxMaybe(uuid: String): Maybe<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE uuid = :uuid")
    abstract fun findByUuidFlow(uuid: String): Flow<PodcastEpisode?>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE download_task_id IS NOT NULL")
    abstract fun findDownloadingEpisodesRxFlowable(): Flowable<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE UPPER(title) = UPPER(:query) LIMIT 1")
    abstract suspend fun findFirstBySearchQuery(query: String): PodcastEpisode?

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE last_playback_interaction_sync_status <> 1 AND last_playback_interaction_date IS NOT NULL ORDER BY last_playback_interaction_date DESC LIMIT 1000")
    abstract fun findEpisodesForHistorySyncBlocking(): List<PodcastEpisode>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE playing_status = :episodePlayingStatus AND archived = :archived AND podcast_id = :podcastUuid")
    abstract fun findByEpisodePlayingAndArchiveStatusBlocking(podcastUuid: String, episodePlayingStatus: EpisodePlayingStatus, archived: Boolean): List<PodcastEpisode>

    @Query(
        """
        SELECT
          *
        FROM
          podcast_episodes
        WHERE
          podcast_id = :podcastUuid
        ORDER BY (CASE
          WHEN UPPER(title) LIKE 'THE %' THEN SUBSTR(UPPER(title), 5)
          WHEN UPPER(title) LIKE 'A %' THEN SUBSTR(UPPER(title), 3)
          WHEN UPPER(title) LIKE 'AN %' THEN SUBSTR(UPPER(title), 4)
          ELSE UPPER(title)
        END) ASC
    """,
    )
    abstract fun findByPodcastOrderTitleAscBlocking(podcastUuid: String): List<PodcastEpisode>

    @Query(
        """
        SELECT
          *
        FROM
          podcast_episodes
        WHERE
          podcast_id = :podcastUuid
        ORDER BY (CASE
          WHEN UPPER(title) LIKE 'THE %' THEN SUBSTR(UPPER(title), 5)
          WHEN UPPER(title) LIKE 'A %' THEN SUBSTR(UPPER(title), 3)
          WHEN UPPER(title) LIKE 'AN %' THEN SUBSTR(UPPER(title), 4)
          ELSE UPPER(title)
        END) ASC
    """,
    )
    abstract suspend fun findByPodcastOrderTitleAsc(podcastUuid: String): List<PodcastEpisode>

    @Query(
        """
        SELECT
          *
        FROM
          podcast_episodes
        WHERE
          podcast_id = :podcastUuid
        ORDER BY (CASE
          WHEN UPPER(title) LIKE 'THE %' THEN SUBSTR(UPPER(title), 5)
          WHEN UPPER(title) LIKE 'A %' THEN SUBSTR(UPPER(title), 3)
          WHEN UPPER(title) LIKE 'AN %' THEN SUBSTR(UPPER(title), 4)
          ELSE UPPER(title)
        END) DESC
    """,
    )
    abstract fun findByPodcastOrderTitleDescBlocking(podcastUuid: String): List<PodcastEpisode>

    @Query(
        """
        SELECT
          *
        FROM
          podcast_episodes
        WHERE
          podcast_id = :podcastUuid
        ORDER BY (CASE
          WHEN UPPER(title) LIKE 'THE %' THEN SUBSTR(UPPER(title), 5)
          WHEN UPPER(title) LIKE 'A %' THEN SUBSTR(UPPER(title), 3)
          WHEN UPPER(title) LIKE 'AN %' THEN SUBSTR(UPPER(title), 4)
          ELSE UPPER(title)
        END) DESC
    """,
    )
    abstract suspend fun findByPodcastOrderTitleDesc(podcastUuid: String): List<PodcastEpisode>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date ASC")
    abstract fun findByPodcastOrderPublishedDateAscBlocking(podcastUuid: String): List<PodcastEpisode>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date ASC")
    abstract suspend fun findByPodcastOrderPublishedDateAsc(podcastUuid: String): List<PodcastEpisode>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC")
    abstract fun findByPodcastOrderPublishedDateDescBlocking(podcastUuid: String): List<PodcastEpisode>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC")
    abstract suspend fun findByPodcastOrderPublishedDateDesc(podcastUuid: String): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid AND playing_status != 2 AND archived = 0 ORDER BY published_date DESC LIMIT 1")
    abstract fun findLatestUnfinishedEpisodeByPodcastBlocking(podcastUuid: String): PodcastEpisode?

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY duration ASC")
    abstract fun findByPodcastOrderDurationAscBlocking(podcastUuid: String): List<PodcastEpisode>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY duration ASC")
    abstract suspend fun findByPodcastOrderDurationAsc(podcastUuid: String): List<PodcastEpisode>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY duration DESC")
    abstract fun findByPodcastOrderDurationDescBlocking(podcastUuid: String): List<PodcastEpisode>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY duration DESC")
    abstract suspend fun findByPodcastOrderDurationDesc(podcastUuid: String): List<PodcastEpisode>

    // Find new episodes to display in notifications.
    @Query(
        """SELECT podcast_episodes.*
        FROM podcast_episodes
        JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid
        WHERE podcasts.subscribed = 1 AND podcasts.show_notifications = 1
        AND (podcasts.added_date IS NULL OR (podcasts.added_date IS NOT NULL AND podcasts.added_date < :date AND podcasts.added_date != podcast_episodes.added_date))
        AND podcast_episodes.archived = 0 AND podcast_episodes.playing_status = :playingStatus AND podcast_episodes.added_date >= :date
        ORDER BY podcast_episodes.added_date DESC, podcast_episodes.published_date DESC LIMIT 100""",
    )
    abstract fun findNotificationEpisodesBlocking(date: Date, playingStatus: Int = EpisodePlayingStatus.NOT_PLAYED.ordinal): List<PodcastEpisode>

    @Transaction
    @Query(
        """
        SELECT
          *
        FROM
          podcast_episodes
        WHERE
          podcast_id = :podcastUuid
        ORDER BY (CASE
          WHEN UPPER(title) LIKE 'THE %' THEN SUBSTR(UPPER(title), 5)
          WHEN UPPER(title) LIKE 'A %' THEN SUBSTR(UPPER(title), 3)
          WHEN UPPER(title) LIKE 'AN %' THEN SUBSTR(UPPER(title), 4)
          ELSE UPPER(title)
        END) ASC
    """,
    )
    abstract fun findByPodcastOrderTitleAscRxFlowable(podcastUuid: String): Flowable<List<PodcastEpisode>>

    @Transaction
    @Query(
        """
        SELECT
          *
        FROM
          podcast_episodes
        WHERE
          podcast_id = :podcastUuid
        ORDER BY (CASE
          WHEN UPPER(title) LIKE 'THE %' THEN SUBSTR(UPPER(title), 5)
          WHEN UPPER(title) LIKE 'A %' THEN SUBSTR(UPPER(title), 3)
          WHEN UPPER(title) LIKE 'AN %' THEN SUBSTR(UPPER(title), 4)
          ELSE UPPER(title)
        END) DESC
    """,
    )
    abstract fun findByPodcastOrderTitleDescRxFlowable(podcastUuid: String): Flowable<List<PodcastEpisode>>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date ASC")
    abstract fun findByPodcastOrderPublishedDateAscRxFlowable(podcastUuid: String): Flowable<List<PodcastEpisode>>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC")
    abstract fun findByPodcastOrderPublishedDateDescFlowable(podcastUuid: String): Flowable<List<PodcastEpisode>>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY duration ASC")
    abstract fun findByPodcastOrderDurationAscFlowable(podcastUuid: String): Flowable<List<PodcastEpisode>>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY duration DESC")
    abstract fun findByPodcastOrderDurationDescFlowable(podcastUuid: String): Flowable<List<PodcastEpisode>>

    @Query("UPDATE podcast_episodes SET downloaded_error_details = NULL, episode_status = :episodeStatusNotDownloaded WHERE episode_status = :episodeStatusFailed")
    abstract fun clearAllDownloadErrorsBlocking(episodeStatusNotDownloaded: EpisodeStatusEnum, episodeStatusFailed: EpisodeStatusEnum)

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC, added_date DESC LIMIT 1")
    abstract fun findLatestBlocking(podcastUuid: String): PodcastEpisode?

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC, added_date DESC LIMIT 1")
    abstract fun findLatestRxMaybe(podcastUuid: String): Maybe<PodcastEpisode>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE (download_task_id IS NOT NULL OR episode_status == :downloadEpisodeStatusEnum OR (episode_status == :failedEpisodeStatusEnum AND last_download_attempt_date > :failedDownloadCutoff AND archived == 0)) ORDER BY last_download_attempt_date DESC")
    abstract fun findDownloadingEpisodesIncludingFailedRxFlowable(failedDownloadCutoff: Long, failedEpisodeStatusEnum: EpisodeStatusEnum = EpisodeStatusEnum.DOWNLOAD_FAILED, downloadEpisodeStatusEnum: EpisodeStatusEnum = EpisodeStatusEnum.DOWNLOADED): Flowable<List<PodcastEpisode>>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE (download_task_id IS NOT NULL AND episode_status == :notDownloaded)")
    abstract suspend fun findStaleDownloads(notDownloaded: EpisodeStatusEnum = EpisodeStatusEnum.NOT_DOWNLOADED): List<PodcastEpisode>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE (download_task_id IS NOT NULL AND episode_status != :status)")
    abstract suspend fun findNotFinishedDownloads(status: EpisodeStatusEnum = EpisodeStatusEnum.DOWNLOADED): List<PodcastEpisode>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE episode_status == :downloadEpisodeStatusEnum ORDER BY last_download_attempt_date DESC")
    abstract fun findDownloadedEpisodesRxFlowable(downloadEpisodeStatusEnum: EpisodeStatusEnum = EpisodeStatusEnum.DOWNLOADED): Flowable<List<PodcastEpisode>>

    @Query("SELECT COUNT(*) FROM podcast_episodes WHERE episode_status == :downloadEpisodeStatusEnum AND playing_status == :playingStatus")
    abstract suspend fun downloadedEpisodesThatHaveNotBeenPlayedCount(
        downloadEpisodeStatusEnum: EpisodeStatusEnum = EpisodeStatusEnum.DOWNLOADED,
        playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.NOT_PLAYED,
    ): Int

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE starred = 1")
    abstract fun findStarredEpisodesRxFlowable(): Flowable<List<PodcastEpisode>>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE starred = 1")
    abstract suspend fun findStarredEpisodes(): List<PodcastEpisode>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE last_playback_interaction_date IS NOT NULL AND last_playback_interaction_date > 0 ORDER BY last_playback_interaction_date DESC LIMIT 1000")
    abstract fun findPlaybackHistoryRxFlowable(): Flowable<List<PodcastEpisode>>

    @Transaction
    @Query(
        """
        SELECT podcast_episodes.*
        FROM podcast_episodes
        LEFT JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid
        WHERE last_playback_interaction_date IS NOT NULL
          AND last_playback_interaction_date > 0
          AND (UPPER(podcast_episodes.title) LIKE '%' || UPPER(:query) || '%'  ESCAPE '\'
               OR UPPER(podcasts.title) LIKE '%' || UPPER(:query) || '%'  ESCAPE '\')
        ORDER BY last_playback_interaction_date DESC
        LIMIT 100
    """,
    )
    abstract fun filteredPlaybackHistoryFlow(query: String): Flow<List<PodcastEpisode>>

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE last_playback_interaction_date IS NOT NULL AND last_playback_interaction_date > 0 ORDER BY last_playback_interaction_date DESC LIMIT 1000")
    abstract suspend fun findPlaybackHistoryEpisodes(): List<PodcastEpisode>

    @Update
    abstract fun updateBlocking(episode: PodcastEpisode)

    @Update
    abstract suspend fun update(episode: PodcastEpisode)

    @Delete
    abstract fun deleteBlocking(episode: PodcastEpisode)

    @Delete
    abstract fun deleteAllBlocking(episode: List<PodcastEpisode>)

    @Delete
    abstract suspend fun deleteAll(episode: List<PodcastEpisode>)

    @Query("DELETE FROM podcast_episodes")
    abstract suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertBlocking(episode: PodcastEpisode)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(episode: PodcastEpisode)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertAllBlocking(episodes: List<PodcastEpisode>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAll(episodes: List<PodcastEpisode>)

    @Query("UPDATE podcast_episodes SET file_type = :fileType WHERE uuid = :uuid")
    abstract fun updateFileTypeBlocking(fileType: String, uuid: String)

    @Query("UPDATE podcast_episodes SET size_in_bytes = :sizeInBytes WHERE uuid = :uuid")
    abstract fun updateSizeInBytesBlocking(sizeInBytes: Long, uuid: String)

    @Query("UPDATE podcast_episodes SET download_url = :url WHERE uuid = :uuid")
    abstract suspend fun updateDownloadUrl(url: String, uuid: String)

    @Query("UPDATE podcast_episodes SET download_task_id = :taskId WHERE uuid = :uuid")
    abstract suspend fun updateDownloadTaskId(uuid: String, taskId: String?)

    @Query("UPDATE podcast_episodes SET last_download_attempt_date = :lastDownloadAttemptDate WHERE uuid = :uuid")
    abstract fun updateLastDownloadAttemptDateBlocking(lastDownloadAttemptDate: Date, uuid: String)

    @Query("UPDATE podcast_episodes SET downloaded_error_details = :errorMessage, episode_status = :episodeStatus WHERE uuid = :uuid")
    abstract fun updateDownloadErrorBlocking(uuid: String, errorMessage: String?, episodeStatus: EpisodeStatusEnum)

    @Query("UPDATE podcast_episodes SET downloaded_file_path = :downloadedFilePath WHERE uuid = :uuid")
    abstract fun updateDownloadedFilePathBlocking(downloadedFilePath: String, uuid: String)

    @Query("UPDATE podcast_episodes SET auto_download_status = :autoDownloadStatus WHERE uuid = :uuid")
    abstract suspend fun updateAutoDownloadStatus(autoDownloadStatus: Int, uuid: String)

    @Query("UPDATE podcast_episodes SET play_error_details = :playErrorDetails WHERE uuid = :uuid")
    abstract fun updatePlayErrorDetailsBlocking(playErrorDetails: String?, uuid: String)

    @Query("UPDATE podcast_episodes SET downloaded_error_details = :downloadErrorDetails WHERE uuid = :uuid")
    abstract fun updateDownloadErrorDetailsBlocking(downloadErrorDetails: String?, uuid: String)

    @Query("UPDATE podcast_episodes SET episode_status = :episodeStatus WHERE uuid = :uuid")
    abstract suspend fun updateEpisodeStatus(episodeStatus: EpisodeStatusEnum, uuid: String)

    @Query("UPDATE podcast_episodes SET episode_status = :episodeStatus")
    abstract fun updateAllEpisodeStatusBlocking(episodeStatus: EpisodeStatusEnum)

    @Query("UPDATE podcast_episodes SET last_playback_interaction_date = 0 WHERE last_playback_interaction_date <= :lastCleared")
    abstract fun clearEpisodePlaybackInteractionDatesBeforeBlocking(lastCleared: Date)

    @Query("UPDATE podcast_episodes SET last_playback_interaction_date = 0, last_playback_interaction_sync_status = 1")
    abstract suspend fun clearAllEpisodePlaybackInteractions()

    @Query("UPDATE podcast_episodes SET last_playback_interaction_date = 0, last_playback_interaction_sync_status = 2 WHERE uuid IN (:episodeUuids)")
    abstract suspend fun clearEpisodePlaybackInteractions(episodeUuids: List<String>)

    @Query("UPDATE podcast_episodes SET last_playback_interaction_sync_status = 1")
    abstract fun markPlaybackHistorySyncedBlocking()

    @Query("SELECT COUNT(*) FROM podcast_episodes")
    abstract suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM podcast_episodes WHERE uuid = :uuid")
    abstract fun countByUuidBlocking(uuid: String): Int

    fun existsBlocking(uuid: String): Boolean {
        return countByUuidBlocking(uuid) != 0
    }

    fun existsRxSingle(uuid: String): Single<Boolean> {
        return Single.fromCallable { existsBlocking(uuid) }
    }

    @Query("UPDATE podcast_episodes SET starred_modified = NULL, archived_modified = NULL, duration_modified = NULL, played_up_to_modified = NULL, playing_status_modified = NULL WHERE uuid IN (:episodeUuids)")
    abstract suspend fun markAllSynced(episodeUuids: List<String>)

    @Query("SELECT podcasts.uuid AS uuid, count(podcast_episodes.uuid) AS count FROM podcast_episodes, podcasts WHERE podcast_episodes.podcast_id = podcasts.uuid AND (podcast_episodes.playing_status = :playingStatusNotPlayed OR podcast_episodes.playing_status = :playingStatusInProgress) AND podcast_episodes.archived = 0 GROUP BY podcasts.uuid")
    protected abstract fun podcastToUnfinishedEpisodeCount(playingStatusNotPlayed: Int = EpisodePlayingStatus.NOT_PLAYED.ordinal, playingStatusInProgress: Int = EpisodePlayingStatus.IN_PROGRESS.ordinal): Flow<List<UuidCount>>

    @Query("SELECT podcasts.uuid AS uuid, count(podcast_episodes.uuid) AS count FROM podcast_episodes, podcasts WHERE podcasts.latest_episode_uuid = podcast_episodes.uuid AND podcast_episodes.playing_status = :playingStatusNotPlayed AND podcast_episodes.archived = 0 GROUP BY podcasts.uuid")
    protected abstract fun podcastToLatestEpisodeCount(playingStatusNotPlayed: Int = EpisodePlayingStatus.NOT_PLAYED.ordinal): Flow<List<UuidCount>>

    fun observeUuidToUnfinishedEpisodeCount(): Flow<Map<String, Int>> {
        return podcastToUnfinishedEpisodeCount().map { it.associateBy({ it.uuid }, { it.count }) }
    }

    fun observeUuidToLatestEpisodeCount(): Flow<Map<String, Int>> {
        return podcastToLatestEpisodeCount().map { it.associateBy({ it.uuid }, { it.count }) }
    }

    @Query("SELECT podcast_episodes.* FROM podcast_episodes JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid WHERE podcasts.subscribed = 1 AND podcast_episodes.playing_status != 2 AND podcast_episodes.archived = 0 ORDER BY podcast_episodes.published_date DESC LIMIT 1")
    abstract fun findLatestEpisodeToPlayBlocking(): PodcastEpisode?

    @Query("UPDATE podcast_episodes SET starred = :starred, starred_modified = :modified WHERE uuid = :uuid")
    abstract suspend fun updateStarred(starred: Boolean, modified: Long, uuid: String)

    @Query("UPDATE podcast_episodes SET starred = :starred, starred_modified = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun updateAllStarred(episodesUUIDs: List<String>, starred: Boolean, modified: Long)

    @Query("UPDATE podcast_episodes SET archived = 0, archived_modified = :modified, last_archive_interaction_date = :modified, exclude_from_episode_limit = 1 WHERE uuid = :uuid")
    abstract fun unarchiveBlocking(uuid: String, modified: Long)

    @Query("UPDATE podcast_episodes SET archived = :archived, archived_modified = :modified, last_archive_interaction_date = :modified WHERE uuid = :uuid")
    abstract fun updateArchivedBlocking(archived: Boolean, modified: Long, uuid: String)

    @Query("UPDATE podcast_episodes SET archived = :archived, last_archive_interaction_date = :modified WHERE uuid = :uuid")
    abstract fun updateArchivedNoSyncBlocking(archived: Boolean, modified: Long, uuid: String)

    @Query("UPDATE podcast_episodes SET playing_status = :playingStatus, playing_status_modified = :modified WHERE uuid = :uuid")
    abstract fun updatePlayingStatusBlocking(playingStatus: EpisodePlayingStatus, modified: Long, uuid: String)

    @Query("UPDATE podcast_episodes SET last_playback_interaction_date = :modified, last_playback_interaction_sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updatePlaybackInteractionDate(uuid: String, modified: Long)

    @Query("UPDATE podcast_episodes SET duration = :duration, duration_modified = :modified WHERE uuid = :uuid")
    abstract fun updateDurationBlocking(duration: Double, modified: Long, uuid: String)

    @Query("UPDATE podcast_episodes SET duration = :duration WHERE uuid = :uuid")
    abstract fun updateDurationNoSyncBlocking(duration: Double, uuid: String)

    @Query("UPDATE podcast_episodes SET played_up_to = :playedUpTo, played_up_to_modified = :modified WHERE uuid = :uuid AND (played_up_to IS NULL OR played_up_to < :playedUpToMin OR played_up_to > :playedUpToMax)")
    abstract fun updatePlayedUpToIfChangedBlocking(playedUpTo: Double, playedUpToMin: Double, playedUpToMax: Double, modified: Long, uuid: String)

    fun countWhereBlocking(queryAfterWhere: String, appDatabase: AppDatabase): Int {
        val result = QueryHelper.firstRowArrayBlocking("SELECT count(*) FROM podcast_episodes JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid WHERE podcasts.subscribed = 1 AND $queryAfterWhere", null, appDatabase) ?: return 0
        val firstResult = result[0] ?: return 0
        return Integer.parseInt(firstResult)
    }

    @Transaction
    @Query("SELECT * FROM podcast_episodes WHERE (playing_status_modified IS NOT NULL OR played_up_to_modified IS NOT NULL OR duration_modified IS NOT NULL OR archived_modified IS NOT NULL OR starred_modified IS NOT NULL OR deselected_chapters_modified IS NOT NULL) AND uuid IS NOT NULL LIMIT 2000")
    abstract fun findEpisodesToSyncBlocking(): List<PodcastEpisode>

    @Query("SELECT podcast_episodes.* FROM podcasts, podcast_episodes WHERE podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.podcast_id = :podcastUuid AND podcasts.subscribed = 1 AND podcast_episodes.archived = 0 AND (podcast_episodes.added_date < :inactiveTime AND (CASE WHEN podcast_episodes.last_playback_interaction_date IS NULL THEN 0 ELSE podcast_episodes.last_playback_interaction_date END) < :inactiveTime AND (CASE WHEN podcast_episodes.last_download_attempt_date IS NULL THEN 0 ELSE podcast_episodes.last_download_attempt_date END) < :inactiveDate AND (CASE WHEN podcast_episodes.last_archive_interaction_date IS NULL THEN 0 ELSE podcast_episodes.last_archive_interaction_date END) < :inactiveTime )")
    abstract fun findInactiveEpisodesBlocking(podcastUuid: String, inactiveDate: Date, inactiveTime: Long = inactiveDate.time): List<PodcastEpisode>

    @Query("UPDATE podcast_episodes SET archived = 1, archived_modified = :modified, last_archive_interaction_date = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun archiveAllInList(episodesUUIDs: List<String>, modified: Long)

    @Query("UPDATE podcast_episodes SET archived = 0, archived_modified = :modified, last_archive_interaction_date = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract fun unarchiveAllInListBlocking(episodesUUIDs: List<String>, modified: Long)

    @Query("UPDATE podcast_episodes SET playing_status = :playingStatus, playing_status_modified = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun updateAllPlayingStatus(episodesUUIDs: List<String>, modified: Long, playingStatus: EpisodePlayingStatus)

    @Query("UPDATE podcast_episodes SET playing_status = :playingStatus, playing_status_modified = :modified, played_up_to = 0, played_up_to_modified = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun markAllUnplayed(episodesUUIDs: List<String>, modified: Long, playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.NOT_PLAYED)

    @Query(
        """
        SELECT 
          COUNT(*) AS count, 
          MAX(last_download_attempt_date) AS newest_timestamp,
          MIN(last_download_attempt_date) AS oldest_timestamp 
        FROM 
          podcast_episodes
        WHERE
          episode_status IS 3;
        """,
    )
    abstract suspend fun getFailedDownloadsStatistics(): EpisodeDownloadFailureStatistics

    @Query("SELECT * FROM podcast_episodes LIMIT :limit OFFSET :offset")
    abstract suspend fun getAllPodcastEpisodes(limit: Int, offset: Int): List<PodcastEpisode>
}
