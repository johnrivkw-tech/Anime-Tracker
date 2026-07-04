package com.example.animetracker.data

import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for anime data. The ViewModel only ever talks to
 * this class, never to the DAO directly — that keeps Room out of the UI layer
 * and makes it easy to swap the data source later if you ever need to.
 */
class AnimeRepository(private val animeDao: AnimeDao) {

    val allAnime: Flow<List<Anime>> = animeDao.getAllAnime()

    suspend fun insert(anime: Anime) = animeDao.insert(anime)

    suspend fun update(anime: Anime) = animeDao.update(anime)

    suspend fun delete(anime: Anime) = animeDao.delete(anime)
}
