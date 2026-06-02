package task2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    val directory = args.getOrNull(0)?.let(::Path) ?: Path(".")
    val timeoutSeconds = args.getOrNull(1)?.toLongOrNull() ?: 5L
    runTask2(directory, timeoutSeconds)
}

fun runTask2(directory: Path, timeoutSeconds: Long) = runBlocking {
    val duplicateGroups = findDuplicateJsonFiles(directory, timeoutSeconds)

    if (duplicateGroups == null) {
        println("Поиск прерван по таймауту")
        return@runBlocking
    }

    if (duplicateGroups.isEmpty()) {
        println("Дубликаты не найдены")
        return@runBlocking
    }

    println("Найдены группы дубликатов:")
    duplicateGroups.forEachIndexed { index, files ->
        println("Группа ${index + 1}:")
        files.forEach { println("  ${it.pathString}") }
    }
}

suspend fun findDuplicateJsonFiles(directory: Path, timeoutSeconds: Long): List<List<Path>>? =
    withTimeoutOrNull(timeoutSeconds.seconds) {
        coroutineScope {
            val jsonFiles = findJsonFiles(directory)
            val hashes = jsonFiles
                .map { file -> async { file to calculateSha256(file) } }
                .awaitAll()

            hashes
                .groupBy(keySelector = { it.second }, valueTransform = { it.first })
                .values
                .filter { it.size > 1 }
        }
    }

suspend fun calculateSha256(file: Path): String = withContext(Dispatchers.IO) {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

    Files.newInputStream(file).use { input ->
        while (true) {
            ensureActive()
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            digest.update(buffer, 0, bytesRead)
        }
    }

    digest.digest().joinToString("") { "%02x".format(it) }
}

private suspend fun findJsonFiles(directory: Path): List<Path> = withContext(Dispatchers.IO) {
    if (!Files.exists(directory)) {
        error("Директория не найдена: ${directory.pathString}")
    }

    Files.walk(directory).use { paths ->
        paths
            .filter { path -> path.isRegularFile() && path.extension.equals("json", ignoreCase = true) }
            .sorted { left, right -> left.name.compareTo(right.name) }
            .toList()
    }
}
