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
package opennlp.textgrounder.bayesian.rlda.io;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import opennlp.textgrounder.bayesian.apps.ExperimentParameters;

/**
 *
 * @author Taesun Moon <tsunmoon@gmail.com>
 */
public class TextInputReader extends InputReader {

    protected BufferedReader tokenArrayReader;
    protected BufferedReader toponymRegionReader;

    public TextInputReader(ExperimentParameters _experimentParameters) {
        super(_experimentParameters);

        openTokenArrayReader();
        openToponymRegionReader();
    }

    /**
     * 
     * @return
     * @throws EOFException
     */
    @Override
    public int[] nextTokenArrayRecord() throws EOFException {
        String line = null;
        try {
            line = tokenArrayReader.readLine();
            if (line == null) {
                throw new EOFException();
            }
        } catch (IOException ex) {
            Logger.getLogger(TextInputReader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        String[] fields = line.split("\\w+");
        int[] record = new int[4];
        if (fields.length == 4) {
            int wordid = Integer.parseInt(fields[0]);
            record[0] = wordid;
            int docid = Integer.parseInt(fields[1]);
            record[1] = docid;
            int topstatus = Integer.parseInt(fields[2]);
            record[2] = topstatus;
            int stopstatus = Integer.parseInt(fields[3]);
            record[3] = stopstatus;

            return record;
        }
        return null;
    }

    /**
     * 
     * @return
     * @throws EOFException
     */
    @Override
    public int[] nextToponymRegionFilter() throws EOFException {

        String line = null;
        try {
            line = toponymRegionReader.readLine();
            if (line == null) {
                throw new EOFException();
            }
        } catch (IOException ex) {
            Logger.getLogger(TextInputReader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }

        String[] fields = line.split("\\w+");
        int[] record = new int[fields.length];
        for (int i = 0; i < fields.length; ++i) {
            int token = Integer.parseInt(fields[i]);
            record[i] = token;
        }

        if (fields.length == 0) {
            return null;
        } else {
            return record;
        }
    }

    @Override
    public int getMaxRegionID() {
        String line = null;
        int maxreg = 0;
        try {
            while ((line = toponymRegionReader.readLine()) != null) {
                String[] fields = line.split("\\w+");
                int[] record = new int[fields.length];
                for (int i = 0; i < fields.length; ++i) {
                    int regid = Integer.parseInt(fields[i]);
                    if (regid > maxreg) {
                        maxreg = regid;
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(TextInputReader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }

        assert maxreg != 0;

        return maxreg;
    }

    /**
     * 
     */
    @Override
    public final void openTokenArrayReader() {
        try {
            if (tokenArrayFile.getName().endsWith(".gz")) {
                tokenArrayReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(tokenArrayFile))));
            } else {
                tokenArrayReader = new BufferedReader(new FileReader(tokenArrayFile));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TextInputReader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        } catch (IOException ex) {
            Logger.getLogger(TextInputReader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }

    /**
     *
     */
    @Override
    public final void openToponymRegionReader() {
        try {
            if (toponymRegionFile.getName().endsWith(".gz")) {
                toponymRegionReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(toponymRegionFile))));
            } else {
                toponymRegionReader = new BufferedReader(new FileReader(toponymRegionFile));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TextInputReader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        } catch (IOException ex) {
            Logger.getLogger(TextInputReader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }

    /**
     * 
     */
    @Override
    public void closeTokenArrayReader() {
        try {
            tokenArrayReader.close();
        } catch (IOException ex) {
        }
    }

    /**
     *
     */
    @Override
    public void closeToponymRegionReader() {
        try {
            toponymRegionReader.close();
        } catch (IOException ex) {
        }
    }

    @Override
    public void resetTokenArrayReader() {
        closeTokenArrayReader();
        openTokenArrayReader();
    }

    @Override
    public void resetToponymRegionReader() {
        closeToponymRegionReader();
        openToponymRegionReader();
    }
}