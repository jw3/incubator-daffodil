/* Copyright (c) 2012-2015 Tresys Technology, LLC. All rights reserved.
 *
 * Developed by: Tresys Technology, LLC
 *               http://www.tresys.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal with
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimers.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimers in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the names of Tresys Technology, nor the names of its contributors
 *     may be used to endorse or promote products derived from this Software
 *     without specific prior written permission.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH THE
 * SOFTWARE.
 */

package edu.illinois.ncsa.daffodil.processors.charset

import edu.illinois.ncsa.daffodil.exceptions.Assert
import edu.illinois.ncsa.daffodil.util.Maybe
import edu.illinois.ncsa.daffodil.util.Maybe._
import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

/*
 * These are needed because the ordinary java/scala Charset, Encoder, and Decoder objects
 * are not serializable. So we must go back to the string from which they can be created
 * in order to serialize these.
 */

/**
 * Serializable Charset
 */
class DFDLCharset(val charsetName: String) extends Serializable {
  charset // Force charset to be evaluted to ensure it's valid at compile time.  It's a lazy val so it will be evaluated when de-serialized
  @transient lazy val charset = CharsetUtils.getCharset(charsetName)
}

/**
 * Serializable Encoder
 */
class DFDLEncoder(private val dfdlCharset: DFDLCharset) extends Serializable {
  @transient private lazy val encoder_ = dfdlCharset.charset.newEncoder()

  def encoder = {
    encoder_.reset() // each time this is asked for it gets reset.
    encoder_
  }
}

/**
 * Serializable Decoder
 */
class DFDLDecoder(private val dfdlCharset: DFDLCharset) extends Serializable {
  @transient private lazy val decoder_ = dfdlCharset.charset.newDecoder()

  def decoder = {
    decoder_.reset() // each time this is asked for it gets reset.
    decoder_
  }
}

object CharsetUtils {

  // keep in case we need to put this check back in temporarily
  // private val noWSNoLowerCasePatern = """[^\sa-z]+""".r.pattern // one or more characters, not whitespace, not lower case.

  def getCharset(charsetName: String): Charset = {
    Assert.usage(charsetName != null);
    //    {
    //      // TODO: expensive check, remove if unnecessary
    //      //
    //      // However, encodingEv should guarantee that we already get an upper case token.
    //      // So we shouldn't need to test this.
    //      val m = noWSNoLowerCasePatern.matcher(charsetName)
    //      Assert.usage(m.matches)
    //    }
    // There is no notion of a default charset in DFDL.
    // So this can be val.
    val csn: String = charsetName

    val cs = try {
      val cs =
        if (csn == "US-ASCII-7-BIT-PACKED" || // deprecated name
          csn == "X-DFDL-US-ASCII-7-BIT-PACKED") // new official name
          USASCII7BitPackedCharset
        else Charset.forName(csn)
      One(cs)
    } catch {
      case e: IllegalCharsetNameException => Nope
    }
    if (cs.isEmpty) throw new UnsupportedEncodingException(csn)
    cs.value
  }

  /**
   * Subtle bug in decoders in Java 7 when there is room for only 1
   * character in the CharBuffer.
   *
   * While we could just test for Java 8, which doesn't have this bug,
   * it is worthwhile to keep this in case we end up trying to support
   * Java 7 at some point in the future.
   */
  lazy val hasJava7DecoderBug = {
    val decoder = Charset.forName("utf-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.REPORT)
    decoder.onUnmappableCharacter(CodingErrorAction.REPORT)
    val bb = ByteBuffer.allocate(6)
    bb.put(-16.toByte) // invalid first utf-8 byte
    bb.limit(6).position(0)
    val cb = CharBuffer.allocate(1)
    val cr = decoder.decode(bb, cb, true)
    if (cr.isOverflow && // This is the bug!
      cb.position == 0 &&
      bb.position == 0) true
    else if (cr.isError) false // no bug
    //    else if (cr.isOverflow && // This is what *should* happen if CodingErrorAction.REPLACE is used.
    //      cb.position == 1 &&
    //      bb.position == 1 &&
    //      cb.get(0) == this.unicodeReplacementChar) false
    else
      Assert.invariantFailed("Unexpected decoder behavior. " + cr)
  }

  val unicodeReplacementChar = '\uFFFD'
}

class CharacterSetAlignmentError(csName: String, requiredAlignmentInBits: Int, alignmentInBitsWas: Int)
  extends Exception("Character set %s requires %s alignment (bits), but alignment was %s (bits)".
    format(csName, requiredAlignmentInBits, alignmentInBitsWas))
