/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.hec.services

import com.google.inject.{ImplementedBy, Inject}
import play.api.Configuration
import uk.gov.hmrc.hec.services.scheduleService.HECTaxCheckExtractionContext
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import javax.inject.Singleton
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

@ImplementedBy(classOf[MongoLockServiceImpl])
trait MongoLockService {

  def withLock[T](lockId: String, data: => Future[T])(implicit
    hecTaxCheckExtractionContext: HECTaxCheckExtractionContext
  ): Future[Option[T]]

}

@Singleton
class MongoLockServiceImpl @Inject() (mongoLockRepository: MongoLockRepository, config: Configuration)
    extends MongoLockService {

  private val forceLockReleaseAfter: FiniteDuration =
    config.get[FiniteDuration]("hec-file-extraction-details.force-lock-release-after")

  def lockService(lockId: String): LockService = LockService(
    mongoLockRepository,
    lockId = lockId,
    ttl = forceLockReleaseAfter
  )

  override def withLock[T](lockId: String, data: => Future[T])(implicit
    hecTaxCheckExtractionContext: HECTaxCheckExtractionContext
  ): Future[Option[T]] =
    lockService(lockId).withLock(data)
}
