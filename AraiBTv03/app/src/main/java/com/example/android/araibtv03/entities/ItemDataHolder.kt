package com.example.android.araibtv03.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ItemDataHolder (
        var imageResource: Int,
        @PrimaryKey var itemName:String,
        var checkStatus:Boolean
        )