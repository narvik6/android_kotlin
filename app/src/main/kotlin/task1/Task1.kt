package task1

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.system.measureTimeMillis

fun main() = runTask1()

fun runTask1() = runBlocking {
    var result: LoadedData? = null
    val elapsed = measureTimeMillis {
        result = loadAllData()
    }

    result?.let { data ->
        println("Пользователи: ${data.users.joinToString()}")
        println("Продажи за день:")
        data.sales.forEach { (product, qty) -> println("  $product: $qty") }
        println("Погода:")
        data.weather.forEach { println("  $it") }
    } ?: println("Не удалось загрузить все данные")

    println("Общее время выполнения: ${elapsed} мс")
}

data class LoadedData(
    val users: List<String>,
    val sales: Map<String, Int>,
    val weather: List<String>
)

suspend fun loadAllData(): LoadedData? = coroutineScope {
    val users = async { runCatching { loadUsers() } }
    val sales = async { runCatching { loadDailySales() } }
    val weather = async { runCatching { loadWeather() } }

    val usersResult = users.await()
    val salesResult = sales.await()
    val weatherResult = weather.await()

    val failure = listOf(usersResult, salesResult, weatherResult).firstOrNull { it.isFailure }
    if (failure != null) {
        println("Ошибка загрузки данных: ${failure.exceptionOrNull()?.message}")
        return@coroutineScope null
    }

    LoadedData(
        users = usersResult.getOrThrow(),
        sales = salesResult.getOrThrow(),
        weather = weatherResult.getOrThrow()
    )
}

suspend fun loadUsers(): List<String> {
    delay(1_800)
    failSometimes("список пользователей")
    return listOf("Alice", "Bob", "Ivan", "Olga")
}

suspend fun loadDailySales(): Map<String, Int> {
    delay(1_200)
    failSometimes("статистика продаж")
    return mapOf("Coffee" to 42, "Tea" to 19)
}

suspend fun loadWeather(): List<String> {
    delay(2_500)
    failSometimes("погода")
    return listOf("Москва: -3°C", "Нью-Йорк: -5°C", "Токио: 11°C")
}

private fun failSometimes(sourceName: String) {
    if (Random.nextInt(100) < 15) {
        error("случайный сбой: $sourceName")
    }
}
