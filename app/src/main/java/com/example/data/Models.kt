package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey
    val id: String,
    val name: String,
    val videosPerWeek: Int,
    val contentType: String,
    val notes: String,
    val colorIndex: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "weeks",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientId")]
)
data class Week(
    @PrimaryKey
    val id: String,
    val clientId: String,
    val label: String,
    val weekNumber: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "videos",
    foreignKeys = [
        ForeignKey(
            entity = Week::class,
            parentColumns = ["id"],
            childColumns = ["weekId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("weekId")]
)
data class Video(
    @PrimaryKey
    val id: String,
    val weekId: String,
    val title: String,
    val status: String, // "pendiente", "en-proceso", "revision", "terminado"
    val note: String,
    val videoIndex: Int, // ordered 0-indexed
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY createdAt DESC")
    fun getAllClients(): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client)

    @Update
    suspend fun updateClient(client: Client)

    @Query("DELETE FROM clients WHERE id = :clientId")
    suspend fun deleteClientById(clientId: String)

    // Weeks
    @Query("SELECT * FROM weeks WHERE clientId = :clientId ORDER BY weekNumber ASC")
    fun getWeeksForClient(clientId: String): Flow<List<Week>>

    @Query("SELECT * FROM weeks WHERE clientId = :clientId ORDER BY weekNumber ASC")
    suspend fun getWeeksForClientSync(clientId: String): List<Week>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeek(week: Week)

    // Videos
    @Query("SELECT * FROM videos WHERE weekId = :weekId ORDER BY videoIndex ASC")
    fun getVideosForWeek(weekId: String): Flow<List<Video>>

    @Query("SELECT * FROM videos WHERE weekId = :weekId ORDER BY videoIndex ASC")
    suspend fun getVideosForWeekSync(weekId: String): List<Video>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: Video)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<Video>)

    @Update
    suspend fun updateVideo(video: Video)
}

@Database(entities = [Client::class, Week::class, Video::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
}

class ClientRepository(private val clientDao: ClientDao) {
    val allClients: Flow<List<Client>> = clientDao.getAllClients()

    suspend fun insertClient(client: Client) = clientDao.insertClient(client)

    suspend fun updateClient(client: Client) = clientDao.updateClient(client)

    suspend fun deleteClientById(id: String) = clientDao.deleteClientById(id)

    fun getWeeksForClient(clientId: String): Flow<List<Week>> = clientDao.getWeeksForClient(clientId)

    suspend fun getWeeksForClientSync(clientId: String): List<Week> = clientDao.getWeeksForClientSync(clientId)

    suspend fun insertWeek(week: Week) = clientDao.insertWeek(week)

    fun getVideosForWeek(weekId: String): Flow<List<Video>> = clientDao.getVideosForWeek(weekId)

    suspend fun getVideosForWeekSync(weekId: String): List<Video> = clientDao.getVideosForWeekSync(weekId)

    suspend fun insertVideo(video: Video) = clientDao.insertVideo(video)

    suspend fun insertVideos(videos: List<Video>) = clientDao.insertVideos(videos)

    suspend fun updateVideo(video: Video) = clientDao.updateVideo(video)
}
