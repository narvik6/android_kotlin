package com.example.module5_t1to3.diary

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaryRepository(private val filesDir: File) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())


    fun loadAllEntries(): List<DiaryEntry> =
        filesDir
            .listFiles { f -> f.isFile && f.extension == "txt" }
            ?.sortedByDescending { extractTimestamp(it) }
            ?.map { fileToEntry(it) }
            ?: emptyList()

    fun saveEntry(title: String, text: String): DiaryEntry {
        val timestamp = System.currentTimeMillis()
        val safeTitle = title.trim()
            .replace(Regex("""[\\/:*?"<>|]"""), "")   // убираем запрещённые символы
            .take(30)
            .replace(' ', '_')
        val fileName = if (safeTitle.isNotEmpty()) "${timestamp}_$safeTitle.txt"
                       else "$timestamp.txt"

        val file = File(filesDir, fileName)
        file.writeText(encode(title, text))
        return fileToEntry(file)
    }

    fun updateEntry(fileName: String, title: String, text: String) {
        File(filesDir, fileName).writeText(encode(title, text))
    }

    fun deleteEntry(fileName: String) {
        File(filesDir, fileName).delete()
    }

    fun readFullEntry(fileName: String): Pair<String, String> {
        val raw = File(filesDir, fileName).readText()
        val nl  = raw.indexOf('\n')
        return if (nl == -1) Pair("", raw)
               else          Pair(raw.substring(0, nl), raw.substring(nl + 1))
    }

    private fun encode(title: String, text: String) = "$title\n$text"

    private fun fileToEntry(file: File): DiaryEntry {
        val raw   = file.readText()
        val nl    = raw.indexOf('\n')
        val title = if (nl == -1) "" else raw.substring(0, nl)
        val body  = if (nl == -1) raw else raw.substring(nl + 1)
        val ts    = extractTimestamp(file)
        return DiaryEntry(
            fileName      = file.name,
            title         = title,
            preview       = body.trim().take(40),
            timestamp     = ts,
            formattedDate = dateFormat.format(Date(ts))
        )
    }

    private fun extractTimestamp(file: File): Long =
        file.nameWithoutExtension.substringBefore('_').toLongOrNull()
            ?: file.lastModified()
}
