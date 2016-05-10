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

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.DriftSVB;
import eu.amidst.lda.BatchSpliteratorByID;
import eu.amidst.lda.PlateauLDA;

import java.io.FileWriter;
import java.util.Arrays;

/**
 * Created by andresmasegosa on 4/5/16.
 */
public class RunDrift {

    public static int nwords(DataOnMemory<DataInstance> batch) {
        int nwords= 0;
        for (DataInstance dataInstance : batch) {
            nwords +=
                    dataInstance.getValue(dataInstance.getAttributes().getAttributeByName("count"));
        }

        return nwords;
    }

    public static void main(String[] args) throws Exception{


        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/";
        String arrffName = "docword.kos.arff";
        int ntopics = 10;
        int niter = 10;
        double threshold = 0.1;
        int docsPerBatch = 100;

        if (args.length>1){
            dataPath=args[0];
            ntopics = Integer.parseInt(args[1]);
            niter = Integer.parseInt(args[2]);
            threshold = Double.parseDouble(args[3]);
            docsPerBatch = Integer.parseInt(args[4]);

            args[0]="";
        }



        DriftSVB svb = new DriftSVB();

        DataStream<DataInstance> dataInstances = DataStreamLoader.openFromFile(dataPath+arrffName);

        PlateauLDA plateauLDA = new PlateauLDA(dataInstances.getAttributes(), "word", "count");
        plateauLDA.setNTopics(ntopics);
        plateauLDA.getVMP().setTestELBO(true);
        plateauLDA.getVMP().setMaxIter(niter);
        plateauLDA.getVMP().setOutput(true);
        plateauLDA.getVMP().setThreshold(threshold);

        svb.setPlateuStructure(plateauLDA);
        svb.setOutput(true);

        svb.initLearning();


        FileWriter fw = new FileWriter(dataPath+"DriftSVBoutput_"+Arrays.toString(args)+"_.txt");


        for (DataOnMemory<DataInstance> batch : BatchSpliteratorByID.iterableOverDocuments(dataInstances, docsPerBatch)) {
            System.out.println("Batch: " + batch.getNumberOfDataInstances());
            double log = svb.predictedLogLikelihood(batch);

            svb.updateModelWithConceptDrift(batch);

            fw.write(log/nwords(batch)+"\t"+nwords(batch)+"\t"+svb.getLambdaValue()+"\n");
            fw.flush();
        }
    }
}
