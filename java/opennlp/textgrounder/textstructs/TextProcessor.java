///////////////////////////////////////////////////////////////////////////////
//  Copyright (C) 2010 Taesun Moon, The University of Texas at Austin
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
package opennlp.textgrounder.textstructs;

import java.io.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import opennlp.textgrounder.ners.*;
import opennlp.textgrounder.topostructs.Coordinate;
import opennlp.textgrounder.topostructs.Location;

/**
 * Class that reads from a file and generates tokens, identified as to whether
 * they are locations (toponyms), possibly with additional properties.  Tokens
 * are added to a Document object.
 * 
 * Multi-word location tokens (e.g. New York) are joined by the code below into
 * a single token.
 * 
 * Actual processing of a file is done in the processFile() method.
 * 
 * @author
 */
public abstract class TextProcessor {
    /**
     * Identify toponyms and populate lexicon from input file.
     * 
     * This method only splits any incoming document into smaller subdocuments
     * based on Lexicon.parAsDocSize. The actual work of identifying toponyms,
     * converting tokens to indexes, and populating arrays is handled in
     * processText.
     * 
     * @param locationOfFile
     *            Path to input. Must be a single file.
     * @param stopwordList
     *            table that contains the list of stopwords. if this is an
     *            instance of NullStopwordList, it will return false through
     *            stopwordList.isStopWord for every string token examined (i.e.
     *            the token is not a stopword).
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void processNER(String locationOfFile, Corpus corpus,
            NamedEntityRecognizer ner, int parAsDocSize)
            throws FileNotFoundException, IOException {

        CorpusDocument doc = new CorpusDocument(corpus, locationOfFile);
        BufferedReader textIn = new BufferedReader(new FileReader(
                locationOfFile));
        System.out.println("Extracting toponym indices from " + locationOfFile
                + " ...");

        String curLine = null;
        StringBuffer buf = new StringBuffer();
        int counter = 1;
        int currentDoc = 0;
        System.err.print("Processing document:" + locationOfFile + ",");
        while (true) {
            curLine = textIn.readLine();
            if (curLine == null || curLine.equals("")) {
                break;
            }
            buf.append(curLine.replaceAll("[<>]", "").replaceAll("&", "and"));
            buf.append(" ");

            if (counter < parAsDocSize) {
                counter++;
            } else {
                ner.processText(doc, buf.toString());

                buf = new StringBuffer();
                currentDoc += 1;
                counter = 1;
                System.err.print(currentDoc + ",");
            }
        }

        /**
         * Add last lines if they have not been processed and added
         */
        if (counter > 1) {
            ner.processText(doc, buf.toString());
            System.err.print(currentDoc + ",");
        }
        System.err.println();

        corpus.add(doc);
    }

    /**
     * Function to processing TEI (text encoding initiative) encoded XML files.
     * 
     * Basically, this function reads in TEI-encoded XML files, ignores
     * everything but the actual text, and runs the text through the Stanford
     * NER to get toponym and non-toponym tokens, the same way that the base
     * TextProcessor class does. There's only one method (besides the
     * constructor), processFile(), which processes a specific file as just
     * described.
     * 
     * The PCL travel corpus is encoded in TEI format and this class is to be
     * used with pcl travel. By using the named entity definitions that come
     * with the dtd for pcl travel, all encoding issues are handled within this
     * class. Any non-lower ascii characters are normalized by first normalizing
     * according to unicode standards and then stripping non-lower ascii
     * portions.
     * 
     * @author tsmoon
     */
    public static void processTEIXML(String locationOfFile, Corpus corpus,
            NamedEntityRecognizer ner, int parAsDocSize)
            throws FileNotFoundException, IOException {

        if (!locationOfFile.endsWith(".xml")) {
            return;
        }

        SAXBuilder builder = new SAXBuilder();
        File file = new File(locationOfFile);
        CorpusDocument cdoc = new CorpusDocument(corpus, locationOfFile);
        Document doc = null;
        int currentDoc = 0;
        try {
            doc = builder.build(file);
            Element element = doc.getRootElement();
            Element child = element.getChild("text").getChild("body");
            List<Element> divs = new ArrayList<Element>(
                    child.getChildren("div"));

            for (Element div : divs) {
                StringBuilder buf = new StringBuilder();
                List<Element> pars = new ArrayList<Element>(
                        div.getChildren("p"));
                for (Element par : pars) {
                    for (char c : Normalizer.normalize(par.getTextNormalize(),
                            Normalizer.Form.NFKC).toCharArray()) {
                        if (((int) c) < 0x7F) {
                            buf.append(c);
                        }
                    }
                    buf.append(System.getProperty("line.separator"));
                }
                String text = buf.toString().trim();
                if (!text.isEmpty()) {
                    ner.processText(cdoc, text);
                    currentDoc += 1;
                    System.err.print(currentDoc + ",");
                }
            }

        } catch (JDOMException ex) {
            Logger.getLogger(TextProcessorTEIXML.class.getName()).log(
                    Level.SEVERE, null, ex);
        }
    }

    /**
     * 
     * @param locationOfFile
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void processTR(String locationOfFile, Corpus corpus)
            throws FileNotFoundException, IOException {

        int currentDoc = 0;
        if (locationOfFile.endsWith("d663.tr")) {
            System.err.println(locationOfFile
                    + " has incorrect format; skipping.");
            return;
        }

        BufferedReader textIn = new BufferedReader(new FileReader(
                locationOfFile));
        CorpusDocument doc = new CorpusDocument(corpus, locationOfFile);
        String curLine = null, cur = null;
        while (true) {
            curLine = textIn.readLine();
            if (curLine == null) {
                break;
            }

            if ((curLine.startsWith("\t") && (!curLine.startsWith("\tc") && !curLine
                    .startsWith("\t>")))
                    || (curLine.startsWith("c") && curLine.length() >= 2 && Character
                            .isDigit(curLine.charAt(1)))) {
                System.err.println(locationOfFile
                        + " has incorrect format; skipping.");
                return;
            }

        }
        textIn.close();

        textIn = new BufferedReader(new FileReader(locationOfFile));

        System.out.println("Extracting gold standard toponym indices from "
                + locationOfFile + " ...");

        curLine = null;

        int wordidx = 0;
        boolean lookingForGoldLoc = false;
        System.err.print("Processing document:" + currentDoc + ",");
        while (true) {
            curLine = textIn.readLine();
            if (curLine == null) {
                break;
            }

            if (lookingForGoldLoc && curLine.startsWith("\t>")) {
                Token tok = new Token(doc, wordidx, true);
                tok.goldLocation = parseLocation(cur, curLine, wordidx);
                doc.add(tok);
                lookingForGoldLoc = false;
                continue;
            } else if (curLine.startsWith("\t")) {
                continue;
            } else if (lookingForGoldLoc && !curLine.startsWith("\t")) {
                // there was no correct gold Location for this toponym
                doc.add(new Token(doc, wordidx, true));
                lookingForGoldLoc = false;
                // continue;
            }

            String[] tokens = curLine.split("\\t");

            if (tokens.length < 2) {

                continue;
            }

            cur = tokens[0].toLowerCase();

            wordidx = corpus.lexicon.addWord(cur);
            if (!tokens[1].equals("LOC")) {
                doc.add(new Token(doc, wordidx, false));
            } else {
                lookingForGoldLoc = true;
                // gold standard Location will be added later, when line
                // starting with tab followed by > occurs
            }
        }

        currentDoc += 1;

        System.err.println();
    }

    public static Location parseLocation(String token, String line, int wordidx) {
        String[] tokens = line.split("\\t");

        if (tokens.length < 6) {
            return null;
        }

        double lon = Double.parseDouble(tokens[3]);
        double lat = Double.parseDouble(tokens[4]);

        String placename;
        int gtIndex = tokens[5].indexOf(">");
        if (gtIndex != -1) {
            placename = tokens[5].substring(0, gtIndex).trim().toLowerCase();
        } else {
            placename = tokens[5].trim().toLowerCase();
        }

        return new Location(-1, placename, "", new Coordinate(lon, lat), 0, "", -1);
    }

    /**
     * 
     * @param locationOfFile
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void processXML(String locationOfFile, Corpus corpus)
            throws FileNotFoundException, IOException {
        CorpusDocument doc = new CorpusDocument(corpus, locationOfFile);
        doc.loadFromXML(locationOfFile);
        corpus.add(doc);
    }
}
