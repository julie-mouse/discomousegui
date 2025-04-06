package org.jmouse.project.discomouse.commands

import dev.kord.common.Color
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredPublicMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.modify.embed
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
import org.jsoup.Jsoup
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
                contains("tumblr.com") -> rehost(ack, url, "www.tumblr.com", "www.tpmblr.com", null)
                contains("bsky.app") -> rehost(ack, url, "bsky.app", "bskyx.app", listOf("bsyy.app"))
                contains("tiktok.com") -> rehost(ack, url, "tiktok", "tnktok", listOf("vxtiktok", "tfxtok", "tiktxk"))
                contains("instagram.com") -> rehost(ack, url, "instagram", "kkinstagram", listOf("instagramez", "ddinstagram"))
                contains("twitter.com") -> rehost(ack, url, "twitter", "twittpr", listOf("fxtwitter", "vxtwitter"))
                contains("x.com") -> rehost(ack, url, "x.com", "twittpr.com", listOf("fxtwitter.com", "vxtwitter.com"))
                contains("reddit.com") -> rehost(ack, url, "reddit", "rxddit", listOf("vxreddit"))
                contains("v.redd.it") -> rehost(ack, getFinalUrl(url, 10), "reddit", "rxddit", listOf("vxreddit"))
                contains("open.spotify.com") ||
                        contains("music.apple.com") ||
                        contains("music.youtube.com") -> buildMusicMessage(ack, getMusicLinks(url))
                contains("www.twitch.tv") -> rehost(ack, url, "www.twitch.tv", "fxtwitch.seria.moe", null)
//                contains("facebook.com/share/r/")  ||
//                        contains("facebook.com/reel/") ||
//                        contains("facebook.com/watch") -> rehost(ack, url, "www.facebook.com", "fxfb.seria.moe", null)
                contains("facebook.com/share") -> facebookEmbed(ack, url)
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
        others: List<String>?
    ) {
        val response = ack.respond { content = url.replace(from, to) }
        val channel = response.message.channel
        val responseId = response.message.id

        if (others != null) {
            delay(7000)
            for (host in others) {
                val embed = channel.getMessage(responseId).embeds.firstOrNull()
                if (embed?.video == null && embed?.image == null && embed?.thumbnail == null) {
                    response.edit { content = url.replace(from, host) }
                } else break
                delay(7000)
            }
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

            ack.respond { content = "## $title\n> *$artist*\n" + linkFields.joinToString(" ") }
    }

    private suspend fun facebookEmbed(ack: DeferredPublicMessageInteractionResponseBehavior, fbLink: String) {
        try {
            val doc = Jsoup.connect(fbLink)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36")
                .header("Cookie","datr=eVPCZz3pvod9NQdud-eotZMs; sb=eVPCZza3ZsKXrB5cdEWhAojk; ps_l=1; ps_n=1; wd=1966x1360")
                .get()

            val fbTitle = doc.select("meta[property=og:title]")[0].attr("content")
            val fbDescription = doc.select("meta[property=og:description]")[0].attr("content")
            val fbImage = doc.select("meta[property=og:image]")[0].attr("content")

            ack.respond {
                embed {
                    color = Color(0x0165E1)
                    author {
                        name = "Facebook"
                        icon = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b9/2023_Facebook_icon.svg/1024px-2023_Facebook_icon.svg.png"
                    }
                    title = fbTitle
                    url = fbLink
                    description = fbDescription
                    image = fbImage
                    footer {
                        text = "This is hugely in beta, sorry if I mess up!"
                    }
                }
            }
        } catch (e: Exception) {
            try {
                val earlyDoc = Jsoup.connect(fbLink)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36")
                    .header("Cookie","datr=eVPCZz3pvod9NQdud-eotZMs; sb=eVPCZza3ZsKXrB5cdEWhAojk; ps_l=1; ps_n=1; wd=1966x1360")
                    .get()

                val fbDirectLink = earlyDoc.select("link[rel=canonical]").attr("href")

                val doc = Jsoup.connect(fbDirectLink)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36")
                    .header("Cookie","datr=eVPCZz3pvod9NQdud-eotZMs; sb=eVPCZza3ZsKXrB5cdEWhAojk; ps_l=1; ps_n=1; wd=1966x1360")
                    .get()

                val fbTitle = doc.select("meta[property=og:title]")[0].attr("content")
                val fbDescription = doc.select("meta[property=og:description]")[0].attr("content")
                val fbImage = doc.select("meta[property=og:image]")[0].attr("content")

                ack.respond {
                    embed {
                        color = Color(0x0165E1)
                        author {
                            name = "Facebook"
                            icon = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b9/2023_Facebook_icon.svg/1024px-2023_Facebook_icon.svg.png"
                        }
                        title = fbTitle
                        url = fbLink
                        description = fbDescription
                        image = fbImage
                        footer {
                            text = "This is hugely in beta, sorry if I mess up!"
                        }
                    }
                }
            } catch (e: Exception) {
                ack.respond { content = "Still learning my limits with this...you found one!" }
            }
        }

    }

    fun shutdown() {
        client.close()
    }
}

