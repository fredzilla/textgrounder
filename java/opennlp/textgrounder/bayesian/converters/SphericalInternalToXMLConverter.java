///////////////////////////////////////////////////////////////////////////////
//  Copyright 2010 Taesun Moon <tsunmoon@gmail.com>.
// 
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
// 
//       http://www.apache.org/licenses/LICENSE-2.0
// 
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//  under the License.
///////////////////////////////////////////////////////////////////////////////
package opennlp.textgrounder.bayesian.converters;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.textgrounder.bayesian.apps.ConverterExperimentParameters;
import opennlp.textgrounder.bayesian.mathutils.TGMath;
import opennlp.textgrounder.bayesian.textstructs.*;
import opennlp.textgrounder.bayesian.topostructs.*;
import opennlp.textgrounder.bayesian.spherical.io.*;
import opennlp.textgrounder.bayesian.structs.AveragedSphericalCountWrapper;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 *
 * @author Taesun Moon <tsunmoon@gmail.com>
 */
public class SphericalInternalToXMLConverter {

    /**
     * 
     */
    double[][][] toponymCoordinateLexicon;
    /**
     * 
     */
    double[][] regionMeans;
    /**
     *
     */
    protected SphericalInputReader inputReader;
    /**
     *
     */
    protected String pathToInput;
    /**
     *
     */
    protected String pathToOutput;
    /**
     * 
     */
    protected Lexicon lexicon;
    /**
     *
     */
    protected ConverterExperimentParameters converterExperimentParameters;

    /**
     *
     * @param _converterExperimentParameters
     */
    public SphericalInternalToXMLConverter(
          ConverterExperimentParameters _converterExperimentParameters) {
        converterExperimentParameters = _converterExperimentParameters;

        pathToInput = converterExperimentParameters.getInputPath();
        pathToOutput = converterExperimentParameters.getOutputPath();

        opennlp.textgrounder.bayesian.wrapper.io.InputReader lReader = new opennlp.textgrounder.bayesian.wrapper.io.BinaryInputReader(_converterExperimentParameters);
        lexicon = lReader.readLexicon();

        inputReader = new SphericalBinaryInputReader(_converterExperimentParameters);
    }

    /**
     * 
     */
    public void convert() {
        readCoordinateList();
        convert(pathToInput);
    }

    public void convert(String TRXMLPath) {

        /**
         * Read in processed tokens
         */
        ArrayList<Integer> wordArray = new ArrayList<Integer>(),
              docArray = new ArrayList<Integer>(),
              toponymArray = new ArrayList<Integer>(),
              stopwordArray = new ArrayList<Integer>(),
              regionArray = new ArrayList<Integer>(),
              coordArray = new ArrayList<Integer>();

        try {
            while (true) {
                int[] record = inputReader.nextTokenArrayRecord();
                if (record != null) {
                    int wordid = record[0];
                    wordArray.add(wordid);
                    int docid = record[1];
                    docArray.add(docid);
                    int topstatus = record[2];
                    toponymArray.add(topstatus);
                    int stopstatus = record[3];
                    stopwordArray.add(stopstatus);
                    int regid = record[4];
                    regionArray.add(regid);
                    int coordid = record[5];
                    coordArray.add(coordid);
                }
            }
        } catch (EOFException ex) {
        } catch (IOException ex) {
            Logger.getLogger(SphericalInternalToXMLConverter.class.getName()).log(Level.SEVERE, null, ex);
        }

        /**
         * read in xml
         */
        File TRXMLPathFile = new File(TRXMLPath);

        SAXBuilder builder = new SAXBuilder();
        Document indoc = null;
        Document outdoc = new Document();
        try {
            indoc = builder.build(TRXMLPathFile);
        } catch (JDOMException ex) {
            Logger.getLogger(SphericalInternalToXMLConverter.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        } catch (IOException ex) {
            Logger.getLogger(SphericalInternalToXMLConverter.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }

        Element inroot = indoc.getRootElement();
        Element outroot = new Element(inroot.getName());
        outdoc.addContent(outroot);

        int counter = 0;

        ArrayList<Element> documents = new ArrayList<Element>(inroot.getChildren());
        for (Element document : documents) {
            Element outdocument = new Element(document.getName());
            copyAttributes(document, outdocument);
            outroot.addContent(outdocument);

            ArrayList<Element> sentences = new ArrayList<Element>(document.getChildren());
            for (Element sentence : sentences) {

                Element outsentence = new Element(sentence.getName());
                copyAttributes(sentence, outsentence);
                outdocument.addContent(outsentence);

                ArrayList<Element> tokens = new ArrayList<Element>(sentence.getChildren());
                for (Element token : tokens) {

                    Element outtoken = new Element(token.getName());
                    copyAttributes(token, outtoken);
                    outsentence.addContent(outtoken);

                    int isstopword = stopwordArray.get(counter);
                    int regid = regionArray.get(counter);
                    int wordid = wordArray.get(counter);
                    int coordid = coordArray.get(counter);
                    String word = "";
                    if (token.getName().equals("w")) {
                        word = token.getAttributeValue("tok").toLowerCase();
                        if (isstopword == 0) {
                            Coordinate coord = new Coordinate(TGMath.cartesianToSpherical(TGMath.normalizeVector(regionMeans[regid])));
                            outtoken.setAttribute("long", String.format("%.2f", coord.longitude));
                            outtoken.setAttribute("lat", String.format("%.2f", coord.latitude));
                        }
                        counter += 1;
                    } else if (token.getName().equals("toponym")) {
                        word = token.getAttributeValue("term").toLowerCase();
                        ArrayList<Element> candidates = new ArrayList<Element>(token.getChild("candidates").getChildren());
                        if (!candidates.isEmpty()) {
                            Coordinate coord = new Coordinate(TGMath.cartesianToSpherical(toponymCoordinateLexicon[wordid][coordid]));
                            outtoken.setAttribute("long", String.format("%.2f", coord.longitude));
                            outtoken.setAttribute("lat", String.format("%.2f", coord.latitude));
                        } else {
                            Coordinate coord = new Coordinate(TGMath.cartesianToSpherical(TGMath.normalizeVector(regionMeans[regid])));
                            outtoken.setAttribute("long", String.format("%.2f", coord.longitude));
                            outtoken.setAttribute("lat", String.format("%.2f", coord.latitude));
                        }
                        counter += 1;
                    } else {
                        continue;
                    }

                    String outword = lexicon.getWordForInt(wordid);
                    if (!word.equals(outword)) {
                        String did = document.getAttributeValue("id");
                        String sid = sentence.getAttributeValue("id");
                        int outdocid = docArray.get(counter);
                        System.err.println(String.format("Mismatch between "
                              + "tokens. Occurred at source document %s, "
                              + "sentence %s, token %s and target document %d, "
                              + "offset %d, token %s, token id %d",
                              did, sid, word, outdocid, counter, outword, wordid));
                        System.exit(1);
                    }
                }
            }
        }

        try {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            xout.output(outdoc, new FileOutputStream(new File(pathToOutput)));
        } catch (IOException ex) {
            Logger.getLogger(SphericalInternalToXMLConverter.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }

    /**
     *
     */
    public void readCoordinateList() {
//        HashMap<Integer, double[]> toprecords = new HashMap<Integer, double[]>();
//        int maxtopid = 0;
//
//        try {
//            while (true) {
//                ArrayList<Object> toprecord = inputReader.nextToponymCoordinateRecord();
//
//                int topid = (Integer) toprecord.get(0);
//                double[] record = (double[]) toprecord.get(1);
//
//                toprecords.put(topid, record);
//                if (topid > maxtopid) {
//                    maxtopid = topid;
//                }
//            }
//        } catch (EOFException e) {
//        } catch (IOException ex) {
//            Logger.getLogger(SphericalInternalToXMLConverter.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        int T = maxtopid + 1;
//        toponymCoordinateLexicon = new double[T][][];
//
//        for (Entry<Integer, double[]> entry : toprecords.entrySet()) {
//            int topid = entry.getKey();
//            double[] sphericalrecord = entry.getValue();
//            double[][] geo = new double[sphericalrecord.length / 2][];
//            for (int i = 0; i < sphericalrecord.length / 2; i++) {
//                double[] crec = {sphericalrecord[2 * i], sphericalrecord[2 * i + 1]};
//                geo[i] = crec;
//            }
//            toponymCoordinateLexicon[topid] = geo;
//        }

        AveragedSphericalCountWrapper ascw = inputReader.readProbabilities();

        regionMeans = ascw.getAveragedRegionMeans();
        toponymCoordinateLexicon = ascw.getToponymCoordinateLexicon();
    }

    protected void copyAttributes(Element src, Element trg) {
        for (Attribute attr : new ArrayList<Attribute>(src.getAttributes())) {
            trg.setAttribute(attr.getName(), attr.getValue());
        }
    }
}