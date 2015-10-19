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

import org.junit.{Test, Ignore}
import org.apache.spark.Logging
import com.basho.riak.client.core.query.timeseries.Row
import com.basho.riak.client.core.query.timeseries.Cell
import com.basho.riak.spark.util.TimeSeriesToSparkSqlConversion
import org.junit.Assert._
import com.basho.riak.client.core.util.BinaryValue
import java.util.Date
import java.util.Calendar
import java.util.Arrays
import scala.collection.JavaConversions._
import com.basho.riak.client.core.query.timeseries.SetCell
import com.fasterxml.jackson.core.`type`.TypeReference
import com.basho.riak.client.core.query.timeseries.MapCell

class TSConversionTest extends Logging{
  
  case class SampleClass(int: Integer, date: Date, string: String)

  def singleCellTest [T] (value: T, cell: Cell): Unit = {
    val singleCellRow = new Row(cell)
    val sparkRow = TimeSeriesToSparkSqlConversion.asSparkRow(singleCellRow)
    assertEquals(1, sparkRow.length)
    assertEquals(Seq(value), sparkRow.toSeq)
    assertEquals(value.getClass, sparkRow.get(0).getClass)
  }
  
  @Test
  def singleRowSingleIntCellTest(): Unit = {
    val int = 1
    singleCellTest(int, new Cell(int))
  } 
  
  @Test
  def singleRowSingleLongCellTest(): Unit = {
    val long = 1L // gets mapped as integer
    singleCellTest(long, new Cell(long))
  } 
  
  @Test
  def singleRowSingleLongCellMaxValueTest(): Unit = {
    val long = Long.MaxValue
    singleCellTest(long, new Cell(long))
  } 
  
  @Test
  def singleRowSingleStringCellTest(): Unit = {
    val string= "abc"
    singleCellTest(string, new Cell(string))
  } 
  
  @Test
  def singleRowSingleBooleanCellTest(): Unit = {
    val bool = true
    singleCellTest(bool, new Cell(bool))
  } 
  
  @Test
  def singleRowSingleFloatCellTest(): Unit = {
    val float = 1.0f
    singleCellTest(float, new Cell(float))
  } 
  
  @Test
  @Ignore
  def singleRowSingleDoubleCellTest(): Unit = {
    val double = 1.0d
    singleCellTest(double, new Cell(double))
  } 
  
  @Test
  @Ignore
  def singleRowSingleDoubleCellMaxValueTest(): Unit = {
    val double = Double.MaxValue
    singleCellTest(double, new Cell(double))
  } 
  
  @Test
  def singleRowSingleBinaryValueCellTest(): Unit = {
    val string = "abc"
    val binary: BinaryValue = BinaryValue.create(string)
    val cell = new Cell(binary)
    singleCellTest(string, cell)
  } 
  
  @Test
  def singleRowSingleDateCellTest(): Unit = {
    val millis = System.currentTimeMillis
    val date = new Date(millis)
    val cell = new Cell(date)
    singleCellTest(millis, cell)
  } 
  
  @Test
  def singleRowSingleCalendarCellTest(): Unit = {
    val millis = System.currentTimeMillis
    val cal = Calendar.getInstance
    cal.setTimeInMillis(millis)
    val cell = new Cell(cal)
    singleCellTest(millis, cell)
  }
  
  @Test
  def singleRowSingleRawNumericTest(): Unit = {
    val float = -42.02f
    val ba = String.valueOf(float).getBytes();
    val cell = Cell.newRawNumeric(ba);
    singleCellTest(float, cell)
  }
  
  @Test
  @Ignore
  def singleRowSingleSetCellTest(): Unit = {
    val set = Set("abc", "111", "?>!")
    val cell = SetCell.fromSet(set, new TypeReference[String] {});
    singleCellTest(set, cell)
  }
  
  @Test
  @Ignore
  def singleRowSingleMapCellTest(): Unit = {
    val millis = System.currentTimeMillis
    val date = new Date(millis)
    val obj = SampleClass(1, date, "abc")
    val cell = MapCell.fromObject(obj,  new TypeReference[SampleClass] {})
    // object or map?
    //val map = Map("int" -> 1, "date" -> date, "string" -> "abc")
    singleCellTest(obj, cell)
  }
  
  @Test
  def singleRowMultipleCellsTest(): Unit = {
    val millis = System.currentTimeMillis
    val date = new Date(millis)
    
    val cells = List(new Cell(1), new Cell(date), new Cell("abc"))
    val row = new Row(cells)
    val sparkRow = TimeSeriesToSparkSqlConversion.asSparkRow(row)
    val expected = List(1, millis, "abc")
    assertSeqEquals(expected, sparkRow.toSeq)
  }

  private def compareElementwise(a: Seq[Any], b: Seq[Any]): Boolean = {
    a match {
      case Nil => true
      case x::xs => 
        {
          val bh = b.head
          val a = x.equals(b.head)
          x.equals(b.head) && compareElementwise(xs, b.tail)
        }
    }
  }

  private def assertSeqEquals(expected: Seq[Any], actual: Seq[Any]): Unit = {
    def stringify = (s: Seq[Any]) => s.mkString("[", ",", "]")
    if (expected.length != actual.length) 
      throw new AssertionError(s"Expected Seq of ${expected.length} elements, but got ${actual.length}")
    if (!compareElementwise(expected, actual)) 
      throw new AssertionError(s"Expected Seq ${stringify(expected)} but got ${stringify(actual)}")
  }  
  
}
