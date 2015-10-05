fork in run := true

mainClass in (Compile, run) := Some("uk.gov.homeoffice.rtp.proxy.Boot")

logLevel in assembly := Level.Info

assemblyExcludedJars in assembly := {
  val testDependencies = (fullClasspath in Test).value
    .sortWith((f1, f2) => f1.data.getName < f2.data.getName)
  println("=========================== Test Dependencies ========================= \n" + testDependencies.map(_.data.getAbsolutePath).mkString("\n"))

  val compileDependencies = (fullClasspath in Compile).value
    .filterNot(_.data.getName.endsWith("-tests.jar"))
    .filterNot(_.data.getName.startsWith("mockito-"))
    .filterNot(_.data.getName.startsWith("specs2-"))
    .filterNot(_.data.getName.startsWith("scalatest"))
    .sortWith((f1, f2) => f1.data.getName < f2.data.getName)
  println(s"=========================== Compile Dependencies =========================== \n" + compileDependencies.map(_.data.getName).mkString("\n"))

  val testOnlyDependencies = testDependencies.diff(compileDependencies).sortWith((f1, f2) => f1.data.getName < f2.data.getName)
  println(s"=========================== Test ONLY Dependencies ===========================\n" + testOnlyDependencies.map(_.data.getName).mkString("\n"))
  testOnlyDependencies
}

assemblyMergeStrategy in assembly := {
  case "logback.xml" => MergeStrategy.first
  case PathList("org", "mozilla", _*) => MergeStrategy.first
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".java" => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".so" => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".jnilib" => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".dll" => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".tooling" => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

test in assembly := {}
