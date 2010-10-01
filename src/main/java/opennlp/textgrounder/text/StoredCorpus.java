///////////////////////////////////////////////////////////////////////////////
//  Copyright (C) 2010 Travis Brown, The University of Texas at Austin
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
package opennlp.textgrounder.text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.textgrounder.topo.Location;
import opennlp.textgrounder.util.CountingLexicon;
import opennlp.textgrounder.util.SimpleCountingLexicon;
import opennlp.textgrounder.util.Span;

public class StoredCorpus extends Corpus<StoredToken> {
  private Corpus<Token> wrapped;
  private final CountingLexicon<String> tokenLexicon;
  private final CountingLexicon<String> toponymLexicon;
  private final ArrayList<Document<StoredToken>> documents;
  private final ArrayList<List<Location>> candidateLists;
  
  StoredCorpus(Corpus<Token> wrapped) {
    this.wrapped = wrapped;
    this.tokenLexicon = new SimpleCountingLexicon<String>();
    this.toponymLexicon = new SimpleCountingLexicon<String>();
    this.documents = new ArrayList<Document<StoredToken>>();
    this.candidateLists = new ArrayList<List<Location>>();
  }

  private void load() throws IOException {
    for (Document<Token> document : wrapped) {
      ArrayList<Sentence<StoredToken>> sentences = new ArrayList<Sentence<StoredToken>>();

      for (Sentence<Token> sentence : document) {
        List<String> tokenStrings = new ArrayList<String>();

        for (Iterator<Token> it = sentence.tokens(); it.hasNext(); ) {
          tokenStrings.add(it.next().getForm());
        }

        int[] tokens = new int[tokenStrings.size()];
        for (int i = 0; i < tokens.length; i++) {
          tokens[i] = this.tokenLexicon.getOrAdd(tokenStrings.get(i));
        }

        StoredSentence stored = new StoredSentence(sentence.getId(), tokens);

        for (Iterator<Span<Token>> it = sentence.toponymSpans(); it.hasNext(); ) {
          Span<Token> span = it.next();
          Toponym toponym = (Toponym) span.getItem();
          int idx = this.toponymLexicon.getOrAdd(toponym.getForm());
          if (toponym.hasGold()) {
            int goldIdx = toponym.getGoldIdx();
            if (toponym.hasSelected()) {
              int selectedIdx = toponym.getSelectedIdx();
              stored.addToponym(span.getStart(), span.getEnd(), idx, goldIdx, selectedIdx);
            } else {
              stored.addToponym(span.getStart(), span.getEnd(), idx, goldIdx);
            }
          } else {
            stored.addToponym(span.getStart(), span.getEnd(), idx);
          }
        }

        stored.compact();
        sentences.add(stored);
      }

      sentences.trimToSize();
      this.documents.add(new StoredDocument(document.getId(), sentences));
    }
  }

  public void addSource(DocumentSource source) {
    if (this.wrapped == null) {
      throw new UnsupportedOperationException("Cannot add a source to a stored corpus after it has been loaded.");
    } else {
      this.wrapped.addSource(source);
    }
  }

  public Iterator<Document<StoredToken>> iterator() {
    if (this.wrapped != null) {
      try {
        this.load();
        this.wrapped.close();
      } catch (IOException e) {
        System.err.println("Error loading the wrapped corpus.");
      }
      this.wrapped = null;
    }

    return this.documents.iterator();
  }

  private class StoredDocument extends Document<StoredToken> {
    private final List<Sentence<StoredToken>> sentences;

    private StoredDocument(String id, List<Sentence<StoredToken>> sentences) {
      super(id);
      this.sentences = sentences;
    }

    public Iterator<Sentence<StoredToken>> iterator() {
      return this.sentences.iterator();
    }
  }

  private class StoredSentence extends Sentence<StoredToken> {
    private final int[] tokens;
    private final ArrayList<Span<StoredToken>> toponymSpans;

    private StoredSentence(String id, int[] tokens) {
      super(id);
      this.tokens = tokens;
      this.toponymSpans = new ArrayList<Span<StoredToken>>();
    }

    private void addToponym(int start, int end, int toponymIdx) {
      this.toponymSpans.add(new Span<StoredToken>(start, end, new CompactToponym(toponymIdx)));
    }

    private void addToponym(int start, int end, int toponymIdx, int goldIdx) {
      this.toponymSpans.add(new Span<StoredToken>(start, end, new CompactToponym(toponymIdx, goldIdx)));
    }

    private void addToponym(int start, int end, int toponymIdx, int goldIdx, int selectedIdx) {
      this.toponymSpans.add(new Span<StoredToken>(start, end, new CompactToponym(toponymIdx, goldIdx, selectedIdx)));
    }

    private void compact() {
      this.toponymSpans.trimToSize();
    }

    private class CompactToponym implements StoredToponym {
      private final int idx;
      private final int goldIdx;
      private int selectedIdx;

      private CompactToponym(int idx) {
        this(idx, -1);
      }

      private CompactToponym(int idx, int goldIdx) {
        this(idx, goldIdx, -1);
      }

      private CompactToponym(int idx, int goldIdx, int selectedIdx) {
        this.idx = idx;
        this.goldIdx = goldIdx;
        this.selectedIdx = selectedIdx;
      }

      public String getForm() {
        return StoredCorpus.this.toponymLexicon.atIndex(this.idx);
      }

      public boolean isToponym() {
        return true;
      }

      public boolean hasGold() { return this.goldIdx > -1; }
      public Location getGold() { return StoredCorpus.this.candidateLists.get(this.idx).get(this.goldIdx); }
      public int getGoldIdx() { return this.goldIdx; }

      public boolean hasSelected() { return this.selectedIdx > -1; }
      public Location getSelected() { return StoredCorpus.this.candidateLists.get(this.idx).get(this.selectedIdx); }
      public int getSelectedIdx() { return this.selectedIdx; }
      public void setSelectedIdx(int idx) { this.selectedIdx = idx; }

      public int getAmbiguity() { return StoredCorpus.this.candidateLists.get(this.idx).size(); }
      public List<Location> getCandidates() { return StoredCorpus.this.candidateLists.get(this.idx); }
      public Iterator<Location> iterator() { return StoredCorpus.this.candidateLists.get(this.idx).iterator(); }

      public List<Token> getTokens() { throw new UnsupportedOperationException(); }

      public int getIdx() {
        return this.idx;
      }

      public int getTypeCount() {
        return StoredCorpus.this.toponymLexicon.countAtIndex(this.idx);
      }
    }

    public Iterator<StoredToken> tokens() {
      return new Iterator<StoredToken>() {
        private int current = 0;

        public boolean hasNext() {
          return this.current < StoredSentence.this.tokens.length;
        }

        public StoredToken next() {
          final int current = this.current++;
          final int idx = StoredSentence.this.tokens[current];
          return new StoredToken() {
            public String getForm() {
              return StoredCorpus.this.tokenLexicon.atIndex(idx);
            }

            public boolean isToponym() {
              return false;
            }

            public int getIdx() {
              return idx;
            }

            public int getTypeCount() {
              return StoredCorpus.this.tokenLexicon.countAtIndex(idx);
            }
          };
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    public Iterator<Span<StoredToken>> toponymSpans() {
      return this.toponymSpans.iterator();
    }
  }

  public void close() throws IOException {
  }
}

