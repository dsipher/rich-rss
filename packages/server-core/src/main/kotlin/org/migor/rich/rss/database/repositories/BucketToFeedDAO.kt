package org.migor.rich.rss.database.repositories

import org.migor.rich.rss.database.models.Subscription
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface BucketToFeedDAO : CrudRepository<Subscription, UUID> {
  @Query(
    """
      select distinct s from Subscription s
        inner join NativeFeedEntity f
            on s.feedId = f.id
        inner join BucketEntity b
            on b.id = s.bucketId
        inner join ExporterEntity e
            on e.bucketId = b.id
        where e.id = :exporterId
        order by s.lastUpdatedAt asc
    """
  )
  fun findAllByExporterId(@Param("exporterId") exporterId: UUID): List<Subscription>

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(
    """
      update Subscription s
      set s.lastUpdatedAt = :lastUpdatedAt
      where s.bucketId = :bucketId
    """
  )
  fun setLastUpdatedAtByBucketId(@Param("bucketId") bucketId: UUID, @Param("lastUpdatedAt") lastUpdatedAt: Date)

  @Query(
    """
    select distinct s from Subscription s
    where s.bucketId = :bucketId
    """
  )
  fun findAllByBucketId(@Param("bucketId") bucketId: UUID): List<Subscription>

}