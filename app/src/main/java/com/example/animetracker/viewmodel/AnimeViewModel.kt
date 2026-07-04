package com.example.animetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.animetracker.data.Anime
import com.example.animetracker.data.AnimeDatabase
import com.example.animetracker.data.AnimeRepository
import com.example.animetracker.data.AnimeStatus
import com.example.animetracker.data.network.JikanAnimeResult
import com.example.animetracker.data.network.JikanCharacterEntry
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
    val allLocalAnime: StateFlow<List<Anime>>
    val localByMalId: StateFlow<Map<Int, AnimeStatus>>
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

    // --- Anime details screen (Jikan) ---
    private val _animeDetails = MutableStateFlow<JikanAnimeResult?>(null)
    val animeDetails: StateFlow<JikanAnimeResult?> = _animeDetails.asStateFlow()

    private val _isDetailsLoading = MutableStateFlow(false)
    val isDetailsLoading: StateFlow<Boolean> = _isDetailsLoading.asStateFlow()

    private val _detailsError = MutableStateFlow<String?>(null)
    val detailsError: StateFlow<String?> = _detailsError.asStateFlow()

    private val _characters = MutableStateFlow<List<JikanCharacterEntry>>(emptyList())
    val characters: StateFlow<List<JikanCharacterEntry>> = _characters.asStateFlow()

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

        allLocalAnime = repository.allAnime.stateIn(
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
            val cap = if (anime.totalEpisodes > 0) anime.totalEpisodes else Int.MAX_VALUE
            val next = (anime.episodesWatched + 1).coerceAtMost(cap)
            repository.update(anime.copy(episodesWatched = next))
        }
    }

    fun deleteAnime(anime: Anime) {
        viewModelScope.launch {
            repository.delete(anime)
        }
    }

    // --- Details screen actions ---

    fun loadAnimeDetails(malId: Int) {
        viewModelScope.launch {
            _isDetailsLoading.value = true
            _detailsError.value = null
            jikanRepository.getAnimeDetails(malId)
                .onSuccess { _animeDetails.value = it }
                .onFailure { _detailsError.value = "Couldn't load details. Check your connection and try again." }
            _isDetailsLoading.value = false
        }
    }

    fun loadAnimeCharacters(malId: Int) {
        viewModelScope.launch {
            jikanRepository.getAnimeCharacters(malId)
                .onSuccess { _characters.value = it }
            // Characters are a nice-to-have; fail silently so a missing
            // characters list never blocks the rest of the details page.
        }
    }

    fun clearAnimeDetails() {
        _animeDetails.value = null
        _detailsError.value = null
        _characters.value = emptyList()
    }

    /** Sets (or creates, if not yet tracked) the list status for this anime. */
    fun setAnimeStatus(details: JikanAnimeResult, existing: Anime?, status: AnimeStatus) {
        viewModelScope.launch {
            if (existing != null) {
                repository.update(existing.copy(status = status))
            } else {
                repository.insert(
                    Anime(
                        name = details.title,
                        totalEpisodes = details.episodes ?: 0,
                        status = status,
                        imageUrl = details.images.jpg.large_image_url ?: details.images.jpg.image_url,
                        malId = details.mal_id
                    )
                )
            }
        }
    }

    fun rateAnime(anime: Anime, rating: Int) {
        viewModelScope.launch {
            repository.update(anime.copy(rating = rating))
        }
    }

    fun toggleFavorite(anime: Anime) {
        viewModelScope.launch {
            repository.update(anime.copy(isFavorite = !anime.isFavorite))
        }
    }
}
