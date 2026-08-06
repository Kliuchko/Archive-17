package com.kliuchko.archive17.data.reader

import android.content.Context
import java.io.File
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

class ReadiumService(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(appContext.contentResolver, httpClient)
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            appContext,
            assetRetriever = assetRetriever,
            httpClient = httpClient,
            pdfFactory = null,
        ),
    )

    suspend fun open(file: File): Publication {
        val asset = assetRetriever.retrieve(file).getOrElse { error ->
            throw IllegalArgumentException(error.toString())
        }
        return publicationOpener.open(asset, allowUserInteraction = false).getOrElse { error ->
            throw IllegalArgumentException(error.toString())
        }
    }
}
