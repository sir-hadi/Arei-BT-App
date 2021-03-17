package com.example.android.araibtv03

import androidx.room.*
import com.example.android.araibtv03.entities.ItemDataHolder

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItem(Item: ItemDataHolder)

    @Query("SELECT * FROM ItemDataHolder")
    fun getAll(): List<ItemDataHolder>

    @Query("SELECT checkStatus FROM ItemDataHolder WHERE itemName = :itemName")
    fun isCheck(itemName: String): Boolean

    @Delete
    fun delete(Item: ItemDataHolder)

    @Query("UPDATE ItemDataHolder SET checkStatus = :check WHERE itemName = :itemName")
    fun updateDone(itemName: String, check: Boolean)


}