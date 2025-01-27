package org.migor.rich.rss.harvest.feedparser

import org.migor.rich.rss.api.dto.RichFeed
import org.migor.rich.rss.harvest.HarvestResponse
import org.springframework.util.MimeType

interface FeedBodyParser {
  fun priority(): Int
  fun canProcess(feedType: FeedType): Boolean
  fun process(corrId: String, response: HarvestResponse): RichFeed
}
