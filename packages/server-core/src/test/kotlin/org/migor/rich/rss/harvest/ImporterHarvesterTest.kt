package org.migor.rich.rss.harvest

import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.migor.rich.rss.database.models.ContentEntity
import java.util.*

internal class ImporterHarvesterTest {

  @Test
  fun createDigestOfArticles() {
    val bucketName = "foo"
    val title1 = "title 1"
    val title2 = "title 2"
    val contents = listOf(
      toContentEntity(title1, "https://foo-domain.com/post/2020-08-01", "description 1", Date()),
      toContentEntity(title2, "https://bar-domain.com/articles/1", "description 2", Date())
    )
    val digest = ImporterHarvester.createDigestOfArticles(bucketName, "dd.MM.yyyy", contents.stream())
    assertTrue(StringUtils.isNotEmpty(digest.contentText))
    assertTrue(StringUtils.isNotEmpty(digest.contentRawMime))
    assertTrue(StringUtils.isNotEmpty(digest.contentRaw))
    assertTrue(StringUtils.containsIgnoreCase(digest.contentText, bucketName))
    assertTrue(StringUtils.containsIgnoreCase(digest.contentRaw, bucketName))
    assertTrue(StringUtils.containsIgnoreCase(digest.contentText, title1))
    assertTrue(StringUtils.containsIgnoreCase(digest.contentRaw, title1))
    assertTrue(StringUtils.containsIgnoreCase(digest.contentText, title2))
    assertTrue(StringUtils.containsIgnoreCase(digest.contentRaw, title2))
  }

  private fun toContentEntity(title: String, url: String, description: String, pubDate: Date): ContentEntity {
    val a = ContentEntity()
    a.title = title
    a.url = url
    a.contentText = description
    a.publishedAt = pubDate

    return a
  }
}
