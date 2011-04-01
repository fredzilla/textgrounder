package opennlp.textgrounder.app;

import opennlp.textgrounder.text.*;
import opennlp.textgrounder.text.io.*;
import opennlp.textgrounder.text.prep.*;
import opennlp.textgrounder.topo.*;
import opennlp.textgrounder.topo.gaz.*;
import opennlp.textgrounder.util.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class LabelPropPreproc extends BaseApp {

    private static final double DPC = 1.0; // degrees per cell

    // the public constants are used by LabelPropComplexResolver //
    private static final String CELL_ = "cell_";
    public static final String CELL_LABEL_ = "cell_label_";
    private static final String LOC_ = "loc_";
    private static final String TPNM_TYPE_ = "tpnm_type_";
    public static final String DOC_ = "doc_";
    private static final String TYPE_ = "type_";
    public static final String TOK_ = "tok_";

    public static void main(String[] args) throws Exception {
        initializeOptionsFromCommandLine(args);
        StoredCorpus corpus = loadCorpus(getInputPath(), getSerializedGazetteerPath(), getSerializedCorpusPath());

        Map<Integer, Set<Integer> > locationCellEdges = new HashMap<Integer, Set<Integer> >();
        Set<Toponym> uniqueToponyms = new HashSet<Toponym>();
        Map<String, Set<Toponym> > docToponyms = new HashMap<String, Set<Toponym> >();
        Map<String, String> docTokenToDocTypeEdges = new HashMap<String, String>();
        Map<String, String> toponymTokenToDocEdges = new HashMap<String, String>();
        Map<String, String> linearTopTokToTopTokEdges = new HashMap<String, String>();

        for(Document<StoredToken> doc : corpus) {
            docToponyms.put(doc.getId(), new HashSet<Toponym>());
            int tokenIndex = 0;
            String prevTokenString = null;
            for(Sentence<StoredToken> sent : doc) {
                for(Toponym toponym : sent.getToponyms()) {
                    if(toponym.getAmbiguity() > 0) {
                        uniqueToponyms.add(toponym);
                        docToponyms.get(doc.getId()).add(toponym);
                        String curTokenString = DOC_ + doc.getId() + "_" + TOK_ + tokenIndex;
                        docTokenToDocTypeEdges.put(curTokenString,
                                DOC_ + doc.getId() + "_" + TYPE_ + toponym.getForm());
                        toponymTokenToDocEdges.put(curTokenString, DOC_ + doc.getId());
                        if(prevTokenString != null)
                            linearTopTokToTopTokEdges.put(prevTokenString, curTokenString);
                        prevTokenString = curTokenString;
                        for(Location location : toponym.getCandidates()) {
                            int locationID = location.getId();
                            Set<Integer> curLocationCellEdges = locationCellEdges.get(locationID);
                            if(curLocationCellEdges != null)
                                continue; // already processed this location
                            curLocationCellEdges = new HashSet<Integer>();
                            for(int cellNumber : TopoUtil.getCellNumbers(location, DPC)) {
                                curLocationCellEdges.add(cellNumber);
                            }
                            locationCellEdges.put(locationID, curLocationCellEdges);
                        }
                    }
                }
                tokenIndex++;
            }
        }

        writeCellSeeds(locationCellEdges, getSeedOutputPath());

        writeCellCellEdges(getGraphOutputPath());
        writeLocationCellEdges(locationCellEdges, getGraphOutputPath());
        writeToponymTypeLocationEdges(uniqueToponyms, getGraphOutputPath());
        writeDocTypeToponymTypeEdges(docToponyms, getGraphOutputPath());
        writeStringStringEdges(docTokenToDocTypeEdges, getGraphOutputPath());
        writeStringStringEdges(toponymTokenToDocEdges, getGraphOutputPath());
        writeStringStringEdges(linearTopTokToTopTokEdges, getGraphOutputPath());
    }

    private static void writeCellSeeds(Map<Integer, Set<Integer> > locationCellEdges, String seedOutputPath) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(seedOutputPath));

        Set<Integer> uniqueCellNumbers = new HashSet<Integer>();

        for(int locationID : locationCellEdges.keySet()) {
            Set<Integer> curLocationCellEdges = locationCellEdges.get(locationID);
            uniqueCellNumbers.addAll(curLocationCellEdges);
        }

        for(int cellNumber : uniqueCellNumbers) {
            writeEdge(out, CELL_ + cellNumber, CELL_LABEL_ + cellNumber, 1.0);
        }

        out.close();
    }

    private static void writeCellCellEdges(String graphOutputPath) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(graphOutputPath));

        for(int lon = 0; lon < 360 / DPC; lon += DPC) {
            for(int lat = 0; lat < 180 / DPC; lat += DPC) {
                int curCellNumber = TopoUtil.getCellNumber(lat, lon, DPC);
                int leftCellNumber = TopoUtil.getCellNumber(lat, lon - DPC, DPC);
                int rightCellNumber = TopoUtil.getCellNumber(lat, lon + DPC, DPC);
                int topCellNumber = TopoUtil.getCellNumber(lat + DPC, lon, DPC);
                int bottomCellNumber = TopoUtil.getCellNumber(lat - DPC, lon, DPC);

                writeEdge(out, CELL_ + curCellNumber, CELL_ + leftCellNumber, 1.0);
                writeEdge(out, CELL_ + curCellNumber, CELL_ + rightCellNumber, 1.0);
                if(topCellNumber >= 0)
                    writeEdge(out, CELL_ + curCellNumber, CELL_ + topCellNumber, 1.0);
                if(bottomCellNumber >= 0)
                    writeEdge(out, CELL_ + curCellNumber, CELL_ + bottomCellNumber, 1.0);
            }
        }

        out.close();
    }

    private static void writeLocationCellEdges(Map<Integer, Set<Integer> > locationCellEdges, String graphOutputPath) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(graphOutputPath, true));

        for(int locationID : locationCellEdges.keySet()) {
            Set<Integer> curLocationCellEdges = locationCellEdges.get(locationID);
            for(int cellNumber : curLocationCellEdges) {
                writeEdge(out, LOC_ + locationID, CELL_ + cellNumber, 1.0);
            }
            //if(curLocationCellEdges.size() > 1)
            //    System.out.println("Wrote " + curLocationCellEdges.size() + " edges for location " + locationID);
        }

        out.close();
    }

    private static void writeToponymTypeLocationEdges(Set<Toponym> uniqueToponyms, String graphOutputPath) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(graphOutputPath, true));

        Set<String> toponymNamesAlreadyWritten = new HashSet<String>();

        for(Toponym toponym : uniqueToponyms) {
            if(!toponymNamesAlreadyWritten.contains(toponym.getForm())) {
                for(Location location : toponym.getCandidates()) {
                    writeEdge(out, TPNM_TYPE_ + toponym.getForm(), LOC_ + location.getId(), 1.0);
                }
                toponymNamesAlreadyWritten.add(toponym.getForm());
            }
        }

        out.close();
    }

    private static void writeDocTypeToponymTypeEdges(Map<String, Set<Toponym> > docToponyms, String graphOutputPath) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(graphOutputPath, true));

        Set<String> docTypesAlreadyWritten = new HashSet<String>();

        for(String docId : docToponyms.keySet()) {
            for(Toponym toponym : docToponyms.get(docId)) {
                String docType = DOC_ + docId + "_" + TYPE_ + toponym.getForm();
                if(!docTypesAlreadyWritten.contains(docType)) {
                    writeEdge(out, docType, TPNM_TYPE_ + toponym.getForm(), 1.0);
                    docTypesAlreadyWritten.add(docType);
                }
            }
        }

        out.close();
    }

    private static void writeStringStringEdges(Map<String, String> edgeMap, String graphOutputPath) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(graphOutputPath, true));

        for(String key : edgeMap.keySet()) {
            writeEdge(out, key, edgeMap.get(key), 1.0);
        }

        out.close();
    }

    private static void writeEdge(BufferedWriter out, String node1, String node2, double weight) throws Exception {
        out.write(node1 + "\t" + node2 + "\t" + weight + "\n");
    }

    private static StoredCorpus loadCorpus(String corpusInputPath, String serGazPath, String serCorpusPath) throws Exception {

        StoredCorpus corpus;
        if(serCorpusPath != null) {
            System.out.print("Reading serialized corpus from " + serCorpusPath + " ...");
            ObjectInputStream ois = null;
            if(serCorpusPath.toLowerCase().endsWith(".gz")) {
                GZIPInputStream gis = new GZIPInputStream(new FileInputStream(serCorpusPath));
                ois = new ObjectInputStream(gis);
            }
            else {
                FileInputStream fis = new FileInputStream(serCorpusPath);
                ois = new ObjectInputStream(fis);
            }
            corpus = (StoredCorpus) ois.readObject();
            System.out.println("done.");
        }
        else {
            corpus = ImportCorpus.doImport(corpusInputPath, serGazPath, isReadAsTR());
        }

        return corpus;
    }
}