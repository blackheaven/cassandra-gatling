import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder
import io.ineedcode.gatling.CassandraScenario

object Engine extends App {


  val props = new GatlingPropertiesBuilder

  //  props.dataDirectory(IDEPathHelper.dataDirectory.toString())
  //  props.resultsDirectory(IDEPathHelper.resultsDirectory.toString())
  //  props.bodiesDirectory(IDEPathHelper.requestBodiesDirectory.toString())
  //  props.binariesDirectory(IDEPathHelper.binariesDirectory.toString())

  props.simulationClass(classOf[CassandraScenario].getName)
  //  println(classOf[CassandraScenario])

  Gatling.fromMap(props.build)
}
