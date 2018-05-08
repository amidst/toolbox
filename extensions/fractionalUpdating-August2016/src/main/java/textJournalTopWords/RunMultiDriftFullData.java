/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package textJournalTopWords;

import eu.amidst.core.datastream.*;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.DriftSVB;
import eu.amidst.lda.core.BatchSpliteratorByID;
import eu.amidst.lda.core.PlateauLDA;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 4/5/16.
 */
public class RunMultiDriftFullData {

    public static void main(String[] args) throws Exception{

        String[] yearsABSTRACT = {"90","91","92","93","94","95","96","97","98","99","00","01","02","03"};
        String[] yearsNIPS = {"0","1","2","3","4","5","6","7","8","9"};
        String[] yearsNIPSjournal = {"1987","1988","1989","1990","1991","1992","1993","1994","1995","1996","1997","1998","1999","2000","2001","2002","2003","2004","2005","2006","2007","2008","2009","2010","2011","2012","2013","2014","2015"};

        String model = "NIPSjournal";
        //String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/NFSAbstracts/abstractByYear/";
        //String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/nipsByYear/";
        String dataPath = "/Users/andresmasegosa/Google Drive/Amidst/svn/AMIDST-public/HPP_journal/NIPS2017_data/top100words/arff/";
        String dataWords = "/Users/andresmasegosa/Google Drive/Amidst/svn/AMIDST-public/HPP_journal/NIPS2017_data/top100words/NIPS_dict_top100words.csv";

        boolean stemmed = false;
        int numberOfTopWords = 100;

        int docsPerBatch = 10000;


/*        String model = "BCC1";
        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/";
        int docsPerBatch = 35000;
*/
        int ntopics = 10;
        int niter = 20;
        double threshold = 0.0001;

        boolean reversed = false;
        boolean priorTruncatedNormal=false;
        double priorTruncatedNormalPrecision=1;

        int ntopicsWords = 20;
        if (args.length>1){
            int cont=0;
            model = args[cont++];
            dataPath=args[cont++];
            dataWords=args[cont++];
            ntopics= Integer.parseInt(args[cont++]);
            niter = Integer.parseInt(args[cont++]);
            threshold = Double.parseDouble(args[cont++]);
            docsPerBatch = Integer.parseInt(args[cont++]);

            numberOfTopWords = Integer.parseInt(args[cont++]);
            stemmed = args[cont++].equals("stem");

            ntopicsWords = Integer.parseInt(args[cont++]);

            priorTruncatedNormal = args[cont++].equals("tnorm");
            if(priorTruncatedNormal)
                priorTruncatedNormalPrecision = Integer.parseInt(args[cont++]);

            reversed = args[cont++].equals("reversed");

            args[1]="";
        }

        String[] years=null;

        if (model.equals("ABSTRACRTS"))
            years=yearsABSTRACT;
        else if (model.equals("NIPS"))
            years=yearsNIPS;
        else if (model.equals("NIPSjournal")) {
            years = yearsNIPSjournal;
        }

        String localPath=null;

        if (model.equals("ABSTRACRTS"))
            localPath="abstract_";
        else if (model.equals("NIPS"))
            localPath="nips_";
        else if (model.equals("NIPSjournal"))
            localPath="NIPS_1987-2015_" + (stemmed ? "stemmed_" : "") + "top" + Integer.toString(numberOfTopWords) + "w_";


        Map<Integer,String> mapWords = Utils.loadWords(dataWords);

        MultiDriftSVB_EB svb = new MultiDriftSVB_EB();

        if(!priorTruncatedNormal) {
            System.out.println("Truncated Exponential");
            svb.setPriorDistribution(DriftSVB.TRUNCATED_EXPONENTIAL, new double[]{-0.1});
        }
        else {
            System.out.println("Truncated Normal with precision " + priorTruncatedNormalPrecision);
            svb.setPriorDistribution(DriftSVB.TRUNCATED_NORMAL, new double[]{0.5, priorTruncatedNormalPrecision});
        }

        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+localPath+years[0]+".arff");

        Attribute wordCountAtt = dataInstances.getAttributes().getAttributeByName("count");
        PlateauLDA plateauLDA = new PlateauLDA(dataInstances.getAttributes(), "word", "count");
        plateauLDA.setNTopics(ntopics);
        plateauLDA.getVMP().setTestELBO(true);
        plateauLDA.getVMP().setMaxIter(niter);
        plateauLDA.getVMP().setOutput(true);
        plateauLDA.getVMP().setThreshold(threshold);

        svb.setSeed(0);

        svb.setPlateuStructure(plateauLDA);
        svb.setOutput(true);

        svb.getPlateuStructure().getVMP().setTestELBO(true);
        svb.getPlateuStructure().getVMP().setMaxIter(niter);
        svb.getPlateuStructure().getVMP().setOutput(true);
        svb.getPlateuStructure().getVMP().setThreshold(threshold);

        svb.setWindowsSize(docsPerBatch);
        svb.initLearning();

        svb.randomInitialize();


        final String path = dataPath;
        final int finalDocsPerBatch = docsPerBatch;

        int count=0;



        Random random = new Random(1);

        double totalLog = 0;


        int year = 0;
        if (reversed)
            year = years.length-1;

        //for (int year = (reversed)? years.length-1: 0; (reversed)? year > 0 : year < years.length; (if(reversed) year--; else year++;)) {
        //for (int year = years.length-1; year > 0; year--) {
        boolean end = false;
        while (!end){

            DataStream<DataInstance> batch=DataStreamLoader.open(dataPath+localPath+years[year]+".arff");
            DataOnMemoryListContainer<DataInstance> train = new DataOnMemoryListContainer<DataInstance>(batch.getAttributes(),batch.stream().collect(Collectors.toList()));


            Iterator<DataOnMemory<DataInstance>> iteratorInner = BatchSpliteratorByID.iterableOverDocuments(train, finalDocsPerBatch).iterator();

            double lambda = 0;
            int n = 0;
            double[] vals = null;

            while (iteratorInner.hasNext()){
                svb.updateModelWithConceptDrift(iteratorInner.next());
                vals =  svb.getLambdaMomentParameters();
                for (int i = 0; i < vals.length; i++) {
                    lambda +=vals[i];
                    n++;
                }
            }
            lambda/=n;

            int wordCount = 0;
            double log = 0;

            System.out.println("OUT"+(years[year])+"\t"+log/wordCount+"\t"+wordCount+"\t"+lambda+"\n");

            totalLog+=log/wordCount;


            Utils.printTopicsTopWordsProbMass(ntopicsWords, svb.getNaturalParameterPrior(), mapWords, vals);
            Utils.printTopicsProportionsNormalized((PlateauLDA)svb.getPlateuStructure());
            System.out.println();

            if (reversed && year <= 0)
                end=true;
            if (!reversed && year>= years.length-1)
                end = true;

            if (reversed)
                year--;
            else
                year++;

        }

        System.out.println("TOTAL LOG: " + totalLog);

    }
}
