package io.ineedcode.gatling

import scala.concurrent.duration.DurationInt
import com.datastax.driver.core._
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.github.gatling.cql.Predef._
import scala.collection.JavaConversions._


// link https://github.com/maxdemarzi/neo_gatling/blob/master/neo4j/CreateNodes.scala
class CassandraScenario extends Simulation {

  val keyspace = "gatling_test"
  val table_name = "test_table"
  val cluster = myCluster("127.0.0.1")
  val session = cluster.connect()


  //  Your C* session
  val cqlConfig = cql.session(session)

  // ------- CREATE KEYSPACE
  session.execute(
    s"""
        CREATE KEYSPACE IF NOT EXISTS $keyspace WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor': '1'}
      """)

  // ------- CREATE TABLE
  session.execute(
    s"""
        CREATE TABLE IF NOT EXISTS $keyspace.$table_name (
          id timeuuid,
          num int,
          str text,
          PRIMARY KEY (id)
        );
      """)

  // ------- CREATE INDEX
  session.execute(f"""CREATE INDEX IF NOT EXISTS $table_name%s_num_idx ON $keyspace.$table_name%s (num)""")


  val preparedInsert = session.prepare(
    s"""
       INSERT INTO $keyspace.$table_name (id, num, str) VALUES (now(), ?, ?)
    """)


  val random = new util.Random
  val feeder = Iterator.continually(
    // this feader will "feed" random data into our Sessions
    Map(
      "randomString" -> random.nextString(20),
      "randomNum" -> random.nextInt()
    ))

  val scn = scenario("Two statements").repeat(100) {
    feed(feeder)
      .exec(cql("prepared SELECT")
        .execute(session.prepare(s"SELECT * FROM $keyspace.$table_name WHERE num = ?"))
        .withParams("${randomNum}")
        .consistencyLevel(ConsistencyLevel.ALL)
      )
      .exec(cql("prepared INSERT")
        .execute(preparedInsert)
        .withParams(Integer.valueOf(random.nextInt()), "${randomString}")
        .consistencyLevel(ConsistencyLevel.ANY)
      )
  }

  setUp(
    //    scn.inject(rampUsersPerSec(100) to 1000 during (15 seconds))
    //    scn.inject(atOnceUsers(10))
    scn.inject(rampUsers(100) over (10 seconds))
  )
    .assertions(
      global.responseTime.max.lessThan(450),
      global.successfulRequests.percent.greaterThan(95),
      details("prepared SELECT").requestsPerSec.greaterThan(1000),
      global.responseTime.percentile1.between(0, 100),
      details("prepared INSERT").responseTime.percentile1.between(0, 100)
    )
    .protocols(cqlConfig)


  after(cluster.close())


  def myCluster(node: String): Cluster = {
    val cluster = Cluster.builder().addContactPoint(node).build()
    val metadata: Metadata = cluster.getMetadata

    println(s"Connected to cluster: ${metadata.getClusterName}")

    for (host <- metadata.getAllHosts.groupBy(_.getDatacenter)) {
      val x1: (Host) => String = a => s"${a.getAddress} - Rack: ${a.getRack}"
      println(s"${host._1} | ${host._2.map(x1)}")
    }

    cluster
  }

}
