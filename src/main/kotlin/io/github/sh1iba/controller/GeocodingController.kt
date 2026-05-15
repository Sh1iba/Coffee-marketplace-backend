package io.github.sh1iba.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

@RestController
@RequestMapping("/api/geocode")
class GeocodingController {

    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    // Semaphore ensures only 1 request to Nominatim at a time
    private val nominatimLock = Semaphore(1)
    private var lastRequestTime = 0L

    // Simple in-memory cache: key -> (responseBody, expiresAt)
    private val cache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val cacheTtlMs = 60_000L

    @GetMapping("/search")
    fun search(@RequestParam q: String): ResponseEntity<String> {
        val encoded = URLEncoder.encode(q, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1&addressdetails=1&accept-language=ru"
        return proxy(url, "search:$q")
    }

    @GetMapping("/reverse")
    fun reverse(@RequestParam lat: Double, @RequestParam lon: Double): ResponseEntity<String> {
        // Round to 4 decimal places (~11m precision) for cache key
        val latR = "%.4f".format(lat)
        val lonR = "%.4f".format(lon)
        val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&accept-language=ru"
        return proxy(url, "reverse:$latR,$lonR")
    }

    private fun proxy(url: String, cacheKey: String): ResponseEntity<String> {
        // Return cached response if still valid
        cache[cacheKey]?.let { (body, expiresAt) ->
            if (System.currentTimeMillis() < expiresAt) {
                return ResponseEntity.ok(body)
            }
            cache.remove(cacheKey)
        }

        nominatimLock.acquire()
        try {
            // Check cache again after acquiring lock (another thread may have fetched it)
            cache[cacheKey]?.let { (body, expiresAt) ->
                if (System.currentTimeMillis() < expiresAt) {
                    return ResponseEntity.ok(body)
                }
            }

            // Enforce minimum 1100ms between Nominatim requests
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < 1100) Thread.sleep(1100 - elapsed)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "CoffeeMarketplace/1.0")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            lastRequestTime = System.currentTimeMillis()

            val body = response.body() ?: ""
            if (response.statusCode() == 200 && body.isNotBlank()) {
                cache[cacheKey] = body to (System.currentTimeMillis() + cacheTtlMs)
            }
            return ResponseEntity.status(response.statusCode()).body(body)
        } catch (e: Exception) {
            return ResponseEntity.internalServerError().body("{\"error\":\"${e.message}\"}")
        } finally {
            nominatimLock.release()
        }
    }
}
