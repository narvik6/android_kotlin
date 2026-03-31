package com.example.module5.gallery

import java.io.File

data class PhotoItem(
    val file : File,
    val name : String   // имя файла = ключ для LazyGrid
)
