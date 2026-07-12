package de.szalkowski.activitylauncher

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

/**
 * JokeGenerator fetches random jokes from an external API
 * Uses the JokeAPI service (https://jokeapi.dev/)
 */
class JokeGenerator(private val context: Context) {

    companion object {
        private const val TAG = "JokeGenerator"
        private const val JOKE_API_URL = "https://v2.jokeapi.dev/joke/Any"
        private const val TIMEOUT_MS = 5000
    }

    /**
     * Fetches a random joke from the API
     * @return Joke object containing the joke data
     */
    suspend fun getRandomJoke(): Joke? = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL(JOKE_API_URL)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.addRequestProperty("User-Agent", "ActivityLauncher/1.0")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseJoke(response)
            } else {
                Log.e(TAG, "API returned status code: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching joke: ${e.message}", e)
            null
        }
    }

    /**
     * Parses the JSON response from the Joke API
     */
    private fun parseJoke(jsonString: String): Joke? {
        return try {
            val jsonObject = JSONObject(jsonString)
            val type = jsonObject.getString("type")

            val joke = if (type == "single") {
                val setup = jsonObject.getString("joke")
                Joke(setup, "", type)
            } else {
                val setup = jsonObject.getString("setup")
                val delivery = jsonObject.getString("delivery")
                Joke(setup, delivery, type)
            }
            joke
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing joke JSON: ${e.message}", e)
            null
        }
    }
}

/**
 * Data class representing a joke
 * @param setup The setup or full joke text
 * @param delivery The punchline (empty for single-line jokes)
 * @param type The type of joke ("single" or "twopart")
 */
data class Joke(
    val setup: String,
    val delivery: String,
    val type: String
) {
    override fun toString(): String {
        return if (type == "single") {
            setup
        } else {
            "$setup\n\n$delivery"
        }
    }
}
