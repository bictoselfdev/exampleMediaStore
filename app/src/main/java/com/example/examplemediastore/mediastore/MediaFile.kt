package com.example.examplemediastore.mediastore

import android.net.Uri

data class MediaFile(
    val contentUri: Uri,
    val name: String,
    val mimeType: String,
    val relativePath: String,
    val fullPath: String,
    val date: Long,
    val size: Int
)