//
// sbt build info
//
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

//
// sbt revolver to reload on file system changes
//
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.0")

//
// twirl for templates
//
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.3.7")

//
// Generate file headers using `sbt createHeaders`
//
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "3.0.2")

//
// Native-Packager - to build the docker image
//
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.2")

//
// tpolecat - scalac flags recommended by Rob Norris
//
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.3")
