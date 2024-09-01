package org.jmouse.project.daylist


import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.serialization.Serializable
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class TestApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            val apiKey: String = System.getenv("LASTFM_API_KEY")
            val username = "SumbreroMouse"
            val client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
            }

            println("Getting tracks...")
            val tracks: Map<String, List<Track>> = groupTracksByTime( fetchRecentTracks(apiKey, client, username) )
            println("Counting Genres")
            val genreCounts = mutableMapOf<String, Int>()
            for (track in tracks[getCurrentTimeOfDay()]!!) {
                val genres = fetchGenresForTrack(apiKey, client, track.name, track.artist.name)
                for (genre in genres) {
                    genreCounts[genre.name] = genreCounts.getOrDefault(genre.name, 0) + 1
                }
            }


//            val topArtists = fetchRecentTopArtists(apiKey, client, username)
//            val genreCounts = mutableMapOf<String, Int>()
//            for (artist in topArtists) {
//                val genres = fetchTopGenresForArtist(apiKey, client, artist.name)
//                for (genre in genres) {
//                    genreCounts[genre.name] = genreCounts.getOrDefault(genre.name, 0) + 1
//                }
//            }


            // Print Output
            client.close()
            genreCounts.entries.sortedByDescending { it.value}.take(6).map{it.toPair()}.forEach { (genre, count) ->
                println("Genre: $genre, Count: $count")
            }

//            tracks.forEach { track ->
//                println("Track: ${track.name}, Artist: ${track.artist.name}, Date: ${track.date?.text}")
//            }
        }

        suspend fun fetchRecentTracks(apiKey: String, client: HttpClient, username: String): List<Track> {
            val response: HttpResponse = client.get("http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=$username&api_key=$apiKey&format=json&limit=500")
            val responseBody: String = response.body()

            val recentTracks = Json{ignoreUnknownKeys = true}.decodeFromString<RecentTracksResponse>(responseBody)

            println("Got tracks...")
            return recentTracks.recenttracks.track
        }

        fun groupTracksByTime(tracks: List<Track>): Map<String, List<Track>> {
            println("Grouping by time...")
            val morning = mutableListOf<Track>()
            val afternoon = mutableListOf<Track>()
            val night = mutableListOf<Track>()

            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")

            tracks.forEach { track->
                track.date?.text?.let { dateString ->
                    val date = dateFormat.parse(dateString)
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York")).apply { time = date }
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)

                    when (hour) {
                        in 6..11 -> morning.add(track)
                        in 12..18 -> afternoon.add(track)
                        else -> night.add(track)
                    }
                }
            }

            println("Got groups")
            return mapOf(
                "morning" to morning,
                "afternoon" to afternoon,
                "night" to night
            )
        }

        fun getCurrentTimeOfDay(): String {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            return when (hour) {
                in 6..11 -> "morning"
                in 12..18 -> "afternoon"
                else -> "night"
            }
        }

        suspend fun fetchGenresForTrack(apiKey: String, client: HttpClient, track: String, artist: String): List<Tag> {
            val encodedArtist = URLEncoder.encode(artist)
            val encodedTrack = URLEncoder.encode(track)

            val response: HttpResponse = client.get("http://ws.audioscrobbler.com/2.0/?method=track.getTopTags&artist=$encodedArtist&track=$encodedTrack&api_key=$apiKey&format=json")
            val responseBody: String = response.body()

            val topGenres = Json{ignoreUnknownKeys = true}.decodeFromString<TopTagsResponse>(responseBody)

            return topGenres.toptags.tag.take(10)
        }

        suspend fun fetchRecentTopArtists(apiKey: String, client: HttpClient, username: String): List<Artist> {
            val response: HttpResponse = client.get("http://ws.audioscrobbler.com/2.0/?method=user.getTopArtists&user=$username&api_key=$apiKey&format=json&period=7day")
            val responseBody: String = response.body()

            val recentArtists = Json{ignoreUnknownKeys = true}.decodeFromString<TopArtistsResponse>(responseBody)

            return recentArtists.topartists.artist
        }

        suspend fun fetchTopGenresForArtist(apiKey: String, client: HttpClient, artistName: String): List<Tag> {
            val encodedArtistName = URLEncoder.encode(artistName)
            val response: HttpResponse = client.get("http://ws.audioscrobbler.com/2.0/?method=artist.getTopTags&artist=$encodedArtistName&api_key=$apiKey&format=json")
            val responseBody: String = response.body()

            val topGenres = Json{ignoreUnknownKeys=true}.decodeFromString<TopTagsResponse>(responseBody)

            return topGenres.toptags.tag.take(10)
        }
    }
}

@Serializable
data class TopArtistsResponse(val topartists: TopArtists)

@Serializable
data class TopArtists(val artist: List<Artist>)

@Serializable
data class Artist(@SerialName("#text") val name: String)

@Serializable
data class TopTagsResponse(val toptags: TopTags)

@Serializable
data class TopTags(val tag: List<Tag>)

@Serializable
data class Tag(val name: String, val count: Int)

@Serializable
data class RecentTracksResponse(val recenttracks: RecentTracks)

@Serializable
data class RecentTracks(val track: List<Track>)

@Serializable
data class Track(val name: String, val artist: Artist, val date: DateWrapper?)

@Serializable
data class DateWrapper(@SerialName("#text") val text: String)