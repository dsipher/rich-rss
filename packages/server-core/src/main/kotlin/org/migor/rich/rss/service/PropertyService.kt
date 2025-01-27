package org.migor.rich.rss.service

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import java.util.*
import javax.annotation.PostConstruct

@Service
@ConfigurationProperties("app")
class PropertyService {

  private val log = LoggerFactory.getLogger(PropertyService::class.simpleName)

  lateinit var publicUrl: String
  lateinit var nitterHost: String
  lateinit var puppeteerHost: String
  lateinit var invidiousHost: String
  lateinit var dateFormat: String
  lateinit var timeFormat: String
  lateinit var webToFeedVersion: String
  lateinit var timezone: String
  lateinit var locale: Locale
  lateinit var defaultLocale: String

  @PostConstruct
  fun onInit() {
    logProperty("publicUrl = $publicUrl")
//    logProperty("nitterHost = $nitterHost")
//    logProperty("invidiousHost = $invidiousHost")
    logProperty("dateFormat = $dateFormat")
    logProperty("timeFormat = $timeFormat")
    logProperty("webToFeedVersion = $webToFeedVersion")
    logProperty("puppeteerHost = $puppeteerHost")
    logProperty("timezone = $timezone")
    locale = Locale.forLanguageTag(defaultLocale)
    logProperty("locale = ${locale}")
  }

  private fun logProperty(value: String) {
    log.info("property ${value}")
  }
}
