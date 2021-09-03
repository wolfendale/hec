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

package uk.gov.hmrc.hec.services

import cats.data.EitherT
import cats.instances.future._
import cats.syntax.eq._
import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.typesafe.config.Config
import configs.syntax._
import uk.gov.hmrc.hec.models.HECTaxCheckData.{CompanyHECTaxCheckData, IndividualHECTaxCheckData}
import uk.gov.hmrc.hec.models.HECTaxCheckMatchResult.{Expired, Match, NoMatch}
import uk.gov.hmrc.hec.models.{Error, HECTaxCheck, HECTaxCheckData, HECTaxCheckMatchRequest, HECTaxCheckMatchResult}
import uk.gov.hmrc.hec.repos.HECTaxCheckStore
import uk.gov.hmrc.hec.util.TimeUtils
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[TaxCheckServiceImpl])
trait TaxCheckService {

  def saveTaxCheck(taxCheckData: HECTaxCheckData)(implicit hc: HeaderCarrier): EitherT[Future, Error, HECTaxCheck]

  def matchTaxCheck(taxCheckMatchRequest: HECTaxCheckMatchRequest)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, HECTaxCheckMatchResult]

}

@Singleton
class TaxCheckServiceImpl @Inject() (
  taxCheckCodeGeneratorService: TaxCheckCodeGeneratorService,
  taxCheckStore: HECTaxCheckStore,
  config: Config
)(implicit ec: ExecutionContext)
    extends TaxCheckService {

  val taxCheckCodeExpiresAfterDays: Long =
    config.get[FiniteDuration]("hec-tax-check.expires-after").value.toDays

  def saveTaxCheck(
    taxCheckData: HECTaxCheckData
  )(implicit hc: HeaderCarrier): EitherT[Future, Error, HECTaxCheck] = {
    val taxCheckCode = taxCheckCodeGeneratorService.generateTaxCheckCode()
    val expiryDate   = TimeUtils.today().plusDays(taxCheckCodeExpiresAfterDays)
    val taxCheck     = HECTaxCheck(taxCheckData, taxCheckCode, expiryDate)

    taxCheckStore.store(taxCheck).map(_ => taxCheck)
  }

  def matchTaxCheck(taxCheckMatchRequest: HECTaxCheckMatchRequest)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, HECTaxCheckMatchResult] =
    taxCheckStore
      .get(taxCheckMatchRequest.taxCheckCode)
      .map(
        _.fold[HECTaxCheckMatchResult](NoMatch(taxCheckMatchRequest))(doMatch(taxCheckMatchRequest, _))
      )

  private def doMatch(
    taxCheckMatchRequest: HECTaxCheckMatchRequest,
    storedTaxCheck: HECTaxCheck
  ): HECTaxCheckMatchResult = {
    lazy val hasExpired = TimeUtils.today().isAfter(storedTaxCheck.expiresAfter)

    val applicantVerifierMatches = (taxCheckMatchRequest.verifier, storedTaxCheck.taxCheckData) match {
      case (Right(dateOfBirth), storedIndividualData: IndividualHECTaxCheckData) =>
        dateOfBirth === storedIndividualData.applicantDetails.dateOfBirth

      case (Left(crn), storedCompanyData: CompanyHECTaxCheckData) =>
        crn === storedCompanyData.applicantDetails.crn

      case _ =>
        false
    }

    val licenceTypeMatches =
      taxCheckMatchRequest.licenceType === storedTaxCheck.taxCheckData.licenceDetails.licenceType

    if (licenceTypeMatches && applicantVerifierMatches) {
      if (hasExpired) Expired(taxCheckMatchRequest)
      else Match(taxCheckMatchRequest)
    } else
      NoMatch(taxCheckMatchRequest)
  }

}
