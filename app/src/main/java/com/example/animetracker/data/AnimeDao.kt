package com.example.animetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {

    // Flow means the UI automatically re-renders whenever the table changes.
    @Query("SELECT * FROM anime_table ORDER BY name COLLATE NOCASE ASC")
    fun getAllAnime(): Flow<List<Anime>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anime: Anime)

    @Update
    suspend fun update(anime: Anime)

    @Delete
    suspend fun delete(anime: Anime)
}
