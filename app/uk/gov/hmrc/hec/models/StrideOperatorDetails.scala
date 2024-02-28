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

package uk.gov.hmrc.hec.models

import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.hec.models.ids.PID

final case class StrideOperatorDetails(
  pid: PID,
  roles: List[String],
  name: Option[String],
  email: Option[String]
)

object StrideOperatorDetails {

  implicit val writes: OWrites[StrideOperatorDetails] = Json.writes

}
