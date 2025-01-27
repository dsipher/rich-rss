package org.migor.rich.rss.service

import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.MeterRegistry
import org.apache.commons.lang3.StringUtils
import org.migor.rich.rss.transform.GenericFeedFetchOptions
import org.migor.rich.rss.util.JsonUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import javax.annotation.PostConstruct

@RestController
class PuppeteerService {

  private var puppeteerHostFound: Boolean = false
  private val log = LoggerFactory.getLogger(PuppeteerService::class.simpleName)

  @Autowired
  lateinit var feedService: FeedService

  @Autowired
  lateinit var meterRegistry: MeterRegistry

  @Autowired
  lateinit var environment: Environment

  @Autowired
  lateinit var httpService: HttpService

  @Value("\${app.puppeteerHost:#{null}}")
  lateinit var puppeteerHost: Optional<String>

  @Value("\${app.puppeteerTimeoutMs}")
  var puppeteerTimeout: Int = 10000

  fun canPrerender(): Boolean = puppeteerHostFound
  fun hasHost(): Boolean = puppeteerHost.map { StringUtils.isNoneBlank(it) }.orElse(false)

  private fun canConnect(corrId: String, host: String): Boolean {
    return runCatching {
      httpService.httpGet(corrId, "${host}/health", 200)
      // todo test contract (params and response) is valid
      true
    }.getOrElse {
      log.error("[${corrId}] Cannot connect to puppeteer ${puppeteerHost.get()}: ${it.message}")
      false
    }
  }

  @PostConstruct
  fun postConstruct() {
    puppeteerHostFound = puppeteerHost.map { canConnect("boot", it) }.orElse(false)

    if (puppeteerHostFound) {
      log.info("Prerendering using ${puppeteerHost.get()}")
    } else {
      log.warn("To use prerendering env PUPPETEER_HOST")
    }
  }

  @Timed
  fun prerender(
    corrId: String,
    url: String,
    options: GenericFeedFetchOptions,
  ): PuppeteerScreenshotResponse {
    meterRegistry.counter("prerender").increment()
    log.info("[$corrId] prerender url=$url, options=$options")
    return try {
      val puppeteerUrl = "${puppeteerHost.get()}/api/intern/prerender/?url=${
        URLEncoder.encode(
          url,
          StandardCharsets.UTF_8
        )
      }&timeout=${puppeteerTimeout}&options=${
        URLEncoder.encode(
          JsonUtil.gson.toJson(options),
          StandardCharsets.UTF_8
        )
      }"

      val response = httpService.httpGetCaching(corrId, puppeteerUrl, 200, mapOf("x-corr-id" to corrId))
      JsonUtil.gson.fromJson(String(response.responseBody), PuppeteerScreenshotResponse::class.java)
    } catch (e: Exception) {
      log.error("[$corrId] Unable to discover feeds using puppeteer: ${e.message}")
      e.printStackTrace()
      // todo mag return error code
      PuppeteerScreenshotResponse(screenshot = "", isError = true, errorMessage = e.message)
    }
  } }

data class PuppeteerScreenshotResponse(
  val screenshot: String? = null,
  val html: String? = null,
  val isError: Boolean,
  val errorMessage: String? = null
)
