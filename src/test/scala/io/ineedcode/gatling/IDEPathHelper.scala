import scala.reflect.io.File


object IDEPathHelper extends App {

  val gatlingConfUrl = getClass.getClassLoader.getResource("gatling.conf").getPath
  val projectRootDir = File(gatlingConfUrl).parents(3)

  println("gatlingConfUrl: " + gatlingConfUrl)
  println("projectRootDir: " + projectRootDir)

  val resourcesDirectory = projectRootDir / "src" / "test" / "resources"
  val targetDirectory = projectRootDir / "target" / "scala-2.11"
  val dataDirectory = resourcesDirectory / "data"
  println("dataDirectory: " + dataDirectory)

  val binariesDirectory = targetDirectory / "test-classes"
  //  val requestBodiesDirectory = resourcesDirectory / "request-bodies"
  val resultsDirectory = targetDirectory / "results"

}
