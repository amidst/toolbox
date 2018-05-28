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

package hppExperiments;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.lda.core.BatchSpliteratorByID;
import eu.amidst.lda.core.PlateauLDA;
import hpp.MultiDriftSVB_Smoothing;
import textJournalTopWords.Utils;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static textJournalTopWords.Utils.SEED;

/**
 * Created by andresmasegosa on 4/5/16.
 */
public class RunMultiDriftSVB_Smoothing {

    public static void main(String[] args) throws Exception{

        String[] yearsABSTRACT = {"90","91","92","93","94","95","96","97","98","99","00","01","02","03"};
        String[] yearsNIPS = {"0","1","2","3","4","5","6","7","8","9"};
        String[] yearsNIPSjournal = {"1987","1988","1989","1990","1991","1992","1993","1994","1995","1996","1997","1998","1999","2000","2001","2002","2003","2004","2005","2006","2007","2008","2009","2010","2011","2012","2013","2014","2015"};
        //String[] yearsNIPSjournal = {"1987","1988","1989", "1990", "1991","1992","1993","1994","1995"};

        String model = "NIPSjournal";

        //String dataPath = "/Users/andresmasegosa/Google Drive/Amidst/svn/AMIDST-public/HPP_journal/NIPS2017_data/top100words/arff/";
        String dataPath = "/Users/dario/Downloads/NIPS_datos/top100words/arff/";

        boolean stemmed = false;
        int numberOfTopWords = 100;

        int docsPerBatch = 10000;


/*        String model = "BCC1";
        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/";
        int docsPerBatch = 35000;
*/
        int ntopics = 5;
        int niter = 50;
        double threshold = 0.0001;

        double learningRate=0.01;
        int totalIterSmoothing=10;


        if (args.length>1) {
            int cont=0;
            model = args[cont++];
            dataPath=args[cont++];
            ntopics= Integer.parseInt(args[cont++]);
            niter = Integer.parseInt(args[cont++]);
            threshold = Double.parseDouble(args[cont++]);
            docsPerBatch = Integer.parseInt(args[cont++]);
            learningRate = Double.parseDouble(args[cont++]);
            totalIterSmoothing = Integer.parseInt(args[cont++]);


            args[1]="";
        }

        String[] years=null;

        if (model.equals("ABSTRACTS"))
            years=yearsABSTRACT;
        else if (model.equals("NIPS"))
            years=yearsNIPS;
        else if (model.equals("NIPSjournal")) {
            years = yearsNIPSjournal;
        }

        String localPath=null;

        if (model.equals("ABSTRACTS"))
            localPath="abstract_";
        else if (model.equals("NIPS"))
            localPath="nips_";
        else if (model.equals("NIPSjournal"))
            localPath="NIPS_1987-2015_" + (stemmed ? "stemmed_" : "") + "top" + Integer.toString(numberOfTopWords) + "w_";

        MultiDriftSVB_Smoothing svb = new MultiDriftSVB_Smoothing();

//        if(!priorTruncatedNormal) {
//            System.out.println("Truncated Exponential");
//            svb.getMultiDriftSVB().setPriorDistribution(DriftSVB.TRUNCATED_EXPONENTIAL, new double[]{-0.1});
//        }
//        else {
//            System.out.println("Truncated Normal with precision " + priorTruncatedNormalPrecision);
//            svb.getMultiDriftSVB().setPriorDistribution(DriftSVB.TRUNCATED_NORMAL, new double[]{0.5, priorTruncatedNormalPrecision});
//        }

        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+localPath+years[0]+".arff");

        Attribute wordCountAtt = dataInstances.getAttributes().getAttributeByName("count");
        PlateauLDA plateauLDA = new PlateauLDA(dataInstances.getAttributes(), "word", "count");
        plateauLDA.setNTopics(ntopics);
        plateauLDA.getVMP().setTestELBO(false);
        plateauLDA.getVMP().setMaxIter(niter);
        plateauLDA.getVMP().setOutput(true);
        plateauLDA.getVMP().setThreshold(threshold);

        svb.getMultiDriftSVB().setPlateuStructure(plateauLDA);
        svb.getMultiDriftSVB().setOutput(true);

        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setTestELBO(false);
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setMaxIter(niter);
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setOutput(true);
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setThreshold(threshold);

        svb.setWindowsSize(docsPerBatch);
        svb.initLearning();

        svb.getMultiDriftSVB().randomInitialize();


        svb.setLearningRate(learningRate);
        svb.setTotalIter(totalIterSmoothing);

        //svb.setLowerInterval(0.5);



        FileWriter fw = new FileWriter(dataPath+"MDSVBFilter_Output_"+Arrays.toString(args)+"_.txt");

//        fw.write("\t\t\t\t");
//        for (Variable var : svb.getMultiDriftSVB().getPlateuStructure().getNonReplicatedVariables()) {
//            fw.write(var.getName() + "\t");
//        }
//        fw.write("\n");


        double preSmoothLog = 0;

        final int finalDocsPerBatch = docsPerBatch;

        int[] wordCounts = new int[years.length];

        for (int year = 0; year < years.length; year++) {

            DataStream<DataInstance> batch=DataStreamLoader.open(dataPath+localPath+years[year]+".arff");

            List<DataOnMemory<DataInstance>> trainTest =  Utils.splitTrainTest(batch,SEED);

            DataOnMemory<DataInstance> trainBatch = trainTest.get(0);
            DataOnMemory<DataInstance> testBatch = trainTest.get(1);


            svb.aggregateTrainBatches(trainBatch);
            svb.aggregateTestBatches(testBatch);




            Iterator<DataOnMemory<DataInstance>> iteratorInner; // = BatchSpliteratorByID.iterableOverDocuments(trainBatch, finalDocsPerBatch).iterator();


            int wordCount = 0;
            iteratorInner = BatchSpliteratorByID.iterableOverDocuments(testBatch, finalDocsPerBatch).iterator();
            while (iteratorInner.hasNext()) {
                DataOnMemory<DataInstance> batchTest = iteratorInner.next();
                wordCount+=batchTest.stream().mapToDouble(d -> d.getValue(wordCountAtt)).sum();
            }

            wordCounts[year]=wordCount;

            double log=svb.getMultiDriftSVB().predictedLogLikelihood(testBatch);
            preSmoothLog+=log;

            String expectedParameters = "";
            double lambda = 0;

            for (int i = 0; i < ntopics-1; i++) {
                lambda+= svb.getMultiDriftSVB().getLambdaMomentParameters()[i];
                expectedParameters = expectedParameters + svb.getMultiDriftSVB().getLambdaMomentParameters()[i] + "\t";
            }
            lambda+= svb.getMultiDriftSVB().getLambdaMomentParameters()[ntopics-1];
            expectedParameters = expectedParameters + svb.getMultiDriftSVB().getLambdaMomentParameters()[ntopics-1];

            lambda = lambda/ntopics;

            String textOutput = year + "\t" + wordCount + "\t" + log/wordCount + "\t" + lambda + "\t" + expectedParameters + "\n";
            System.out.println("Filter:\t" + textOutput);
            fw.write(textOutput);


        }
        fw.close();

//        System.out.println("TOTAL LOG: " + totalLog);




        fw = new FileWriter(dataPath+"MDSVBSmooth_Output_"+Arrays.toString(args)+"_.txt");

//        fw.write("\t\t\t\t");
//        for (Variable var : svb.getMultiDriftSVB().getPlateuStructure().getNonReplicatedVariables()) {
//            fw.write(var.getName() + "\t");
//        }
//        fw.write("\n");


        svb.smooth();
        double[] testLL = svb.predictedLogLikelihood();
        for (int year = 0; year < years.length; year++) {

            String expectedParameters = "";
            double lambda = 0;

            for (int i = 0; i < ntopics-1; i++) {
                lambda+= svb.getOmegaPosteriors().get(year).get(i).getExpectedParameters().get(0);
                expectedParameters = expectedParameters + svb.getOmegaPosteriors().get(year).get(i).getExpectedParameters().get(0) + "\t";
            }
            lambda+= svb.getOmegaPosteriors().get(year).get(ntopics-1).getExpectedParameters().get(0);
            expectedParameters = expectedParameters + svb.getOmegaPosteriors().get(year).get(ntopics-1).getExpectedParameters().get(0);

            lambda = lambda/ntopics;

            int wordCount = wordCounts[year];

            String textOutput = year + "\t" + wordCount + "\t" + testLL[year]/wordCount + "\t" + lambda + "\t" + expectedParameters + "\n";
            System.out.println("Smoothed:\t" + textOutput);
            fw.write(textOutput);
        }

        fw.close();


        System.out.println(preSmoothLog);
        System.out.println(weka.core.Utils.sum(testLL));
    }
}
