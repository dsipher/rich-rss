package org.migor.rich.rss.harvest

import com.github.shyiko.skedule.Schedule
import org.migor.rich.rss.database.enums.ArticleType
import org.migor.rich.rss.database.enums.ImporterRefreshTrigger
import org.migor.rich.rss.database.enums.ReleaseStatus
import org.migor.rich.rss.database.models.ContentEntity
import org.migor.rich.rss.database.models.ImporterEntity
import org.migor.rich.rss.database.repositories.ContentDAO
import org.migor.rich.rss.database.repositories.ImporterDAO
import org.migor.rich.rss.service.ArticleService
import org.migor.rich.rss.service.FilterService
import org.migor.rich.rss.service.ImporterService
import org.migor.rich.rss.service.PropertyService
import org.migor.rich.rss.util.CryptUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

@Service
@Profile("database")
class ImporterHarvester internal constructor() {

  private val log = LoggerFactory.getLogger(ImporterHarvester::class.simpleName)

  @Autowired
  lateinit var articleService: ArticleService

  @Autowired
  lateinit var propertyService: PropertyService

  @Autowired
  lateinit var filterService: FilterService

  @Autowired
  lateinit var importerService: ImporterService

  @Autowired
  lateinit var contentDAO: ContentDAO

  @Autowired
  lateinit var importerDAO: ImporterDAO

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
  fun handleImporter(importer: ImporterEntity) {
    log.info("harvestImporter ${importer.id}")
    if (ImporterRefreshTrigger.CHANGE == importer.triggerRefreshOn) {
      this.harvestOnChangeImporter(importer)
    }
    if (ImporterRefreshTrigger.SCHEDULED == importer.triggerRefreshOn) {
      this.harvestScheduledImporter(importer)
    }
  }

  private fun harvestOnChangeImporter(importer: ImporterEntity) {
    val corrId = CryptUtil.newCorrId()
    try {
      log.info("[$corrId] harvestOnChangeImporter importer ${importer.id}")
//      log.info("[${corrId}] subscription ${subscription.id} is outdated")

      importArticles(corrId, importer)

      val now = Date()
      log.info("[$corrId] Updating lastUpdatedAt for subscription and importer")
      importerDAO.setLastUpdatedAt(importer.id, now)
    } catch (e: Exception) {
      e.printStackTrace()
      log.error("[$corrId] Cannot run importer ${importer.id} for bucket ${importer.bucketId}: ${e.message}")
    }
  }

  private fun harvestScheduledImporter(importer: ImporterEntity) {
    val corrId = CryptUtil.newCorrId()
    log.info("[$corrId] harvestScheduledImporter importer ${importer.id}")
    try {
      if (importer.triggerScheduledNextAt == null) {
        log.info("[$corrId] is unscheduled yet")
        updateScheduledNextAt(corrId, importer)
      } else {
        val appendedCount = scheduledImportArticles(corrId, importer)
        val now = Date()
        log.info("[$corrId] Appended segment of size $appendedCount to bucket ${importer.bucketId}")

        updateScheduledNextAt(corrId, importer)
        importerDAO.setLastUpdatedAt(importer.id, now)
      }

    } catch (e: Exception) {
      log.error("[$corrId] Cannot update scheduled bucket ${importer.id}: ${e.message}")
    }
  }

  private fun updateScheduledNextAt(corrId: String, importer: ImporterEntity) {
    val scheduledNextAt =
      Date.from(Schedule.parse(importer.triggerScheduleExpression).next(ZonedDateTime.now()).toInstant())
    log.info("[$corrId] Next import scheduled for $scheduledNextAt")
    importerDAO.setScheduledNextAt(importer.id, scheduledNextAt)
  }

  private fun scheduledImportArticles(
    corrId: String,
    importer: ImporterEntity
  ) {
    val defaultScheduledLastAt = Date.from(
      LocalDateTime.now().minus(1, ChronoUnit.MONTHS).toInstant(
        ZoneOffset.UTC
      )
    )

    val segmentSize = Optional.ofNullable(importer.segmentSize).orElse(100)
    val segmentSortField = Optional.ofNullable(importer.segmentSortField).orElse("score")
    val segmentSortOrder = if (importer.segmentSortAsc) {
      Sort.Order.asc(segmentSortField)
    } else {
      Sort.Order.desc(segmentSortField)
    }
    val pageable = PageRequest.of(0, segmentSize, Sort.by(segmentSortOrder))
    val articles = contentDAO.findAllThrottled(
      importer.feedId!!,
      Optional.ofNullable(importer.triggerScheduledLastAt).orElse(defaultScheduledLastAt),
      pageable
    )

    refineAndImportArticlesScheduled(corrId, articles, importer)
  }

  private fun importArticles(
    corrId: String,
    importer: ImporterEntity,
  ) {
    val articles = if (importer.lookAheadMin == null) {
      contentDAO.findNewArticlesForImporter(importer.id)
    } else {
      log.info("[${corrId}] with look-ahead ${importer.lookAheadMin}")
      contentDAO.findArticlesForImporterWithLookAhead(importer.id, importer.lookAheadMin!!)
    }

    importArticles(corrId, importer, articles)
  }

  private fun refineAndImportArticlesScheduled(
    corrId: String,
    contents: Stream<ContentEntity>,
    importer: ImporterEntity
  ) {
    if (importer.digest) {
      this.log.info("[${corrId}] digest")

      val bucket = importer.bucket!!
      val user = bucket.owner!!
      val dateFormat = Optional.ofNullable(user.dateFormat).orElse(propertyService.dateFormat)
//      val digest = articleService.create(
//        corrId,
//        createDigestOfArticles(bucket.name!!, dateFormat, contents),
//      )
//
//      importerService.importArticlesToTargets(
//        corrId,
//        listOf(digest),
//        bucket.stream!!,
//        importer.feed!!,
//        ArticleType.digest,
//        ReleaseStatus.released,
//        Date(),
//        importer.targets
//      )
    } else {
      importArticles(corrId, importer, contents)
    }
  }

  companion object {
    fun createDigestOfArticles(
      bucketName: String,
      dateFormat: String,
      contents: Stream<ContentEntity>
    ): ContentEntity {
      val digest = ContentEntity()
      val listOfAttributes = contents.map { article ->
        mapOf(
          "title" to article.title,
          "host" to URL(article.url).host,
          "pubDate" to formatDateForUser(
            article.publishedAt!!,
            dateFormat
          ),
          "url" to article.url,
          "description" to toTextEllipsis(Optional.ofNullable(article.contentText).orElse(""))
        )
      }.collect(Collectors.toList())

      digest.contentRaw = "<ul>${
        listOfAttributes.joinToString("\n") { attributes ->
          """<li>
<a href="${attributes["url"]}">${attributes["pubDate"]} ${attributes["title"]} (${attributes["host"]})</a>
<p>${attributes["description"]}</p>
</li>""".trimMargin()
        }
      }</ul>"
      digest.contentRawMime = "text/html"
      digest.contentText = listOfAttributes.joinToString("\n\n") { attributes ->
        """- ${attributes["pubDate"]} ${attributes["title"]}
  [${attributes["url"]}]

  ${attributes["description"]}"""
      }

      digest.publishedAt = Date()
      digest.title = "${bucketName.uppercase()} Digest ${
        formatDateForUser(
          Date(),
          dateFormat
        )
      }"

      return digest
    }

    private fun toTextEllipsis(contentText: String): String {
      return if (contentText.length > 100) {
        contentText.subSequence(0, 100).toString() + "..."
      } else {
        contentText
      }
    }

    private fun formatDateForUser(date: Date, dateTimeFormat: String): String {
      return SimpleDateFormat(dateTimeFormat).format(date)
    }
  }

  private fun importArticles(
    corrId: String,
    importer: ImporterEntity,
    contents: Stream<ContentEntity>
  ) {
    val bucket = importer.bucket!!
    val status = if (importer.autoRelease) {
      ReleaseStatus.released
    } else {
      ReleaseStatus.needs_approval
    }

    runCatching {
      importerService.importArticlesToTargets(
        corrId,
        contents
//      .filter { filterService.filter(corrId, it, StringUtils.trimToEmpty(importer.filter)) }
//          .map { content ->
//            run {
//              content.publishedAt = if (content.publishedAt!! > Date()) {
//                content.publishedAt!!
//              } else {
//                log.debug("[${corrId}] Overwriting pubDate cause is in past")
//                Date()
//              }
//              content
//            }
//          }
          .collect(Collectors.toList()),
        bucket.stream!!,
        importer.feed!!,
        ArticleType.feed,
        status,
        releasedAt = null,
        targets = importer.targets
      )
    }.onFailure { log.error("[${corrId}] pushArticleToTargets failed: ${it.message}") }
      .onSuccess { log.debug("[${corrId}] Updated bucket-feed ${propertyService.publicUrl}/bucket:${bucket.id}") }
  }
}
