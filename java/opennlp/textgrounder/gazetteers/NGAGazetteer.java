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
package opennlp.textgrounder.gazetteers;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

import gnu.trove.*;
import opennlp.textgrounder.geo.CommandLineOptions;

import opennlp.textgrounder.topostructs.*;
import opennlp.textgrounder.util.*;

/**
 * NGA Gazetteer type.  FIXME: Document me!
 * 
 * @author Taesun Moon
 *
 * @param <E>
 */
public class NGAGazetteer extends Gazetteer {

    public NGAGazetteer(String location) throws FileNotFoundException,
          IOException {
        super(location);
        initialize(Constants.TEXTGROUNDER_DATA + "/gazetteer/geonames_dd_dms_date_20091102.txt.gz");
    }

    @Override
    protected void initialize(String location) throws FileNotFoundException,
          IOException {

        //BufferedReader gazIn = new BufferedReader(new FileReader(location));

        BufferedReader gazIn = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(location))));

        System.out.println("Populating NGA gazetteer from " + location + " ...");

        String curLine;
        String[] tokens;

        TObjectIntHashMap<String> populations = new TObjectIntHashMap<String>();
        //TObjectIntHashMap<String> popClasses = new TObjectIntHashMap<String>();

        curLine = gazIn.readLine(); // first line of gazetteer is legend
        int lines = 1;
        while (true) {
            curLine = gazIn.readLine();
            if (curLine == null) {
                break;
            }
            lines++;
            if (lines % 1000000 == 0) {
                System.out.println("  Read " + lines + " lines; gazetteer has " + populations.size() + " entries so far.");
            }
            //System.out.println(curLine);
            tokens = curLine.split("\t");
            /*for(String token : tokens) {
            System.out.println(token);
            }*/
            //System.out.println(tokens.length);
            if (tokens.length < 25) {
                System.out.println("\nNot enough columns found; this file format should have at least " + 25 + " but only " + tokens.length + " were found. Quitting.");
                System.exit(0);
            }
            Coordinate curCoord =
                  new Coordinate(Double.parseDouble(tokens[4]), Double.parseDouble(tokens[3]));

            String placeName = tokens[24].toLowerCase();

            //if(placeName.contains("sacramento"))
            //  System.out.println(placeName);

            int population;
            //if(allDigits.matcher(tokens[15]).matches()) {
            if (!tokens[15].equals("")) {
                population = Integer.parseInt(tokens[15]);
                //if(placeName.contains("sacramento"))
                //   System.out.println(placeName + ": " + population);
            } else {
                continue; // no population was listed, so skip this entry
            }
            /*int popClass;
            if(!tokens[11].equals("")) {
            popClass = Integer.parseInt(tokens[11]);
            if(placeName.contains("Sacramaneto"))
            System.out.println(placeName + ": " + popClass);
            }
            else
            continue; // no population class was listed, so skip this entry*/

            //if(population > 100000)
            //	System.out.println(placeName + ": " + population);

            int storedPop = populations.get(placeName);
            if (storedPop == 0) { // 0 is not-found sentinal for TObjectIntHashMap
                populations.put(placeName, population);
                int topidx = toponymLexicon.addWord(placeName);
                put(topidx, null);
            } else if (population > storedPop) {
                populations.put(placeName, population);
                int topidx = toponymLexicon.addWord(placeName);
                put(topidx, null);
                //System.out.println("Found a bigger " + placeName + " with population " + population + "; was " + storedPop);
            }

            /*int storedPopClass = popClasses.get(placeName);
            if(storedPopClass == 0) { // 0 is not-found sentinal for TObjectIntHashMap
            popClasses.put(placeName, popClass);
            put(placeName, curCoord);
            }
            else if(popClass < storedPopClass) {
            populations.put(placeName, popClass);
            put(placeName, curCoord);
            //System.out.println("Found a bigger " + placeName + " with population " + population + "; was " + storedPop);
            }*/

            //putIfAbsent(tokens[1].toLowerCase(), curCoord);
            //putIfAbsent(tokens[5].toLowerCase(), curCoord);
            //putIfAbsent(tokens[16].toLowerCase(), curCoord);
        }

        gazIn.close();

        System.out.println("Done. Total number of actual place names = " + size());

        /*System.out.println(placenamesToCoords.get("salt springs"));
        System.out.println(placenamesToCoords.get("galveston"));*/

    }

    public TIntHashSet get(String placename) {
        return new TIntHashSet();
    }
}
