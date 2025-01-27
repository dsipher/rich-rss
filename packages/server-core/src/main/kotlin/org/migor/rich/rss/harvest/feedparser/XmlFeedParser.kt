package org.migor.rich.rss.harvest.feedparser

import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import org.migor.rich.rss.api.dto.RichFeed
import org.migor.rich.rss.harvest.HarvestResponse
import org.migor.rich.rss.util.FeedUtil
import org.slf4j.LoggerFactory
import org.springframework.util.MimeType
import java.io.StringReader

class XmlFeedParser : FeedBodyParser {

  private val log = LoggerFactory.getLogger(XmlFeedParser::class.simpleName)

  override fun priority(): Int {
    return 1
  }

  override fun canProcess(feedType: FeedType): Boolean {
    return arrayOf(FeedType.RSS, FeedType.ATOM, FeedType.XML).indexOf(feedType) > -1
  }

  override fun process(corrId: String, response: HarvestResponse): RichFeed {
    // parse rss/atom/rdf/opml
    val (feedType, _) = FeedUtil.detectFeedTypeForResponse(response.response)
    return when (feedType) {
      FeedType.RSS, FeedType.ATOM, FeedType.XML -> FeedUtil.fromSyndFeed(parseXml(response))
      else -> throw RuntimeException("Not implemented")
    }
  }

  private fun parseXml(harvestResponse: HarvestResponse): SyndFeed {
    val input = SyndFeedInput()
    input.xmlHealerOn = true
    input.isAllowDoctypes = true
    val responseBody = String(harvestResponse.response.responseBody)
    return try {
      input.build(StringReader(responseBody))
    } catch (e: Exception) {
      log.warn("Cannot parse feed ${harvestResponse.url} ${e.message}, trying BrokenXmlParser")
      input.build(StringReader(BrokenXmlParser.parse(responseBody)))
    }
  }
}

enum class FeedType {
  RSS, ATOM, JSON, XML, NONE
}
