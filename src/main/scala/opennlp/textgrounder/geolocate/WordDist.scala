///////////////////////////////////////////////////////////////////////////////
//  Copyright (C) 2011 Ben Wing, The University of Texas at Austin
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
///////////////////////////////////////////////////////////////////////////////

package opennlp.textgrounder.geolocate

import tgutil._
import Debug._
import WordDist.memoizer._
import WordDist.SmoothedWordDist

import math._
import collection.mutable
import com.codahale.trove.{mutable => trovescala}

// val use_sorted_list = false

//////////////////////////////////////////////////////////////////////////////
//                             Word distributions                           //
//////////////////////////////////////////////////////////////////////////////

object IntStringMemoizer {
  type Word = Int
  val invalid_word: Word = 0

  protected var next_word_count: Word = 1

  // For replacing strings with ints.  This should save space on 64-bit
  // machines (string pointers are 8 bytes, ints are 4 bytes) and might
  // also speed lookup.
  protected val word_id_map = mutable.Map[String,Word]()

  // Map in the opposite direction.
  protected val id_word_map = mutable.Map[Word,String]()

  def memoize_word(word: String) = {
    val index = word_id_map.getOrElse(word, 0)
    if (index != 0) index
    else {
      val newind = next_word_count
      next_word_count += 1
      word_id_map(word) = newind
      id_word_map(index) = word
      newind
    }
  }

  def unmemoize_word(word: Word) = id_word_map(word)

  def create_word_int_map() = trovescala.IntIntMap()
  type WordIntMap = trovescala.IntIntMap
  def create_word_double_map() = trovescala.IntDoubleMap()
  type WordDoubleMap = trovescala.IntDoubleMap
}

object IdentityMemoizer {
  type Word = String
  val invalid_word: Word = null
  def memoize_word(word: String): Word = word
  def unmemoize_word(word: Word): String = word

  def create_word_int_map() = intmap[Word]()
  def create_word_double_map() = doublemap[Word]()
}

object TrivialIntMemoizer {
  type Word = Int
  val invalid_word: Word = 0
  def memoize_word(word: String): Word = 1
  def unmemoize_word(word: Word): String = "foo"

  def create_word_int_map() = IntStringMemoizer.create_word_int_map()
  def create_word_double_map() = IntStringMemoizer.create_word_double_map()
}

object WordDist {
  val memoizer = IntStringMemoizer
  type SmoothedWordDist = PseudoGoodTuringSmoothedWordDist
  val SmoothedWordDist = PseudoGoodTuringSmoothedWordDist

  // Total number of word types seen (size of vocabulary)
  var num_word_types = 0

  // Total number of word tokens seen
  var num_word_tokens = 0

  def apply(keys: Array[Word], values: Array[Int], num_words: Int,
            note_globally: Boolean) =
    new SmoothedWordDist(keys, values, num_words, note_globally)

  def apply():SmoothedWordDist =
    apply(Array[Word](), Array[Int](), 0, note_globally=false)
}

abstract class WordDist {
  /** Total number of word tokens seen */
  var total_tokens: Int

  def num_word_types: Int
  
  /** Whether we have finished computing the distribution in 'counts'. */
  var finished = false

  /**
   * Incorporate a document into the distribution.
   */
  def add_document(words: Traversable[String], ignore_case: Boolean=true,
      stopwords: Set[String]=Set[String]())

  /**
   * Incorporate the given distribution into our distribution.
   */
  def add_word_distribution(worddist: WordDist)

  /**
   * Finish computation of distribution.  Called when no more words or
   * distributions will be added to this one.
   * @seealso #finish_after_global()
   * 
   * @param minimum_word_count If greater than zero, eliminate words seen
   * less than this number of times.
   */
  def finish_before_global(minimum_word_count: Int = 0)

  /**
   * Completely finish computation of the word distribution.  This is called
   * after finish_global_distribution() on the factory method, and can be
   * used to compute values that depend on global values computed from all
   * word distributions.
   * , because of the computation below of
   * overall_unseen_mass, which depends on the global overall_word_probs.
   */
  def finish_after_global()

  def finish(minimum_word_count: Int = 0) {
    finish_before_global(minimum_word_count)
    finish_after_global()
  }

  /**
   * Check fast and slow versions against each other.
   */
  def test_kl_divergence(other: WordDist, partial: Boolean=false) = {
    assert(finished)
    assert(other.finished)
    val fast_kldiv = fast_kl_divergence(other, partial)
    val slow_kldiv = slow_kl_divergence(other, partial)
    if (abs(fast_kldiv - slow_kldiv) > 1e-8) {
      errprint("Fast KL-div=%s but slow KL-div=%s", fast_kldiv, slow_kldiv)
      assert(fast_kldiv == slow_kldiv)
    }
    fast_kldiv
  }

  def slow_kl_divergence_debug(other: WordDist, partial: Boolean=false,
      return_contributing_words: Boolean=false):
    (Double, collection.Map[Word, Double])

  def slow_kl_divergence(other: WordDist, partial: Boolean=false) = {
    val (kldiv, contribs) = slow_kl_divergence_debug(other, partial, false)
    kldiv
  }

  def fast_kl_divergence(other: WordDist, partial: Boolean=false): Double

  def fast_cosine_similarity(other: WordDist, partial: Boolean=false): Double

  def fast_smoothed_cosine_similarity(other: WordDist, partial: Boolean=false): Double

  def symmetric_kldiv(other: WordDist, partial: Boolean=false) = {
    0.5*this.fast_kl_divergence(other, partial) +
    0.5*this.fast_kl_divergence(other, partial)
  }

  /**
   * For a document described by its distribution 'worddist', return the
   * log probability log p(worddist|cell) using a Naive Bayes algorithm.
   *
   * @param worddist Distribution of document.
   */
  def get_nbayes_logprob(worddist: WordDist): Double

  def lookup_word(word: Word): Double
  
  /**
   * Look for the most common word matching a given predicate.
   * @param pred Predicate, passed the raw (unmemoized) form of a word.
   *   Should return true if a word matches.
   * @returns Most common word matching the predicate (wrapped with
   *   Some()), or None if no match.
   */
  def find_most_common_word(pred: String => Boolean): Option[Word] 
}

/**
 * Unigram word distribution with a table listing counts for each word,
 * initialized from the given key/value pairs.
 *
 * @param key Array holding keys, possibly over-sized, so that the internal
 *   arrays from DynamicArray objects can be used
 * @param values Array holding values corresponding to each key, possibly
 *   oversize
 * @param num_words Number of actual key/value pairs to be stored 
 *   statistics.
 */

abstract class UnigramWordDist(
  keys: Array[Word],
  values: Array[Int],
  num_words: Int
) extends WordDist {
  /** A map (or possibly a "sorted list" of tuples, to save memory?) of
      (word, count) items, specifying the counts of all words seen
      at least once.
   */
  val counts = create_word_int_map()
  for (i <- 0 until num_words)
    counts(keys(i)) = values(i)
  var total_tokens = counts.values.sum
  
  def num_word_types = counts.size

  def innerToString: String

  override def toString = {
    val finished_str =
      if (!finished) ", unfinished" else ""
    val num_words_to_print = 15
    val need_dots = counts.size > num_words_to_print
    val items =
      for ((word, count) <- counts.view(0, num_words_to_print))
      yield "%s=%s" format (unmemoize_word(word), count) 
    val words = (items mkString " ") + (if (need_dots) " ..." else "")
    "WordDist(%d tokens%s%s, %s)" format (
        total_tokens, innerToString, finished_str, words)
  }

  def add_document(words: Traversable[String], ignore_case: Boolean=true,
      stopwords: Set[String]=Set[String]()) {
    assert(!finished)
    for {word <- words
         val wlower = if (ignore_case) word.toLowerCase() else word
         if !stopwords(wlower) } {
      counts(memoize_word(wlower)) += 1
      total_tokens += 1
    }
  }

  def add_word_distribution(xworddist: WordDist) {
    assert(!finished)
    val worddist = xworddist.asInstanceOf[UnigramWordDist]
    for ((word, count) <- worddist.counts)
      counts(word) += count
    total_tokens += worddist.total_tokens
  }

  def finish_before_global(minimum_word_count: Int = 0) {
    // make sure counts not null (eg article in coords file but not counts file)
    if (counts == null || finished) return

    // If 'minimum_word_count' was given, then eliminate words whose count
    // is too small.
    if (minimum_word_count > 1) {
      for ((word, count) <- counts if count < minimum_word_count) {
        total_tokens -= count
        counts -= word
      }
    }
  }
   
  def get_nbayes_logprob(xworddist: WordDist) = {
    val worddist = xworddist.asInstanceOf[UnigramWordDist]
    var logprob = 0.0
    for ((word, count) <- worddist.counts) {
      val value = lookup_word(word)
      if (value <= 0) {
        // FIXME: Need to figure out why this happens (perhaps the word was
        // never seen anywhere in the training data? But I thought we have
        // a case to handle that) and what to do instead.
        errprint("Warning! For word %s, prob %s out of range", word, value)
      } else
        logprob += log(value)
    }
    // FIXME: Also use baseline (prior probability)
    logprob
  }

  def find_most_common_word(pred: String => Boolean) = {
    val filtered =
      (for ((word, count) <- counts if pred(unmemoize_word(word)))
        yield (word, count)).toSeq
    if (filtered.length == 0) None
    else {
      val (maxword, maxcount) = filtered maxBy (_._2)
      Some(maxword)
    }
  }
}  

