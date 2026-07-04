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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AnimeRepository
    private val jikanRepository = JikanRepository()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<AnimeStatus?>(null)
    val statusFilter: StateFlow<AnimeStatus?> = _statusFilter.asStateFlow()

    val filteredAnime: StateFlow<List<Anime>>

    /** Locally-tracked anime that also have a MyAnimeList ID, keyed by that ID. */
    val localByMalId: StateFlow<Map<Int, AnimeStatus>>

    /** Anime currently marked WATCHING, for the "Continue Tracking" home section. */
    val continueTracking: StateFlow<List<Anime>>

    // --- Online "add anime" search (Jikan) ---
    private val _searchResults = MutableStateFlow<List<JikanAnimeResult>>(emptyList())
    val searchResults: StateFlow<List<JikanAnimeResult>> = _searchResults.asStateFlow()

    private val _isSearchingApi = MutableStateFlow(false)
    val isSearchingApi: StateFlow<Boolean> = _isSearchingApi.asStateFlow()

    private val _searchApiError = MutableStateFlow<String?>(null)
    val searchApiError: StateFlow<String?> = _searchApiError.asStateFlow()

    private var searchJob: Job? = null

    // --- Home feed sections (Jikan) ---
    private val _trending = MutableStateFlow<List<JikanAnimeResult>>(emptyList())
    val trending: StateFlow<List<JikanAnimeResult>> = _trending.asStateFlow()

    private val _popularThisSeason = MutableStateFlow<List<JikanAnimeResult>>(emptyList())
    val popularThisSeason: StateFlow<List<JikanAnimeResult>> = _popularThisSeason.asStateFlow()

    private val _topRated = MutableStateFlow<List<JikanAnimeResult>>(emptyList())
    val topRated: StateFlow<List<JikanAnimeResult>> = _topRated.asStateFlow()

    private val _newReleases = MutableStateFlow<List<JikanAnimeResult>>(emptyList())
    val newReleases: StateFlow<List<JikanAnimeResult>> = _newReleases.asStateFlow()

    private val _recommended = MutableStateFlow<List<JikanAnimeResult>>(emptyList())
    val recommended: StateFlow<List<JikanAnimeResult>> = _recommended.asStateFlow()

    private val _isHomeFeedLoading = MutableStateFlow(false)
    val isHomeFeedLoading: StateFlow<Boolean> = _isHomeFeedLoading.asStateFlow()

    private val _homeFeedError = MutableStateFlow<String?>(null)
    val homeFeedError: StateFlow<String?> = _homeFeedError.asStateFlow()

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

        localByMalId = repository.allAnime
            .map { list ->
                list.mapNotNull { anime -> anime.malId?.let { it to anime.status } }.toMap()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )

        continueTracking = repository.allAnime
            .map { list -> list.filter { it.status == AnimeStatus.WATCHING } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

        loadHomeFeed()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterChange(status: AnimeStatus?) {
        _statusFilter.value = status
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            _isHomeFeedLoading.value = true
            _homeFeedError.value = null

            val trendingResult = jikanRepository.getTrending()
            val popularResult = jikanRepository.getPopularThisSeason()
            val topRatedResult = jikanRepository.getTopRated()
            val newReleasesResult = jikanRepository.getNewReleases()
            val recommendedResult = jikanRepository.getRecommended()

            trendingResult.onSuccess { _trending.value = it }
            popularResult.onSuccess { _popularThisSeason.value = it }
            topRatedResult.onSuccess { _topRated.value = it }
            newReleasesResult.onSuccess { _newReleases.value = it }
            recommendedResult.onSuccess { _recommended.value = it }

            val allFailed = listOf(trendingResult, popularResult, topRatedResult, newReleasesResult, recommendedResult)
                .all { it.isFailure }
            if (allFailed) {
                _homeFeedError.value = "Couldn't load your home feed. Check your connection and try again."
            }

            _isHomeFeedLoading.value = false
        }
    }

    fun searchOnline(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchApiError.value = null
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
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
