/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.hec.models.hecTaxCheck

import ai.x.play.json.Jsonx
import ai.x.play.json.SingletonEncoder.simpleName
import ai.x.play.json.implicits.formatSingleton
import enumeratum.{Enum, EnumEntry}
import play.api.libs.json.Format

sealed trait CorrectiveAction extends EnumEntry with Product with Serializable

object CorrectiveAction extends Enum[CorrectiveAction] {
  case object RegisterNewSAAccount extends CorrectiveAction
  case object DormantAccountReactivated extends CorrectiveAction
  case object Other extends CorrectiveAction

  val values = findValues

  implicit val format: Format[CorrectiveAction] = Jsonx.formatSealed[CorrectiveAction]

}
