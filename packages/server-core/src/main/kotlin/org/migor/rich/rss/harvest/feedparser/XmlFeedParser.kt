package org.migor.rich.rss.harvest.feedparser

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.WireFeedInput
import org.migor.rich.rss.harvest.FeedData
import org.migor.rich.rss.harvest.HarvestResponse
import org.migor.rich.rss.util.FeedUtil
import org.migor.rich.rss.util.XmlUtil
import org.slf4j.LoggerFactory
import org.springframework.util.MimeType
import java.io.StringReader

class XmlFeedParser : FeedBodyParser {

  private val log = LoggerFactory.getLogger(XmlFeedParser::class.simpleName)

  override fun priority(): Int {
    return 1
  }

  override fun canProcess(feedType: FeedType, mimeType: MimeType?): Boolean {
    return arrayOf(FeedType.RSS, FeedType.ATOM, FeedType.XML).indexOf(feedType) > -1
  }

  override fun process(corrId: String, response: HarvestResponse): FeedData {
    // parse rss/atom/rdf/opml
    val (feedType, _) = FeedUtil.detectFeedTypeForResponse(response.response)
    return when (feedType) {
      FeedType.RSS, FeedType.ATOM, FeedType.XML -> parseXml(response)
      else -> throw RuntimeException("Not implemented")
    }
  }

  private fun parseXml(harvestResponse: HarvestResponse): FeedData {
    val input = SyndFeedInput()
    val winput = WireFeedInput()
    input.xmlHealerOn = true
    input.isAllowDoctypes = true
    val responseBody = XmlUtil.explicitCloseTags(harvestResponse.response.responseBody!!)
    val feed = try {
      input.build(StringReader(responseBody))
    } catch (e: Exception) {
      log.warn("Cannot parse feed ${harvestResponse.url} ${e.message}, trying BrokenXmlParser")
      input.build(StringReader(BrokenXmlParser.parse(responseBody)))
    }

    return FeedData(feed)
  }
}

enum class FeedType {
  RSS, ATOM, JSON, XML, NONE
}