package com.example.animetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.animetracker.data.Anime
import com.example.animetracker.data.AnimeDatabase
import com.example.animetracker.data.AnimeRepository
import com.example.animetracker.data.AnimeStatus
import com.example.animetracker.data.network.JikanAnimeResult
import com.example.animetracker.data.network.JikanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds UI state for the watchlist and exposes the actions the UI can trigger.
 * Extends AndroidViewModel (instead of plain ViewModel) so it can grab an
 * Application context to build the Room database, without needing a separate
 * ViewModelFactory or a DI framework — the simplest thing that works.
 */
class AnimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AnimeRepository
    private val jikanRepository = JikanRepository()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // null means "no status filter, show everything"
    private val _statusFilter = MutableStateFlow<AnimeStatus?>(null)
    val statusFilter: StateFlow<AnimeStatus?> = _statusFilter.asStateFlow()

    /** The list the UI should actually render: search + status filter already applied. */
    val filteredAnime: StateFlow<List<Anime>>

    // --- Online anime search (Jikan) ---
    private val _searchResults = MutableStateFlow<List<JikanAnimeResult>>(emptyList())
    val searchResults: StateFlow<List<JikanAnimeResult>> = _searchResults.asStateFlow()

    private val _isSearchingApi = MutableStateFlow(false)
    val isSearchingApi: StateFlow<Boolean> = _isSearchingApi.asStateFlow()

    private val _searchApiError = MutableStateFlow<String?>(null)
    val searchApiError: StateFlow<String?> = _searchApiError.asStateFlow()

    private var searchJob: Job? = null

    init {
        val dao = AnimeDatabase.getDatabase(application).animeDao()
        repository = AnimeRepository(dao)

        filteredAnime = combine(
            repository.allAnime,
            _searchQuery,
            _statusFilter
        ) { list, query, statusFilter ->
            list.filter { anime ->
                val matchesQuery = query.isBlank() ||
                    anime.name.contains(query, ignoreCase = true)
                val matchesStatus = statusFilter == null || anime.status == statusFilter
                matchesQuery && matchesStatus
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterChange(status: AnimeStatus?) {
        _statusFilter.value = status
    }

    /** Debounced online search against the Jikan API, used by the "add via search" dialog. */
    fun searchOnline(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchApiError.value = null
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // wait for the user to stop typing before hitting the network
            _isSearchingApi.value = true
            _searchApiError.value = null
            jikanRepository.searchAnime(query)
                .onSuccess { _searchResults.value = it }
                .onFailure { _searchApiError.value = "Couldn't reach the anime database. Check your connection." }
            _isSearchingApi.value = false
        }
    }

    fun clearSearchResults() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
        _searchApiError.value = null
        _isSearchingApi.value = false
    }

    fun addAnimeFromSearchResult(result: JikanAnimeResult) {
        viewModelScope.launch {
            repository.insert(
                Anime(
                    name = result.title,
                    totalEpisodes = result.episodes ?: 0,
                    imageUrl = result.images.jpg.large_image_url ?: result.images.jpg.image_url,
                    malId = result.mal_id
                )
            )
        }
    }

    fun addAnime(
        name: String,
        episodesWatched: Int,
        totalEpisodes: Int,
        status: AnimeStatus,
        rating: Int
    ) {
        viewModelScope.launch {
            repository.insert(
                Anime(
                    name = name,
                    episodesWatched = episodesWatched,
                    totalEpisodes = totalEpisodes,
                    status = status,
                    rating = rating
                )
            )
        }
    }

    fun updateAnime(anime: Anime) {
        viewModelScope.launch {
            repository.update(anime)
        }
    }

    fun incrementEpisode(anime: Anime) {
        viewModelScope.launch {
            repository.update(anime.copy(episodesWatched = anime.episodesWatched + 1))
        }
    }

    fun deleteAnime(anime: Anime) {
        viewModelScope.launch {
            repository.delete(anime)
        }
    }
}
