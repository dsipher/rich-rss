package org.migor.rich.rss.discovery

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.migor.rich.rss.service.FeedService.Companion.absUrl
import org.migor.rich.rss.util.FeedUtil
import org.springframework.stereotype.Service

@Service
class NativeFeedLocator {

  fun locateInDocument(document: Document, url: String): List<FeedReference> {
    //    <link rel="alternate" type="application/rss+xml" title="yellowchicken &raquo; Feed" href="https://yellowchicken.wordpress.com/feed/" />
    return document.select("link[rel=alternate][title][type]").mapNotNull { element -> toFeedReference(element, url) }
  }

  private fun toFeedReference(element: Element, url: String): FeedReference? {
    try {
      return FeedReference(
        absUrl(url, element.attr("href")),
        FeedUtil.detectFeedType(element.attr("type")),
        element.attr("title")
      )
    } catch (e: Exception) {
      return null
    }
  }
}
