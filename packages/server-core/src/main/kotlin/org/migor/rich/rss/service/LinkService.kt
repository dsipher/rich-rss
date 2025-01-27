package org.migor.rich.rss.service

import org.jsoup.Jsoup
import org.migor.rich.rss.database.models.ContentEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URL
import java.util.*


@Service
class LinkService {

  private val log = LoggerFactory.getLogger(LinkService::class.simpleName)

  fun extractLinkTargets(corrId: String, content: ContentEntity): List<LinkTarget> {
    val document = if (content.hasFulltext) {
      content.getContentOfMime("text/html")
    } else {
      content.getContentOfMime("text/html")
    }.also { Optional.ofNullable(it).map { Jsoup.parse(it) }.orElse(null) }

    document?.let {
      val doc = Jsoup.parse(it)
      val fromUrl = content.url!!
      return doc.body().select("a[href]").mapNotNull { link ->
        try {
          LinkTarget(URL(FeedService.absUrl(fromUrl, link.attr("href"))), link.text())
        } catch (e: Exception) {
          log.warn("[${corrId}] ${e.message}")
          null
        }
      }
        .distinct()
        .filter { isNotBlacklisted(it) }
      }
    return emptyList()
  }

  private fun isNotBlacklisted(linkTarget: LinkTarget): Boolean {
    return arrayOf(
      "facebook.com",
      "twitter.com",
      "amazon.com",
      "patreon.com"
    ).none { blackListedUrl -> linkTarget.url.host.contains(blackListedUrl) }
  }
}

data class LinkTarget(val url: URL, val text: String)
