/**
 * Copyright (c) 2015 Basho Technologies, Inc.
 *
 * This file is provided to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.basho.riak.spark.rdd

import java.util.concurrent.ExecutionException
import java.util.{Calendar, TimeZone, GregorianCalendar}

import com.basho.riak.client.core.netty.RiakResponseException
import com.basho.riak.client.core.operations.{FetchBucketPropsOperation, TimeSeriesStoreOperation}
import com.basho.riak.client.core.query.{BucketProperties, Namespace}
import com.basho.riak.client.core.query.timeseries.{Cell, Row}
import org.junit.{Assume, Before, Test}
import scala.collection.JavaConversions._
import com.basho.riak.spark.toSparkContextFunctions

case class TimeSeriesData(time: Long, user_id: String, temperature_k: Float)

class TimeSeriesTest extends AbstractRDDTest {

  protected val DEFAULT_TS_NAMESPACE = new Namespace("time_series_test","time_series_test")

  var tsRangeStart: Calendar = null
  var tsRangeEnd: Calendar = null

  private def mkTimestamp(timeInMillis: Long): Calendar = {
    val c = new GregorianCalendar(TimeZone.getTimeZone("UTC"))
    c.setTimeInMillis(timeInMillis)
    c
  }

  @Before
  def setupData(): Unit = {

    val fetchProps = new FetchBucketPropsOperation.Builder(DEFAULT_TS_NAMESPACE).build()

    withRiakDo( session => {
      session.getRiakCluster.execute(fetchProps)
    })

    try {
      val props: BucketProperties = fetchProps.get().getBucketProperties
    } catch {
      case ex :ExecutionException if ex.getCause.isInstanceOf[RiakResponseException]
          && ex.getCause.getMessage.startsWith("No bucket-type named")  =>

        val msg = "Bucket type for Time Series test data is not created, Time series tests will be skipped"

        logWarning(msg + "\n\n" +
          "To create and activate TS test bucket, please use the following commands:\n" +
          "\t./riak-admin bucket-type create time_series_test '{\"props\":{\"n_val\":3, \"table_def\": \"create table time_series_test (time timestamp not null, user_id varchar not null, temperature_k float, primary key ((quantum(time, 10, s)), time))\"}}'\n" +
          "\t./riak-admin bucket-type activate time_series_test")

        /**
         * ./riak-admin bucket-type create time_series_test '{"props":{"n_val":3, "table_def": "create table time_series_test (time timestamp not null, user_id varchar not null, temperature_k float, primary key ((quantum(time, 10, s)), time))"}}'
         * ./riak-admin bucket-type activate time_series_test
         */
        Assume.assumeTrue(msg + " (See logs for the details)", false)
    }

    //Store data in RIAK TS

    tsRangeStart = mkTimestamp(111111)
    tsRangeEnd = mkTimestamp(111555)

    val tableName = DEFAULT_TS_NAMESPACE.getBucketType

    val rows = List(
      new Row(
          new Cell(tsRangeStart),
          new Cell("bryce"),
          new Cell(305.37)
        ),

      new Row(
          new Cell(mkTimestamp(111222)),
          new Cell("bryce"),
          new Cell(300.12)
        ),

      new Row(
          new Cell(mkTimestamp(111333)),
          new Cell("bryce"),
          new Cell(295.95)
        ),

      new Row(
          new Cell(mkTimestamp(111444)),
          new Cell("ratman"),
          new Cell(362.121)
        ),

      new Row(
          new Cell(tsRangeEnd),
          new Cell("ratman"),
          new Cell(3502.212)
        )
    )

    val storeOp = new TimeSeriesStoreOperation.Builder(tableName).withRows(rows).build()

    withRiakDo(session=>{
      val r = session.getRiakCluster.execute(storeOp).get()
      assert(true)
    })
  }

  @Test
  def readDataAsSqlRow(): Unit ={
    val from = tsRangeStart.getTimeInMillis - 5
    val to = tsRangeEnd.getTimeInMillis + 10

    val bucketName =  DEFAULT_TS_NAMESPACE.getBucketTypeAsString
    val rdd = sc.riakTSBucket[org.apache.spark.sql.Row](bucketName)
      .sql(s"SELECT user_id, temperature_k FROM $bucketName WHERE time > $from AND time < $to "/*+ " AND user_id = 'bryce'"*/)

    val data = rdd.collect() map (r => r.toSeq)

    assertEqualsUsingJSONIgnoreOrder(
      "[" +
          "['bryce',305.37]," +
          "['bryce',300.12]," +
          "['bryce',295.95]," +
          "['ratman',362.121]," +
          "['ratman',3502.212]" +
        "]"
      , data)
  }

  private def stringify = (s: Array[String]) => s.mkString("[", ",", "]")

  // TODO: Consider possibility of moving this case to the SparkDataframesTest
  @Test
  def riakTSRDDToDataFrame(): Unit ={
    val from = tsRangeStart.getTimeInMillis - 5
    val to = tsRangeEnd.getTimeInMillis + 10

    val bucketName =  DEFAULT_TS_NAMESPACE.getBucketTypeAsString


    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    import sqlContext.implicits._

    val df = sc.riakTSBucket[org.apache.spark.sql.Row](bucketName)
      .sql(s"SELECT time, user_id, temperature_k FROM $bucketName WHERE time > $from AND time < $to "/*+ " AND user_id = 'bryce'"*/)
      .map(r=> TimeSeriesData(r.getLong(0), r.getString(1), r.getFloat(2)))
      .toDF()

    df.registerTempTable("test")

    val data = sqlContext.sql("select * from test").toJSON.collect()

    assertEqualsUsingJSONIgnoreOrder("[" +
      "{time:111111, user_id:'bryce', temperature_k:305.37}," +
      "{time:111222, user_id:'bryce', temperature_k:300.12}," +
      "{time:111333, user_id:'bryce', temperature_k:295.95}," +
      "{time:111444, user_id:'ratman',temperature_k:362.121}," +
      "{time:111555, user_id:'ratman',temperature_k:3502.212}" +
      "]", stringify(data))
  }
}
