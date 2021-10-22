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

package uk.gov.hmrc.hec.repos

import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.libs.json.{JsNumber, JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.hec.models.ApplicantDetails.CompanyApplicantDetails
import uk.gov.hmrc.hec.models.HECTaxCheckData.CompanyHECTaxCheckData
import uk.gov.hmrc.hec.models.TaxDetails.CompanyTaxDetails
import uk.gov.hmrc.hec.models.ids.{CRN, CTUTR, GGCredId}
import uk.gov.hmrc.hec.models.licence.{LicenceDetails, LicenceTimeTrading, LicenceType, LicenceValidityPeriod}
import uk.gov.hmrc.hec.models.{CTAccountingPeriod, CTStatus, CTStatusResponse, CompanyHouseName, HECTaxCheck, HECTaxCheckCode, HECTaxCheckSource, YesNoAnswer}
import uk.gov.hmrc.hec.util.TimeUtils
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}

import java.time.{LocalDate, ZoneId, ZonedDateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HECTaxCheckStoreImplSpec extends AnyWordSpec with Matchers with Eventually with MongoSupport {

  val config: Configuration = Configuration(
    ConfigFactory.parseString(
      """
        | hec-tax-check.ttl = 1 day
        |""".stripMargin
    )
  )

  val taxCheckStore = new HECTaxCheckStoreImpl(mongoComponent, config)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "HECTaxCheckStoreImpl" must {

    val taxCheckStartDateTime = ZonedDateTime.of(2021, 10, 9, 9, 12, 34, 0, ZoneId.of("Europe/London"))

    val taxCheckData = CompanyHECTaxCheckData(
      CompanyApplicantDetails(GGCredId(""), CRN(""), CompanyHouseName("Test Tech Ltd")),
      LicenceDetails(
        LicenceType.ScrapMetalDealerSite,
        LicenceTimeTrading.EightYearsOrMore,
        LicenceValidityPeriod.UpToOneYear
      ),
      CompanyTaxDetails(
        CTUTR("1111111111"),
        Some(CTUTR("1111111111")),
        Some(YesNoAnswer.Yes),
        CTStatusResponse(
          CTUTR("1111111111"),
          LocalDate.of(2020, 10, 9),
          LocalDate.of(2021, 10, 9),
          Some(CTAccountingPeriod(LocalDate.of(2020, 10, 9), LocalDate.of(2021, 10, 9), CTStatus.ReturnFound))
        ),
        None,
        Some(YesNoAnswer.Yes)
      ),
      taxCheckStartDateTime,
      HECTaxCheckSource.Digital
    )

    val taxCheckCode1 = HECTaxCheckCode("code1")
    val taxCheckCode2 = HECTaxCheckCode("code12")
    val taxCheckCode3 = HECTaxCheckCode("code13")
    val taxCheck1     = HECTaxCheck(taxCheckData, taxCheckCode1, TimeUtils.today(), TimeUtils.now(), false, None)
    val taxCheck2     = taxCheck1.copy(taxCheckCode = taxCheckCode2)
    val taxCheck3     = taxCheck1.copy(taxCheckCode = taxCheckCode3, isExtracted = true)

    "be able to insert tax checks into mongo, read it back and delete it" in {

      // store a tax check
      await(taxCheckStore.store(taxCheck1).value) shouldBe Right(())

      // check we can get it back
      eventually {
        await(taxCheckStore.get(taxCheckCode1).value) should be(Right(Some(taxCheck1)))
      }

      // check that delete returns ok
      eventually {
        await(taxCheckStore.delete(taxCheckCode1).value) should be(Right(()))
      }

      // check that delete actually happened
      eventually {
        await(taxCheckStore.get(taxCheckCode1).value) should be(Right(None))
      }

    }

    "be able to delete all records" in {
      await(taxCheckStore.store(taxCheck1).value) shouldBe Right(())
      await(taxCheckStore.store(taxCheck2).value) shouldBe Right(())

      // check we can get them back
      eventually {
        await(taxCheckStore.get(taxCheckCode1).value) should be(Right(Some(taxCheck1)))
      }

      eventually {
        await(taxCheckStore.get(taxCheckCode2).value) should be(Right(Some(taxCheck2)))
      }

      // check that delete all returns ok
      eventually {
        await(taxCheckStore.deleteAll().value) should be(Right(()))
      }

      // check that delete actually happened
      eventually {
        await(taxCheckStore.get(taxCheckCode1).value) should be(Right(None))
      }

      eventually {
        await(taxCheckStore.get(taxCheckCode2).value) should be(Right(None))
      }
    }

    "return nothing if there is no data in mongo" in {
      await(taxCheckStore.get(HECTaxCheckCode("abc")).value) shouldBe Right(None)
    }

    "return an error" when {

      "the data in mongo cannot be parsed" in {
        val taxCheckCode              = HECTaxCheckCode("invalid-data")
        val invalidData               = JsObject(Map("hec-tax-check" -> JsNumber(1)))
        val create: Future[CacheItem] =
          taxCheckStore.put(taxCheckCode.value)(DataKey("hec-tax-check"), invalidData)

        await(create).id                                    shouldBe "invalid-data"
        await(taxCheckStore.get(taxCheckCode).value).isLeft shouldBe true
      }

    }

    "be able to fetch all tax check codes using the GGCredId" in {
      val ggCredId = taxCheckData.applicantDetails.ggCredId

      // store some tax check codes in mongo
      await(taxCheckStore.store(taxCheck1).value) shouldBe Right(())
      await(taxCheckStore.store(taxCheck2).value) shouldBe Right(())

      eventually {
        await(taxCheckStore.getTaxCheckCodes(ggCredId).value).map(_.toSet) should be(Right(Set(taxCheck1, taxCheck2)))
      }
    }

    "return an error when data can't be parsed when fetching tax check codes based on GGCredId" in {
      val taxCheckCode = "code1"
      val ggCredId     = "ggCredId"

      val invalidData = Json.parse(s"""{
                                   | "taxCheckData" : {
                                   | 	"applicantDetails" : {
                                   | 		"ggCredId" : "ggCredId",
                                   | 		"crn" : ""
                                   | 	}
                                   | },
                                   | "taxCheckCode" : "$taxCheckCode",
                                   | "expiresAfter" : "2021-09-24"
                                   |}""".stripMargin)

      // insert invalid data
      await(taxCheckStore.put(taxCheckCode)(DataKey("hec-tax-check"), invalidData))
      await(taxCheckStore.getTaxCheckCodes(GGCredId(ggCredId)).value).isLeft shouldBe true
    }

    "be able to fetch all tax check codes with isExtracted false" in {

      // store some tax check codes in mongo
      await(taxCheckStore.store(taxCheck1).value) shouldBe Right(())
      await(taxCheckStore.store(taxCheck2).value) shouldBe Right(())
      await(taxCheckStore.store(taxCheck3).value) shouldBe Right(())
      eventually {
        await(taxCheckStore.getAllTaxCheckCodesByExtractedStatus(false).value).map(_.toSet) should be(
          Right(Set(taxCheck1, taxCheck2))
        )
      }
    }

    "return an error when data can't be parsed when fetching tax check codes based on isExtracted field" in {
      val taxCheckCode = "code1"

      val invalidData = Json.parse(s"""{
                                      | "taxCheckData" : {
                                      | 	"applicantDetails" : {
                                      | 		"ggCredId" : "ggCredId",
                                      | 		"crn" : ""
                                      | 	}
                                      | },
                                      | "taxCheckCode" : "$taxCheckCode",
                                      | "expiresAfter" : "2021-09-24",
                                      | "isExtracted" : false
                                      |}""".stripMargin)

      // insert invalid data
      await(taxCheckStore.put(taxCheckCode)(DataKey("hec-tax-check"), invalidData))
      await(taxCheckStore.getAllTaxCheckCodesByExtractedStatus(false).value).isLeft shouldBe true
    }
  }

}
