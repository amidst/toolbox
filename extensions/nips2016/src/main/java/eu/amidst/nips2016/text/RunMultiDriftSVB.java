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

package eu.amidst.nips2016.text;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.MultiDriftSVB;
import eu.amidst.lda.core.BatchSpliteratorByID;
import eu.amidst.lda.core.PlateauLDA;

import java.io.FileWriter;
import java.util.Arrays;

/**
 * Created by andresmasegosa on 4/5/16.
 */
public class RunMultiDriftSVB {

    public static int nwords(DataOnMemory<DataInstance> batch) {
        int nwords= 0;
        for (DataInstance dataInstance : batch) {
            nwords +=
                    dataInstance.getValue(dataInstance.getAttributes().getAttributeByName("count"));
        }

        return nwords;
    }

    public static void main(String[] args) throws Exception{

        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/NFSAbstracts/abstractByYear/";
        int ntopics = 10;
        int niter = 10;
        double threshold = 0.1;
        int docsPerBatch = 10;
        double delta = 5;

        if (args.length>1){
            dataPath=args[0];
            ntopics = Integer.parseInt(args[1]);
            niter = Integer.parseInt(args[2]);
            threshold = Double.parseDouble(args[3]);
            docsPerBatch = Integer.parseInt(args[4]);
            delta = Double.parseDouble(args[3]);
            args[0]="";
        }



        String[] years = {"90","91","92","93","94","95","96","97","98","99","00","01","02","03"};

        MultiDriftSVB svb = new MultiDriftSVB();
        svb.setDelta(delta);
        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+"abstract_"+years[0]+".arff");

        PlateauLDA plateauLDA = new PlateauLDA(dataInstances.getAttributes(), "word", "count");
        plateauLDA.setNTopics(ntopics);
        plateauLDA.getVMP().setTestELBO(true);
        plateauLDA.getVMP().setMaxIter(niter);
        plateauLDA.getVMP().setOutput(true);
        plateauLDA.getVMP().setThreshold(threshold);

        svb.setPlateuStructure(plateauLDA);
        svb.setOutput(true);

        svb.initLearning();


        FileWriter fw = new FileWriter(dataPath+"MultiDriftSVBoutput_"+Arrays.toString(args)+"_.txt");

        for (int i = 0; i < years.length; i+=3) {

            dataInstances = DataStreamLoader.open(dataPath+"abstract_"+years[i]+".arff");

            int count = 0;
            for (DataOnMemory<DataInstance> batch : BatchSpliteratorByID.iterableOverDocuments(dataInstances, docsPerBatch)) {
                count++;

                if (count%10!=1)
                    continue;


                System.out.println("Batch: " + batch.getNumberOfDataInstances());

                double log = svb.predictedLogLikelihood(batch);

                svb.updateModelWithConceptDrift(batch);

                fw.write(years[i]+"\t"+log/nwords(batch)+"\t"+nwords(batch)+"\t"+svb.getLambdaValue()+"\n");
                fw.flush();
            }
        }
    }
}
