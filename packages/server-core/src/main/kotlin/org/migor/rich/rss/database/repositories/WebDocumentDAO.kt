package org.migor.rich.rss.database.repositories

import org.migor.rich.rss.database.models.WebDocumentEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface WebDocumentDAO : CrudRepository<WebDocumentEntity, UUID> {
  fun findByUrlEquals(url: String): Optional<WebDocumentEntity>
  fun existsByUrl(url: String): Boolean
  @Query(
    """
      select WD from WebDocumentEntity WD
      inner join HyperLinkEntity HL on HL.toId = WD.id
      where HL.fromId = ?1 and WD.finished is TRUE
    """
  )
  fun findAllOutgoingHyperLinksByContentId(fromId: UUID, pageable: Pageable): Page<WebDocumentEntity>

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
  override fun deleteById(id: UUID)
}
