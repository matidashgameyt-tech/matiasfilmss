package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Client
import com.example.data.ClientRepository
import com.example.data.Week
import com.example.data.Video
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ClientViewModel(application: Application) : AndroidViewModel(application) {

    private val db = androidx.room.Room.databaseBuilder(
        application,
        AppDatabase::class.java, "matias_films_db"
    ).fallbackToDestructiveMigration().build()

    val repository = ClientRepository(db.clientDao())

    val allClients: StateFlow<List<Client>> = repository.allClients
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedClient = MutableStateFlow<Client?>(null)
    val selectedClient: StateFlow<Client?> = _selectedClient.asStateFlow()

    private val _weeks = MutableStateFlow<List<Week>>(emptyList())
    val weeksOfSelectedClient: StateFlow<List<Week>> = _weeks.asStateFlow()

    private val _selectedWeek = MutableStateFlow<Week?>(null)
    val selectedWeek: StateFlow<Week?> = _selectedWeek.asStateFlow()

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videosOfSelectedWeek: StateFlow<List<Video>> = _videos.asStateFlow()

    private var weeksJob: Job? = null
    private var videosJob: Job? = null

    init {
        // Automatically check if database is empty and load default clean samples if wanted
        viewModelScope.launch {
            allClients.collect { clients ->
                // If the app starts and selectedClient is null, select the first client if available
                if (_selectedClient.value == null && clients.isNotEmpty()) {
                    selectClient(clients.first())
                }
            }
        }
    }

    fun selectClient(client: Client?) {
        _selectedClient.value = client
        weeksJob?.cancel()
        videosJob?.cancel()
        _videos.value = emptyList()

        if (client != null) {
            weeksJob = viewModelScope.launch {
                repository.getWeeksForClient(client.id).collect { weeksList ->
                    _weeks.value = weeksList

                    val currentWeek = _selectedWeek.value
                    val isStillValid = weeksList.any { it.id == currentWeek?.id }
                    if (!isStillValid) {
                        if (weeksList.isNotEmpty()) {
                            selectWeek(weeksList.last())
                        } else {
                            selectWeek(null)
                        }
                    } else {
                        // Keep selected week updated
                        val updatedWeek = weeksList.find { it.id == currentWeek?.id }
                        if (updatedWeek != null && updatedWeek != currentWeek) {
                            _selectedWeek.value = updatedWeek
                        }
                    }
                }
            }
        } else {
            _weeks.value = emptyList()
            selectWeek(null)
        }
    }

    fun selectWeek(week: Week?) {
        _selectedWeek.value = week
        videosJob?.cancel()

        if (week != null) {
            videosJob = viewModelScope.launch {
                repository.getVideosForWeek(week.id).collect { videosList ->
                    _videos.value = videosList
                }
            }
        } else {
            _videos.value = emptyList()
        }
    }

    fun createClient(name: String, videosPerWeek: Int, contentType: String, notes: String) {
        viewModelScope.launch {
            val clientId = UUID.randomUUID().toString()
            val colorIndex = (UUID.randomUUID().hashCode() and Int.MAX_VALUE) % 6

            val newClient = Client(
                id = clientId,
                name = name,
                videosPerWeek = videosPerWeek,
                contentType = contentType,
                notes = notes,
                colorIndex = colorIndex
            )

            repository.insertClient(newClient)

            // Auto-create Week 1
            val weekId = UUID.randomUUID().toString()
            val newWeek = Week(
                id = weekId,
                clientId = clientId,
                label = "Semana 1",
                weekNumber = 1
            )
            repository.insertWeek(newWeek)

            // Auto-create list of videos
            val videos = List(videosPerWeek) { index ->
                Video(
                    id = UUID.randomUUID().toString(),
                    weekId = weekId,
                    title = "",
                    status = "pendiente",
                    note = "",
                    videoIndex = index
                )
            }
            repository.insertVideos(videos)

            // Automatically select the new client
            selectClient(newClient)
        }
    }

    fun updateClient(client: Client, name: String, videosPerWeek: Int, contentType: String, notes: String) {
        viewModelScope.launch {
            val updatedClient = client.copy(
                name = name,
                videosPerWeek = videosPerWeek,
                contentType = contentType,
                notes = notes
            )
            repository.updateClient(updatedClient)
            if (_selectedClient.value?.id == client.id) {
                _selectedClient.value = updatedClient
            }
        }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            repository.deleteClientById(clientId)
            if (_selectedClient.value?.id == clientId) {
                selectClient(null)
            }
        }
    }

    fun addWeekForSelectedClient() {
        val client = _selectedClient.value ?: return
        viewModelScope.launch {
            val currentWeeks = _weeks.value
            val nextNum = currentWeeks.size + 1

            val weekId = UUID.randomUUID().toString()
            val newWeek = Week(
                id = weekId,
                clientId = client.id,
                label = "Semana $nextNum",
                weekNumber = nextNum
            )
            repository.insertWeek(newWeek)

            val videos = List(client.videosPerWeek) { index ->
                Video(
                    id = UUID.randomUUID().toString(),
                    weekId = weekId,
                    title = "",
                    status = "pendiente",
                    note = "",
                    videoIndex = index
                )
            }
            repository.insertVideos(videos)
            selectWeek(newWeek)
        }
    }

    fun updateVideo(video: Video, title: String, status: String, note: String) {
        viewModelScope.launch {
            val updatedVideo = video.copy(
                title = title,
                status = status,
                note = note
            )
            repository.updateVideo(updatedVideo)
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            // Sample Client 1
            val id1 = UUID.randomUUID().toString()
            val client1 = Client(
                id = id1,
                name = "Oscar Santos",
                videosPerWeek = 4,
                contentType = "Reels de Cocina",
                notes = "Preferencia por ritmos rápidos, cortes dinámicos y color grading vibrante. Entregar los viernes por la tarde.",
                colorIndex = 1
            )
            repository.insertClient(client1)

            val w1Id = UUID.randomUUID().toString()
            val week1 = Week(id = w1Id, clientId = id1, label = "Semana 1", weekNumber = 1)
            repository.insertWeek(week1)

            repository.insertVideos(listOf(
                Video(id = UUID.randomUUID().toString(), weekId = w1Id, title = "Tarta de Manzana Especial", status = "terminado", note = "¡Quedó excelente! Muy buen engagement.", videoIndex = 0),
                Video(id = UUID.randomUUID().toString(), weekId = w1Id, title = "Secreto del Arroz Cremoso", status = "revision", note = "Falta ajustar música de fondo según comentarios.", videoIndex = 1),
                Video(id = UUID.randomUUID().toString(), weekId = w1Id, title = "Truco Cortar Cebolla Rápido", status = "en-proceso", note = "En fase de edición de subtítulos.", videoIndex = 2),
                Video(id = UUID.randomUUID().toString(), weekId = w1Id, title = "Vlog del Chef en Mercado", status = "pendiente", note = "", videoIndex = 3)
            ))

            // Sample Client 2
            val id2 = UUID.randomUUID().toString()
            val client2 = Client(
                id = id2,
                name = "Inmobiliaria Norte",
                videosPerWeek = 2,
                contentType = "YouTube Tours",
                notes = "Tours de casas de lujo. Formato horizontal lento. Música jazz suave y transiciones de disolvencia limpia.",
                colorIndex = 3
            )
            repository.insertClient(client2)

            val w2Id = UUID.randomUUID().toString()
            val week2 = Week(id = w2Id, clientId = id2, label = "Semana 1", weekNumber = 1)
            repository.insertWeek(week2)

            repository.insertVideos(listOf(
                Video(id = UUID.randomUUID().toString(), weekId = w2Id, title = "Tour Mansión de La Colina", status = "terminado", note = "Publicado con gran éxito.", videoIndex = 0),
                Video(id = UUID.randomUUID().toString(), weekId = w2Id, title = "Semipiso Premium en Ramos", status = "revision", note = "Esperando feedback sobre el audio de la locución.", videoIndex = 1)
            ))

            selectClient(client1)
        }
    }
}
