package com.example.animetracker.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.animetracker.data.Anime
import com.example.animetracker.data.AnimeDatabase
import com.example.animetracker.data.AnimeRepository
import com.example.animetracker.data.AnimeStatus
import com.example.animetracker.data.ProfilePrefs
import com.example.animetracker.data.network.AniListCharacterEdge
import com.example.animetracker.data.network.AniListMedia
import com.example.animetracker.data.network.AniListRepository
import com.example.animetracker.ui.model.ProfileStats
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AnimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AnimeRepository
    private val aniListRepository = AniListRepository()
    private val profilePrefs = ProfilePrefs(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<AnimeStatus?>(null)
    val statusFilter: StateFlow<AnimeStatus?> = _statusFilter.asStateFlow()

    val filteredAnime: StateFlow<List<Anime>>
    val allLocalAnime: StateFlow<List<Anime>>
    val localByAniListId: StateFlow<Map<Int, AnimeStatus>>
    val continueTracking: StateFlow<List<Anime>>

    // --- Online "add anime" search (AniList) ---
    private val _searchResults = MutableStateFlow<List<AniListMedia>>(emptyList())
    val searchResults: StateFlow<List<AniListMedia>> = _searchResults.asStateFlow()

    private val _isSearchingApi = MutableStateFlow(false)
    val isSearchingApi: StateFlow<Boolean> = _isSearchingApi.asStateFlow()

    private val _searchApiError = MutableStateFlow<String?>(null)
    val searchApiError: StateFlow<String?> = _searchApiError.asStateFlow()

    private var searchJob: Job? = null

    // --- Home feed sections (AniList) ---
    private val _trending = MutableStateFlow<List<AniListMedia>>(emptyList())
    val trending: StateFlow<List<AniListMedia>> = _trending.asStateFlow()

    private val _popularThisSeason = MutableStateFlow<List<AniListMedia>>(emptyList())
    val popularThisSeason: StateFlow<List<AniListMedia>> = _popularThisSeason.asStateFlow()

    private val _topRated = MutableStateFlow<List<AniListMedia>>(emptyList())
    val topRated: StateFlow<List<AniListMedia>> = _topRated.asStateFlow()

    private val _newReleases = MutableStateFlow<List<AniListMedia>>(emptyList())
    val newReleases: StateFlow<List<AniListMedia>> = _newReleases.asStateFlow()

    private val _recommended = MutableStateFlow<List<AniListMedia>>(emptyList())
    val recommended: StateFlow<List<AniListMedia>> = _recommended.asStateFlow()

    private val _isHomeFeedLoading = MutableStateFlow(false)
    val isHomeFeedLoading: StateFlow<Boolean> = _isHomeFeedLoading.asStateFlow()

    private val _homeFeedError = MutableStateFlow<String?>(null)
    val homeFeedError: StateFlow<String?> = _homeFeedError.asStateFlow()

    // --- Anime details screen (AniList) ---
    private val _animeDetails = MutableStateFlow<AniListMedia?>(null)
    val animeDetails: StateFlow<AniListMedia?> = _animeDetails.asStateFlow()

    private val _isDetailsLoading = MutableStateFlow(false)
    val isDetailsLoading: StateFlow<Boolean> = _isDetailsLoading.asStateFlow()

    private val _detailsError = MutableStateFlow<String?>(null)
    val detailsError: StateFlow<String?> = _detailsError.asStateFlow()

    private val _characters = MutableStateFlow<List<AniListCharacterEdge>>(emptyList())
    val characters: StateFlow<List<AniListCharacterEdge>> = _characters.asStateFlow()

    // --- Discover tab: browse by genre/season/year (AniList) ---
    private val _discoverGenre = MutableStateFlow<String?>(null)
    val discoverGenre: StateFlow<String?> = _discoverGenre.asStateFlow()

    private val _discoverSeason = MutableStateFlow<String?>(null)
    val discoverSeason: StateFlow<String?> = _discoverSeason.asStateFlow()

    private val _discoverYear = MutableStateFlow<Int?>(null)
    val discoverYear: StateFlow<Int?> = _discoverYear.asStateFlow()

    private val _discoverResults = MutableStateFlow<List<AniListMedia>>(emptyList())
    val discoverResults: StateFlow<List<AniListMedia>> = _discoverResults.asStateFlow()

    private val _isDiscoverLoading = MutableStateFlow(false)
    val isDiscoverLoading: StateFlow<Boolean> = _isDiscoverLoading.asStateFlow()

    private val _discoverError = MutableStateFlow<String?>(null)
    val discoverError: StateFlow<String?> = _discoverError.asStateFlow()

    private var discoverJob: Job? = null

    // --- Search tab: full-catalog search, separate from the "add to list" dialog ---
    private val _catalogQuery = MutableStateFlow("")
    val catalogQuery: StateFlow<String> = _catalogQuery.asStateFlow()

    private val _catalogResults = MutableStateFlow<List<AniListMedia>>(emptyList())
    val catalogResults: StateFlow<List<AniListMedia>> = _catalogResults.asStateFlow()

    private val _isCatalogSearching = MutableStateFlow(false)
    val isCatalogSearching: StateFlow<Boolean> = _isCatalogSearching.asStateFlow()

    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError: StateFlow<String?> = _catalogError.asStateFlow()

    private var catalogJob: Job? = null

    // --- Profile: banner image + display name (SharedPreferences) ---
    private val _profileBannerPath = MutableStateFlow(profilePrefs.getBannerPath())
    val profileBannerPath: StateFlow<String?> = _profileBannerPath.asStateFlow()

    private val _profileDisplayName = MutableStateFlow(profilePrefs.getDisplayName())
    val profileDisplayName: StateFlow<String> = _profileDisplayName.asStateFlow()

    private val _isBannerSaving = MutableStateFlow(false)
    val isBannerSaving: StateFlow<Boolean> = _isBannerSaving.asStateFlow()

    // --- Profile: aggregate watchlist stats (local) ---
    val profileStats: StateFlow<ProfileStats>

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

        localByAniListId = repository.allAnime
            .map { list ->
                list.mapNotNull { anime -> anime.aniListId?.let { it to anime.status } }.toMap()
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

        profileStats = repository.allAnime
            .map { list ->
                // AniList's per-episode runtime when we have it; a flat
                // 24 min/episode (typical TV anime) for entries that don't.
                val totalMinutes = list.sumOf { anime ->
                    val perEpisode = (anime.episodeDurationMinutes ?: 24).toLong()
                    perEpisode * anime.episodesWatched
                }
                ProfileStats(
                    totalAnime = list.size,
                    completed = list.count { it.status == AnimeStatus.COMPLETED },
                    watching = list.count { it.status == AnimeStatus.WATCHING },
                    planToWatch = list.count { it.status == AnimeStatus.PLAN_TO_WATCH },
                    favorites = list.count { it.isFavorite },
                    totalWatchMinutes = totalMinutes
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ProfileStats()
            )

        loadHomeFeed()
        loadDiscover()
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

            val trendingResult = aniListRepository.getTrending()
            val popularResult = aniListRepository.getPopularThisSeason()
            val topRatedResult = aniListRepository.getTopRated()
            val newReleasesResult = aniListRepository.getNewReleases()
            val recommendedResult = aniListRepository.getRecommended()

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
            aniListRepository.searchAnime(query)
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

    fun addAnimeFromSearchResult(result: AniListMedia) {
        viewModelScope.launch {
            repository.insert(
                Anime(
                    name = result.displayTitle,
                    totalEpisodes = result.episodes ?: 0,
                    imageUrl = result.posterUrl,
                    aniListId = result.id,
                    episodeDurationMinutes = result.duration
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

    fun loadAnimeDetails(aniListId: Int) {
        viewModelScope.launch {
            _isDetailsLoading.value = true
            _detailsError.value = null
            aniListRepository.getAnimeDetails(aniListId)
                .onSuccess { _animeDetails.value = it }
                .onFailure { _detailsError.value = "Couldn't load details. Check your connection and try again." }
            _isDetailsLoading.value = false
        }
    }

    fun loadAnimeCharacters(aniListId: Int) {
        viewModelScope.launch {
            aniListRepository.getAnimeCharacters(aniListId)
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
    fun setAnimeStatus(details: AniListMedia, existing: Anime?, status: AnimeStatus) {
        viewModelScope.launch {
            if (existing != null) {
                repository.update(existing.copy(status = status))
            } else {
                repository.insert(
                    Anime(
                        name = details.displayTitle,
                        totalEpisodes = details.episodes ?: 0,
                        status = status,
                        imageUrl = details.posterUrl,
                        aniListId = details.id,
                        episodeDurationMinutes = details.duration
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

    // --- Discover tab actions ---

    fun setDiscoverGenre(genre: String?) {
        _discoverGenre.value = genre
        loadDiscover()
    }

    fun setDiscoverSeason(season: String?) {
        _discoverSeason.value = season
        loadDiscover()
    }

    fun setDiscoverYear(year: Int?) {
        _discoverYear.value = year
        loadDiscover()
    }

    fun loadDiscover() {
        discoverJob?.cancel()
        discoverJob = viewModelScope.launch {
            _isDiscoverLoading.value = true
            _discoverError.value = null
            aniListRepository.discoverAnime(
                genre = _discoverGenre.value,
                season = _discoverSeason.value,
                seasonYear = _discoverYear.value
            ).onSuccess { _discoverResults.value = it }
                .onFailure { _discoverError.value = "Couldn't load results. Check your connection and try again." }
            _isDiscoverLoading.value = false
        }
    }

    // --- Search tab actions ---

    fun onCatalogQueryChange(query: String) {
        _catalogQuery.value = query
        catalogJob?.cancel()
        if (query.isBlank()) {
            _catalogResults.value = emptyList()
            _catalogError.value = null
            _isCatalogSearching.value = false
            return
        }
        catalogJob = viewModelScope.launch {
            delay(400)
            _isCatalogSearching.value = true
            _catalogError.value = null
            aniListRepository.searchAnime(query)
                .onSuccess { _catalogResults.value = it }
                .onFailure { _catalogError.value = "Couldn't reach the anime database. Check your connection." }
            _isCatalogSearching.value = false
        }
    }

    // --- Profile actions ---

    fun setDisplayName(name: String) {
        profilePrefs.setDisplayName(name)
        _profileDisplayName.value = name
    }

    /**
     * Copies the picked photo into the app's private storage and remembers
     * its path, since content:// URIs from the photo picker aren't
     * guaranteed to stay readable after the app process dies.
     */
    fun setProfileBanner(uri: Uri) {
        viewModelScope.launch {
            _isBannerSaving.value = true
            val savedPath = withContext(Dispatchers.IO) {
                try {
                    val context = getApplication<Application>()
                    val destFile = File(context.filesDir, "profile_banner.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output -> input.copyTo(output) }
                    }
                    destFile.absolutePath
                } catch (e: Exception) {
                    null
                }
            }
            if (savedPath != null) {
                profilePrefs.setBannerPath(savedPath)
                _profileBannerPath.value = savedPath
            }
            _isBannerSaving.value = false
        }
    }
}
