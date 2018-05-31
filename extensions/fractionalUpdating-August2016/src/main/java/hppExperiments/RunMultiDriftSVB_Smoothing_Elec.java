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

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import gps.DAGsGeneration;
import gps.Main;
import hpp.MultiDriftSVB_Smoothingv2;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

/**
 * Created by andresmasegosa on 4/5/16.
 */
public class RunMultiDriftSVB_Smoothing_Elec {

    public static void main(String[] args) throws Exception{

        //String model = "GPS0";
        //String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/Geo/out_month_10/";
        //int docsPerBatch = 35000;

//        String model = "GPS0";
//        String dataPath = "/Users/dario/Desktop/dataHPPJournal/data/out_hour_100/";
//        int docsPerBatch = 8000;

        String model = "ELEC";
        String dataPath = "/Users/dario/Desktop/dataHPPJournal/data/electricityByMonth/";
        int docsPerBatch = 1600;


/*      String model = "BCC1";
        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/";
        int docsPerBatch = 35000;
*/
        int ntopics = 5;
        int niter = 100;
        double threshold = 0.1;


        double learningRate=0.0001;
        int totalIterSmoothing=5;


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


        MultiDriftSVB_Smoothingv2 svb = new MultiDriftSVB_Smoothingv2();


        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+
                Arrays.asList(new File(dataPath).list())
                        .stream()
                        .filter(string -> string.endsWith(".arff")).findFirst().get());


        if (model.compareTo("GPS0")==0) {
            svb.setDAG(DAGsGeneration.getGPSMixtureDAGNoDay(dataInstances.getAttributes(), ntopics));
        }else if (model.compareTo("GPS1")==0) {
            svb.setDAG(DAGsGeneration.getGPSMixtureDAG(dataInstances.getAttributes(), ntopics));
        }else if (model.compareTo("GPS2")==0) {
            svb.setDAG(DAGsGeneration.getGPSFADAG(dataInstances.getAttributes(), ntopics));
        }else if (model.compareTo("BCC0")==0) {
            svb.setDAG(DAGsGeneration.getBCCMixtureDAG(dataInstances.getAttributes(), ntopics));
        }else if (model.compareTo("BCC1")==0) {
            svb.setDAG(DAGsGeneration.getBCCFullMixtureDAG(dataInstances.getAttributes(), ntopics));
        }else if (model.compareTo("BCC2")==0) {
            svb.setDAG(DAGsGeneration.getBCCFADAG(dataInstances.getAttributes(), ntopics));
        }else if (model.compareTo("BCC3")==0) {
            svb.setDAG(DAGsGeneration.getBCCLocalMixtureDAG(dataInstances.getAttributes(), ntopics));
        }else if (model.compareTo("BCC4")==0) {
            svb.setDAG(DAGsGeneration.getBCCNB(dataInstances.getAttributes()));
        }else if (model.compareTo("BCC5")==0) {
            svb.setDAG(DAGsGeneration.getBCCNBNoClass(dataInstances.getAttributes()));
        }else if (model.compareTo("ELEC")==0) {
            svb.setDAG(DAGsGeneration.getLinearRegressionElectricity(dataInstances.getAttributes(),ntopics));
        }





//        PlateauLDA plateauLDA = new PlateauLDA(dataInstances.getAttributes(), "word", "count");
//        plateauLDA.setNTopics(ntopics);
//        plateauLDA.getVMP().setTestELBO(false);
//        plateauLDA.getVMP().setMaxIter(niter);
//        plateauLDA.getVMP().setOutput(true);
//        plateauLDA.getVMP().setThreshold(threshold);
//
//        svb.getMultiDriftSVB().setPlateuStructure(plateauLDA);
//        svb.getMultiDriftSVB().setOutput(true);

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


        double preSmoothLog = 0;
        final int finalDocsPerBatch = docsPerBatch;


        final String path = dataPath;
        int count=0;


        Random random = new Random(1);

        String[] strings = new File(dataPath).list();
        Arrays.sort(strings);

        int[] instanceCounts = new int[(int)Arrays.stream(strings).filter(string -> string.endsWith(".arff")).count()];

        for (String string : strings) {

            if (!string.endsWith(".arff"))
                continue;

            //System.out.println("EPOCH: " + count +", "+ string);

            DataOnMemory<DataInstance> batch= DataStreamLoader.loadDataOnMemoryFromFile(path+string);

            if (batch.getNumberOfDataInstances()< Main.MIN)
                continue;

            Collections.shuffle(batch.getList(),random);






            int limit = (int) ((batch.getNumberOfDataInstances()*2.0)/3.0);

            DataOnMemoryListContainer<DataInstance> train= new
                    DataOnMemoryListContainer(batch.getAttributes());
            train.addAll(batch.getList().subList(0,limit));

            DataOnMemoryListContainer<DataInstance> test= new
                    DataOnMemoryListContainer(batch.getAttributes());
            test.addAll(batch.getList().subList(limit+1,batch.getNumberOfDataInstances()));


            svb.aggregateTrainBatches(train);

            svb.aggregateTestBatches(test);



            Iterator<DataOnMemory<DataInstance>> iteratorInner = train.streamOfBatches(finalDocsPerBatch).iterator();

            double lambdaMoment = 0;
            int n = 0;
            double[] valsMoments = null;
            double[] valsNatural = null;

            while (iteratorInner.hasNext()){
                svb.getMultiDriftSVB().updateModelWithConceptDrift(iteratorInner.next());
                valsMoments =  svb.getMultiDriftSVB().getLambdaMomentParameters();
                valsNatural =  svb.getMultiDriftSVB().getLambdaNaturalParameters();
                for (int i = 0; i < valsMoments.length; i++) {
                    lambdaMoment +=valsMoments[i];
                    n++;
                }
            }
            lambdaMoment/=n;

            double log = 0;
            iteratorInner = test.streamOfBatches(finalDocsPerBatch).iterator();
            while (iteratorInner.hasNext()) {
                log+=svb.getMultiDriftSVB().predictedLogLikelihood(iteratorInner.next());
            }

            int inst = test.getNumberOfDataInstances();
            instanceCounts[count]=inst;


            System.out.println("Filter:\t"+(count)+"\t" + log/inst+"\t"+inst+"\t"+lambdaMoment+"\n");

            fw.write((count++)+"\t"+log/inst+"\t"+inst+"\t"+lambdaMoment);
            for (int i = 0; i < valsMoments.length; i++) {
                fw.write("\t"+valsMoments[i]);
            }

            for (int i = 0; i < valsNatural.length; i++) {
                fw.write("\t"+valsNatural[i]);
            }

            fw.write("\n");

            fw.flush();

            preSmoothLog+=log/inst;

            //System.out.println(svb.getMultiDriftSVB().getLearntBayesianNetwork());

        }
        fw.close();

        // System.out.println("TOTAL LOG FILTER: " + preSmoothLog);




        try {
            svb.smooth();

        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }


        fw = new FileWriter(dataPath+"MDSVBSmooth_Output_"+Arrays.toString(args)+"_.txt");


        double[] testLL = svb.predictedLogLikelihood();

        for (int j = 0; j < testLL.length; j++) {

            String expectedParameters = "";
            double lambda = 0;

            for (int i = 0; i < ntopics-1; i++) {
                lambda+= svb.getOmegaPosteriors().get(j).get(i).getExpectedParameters().get(0);
                expectedParameters = expectedParameters + svb.getOmegaPosteriors().get(j).get(i).getExpectedParameters().get(0) + "\t";
            }
            lambda+= svb.getOmegaPosteriors().get(j).get(ntopics-1).getExpectedParameters().get(0);
            expectedParameters = expectedParameters + svb.getOmegaPosteriors().get(j).get(ntopics-1).getExpectedParameters().get(0);

            lambda = lambda/ntopics;

            int instanceCount = instanceCounts[j];

            String textOutput = j + "\t" + instanceCount + "\t" + testLL[j]/instanceCount + "\t" + lambda + "\t" + expectedParameters + "\n";
            System.out.println("Smoothed:\t" + textOutput);
            fw.write(textOutput);

        }

        fw.close();


        System.out.println(preSmoothLog);
        System.out.println(weka.core.Utils.sum(testLL));
    }
}
