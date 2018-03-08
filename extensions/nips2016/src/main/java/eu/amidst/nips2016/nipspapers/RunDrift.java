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

package eu.amidst.nips2016.nipspapers;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.DriftSVB;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.lda.core.BatchSpliteratorByID;
import eu.amidst.lda.core.PlateauLDAReduced;


import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 4/5/16.
 */
public class RunDrift {

    public static int nwords(DataOnMemory<DataInstance> batch) {
        int nwords= 0;
        for (DataInstance dataInstance : batch) {
            nwords += (int) dataInstance.getValue(dataInstance.getAttributes().getAttributeByName("count"));
        }

        return nwords;
    }

    public static void printTopics(CompoundVector vector) {
        for (int j = 0; j < vector.getNumberOfBaseVectors(); j++) {

            ArrayVector arrayVector = (ArrayVector)vector.getVectorByPosition(j);

            int[] index = weka.core.Utils.sort(arrayVector.toArray());

            for (int i = index.length-1; i > index.length-100 && i>=0; i--) {
                System.out.print("("+index[i]+", "+arrayVector.get(index[i])+"), ");
            }

            System.out.println();

           /* SparseVectorDefaultValue sparseVector = (SparseVectorDefaultValue)vector.getVectorByPosition(j);
            System.out.print(sparseVector.sum()+": ");

            List<Map.Entry<Integer,Double>> list = new ArrayList(sparseVector.getValues().entrySet());

            list.sort(new Comparator<Map.Entry<Integer, Double>>() {
                @Override
                public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                    if (o1.getValue() > o2.getValue())
                        return -1;
                    else if (o1.getValue() < o2.getValue())
                        return 1;
                    else
                        return 0;
                }
            });

            List<Map.Entry<Integer,Double>> top5 = list;//list.subList(0, Math.min(list.size(),100));

            for (Map.Entry<Integer, Double> integerDoubleEntry : top5) {
                System.out.print("("+integerDoubleEntry.getKey()+", " +integerDoubleEntry.getValue()+"), ");
            }

            System.out.println();
            */
        }
    }

    public static void processBatch(DataOnMemory<DataInstance> batch) {
        Attribute attribute = batch.getAttributes().getAttributeByName("word");
        for (DataInstance dataInstance : batch) {
            //dataInstance.setValue(attribute,dataInstance.getValue(attribute)%attribute.getNumberOfStates());

            if (dataInstance.getValue(attribute)==158) //Adaptive
                dataInstance.setValue(attribute,0);
            else if (dataInstance.getValue(attribute)==958) //Bayesian
                dataInstance.setValue(attribute,1);
            else
                dataInstance.setValue(attribute,2);

//            if (dataInstance.getValue(attribute)==11025) //Table
//                dataInstance.setValue(attribute,0);
//            else if (dataInstance.getValue(attribute)==11026) //Tables
//                dataInstance.setValue(attribute,1);
//            else
//                dataInstance.setValue(attribute,2);

        }
    }
    public static void main(String[] args) throws Exception{


        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/";
        String arrffName = "docword.nips.arff";
        int ntopics = 5;
        int niter = 100;
        double threshold = 0.01;
        int docsPerBatch = 150;

        if (args.length>1){
            dataPath=args[0];
            arrffName=args[1];
            ntopics = Integer.parseInt(args[2]);
            niter = Integer.parseInt(args[3]);
            threshold = Double.parseDouble(args[4]);
            docsPerBatch = Integer.parseInt(args[5]);

            args[0]="";
        }



        DriftSVB svb = new DriftSVB();

        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+arrffName);

        PlateauLDAReduced plateauLDA = new PlateauLDAReduced(dataInstances.getAttributes(), "word", "count");
        plateauLDA.setNTopics(ntopics);
        plateauLDA.getVMP().setTestELBO(true);
        plateauLDA.getVMP().setMaxIter(niter);
        plateauLDA.getVMP().setOutput(false);
        plateauLDA.getVMP().setThreshold(threshold);

        svb.setPlateuStructure(plateauLDA);
        svb.setOutput(false);

        svb.initLearning();


        FileWriter fw = new FileWriter(dataPath+"DriftSVBoutput_"+Arrays.toString(args)+"_.txt");

/*
        for (DataOnMemory<DataInstance> batch : BatchSpliteratorByID.iterableOverDocuments(dataInstances, docsPerBatch)) {
            System.out.println("Batch: " + batch.getNumberOfDataInstances());
            double log = svb.predictedLogLikelihood(batch);

            svb.updateModelWithConceptDrift(batch);

            fw.write(log/nwords(batch)+"\t"+nwords(batch)+"\t"+svb.getLambdaMomentParameter()+"\n");
            fw.flush();
        }
*/

        System.out.println(svb.getPlateuStructure().getPosteriorSampleSize());

        List<DataOnMemory<DataInstance>> batches = BatchSpliteratorByID.streamOverDocuments(dataInstances,docsPerBatch).collect(Collectors.toList());

        for (int i = 0; i < batches.size(); i++) {

            System.out.println("Batch: " + batches.get(i).getNumberOfDataInstances()+ ", " + nwords(batches.get(i)));

            double log = 0;
            int nwords = 0;

            //processBatch(batches.get(i));
            /*for (int j = i+1; j < (i+1+1) && j< batches.size(); j++) {
                log += svb.predictedLogLikelihood(batches.get(j));
                nwords +=nwords(batches.get(j));
            }*/

            svb.updateModelWithConceptDrift(batches.get(i));

            fw.write(log/nwords+"\t"+nwords+"\t"+svb.getLambdaMomentParameter()+"\t"+svb.getPlateuStructure().getPosteriorSampleSize()+"\n");
            fw.flush();


            System.out.println();
            System.out.println();
            printTopics(svb.getPlateuStructure().getPlateauNaturalParameterPosterior());
            System.out.println();
            printTopics(svb.getPlateuStructure().getPlateauMomentParameterPosterior());
            System.out.println();
            System.out.println();

            System.out.println("ALPHA:" + svb.getLambdaMomentParameter());

            System.out.println("Sample Size:" + svb.getPlateuStructure().getPosteriorSampleSize());


            /*if (i>1 && i%20==10){
                System.out.println("CHANGE");
                arrffName = "docword.kos.tmp.arff";
                dataInstances = DataStreamLoader.open(dataPath+arrffName);
                batches = BatchSpliteratorByID.streamOverDocuments(dataInstances,docsPerBatch).collect(Collectors.toList());
                fw.write("CHANGE\n");
            }
            if (i>1 && i%20==0){
                System.out.println("CHANGE");
                arrffName = "docword.nips.arff";
                dataInstances = DataStreamLoader.open(dataPath+arrffName);
                batches = BatchSpliteratorByID.streamOverDocuments(dataInstances,docsPerBatch).collect(Collectors.toList());
                fw.write("CHANGE\n");
            }*/

        }

    }
}
