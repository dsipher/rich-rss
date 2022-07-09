package org.migor.rich.rss.database2.models

import org.migor.rich.rss.database2.EntityWithUUID
import javax.persistence.Basic
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.Table

@Entity
@Table
open class BucketEntity : EntityWithUUID() {

    @Basic
    @Column(name = "title", nullable = false)
    open var title: String? = null

    @Basic
    @Column(name = "description")
    open var description: String? = null

//    @Basic
//    @Column(name = "listed", nullable = false)
//    open var listed: Boolean? = null

//    @Basic
//    @Column(name = "tags")
//    open var tags: Any? = null

    @Basic
    @Column(name = "streamId", nullable = false, insertable = false, updatable = false)
    open var streamId: String? = null

    @Basic
    @Column(name = "lastUpdatedAt")
    open var lastUpdatedAt: java.sql.Timestamp? = null

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "streamId", referencedColumnName = "id")
//    open var refStreamEntity: StreamEntity? = null

//    @OneToMany(mappedBy = "refBucketEntity")
//    open var refArticleExporterEntities: List<ArticleExporterEntity>? = null
//
//    @OneToMany(mappedBy = "refBucketEntity")
//    open var refSubscriptionEntities: List<SubscriptionEntity>? = null
//
//    @OneToMany(mappedBy = "refBucketEntity")
//    open var refArticlePostProcessorToBucketEntities: List<ArticlePostProcessorToBucketEntity>? = null

  @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
  @JoinTable(
    name = "bucket2feed",
    joinColumns = [
      JoinColumn(
        name = "user_id", referencedColumnName = "id",
        nullable = false, updatable = false
      )],
    inverseJoinColumns = [
      JoinColumn(
        name = "generic_feed_id", referencedColumnName = "id",
        nullable = false, updatable = false
      )
    ]
  )
  open var feeds: MutableList<FeedEntity> = mutableListOf()


}

