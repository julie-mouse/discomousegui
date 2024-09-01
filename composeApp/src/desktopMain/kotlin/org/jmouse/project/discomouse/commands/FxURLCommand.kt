package org.jmouse.project.discomouse.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Message
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string
import java.net.HttpURLConnection
import java.net.URL
import java.util.ResourceBundle

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jmouse.project.discomouse.network.OdesliResponse
import org.jmouse.project.util.COMMAND_FXURL
import org.jmouse.project.util.COMMAND_FXURL2
import org.jmouse.project.util.ERROR_MSG
import org.jmouse.project.util.FXURL_ARG1


class FxURLCommand(
    override val bundle: ResourceBundle,
    override val bot: Kord
): Command {
    override val name = COMMAND_FXURL
    override val description: String = bundle.getString("fx_url_description")

    private val nameForJohn = COMMAND_FXURL2
    private val descForJohn = bundle.getString("fx_url_johns_description")

    override suspend fun register() {
        bot.createGlobalChatInputCommand(name, description) {
            string(FXURL_ARG1, bundle.getString("fx_url_link_description")) {
                required = true
            }
        }
        // John complained about "/fx" being dumb, so I made a second: "/fix"
        bot.createGlobalChatInputCommand(nameForJohn, descForJohn) {
            string(FXURL_ARG1, bundle.getString("fx_url_link_description")) {
                required = true
            }
        }
    }

    override suspend fun execute(interaction: GuildChatInputCommandInteraction) {

        val theirMsg = interaction.command.strings[FXURL_ARG1]!!

        val myResponse = interaction.deferPublicResponse()

        val myMsg = tryToFix(theirMsg)

        myResponse.respond {
            content = myMsg
        }
    }

    private fun tryToFix(url: String): String {
        return with(url) {
            when {
                contains("instagram.com/reel") -> url.replace("instagram", "instagramez")
                contains("twitter.com") -> url.replace("twitter", "twittpr")
                contains("x.com") -> url.replace("x.com", "twittpr.com")
                contains("tiktok.com") -> url.replace("tiktok", "tnktok")
                contains("reddit.com") -> url.replace("reddit", "rxddit")
                contains("v.redd.it") -> getFinalUrl(url, 10).replace("reddit", "rxddit")
                contains("open.spotify.com") ||
                        contains("music.apple.com") ||
                        contains("music.youtube.com") -> buildMusicMessage(getMusicLinks(url))
                else -> ERROR_MSG
            }
        }
    }

    suspend fun fxUrlTextBased(message: Message) {
        println("Message Received: ${message.content}")

        val cleanUrl: String = sanitize(message.content)

        val response = tryToFix(cleanUrl)

        message.channel.createMessage("${message.author?.mention}: $response")
        message.delete()
    }

    private fun sanitize(message: String): String {
        val cleanMessage = message
            .removePrefix("/$COMMAND_FXURL")
            .trim()

        val end = cleanMessage.indexOfFirst{ it == ' ' } + 1

        return if (end == 0) {
            cleanMessage
        } else {
            cleanMessage.substring(0, end)
        }
    }

    private fun getFinalUrl(url: String, maxRedirects: Int): String {
        var redirects = 0
        var currentUrl = url
        while (redirects <= maxRedirects) {
            val connection = URL(currentUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false

            try {
                connection.connect()

                if (connection.responseCode in 300..399) {
                    val redirectedUrl = connection.getHeaderField("Location")
                    if (redirectedUrl != null) {
                        val newUrl = URL(URL(currentUrl), redirectedUrl).toString()
                        println("Redirected to: $newUrl")
                        currentUrl = newUrl
                        redirects++
                    } else {
                        println("No location header for redirect")
                        return ERROR_MSG
                    }
                } else {
                    println("Final URL: $currentUrl")
                    return currentUrl
                }
            } finally {
                connection.disconnect()
            }
        }

        println("Too many redirects")
        return ERROR_MSG
    }

    private fun getMusicLinks(url: String): Map<String, String?> = runBlocking {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }

        try {
            val response: HttpResponse = client.get("https://api.song.link/v1-alpha.1/links?url=$url")
            val odesliResponse: OdesliResponse = response.body()
            mapOf(
                "Apple Music" to odesliResponse.linksByPlatform.appleMusic?.url,
                "Spotify" to odesliResponse.linksByPlatform.spotify?.url,
                "YouTube Music" to odesliResponse.linksByPlatform.youtubeMusic?.url,
                "Title" to odesliResponse.entitiesByUniqueId.values.firstOrNull()?.title,
                "Artist" to odesliResponse.entitiesByUniqueId.values.firstOrNull()?.artistName
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        } finally {
            client.close()
        }
    }

    private fun buildMusicMessage(links: Map<String, String?>): String {
        val title = links["Title"] ?: "Unknown Title"
        val artist = links["Artist"] ?: "Unknown Artist"
        val spotifyLink = links["Spotify"]
        val appleMusicLink = links["Apple Music"]
        val youtubeMusicLink = links["YouTube Music"]

        if (spotifyLink.isNullOrBlank() && appleMusicLink.isNullOrBlank() && youtubeMusicLink.isNullOrBlank()) {
            return ERROR_MSG
        }

        val linkFields = mutableListOf<String>()
        spotifyLink?.let {linkFields.add("[.]($it)")}
        appleMusicLink?.let {linkFields.add("[.]($it)")}
        youtubeMusicLink?.let {linkFields.add("[.]($it)")}

        return "## $title\n> *$artist*\n" + linkFields.joinToString(" ")
    }
}