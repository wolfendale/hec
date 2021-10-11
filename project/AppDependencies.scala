import play.core.PlayVersion
import sbt._

object AppDependencies {

  val akkaVersion = "2.6.10"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"   % "5.14.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"          % "0.55.0",
    "uk.gov.hmrc"             %% "domain"                      % "6.2.0-play-28",
    "ai.x"                    %% "play-json-extensions"        % "0.42.0",
    "org.typelevel"           %% "cats-core"                   % "2.6.1",
    "com.github.kxbmap"       %% "configs"                     % "0.4.4",
    "uk.gov.hmrc.objectstore" %% "object-store-client-play-28" % "0.35.0",
    "com.beachape"            %% "enumeratum"                  % "1.7.0",
    "com.miguno.akka"         %% "akka-mock-scheduler"         % "0.5.1" exclude ("com.typesafe.akka", "akka-actor")
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % "5.14.0"            % Test,
    "uk.gov.hmrc"            %% "reactivemongo-test"     % "5.0.0-play-28"     % Test,
    "org.scalatest"          %% "scalatest"              % "3.2.10"            % Test,
    "com.typesafe.play"      %% "play-test"              % PlayVersion.current % Test,
    "org.scalamock"          %% "scalamock"              % "5.1.0"             % Test,
    "com.vladsch.flexmark"    % "flexmark-all"           % "0.62.2"            % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"     % "5.1.0"             % "test, it",
    "com.typesafe.akka"      %% "akka-testkit"           % akkaVersion         % Test,
    "com.miguno.akka"        %% "akka-mock-scheduler"    % "0.5.1"             % Test
  )
}
