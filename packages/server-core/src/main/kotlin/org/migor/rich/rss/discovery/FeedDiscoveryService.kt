package org.migor.rich.rss.discovery

import org.jsoup.nodes.Document
import org.migor.rich.rss.api.dto.FeedDiscovery
import org.migor.rich.rss.api.dto.FeedDiscoveryDocument
import org.migor.rich.rss.api.dto.FeedDiscoveryOptions
import org.migor.rich.rss.api.dto.FeedDiscoveryResults
import org.migor.rich.rss.harvest.HarvestResponse
import org.migor.rich.rss.harvest.PageInspection
import org.migor.rich.rss.harvest.PageInspectionService
import org.migor.rich.rss.harvest.feedparser.FeedType
import org.migor.rich.rss.service.FeedService
import org.migor.rich.rss.service.HttpService
import org.migor.rich.rss.service.PropertyService
import org.migor.rich.rss.service.PuppeteerService
import org.migor.rich.rss.transform.GenericFeedFetchOptions
import org.migor.rich.rss.transform.GenericFeedRule
import org.migor.rich.rss.util.FeedUtil
import org.migor.rich.rss.util.HtmlUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class FeedDiscoveryService {
  private val log = LoggerFactory.getLogger(FeedDiscoveryService::class.simpleName)

  @Autowired
  lateinit var nativeFeedLocator: NativeFeedLocator

  @Autowired
  lateinit var propertyService: PropertyService

  @Autowired
  lateinit var genericFeedLocator: GenericFeedLocator

  @Autowired
  lateinit var puppeteerService: PuppeteerService

  @Autowired
  lateinit var httpService: HttpService

  @Autowired
  lateinit var feedService: FeedService

  @Autowired
  lateinit var pageInspectionService: PageInspectionService

  fun discoverFeeds(
    corrId: String,
    fetchOptions: GenericFeedFetchOptions
  ): FeedDiscovery {
    val homepageUrl = fetchOptions.websiteUrl
    fun toFeedDiscovery(
      url: String,
      nativeFeeds: List<FeedReference>,
      genericFeedRules: List<GenericFeedRule> = emptyList(),
      document: FeedDiscoveryDocument? = null,
      failed: Boolean = false,
      errorMessage: String? = null
    ): FeedDiscovery {
      return FeedDiscovery(
        options = FeedDiscoveryOptions(
          harvestUrl = url,
          originalUrl = homepageUrl,
        ),
        results = FeedDiscoveryResults(
          nativeFeeds = nativeFeeds,
          genericFeedRules = genericFeedRules,
          failed = failed,
          errorMessage = errorMessage,
          document = document
        )
      )
    }
    log.info("[$corrId] feeds/discover url=$homepageUrl, prerender=${fetchOptions.prerender}")
    return runCatching {
      val url = rewriteUrl(corrId, httpService.prefixUrl(homepageUrl.trim()))

      httpService.guardedHttpResource(corrId, url, 200, listOf("text/", "application/xml", "application/json", "application/rss", "application/atom", "application/rdf"))
      val staticResponse = httpService.httpGetCaching(corrId, url, 200)

      val (feedType, mimeType) = FeedUtil.detectFeedTypeForResponse(staticResponse)

      if (feedType !== FeedType.NONE) {
        val feed = feedService.parseFeed(corrId, HarvestResponse(url, staticResponse))
        log.info("[$corrId] is native-feed")
        toFeedDiscovery(
          url,
          nativeFeeds = listOf(FeedReference(url = url, type = feedType, title = feed.title, description = feed.description))
        )
      } else {
        if (fetchOptions.prerender) {
          val puppeteerResponse = puppeteerService.prerender(corrId, url, fetchOptions)
          val document = HtmlUtil.parse(puppeteerResponse.html!!)
          val (nativeFeeds, genericFeedRules) = extractFeeds(corrId, document, url, false)
          toFeedDiscovery(
            url,
            nativeFeeds = nativeFeeds,
            genericFeedRules = genericFeedRules,
            document = toDiscoveryDocument(
              inspection = pageInspectionService.fromDocument(document),
              screenshot = puppeteerResponse.screenshot,
              body = puppeteerResponse.html,
              mimeType = mimeType
            ),
            errorMessage = puppeteerResponse.errorMessage,
          )
        } else {
          val body = String(staticResponse.responseBody)
          val document = HtmlUtil.parse(body)
          val (nativeFeeds, genericFeedRules) = extractFeeds(corrId, document, url, false)
          toFeedDiscovery(
            url,
            nativeFeeds = nativeFeeds,
            genericFeedRules = genericFeedRules,
            toDiscoveryDocument(
              inspection = pageInspectionService.fromDocument(document),
              screenshot = null,
              body = body,
              mimeType = mimeType
            )
          )
        }
      }
    }.getOrElse {
      log.error("[$corrId] Unable to discover feeds: ${it.message}")
      it.printStackTrace()
      // todo mag return error code
      toFeedDiscovery(
        url = homepageUrl,
        nativeFeeds = emptyList(),
        failed = true,
        errorMessage = it.message
      )
    }
  }

  private fun toDiscoveryDocument(
    inspection: PageInspection,
    screenshot: String?,
    body: String,
    mimeType: String
  ): FeedDiscoveryDocument = FeedDiscoveryDocument(
    screenshot = screenshot,
    body = body,
    mimeType = mimeType,
    title = inspection.valueOf("title"),
    description = inspection.valueOf("description"),
    language = inspection.valueOf("language"),
    imageUrl = inspection.valueOf("imageUrl")
  )

  private fun rewriteUrl(corrId: String, url: String): String {
    val rewrite = url.replace("https://twitter.com", propertyService.nitterHost)
    if (rewrite != url) {
      log.info("[$corrId] rewrote url $url -> $rewrite")
    }
    return rewrite
  }


  private fun extractFeeds(
    corrId: String,
    document: Document,
    url: String,
    strictMode: Boolean
  ): Pair<List<FeedReference>, List<GenericFeedRule>> {
    val genericFeedRules = genericFeedLocator.locateInDocument(corrId, document, url, strictMode)
    val nativeFeeds = nativeFeedLocator.locateInDocument(document, url)
    log.info("[$corrId] Found feedRules=${genericFeedRules.size} nativeFeeds=${nativeFeeds.size}")
    return Pair(nativeFeeds, genericFeedRules)
  }
}
