package org.migor.rich.rss.trigger

import org.migor.rich.rss.database.repositories.ImporterDAO
import org.migor.rich.rss.harvest.ImporterHarvester
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Profile("database")
class TriggerImporters internal constructor() {

  @Autowired
  lateinit var importerDAO: ImporterDAO

  @Autowired
  lateinit var importerHarvester: ImporterHarvester

  @Scheduled(fixedDelay = 2345)
  @Transactional(readOnly = true)
  fun fillImporters() {
    val pageable = PageRequest.ofSize(10)
    importerDAO.findSomeDueToImporters(Date(), pageable)
      .forEach { importerHarvester.handleImporter(it) }
  }
}
