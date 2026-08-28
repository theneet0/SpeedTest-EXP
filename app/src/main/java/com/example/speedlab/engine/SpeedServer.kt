package com.example.speedlab.engine

import com.example.speedlab.model.AppSettings
import java.net.URI

data class SpeedServer(
    val id: String,
    val name: String,
    val location: String,
    val downloadUrlTemplate: String,
    val uploadUrl: String,
    val provider: String,
) {
    val host: String
        get() = runCatching { URI(downloadUrlTemplate.replace("{bytes}", "1")).host }
            .getOrNull().orEmpty()

    fun downloadUrl(bytes: Long, nonce: Long): String {
        val withBytes = if ("{bytes}" in downloadUrlTemplate) {
            downloadUrlTemplate.replace("{bytes}", bytes.toString())
        } else {
            val separator = if ("?" in downloadUrlTemplate) "&" else "?"
            "$downloadUrlTemplate${separator}bytes=$bytes"
        }
        val separator = if ("?" in withBytes) "&" else "?"
        return "$withBytes${separator}speedlab_nonce=$nonce"
    }
}

object ServerCatalog {
    val builtIn: List<SpeedServer> = listOf(
        SpeedServer(
            id = "cloudflare-global",
            name = "Cloudflare",
            location = "Global anycast",
            downloadUrlTemplate = "https://speed.cloudflare.com/__down?bytes={bytes}",
            uploadUrl = "https://speed.cloudflare.com/__up",
            provider = "Native HTTPS / Cloudflare",
        ),
    )

    fun custom(settings: AppSettings): SpeedServer = SpeedServer(
        id = "custom",
        name = settings.customServerName.trim(),
        location = settings.customServerLocation.trim().ifBlank { "Custom" },
        downloadUrlTemplate = settings.customDownloadUrl.trim(),
        uploadUrl = settings.customUploadUrl.trim(),
        provider = "Custom native HTTPS",
    )
}
