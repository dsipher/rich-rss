package org.migor.rss.rich.trigger

import org.migor.rss.rich.database.enums.FeedStatus
import org.migor.rss.rich.database.model.Feed
import org.migor.rss.rich.database.repository.FeedRepository
import org.migor.rss.rich.harvest.FeedHarvester
import org.migor.rss.rich.util.CryptUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TriggerFeed internal constructor() {

  @Autowired
  lateinit var feedRepository: FeedRepository

  @Autowired
  lateinit var feedHarvester: FeedHarvester

  @Scheduled(fixedDelay = 1234)
  @Transactional(readOnly = true)
  fun fetchFeeds() {
    val excludedStates = arrayOf(FeedStatus.expired, FeedStatus.stopped, FeedStatus.manual)
    feedRepository.findAllDueToFeeds(Date(), excludedStates)
      .forEach { feed: Feed ->
        run {
          feedHarvester.harvestFeed(CryptUtil.newCorrId(), feed)
        }
      }
  }

  //  @PostMapping("/triggers/update/feed/{feedId}", produces = ["application/json;charset=UTF-8"])
//  fun triggerUpdate(@PathVariable("feedId") feedId: String) {
//    return streamService.triggerUpdate(streamId);
//  }
}