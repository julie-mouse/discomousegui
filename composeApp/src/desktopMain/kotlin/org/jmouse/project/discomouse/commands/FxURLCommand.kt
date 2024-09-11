package org.jmouse.project.discomouse.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredPublicMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Message
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.jmouse.project.discomouse.network.OdesliResponse
import org.jmouse.project.util.COMMAND_FXURL
import org.jmouse.project.util.COMMAND_FXURL2
import org.jmouse.project.util.ERROR_MSG
import org.jmouse.project.util.FXURL_ARG1
import java.net.HttpURLConnection
import java.net.URL
import java.util.ResourceBundle

class FxURLCommand(
    override val bundle: ResourceBundle,
    override val bot: Kord
): Command {
    override val name = COMMAND_FXURL
    override val description: String = bundle.getString("fx_url_description")

    private val nameForJohn = COMMAND_FXURL2
    private val descForJohn = bundle.getString("fx_url_johns_description")

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

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

        val url = interaction.command.strings[FXURL_ARG1]!!

        val ack = interaction.deferPublicResponse()

        with(url){
            when {
                contains("tiktok.com") -> rehost(ack, url, "tiktok", "tntok", listOf("vxtiktok", "tfxtok", "tiktxk"))
                contains("instagram.com") -> rehost(ack, url, "instagram", "ddinstagram", listOf("instagramez"))
                contains("twitter.com") -> rehost(ack, url, "twitter", "fxtwitter", listOf("twittpr", "vxtwitter"))
                contains("x.com") -> rehost(ack, url, "x.com", "fxtwitter.com", listOf("twittpr.com", "vxtwitter.com"))
                contains("reddit.com") -> rehost(ack, url, "reddit", "rxddit", listOf("vxreddit"))
                contains("v.redd.it") -> rehost(ack, getFinalUrl(url, 10), "reddit", "rxddit", listOf("vxreddit"))
                contains("open.spotify.com") ||
                        contains("music.apple.com") ||
                        contains("music.youtube.com") -> buildMusicMessage(ack, getMusicLinks(url))
                else -> {
                    ack.respond { content = ERROR_MSG }
                }
            }
        }
    }

    private suspend fun rehost(
        ack: DeferredPublicMessageInteractionResponseBehavior,
        url: String,
        from: String,
        to: String,
        others: List<String>
    ) {
        val response = ack.respond { content = url.replace(from, to) }
        delay(7000)

        for (host in others) {
            if (response.message.embeds.isEmpty()) {
                response.edit { content = url.replace(from, host) }
            } else break
            delay(7000)
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

    private suspend fun getMusicLinks(url: String): Map<String, String?> {
        return try {
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
        }
    }

    private suspend fun buildMusicMessage(ack: DeferredPublicMessageInteractionResponseBehavior, links: Map<String, String?>) {
            val title = links["Title"] ?: "Unknown Title"
            val artist = links["Artist"] ?: "Unknown Artist"
            val spotifyLink = links["Spotify"]
            val appleMusicLink = links["Apple Music"]
            val youtubeMusicLink = links["YouTube Music"]

            if (spotifyLink.isNullOrBlank() && appleMusicLink.isNullOrBlank() && youtubeMusicLink.isNullOrBlank()) {
                ack.respond { content = ERROR_MSG }
                return
            }

            val linkFields = mutableListOf<String>()
            spotifyLink?.let {linkFields.add("[.]($it)")}
            appleMusicLink?.let {linkFields.add("[.]($it)")}
            youtubeMusicLink?.let {linkFields.add("[.]($it)")}

            ack.respond { "## $title\n> *$artist*\n" + linkFields.joinToString(" ") }
    }

    fun shutdown() {
        client.close()
    }
}