/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.hec.controllers

import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, v2}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait AuthSupport { this: ControllerSpec =>

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  def mockAuth[R](predicate: Predicate, retrieval: Retrieval[R])(
    result: Future[R]
  ): Unit =
    (mockAuthConnector
      .authorise(_: Predicate, _: Retrieval[R])(
        _: HeaderCarrier,
        _: ExecutionContext
      ))
      .expects(predicate, retrieval, *, *)
      .returning(result)

  def mockAuthWithGGRetrieval(ggCredId: String): Unit =
    mockAuth(AuthProviders(GovernmentGateway), v2.Retrievals.credentials)(
      Future.successful(Some(Credentials(ggCredId, ggCredId)))
    )

  def mockAuthWithForbidden(): Unit =
    mockAuth(AuthProviders(GovernmentGateway), v2.Retrievals.credentials)(
      Future.successful(None)
    )

}
