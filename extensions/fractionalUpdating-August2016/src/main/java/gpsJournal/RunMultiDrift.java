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

package gpsJournal;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.DriftSVB;
import eu.amidst.core.variables.Variable;
import gps.DAGsGeneration;
import gps.Main;
import hpp.MultiDriftSVB_EB;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

/**
 * Created by andresmasegosa on 4/5/16.
 */
public class RunMultiDrift {

    public static void main(String[] args) throws Exception{

        //String model = "GPS0";
        //String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/Geo/out_month_10/";
        //int docsPerBatch = 35000;

        //String model = "GPS0";
        //String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/Geo/out_hour_100/";
        //int docsPerBatch = 8000;

        String model = "ELEC";
        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/DriftSets/electricityByMonth/";
        int docsPerBatch = 1600;


/*      String model = "BCC1";
        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/";
        int docsPerBatch = 35000;
*/  
        int ntopics = 5;
        int niter = 100;
        double threshold = 0.1;

        boolean priorTruncatedNormal=false;
        double priorTruncatedNormalPrecision=1;

        if (args.length>1){
            int cont=0;
            model = args[cont++];
            dataPath=args[cont++];
            ntopics= Integer.parseInt(args[cont++]);
            niter = Integer.parseInt(args[cont++]);
            threshold = Double.parseDouble(args[cont++]);
            docsPerBatch = Integer.parseInt(args[cont++]);

            priorTruncatedNormal = args[cont++].equals("tnorm");
            if(priorTruncatedNormal)
                priorTruncatedNormalPrecision = Integer.parseInt(args[cont++]);

            args[1]="";
        }



        MultiDriftSVB_EB svb = new MultiDriftSVB_EB();

        if(!priorTruncatedNormal) {
            System.out.println("Truncated Exponential");
            svb.setPriorDistribution(DriftSVB.TRUNCATED_EXPONENTIAL, new double[]{-0.1});
        }
        else {
            System.out.println("Truncated Normal with precision " + priorTruncatedNormalPrecision);
            svb.setPriorDistribution(DriftSVB.TRUNCATED_NORMAL, new double[]{0.5, priorTruncatedNormalPrecision});
        }



        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+
                Arrays.asList(new File(dataPath).list())
                .stream()
                .filter(string -> string.endsWith(".arff")).findFirst().get());

        svb.getPlateuStructure().getVMP().setTestELBO(true);
        svb.getPlateuStructure().getVMP().setMaxIter(niter);
        svb.getPlateuStructure().getVMP().setOutput(true);
        svb.getPlateuStructure().getVMP().setThreshold(threshold);

        svb.setWindowsSize(docsPerBatch);

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


        svb.setOutput(true);

        svb.initLearning();

        svb.randomInitialize();

        System.out.println(svb.getLearntBayesianNetwork());


        //svb.setLowerInterval(0.5);

        FileWriter fw = new FileWriter(dataPath+"MultiDriftSVB_Output_"+Arrays.toString(args)+"_.txt");


        fw.write("\t\t\t\t");
        for (Variable var : svb.getPlateuStructure().getNonReplicatedVariables()) {
            fw.write(var.getName() + "\t");
        }
        fw.write("\n");

        final String path = dataPath;
        final int finalDocsPerBatch = docsPerBatch;

        int count=0;



        Random random = new Random(1);

        double totalLog = 0;
        String[] strings = new File(dataPath).list();
        Arrays.sort(strings);
        for (String string : strings) {

            if (!string.endsWith(".arff"))
                continue;

            System.out.println("EPOCH: " + count +", "+ string);

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

            Iterator<DataOnMemory<DataInstance>> iteratorInner = train.streamOfBatches(finalDocsPerBatch).iterator();

            double lambdaMoment = 0;
            int n = 0;
            double[] valsMoments = null;
            double[] valsNatural = null;

            while (iteratorInner.hasNext()){
                svb.updateModelWithConceptDrift(iteratorInner.next());
                valsMoments =  svb.getLambdaMomentParameters();
                valsNatural =  svb.getLambdaNaturalParameters();
                for (int i = 0; i < valsMoments.length; i++) {
                    lambdaMoment +=valsMoments[i];
                    n++;
                }
            }
            lambdaMoment/=n;

            double log = 0;
            iteratorInner = test.streamOfBatches(finalDocsPerBatch).iterator();
            while (iteratorInner.hasNext()) {
                log+=svb.predictedLogLikelihood(iteratorInner.next());
            }

            double inst =test.getNumberOfDataInstances();

            System.out.println("OUT"+(count)+"\t"+log/inst+"\t"+inst+"\t"+lambdaMoment+"\n");

            fw.write((count++)+"\t"+log/inst+"\t"+inst+"\t"+lambdaMoment);
            for (int i = 0; i < valsMoments.length; i++) {
                fw.write("\t"+valsMoments[i]);
            }

            for (int i = 0; i < valsNatural.length; i++) {
                fw.write("\t"+valsNatural[i]);
            }

            fw.write("\n");

            fw.flush();

            totalLog+=log/inst;

            System.out.println(svb.getLearntBayesianNetwork());

        }
        fw.close();

        System.out.println("TOTAL LOG: " + totalLog);

    }
}
