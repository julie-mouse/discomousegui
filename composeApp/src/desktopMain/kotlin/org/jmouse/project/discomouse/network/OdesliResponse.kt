package org.jmouse.project.discomouse.network

import kotlinx.serialization.Serializable

@Serializable
data class OdesliResponse(
    val entitiesByUniqueId: Map<String, Entity> = emptyMap(),
    val linksByPlatform: LinksByPlatform
)

@Serializable
data class Entity(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val artistName: String = "",
    val thumbnailUrl: String = "",
    val thumbnailWidth: Int = 0,
    val thumbnailHeight: Int = 0,
    val apiProvider: String = "",
    val platforms: List<String> = emptyList()
)

@Serializable
data class LinksByPlatform(
    val amazonMusic: LinkInfo? = null,
    val amazonStore: LinkInfo? = null,
    val audiomack: LinkInfo? = null,
    val anghami: LinkInfo? = null,
    val boomplay: LinkInfo? = null,
    val deezer: LinkInfo? = null,
    val appleMusic: LinkInfo? = null,
    val itunes: LinkInfo? = null,
    val napster: LinkInfo? = null,
    val pandora: LinkInfo? = null,
    val soundcloud: LinkInfo? = null,
    val tidal: LinkInfo? = null,
    val yandex: LinkInfo? = null,
    val youtube: LinkInfo? = null,
    val youtubeMusic: LinkInfo? = null,
    val spotify: LinkInfo? = null
)

@Serializable
data class LinkInfo(
    val url: String = ""
)