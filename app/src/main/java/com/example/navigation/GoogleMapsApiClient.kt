package com.example.navigation

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GoogleDirectionsResponse(
    @Json(name = "routes") val routes: List<GoogleRoute> = emptyList(),
    @Json(name = "status") val status: String = ""
)

@JsonClass(generateAdapter = true)
data class GoogleRoute(
    @Json(name = "legs") val legs: List<GoogleRouteLeg> = emptyList(),
    @Json(name = "overview_polyline") val overviewPolyline: GooglePolyline? = null,
    @Json(name = "summary") val summary: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleRouteLeg(
    @Json(name = "distance") val distance: GoogleTextValue? = null,
    @Json(name = "duration") val duration: GoogleTextValue? = null,
    @Json(name = "start_address") val startAddress: String? = null,
    @Json(name = "end_address") val endAddress: String? = null,
    @Json(name = "start_location") val startLocation: GoogleLatLng? = null,
    @Json(name = "end_location") val endLocation: GoogleLatLng? = null,
    @Json(name = "steps") val steps: List<GoogleStep> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GoogleStep(
    @Json(name = "html_instructions") val htmlInstructions: String? = null,
    @Json(name = "distance") val distance: GoogleTextValue? = null,
    @Json(name = "duration") val duration: GoogleTextValue? = null,
    @Json(name = "maneuver") val maneuver: String? = null,
    @Json(name = "start_location") val startLocation: GoogleLatLng? = null,
    @Json(name = "end_location") val endLocation: GoogleLatLng? = null,
    @Json(name = "polyline") val polyline: GooglePolyline? = null
)

@JsonClass(generateAdapter = true)
data class GoogleTextValue(
    @Json(name = "text") val text: String = "",
    @Json(name = "value") val value: Int = 0
)

@JsonClass(generateAdapter = true)
data class GoogleLatLng(
    @Json(name = "lat") val lat: Double = 0.0,
    @Json(name = "lng") val lng: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class GooglePolyline(
    @Json(name = "points") val points: String = ""
)

@JsonClass(generateAdapter = true)
data class GoogleGeocodingResponse(
    @Json(name = "results") val results: List<GoogleGeocodeResult> = emptyList(),
    @Json(name = "status") val status: String = ""
)

@JsonClass(generateAdapter = true)
data class GoogleGeocodeResult(
    @Json(name = "formatted_address") val formattedAddress: String = "",
    @Json(name = "geometry") val geometry: GoogleGeometry? = null
)

@JsonClass(generateAdapter = true)
data class GoogleGeometry(
    @Json(name = "location") val location: GoogleLatLng? = null
)

interface GoogleMapsApiService {
    @GET("maps/api/directions/json")
    suspend fun getWalkingDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "walking",
        @Query("key") apiKey: String,
        @Query("language") language: String = "en"
    ): GoogleDirectionsResponse

    @GET("maps/api/geocode/json")
    suspend fun geocodeAddress(
        @Query("address") address: String,
        @Query("key") apiKey: String,
        @Query("language") language: String = "en"
    ): GoogleGeocodingResponse
}

object GoogleMapsApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val apiService: GoogleMapsApiService = retrofit.create(GoogleMapsApiService::class.java)

    /**
     * Decodes standard Google Encoded Polyline algorithm into Lat/Lon pairs.
     */
    fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
        val poly = mutableListOf<Pair<Double, Double>>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng

            val p = Pair(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    /**
     * Strips HTML formatting from Google Directions html_instructions.
     */
    fun cleanHtmlInstructions(html: String?): String {
        if (html == null) return ""
        return html
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
