package opennlp.textgrounder.resolver;

import opennlp.textgrounder.text.*;
import opennlp.textgrounder.topo.*;
import opennlp.textgrounder.util.*;
import java.io.*;
import java.util.*;

public class LabelPropContextSensitiveResolver extends Resolver {

    public static final int DEGREES_PER_REGION = 1;

    private static final double CUR_TOP_WEIGHT = 1.0;
    private static final double SAME_SENT_WEIGHT = 1.0;
    private static final double OTHER_WEIGHT = 1.0;

    private String pathToGraph;
    private Lexicon<String> lexicon = new SimpleLexicon<String>();
    private HashMap<Integer, String> reverseLexicon = new HashMap<Integer, String>();

    private HashMap<Integer, HashMap<Integer, Double> > regionDistributions = null;

    public LabelPropContextSensitiveResolver(String pathToGraph) {
        this.pathToGraph = pathToGraph;
    }

    @Override
    public void train(StoredCorpus corpus){
        TopoUtil.buildLexicons(corpus, lexicon, reverseLexicon);

        regionDistributions = new HashMap<Integer, HashMap<Integer, Double> >();

        try {
            BufferedReader in = new BufferedReader(new FileReader(pathToGraph));

            String curLine;
            while(true) {
                curLine = in.readLine();
                if(curLine == null)
                    break;

                String[] tokens = curLine.split("\t");

                int idx = Integer.parseInt(tokens[0]);

                if(!reverseLexicon.containsKey(idx))
                    continue;

                HashMap<Integer, Double> curDist = new HashMap<Integer, Double>();
                regionDistributions.put(idx, curDist);

                //int regionNumber = -1;
                for(int i = 1; i < tokens.length; i++) {
                    String curToken = tokens[i];
                    if(curToken.length() == 0)
                        continue;

                    String[] innerTokens = curToken.split(" ");
                    for(int j = 0; j < innerTokens.length; j++) {
                        if(/*!innerTokens[j].startsWith("__DUMMY__") && */innerTokens[j].endsWith("L")) {
                            int regionNumber = Integer.parseInt(innerTokens[j].substring(0, innerTokens[j].length()-1));
                            double labelWeight = Double.parseDouble(innerTokens[j+1]);
                            curDist.put(regionNumber, labelWeight);
                        }
                    }
                }
            }

            in.close();
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public StoredCorpus disambiguate(StoredCorpus corpus) {

        if(regionDistributions == null)
            train(corpus);

        for(Document<StoredToken> doc : corpus) {
            int outerSentIndex = 0;
            for(Sentence<StoredToken> outerSent : doc) {
                for(Toponym outerToponym : outerSent.getToponyms()) {
                    if(outerToponym.getAmbiguity() > 0) {
                        int idx = lexicon.get(outerToponym.getForm());

                        HashMap<Integer, Double> toponymWeights = new HashMap<Integer, Double>();
                        toponymWeights.put(idx, CUR_TOP_WEIGHT);

                        for(Toponym otherToponym : outerSent.getToponyms()) {
                            if(otherToponym.getAmbiguity() > 0) {
                                int otherToponymIdx = lexicon.get(otherToponym.getForm());
                                if(!toponymWeights.containsKey(otherToponymIdx))
                                    toponymWeights.put(otherToponymIdx, SAME_SENT_WEIGHT);
                            }
                        }

                        int innerSentIndex = 0;
                        for(Sentence<StoredToken> innerSent : doc) {
                            for(Toponym innerToponym : innerSent.getToponyms()) {
                                if(innerToponym.getAmbiguity() > 0) {
                                    int innerToponymIdx = lexicon.get(innerToponym.getForm());
                                    if(!toponymWeights.containsKey(innerToponymIdx))
                                        toponymWeights.put(innerToponymIdx, OTHER_WEIGHT);
                                }
                            }
                            innerSentIndex++;
                        }

                        int bestRegionNumber = getBestRegionNumber(outerToponym, toponymWeights);
                        int indexToSelect = TopoUtil.getCorrectCandidateIndex(outerToponym, bestRegionNumber, DEGREES_PER_REGION);
                        if(indexToSelect == -1) {
                            System.out.println(outerToponym.getForm());
                        }
                        outerToponym.setSelectedIdx(indexToSelect);
                    }
                }
                outerSentIndex++;
            }
        }

        return corpus;
    }

    private int getBestRegionNumber(Toponym toponym, Map<Integer, Double> toponymWeights) {

        Map<Integer, Double> weightedSum = new HashMap<Integer, Double>();

        for(int outerIdx : toponymWeights.keySet()) {
            double weight = toponymWeights.get(outerIdx);
            Map<Integer, Double> curDist = regionDistributions.get(outerIdx);
            for(int innerIdx : curDist.keySet()) {
                Double prev = weightedSum.get(innerIdx);
                if(prev == null)
                    prev = 0.0;
                weightedSum.put(innerIdx, prev + weight * curDist.get(innerIdx));
            }
        }

        int bestRegionNumber = -1;
        double greatestMass = 0.0;
        for(int idx : weightedSum.keySet()) {
            if(TopoUtil.getCorrectCandidateIndex(toponym, idx, DEGREES_PER_REGION) >= 0) {
                double curMass = weightedSum.get(idx);
                if(curMass > greatestMass) {
                    bestRegionNumber = idx;
                    greatestMass = curMass;
                }
            }
        }

        return bestRegionNumber;
    }
}