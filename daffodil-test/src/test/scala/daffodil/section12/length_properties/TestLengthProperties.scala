package daffodil.section12.length_properties

import junit.framework.Assert._
import org.scalatest.junit.JUnitSuite
import org.junit.Test
import scala.xml._
import daffodil.xml.XMLUtils
import daffodil.xml.XMLUtils._
import daffodil.compiler.Compiler
import daffodil.util._
import daffodil.tdml.DFDLTestSuite
import java.io.File

class TestLengthProperties extends JUnitSuite {
  val testDir_01 = "/daffodil/ibm-tests/"
  val tdml_01 = testDir_01 + "dpaext1.tdml"
  lazy val runner_01 = new DFDLTestSuite(Misc.getRequiredResource(tdml_01))
  
  @Test def test_length_explicit_12_01() { runner_01.runOneTest("length_explicit_12_01") }
  @Test def test_length_explicit_12_02() { runner_01.runOneTest("length_explicit_12_02") }
  
  val testDir_02 = "/daffodil/section12/length_properties/"
  val tdml_02 = testDir_02 + "LengthProperties.tdml"
  lazy val runner_02 = new DFDLTestSuite(Misc.getRequiredResource(tdml_02))
  
  @Test def test_LengthProp_01() { runner_02.runOneTest("LengthProp_01") }
  @Test def test_LengthProp_02() { runner_02.runOneTest("LengthProp_02") }
  @Test def test_LengthProp_03() { runner_02.runOneTest("LengthProp_03") }
  @Test def test_LengthProp_04() { runner_02.runOneTest("LengthProp_04") }
  @Test def test_LengthProp_05() { runner_02.runOneTest("LengthProp_05") }
  }
