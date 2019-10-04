import Dependencies._

organizationName := "Raymond Tay"
startYear := Some(2019)
licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))

scalaVersion := "2.12.9"
scalacOptions := Seq("-deprecation", "-Ypartial-unification", "-unchecked", "-feature", "-language:higherKinds", "-language:postfixOps")
libraryDependencies ++= myDependencies
resolvers += Resolver.mavenLocal
