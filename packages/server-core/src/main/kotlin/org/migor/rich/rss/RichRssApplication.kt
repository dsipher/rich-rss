package org.migor.rich.rss

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

@Configuration
@EnableAspectJAutoProxy
@SpringBootApplication
class RichRssApplication

fun main(args: Array<String>) {
  runApplication<RichRssApplication>(*args)
}
