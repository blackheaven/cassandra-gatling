package io.ineedcode.gatling

import scala.concurrent.duration.DurationInt

import com.datastax.driver.core.{Host, Metadata, Row, Session, Cluster, ConsistencyLevel}

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation

import io.github.gatling.cql.Predef._


class CassandraScenario extends Simulation {

  val keyspace = "test_gatling"
  val table_name = "test_table"
  val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
  val session = cluster.connect(s"$keyspace")


  //  Your C* session
  val cqlConfig = cql.session(session) //Initialize Gatling DSL with your session


  //Setup
  session.execute(
    s"""CREATE KEYSPACE IF NOT EXISTS $keyspace
        WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor': '1'}""")


  session.execute(
    s"""
        CREATE TABLE IF NOT EXISTS $table_name (
          id timeuuid,
          num int,
          str text,
          PRIMARY KEY (id)
        );
    """)
  //It's generally not advisable to use secondary indexes in you schema
  session.execute(
    f"""CREATE INDEX IF NOT EXISTS $table_name%s_num_idx ON $table_name%s (num)""")


  //Prepare your statement, we want to be effective, right?
  val prepared = session.prepare(
    s"""
       INSERT INTO $table_name (id, num, str) VALUES (now(), ?, ?)
    """.stripMargin)


  val random = new util.Random
  val feeder = Iterator.continually(
    // this feader will "feed" random data into our Sessions
    Map(
      "randomString" -> random.nextString(20),
      "randomNum" -> random.nextInt()
    ))

  //Name your scenario
  val scn = scenario("Two statements").repeat(2) {
    feed(feeder)
      .exec(cql("simple SELECT")
        // 'execute' can accept a string
        // and understands Gatling expression language (EL), i.e. ${randomNum}
        .execute("SELECT * FROM test_table WHERE num = ${randomNum}"))
      .exec(cql("prepared INSERT")
        // alternatively 'execute' accepts a prepared statement
        .execute(prepared)
        // you need to provide parameters for that (EL is supported as well)
        .withParams(Integer.valueOf(random.nextInt()), "${randomString}")
        // and set a ConsistencyLevel optionally
        .consistencyLevel(ConsistencyLevel.ANY))
  }

  setUp(
    //    scn.inject(rampUsersPerSec(100) to 1000 during (15 seconds))
    scn.inject(atOnceUsers(10))
  ).protocols(cqlConfig)


  after(cluster.close()) // close session and stop associated threads started by the Java/Scala driver


}
