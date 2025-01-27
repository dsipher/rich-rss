package org.migor.rich.rss.graphql

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import kotlinx.coroutines.coroutineScope
import org.migor.rich.rss.generated.GenericFeedDto
import org.migor.rich.rss.generated.ImporterDto
import org.migor.rich.rss.generated.NativeFeedDto
import org.migor.rich.rss.graphql.DtoResolver.toDTO
import org.migor.rich.rss.service.ArticleService
import org.migor.rich.rss.service.GenericFeedService
import org.migor.rich.rss.service.ImporterService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

@DgsComponent
class NativeFeedDataResolver {

  @Autowired
  lateinit var genericFeedService: GenericFeedService

  @Autowired
  lateinit var articleService: ArticleService

  @Autowired
  lateinit var importerService: ImporterService

  @DgsData(parentType = "NativeFeed")
  @Transactional(propagation = Propagation.REQUIRED)
  suspend fun genericFeed(dfe: DgsDataFetchingEnvironment): GenericFeedDto? = coroutineScope {
    val feed: NativeFeedDto = dfe.getSource()
    genericFeedService.findByNativeFeedId(UUID.fromString(feed.id)).map { toDTO(it) }.orElse(null)
  }

  @DgsData(parentType = "NativeFeed")
  @Transactional(propagation = Propagation.REQUIRED)
  suspend fun importers(dfe: DgsDataFetchingEnvironment): List<ImporterDto> = coroutineScope {
    val feed: NativeFeedDto = dfe.getSource()
    importerService.findAllByFeedId(UUID.fromString(feed.id)).map { toDTO(it) }
  }

//  @DgsData(parentType = "NativeFeed")
//  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
//  suspend fun importersCount(dfe: DgsDataFetchingEnvironment): Long = coroutineScope {
//    val feed: NativeFeedDto = dfe.getSource()
//    importerService.countByBucketId(UUID.fromString(feed.id))
//  }

  @DgsData(parentType = "NativeFeed")
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  suspend fun articlesCount(dfe: DgsDataFetchingEnvironment): Long = coroutineScope {
    val feed: NativeFeedDto = dfe.getSource()
    articleService.countByStreamId(UUID.fromString(feed.streamId))
  }

}
