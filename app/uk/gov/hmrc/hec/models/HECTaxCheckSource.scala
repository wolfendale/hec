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

package uk.gov.hmrc.hec.models

import ai.x.play.json.Jsonx
import ai.x.play.json.SingletonEncoder.simpleName
import ai.x.play.json.implicits.formatSingleton
import cats.Eq
import play.api.libs.json.Format

sealed trait HECTaxCheckSource extends Product with Serializable

object HECTaxCheckSource {

  case object Digital extends HECTaxCheckSource

  case object Stride extends HECTaxCheckSource

  @SuppressWarnings(Array("org.wartremover.warts.Throw", "org.wartremover.warts.Equals"))
  implicit val format: Format[HECTaxCheckSource] = Jsonx.formatSealed[HECTaxCheckSource]

  implicit val eq: Eq[HECTaxCheckSource] = Eq.fromUniversalEquals

}
