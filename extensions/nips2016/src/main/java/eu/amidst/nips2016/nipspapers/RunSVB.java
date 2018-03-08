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
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.lda.core.BatchSpliteratorByID;
import eu.amidst.lda.core.PlateauLDAReduced;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 4/5/16.
 */
public class RunSVB {

    public static int nwords(DataOnMemory<DataInstance> batch) {
        int nwords= 0;
        for (DataInstance dataInstance : batch) {
            nwords += dataInstance.getValue(dataInstance.getAttributes().getAttributeByName("count"));
        }

        return nwords;
    }

    public static void main(String[] args) throws Exception{


        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/";
        String arrffName = "docword.kos.arff";
        int ntopics = 2;
        int niter = 10;
        double threshold = 0.1;
        int docsPerBatch = 10;

        if (args.length>1){
            dataPath=args[0];
            arrffName=args[1];
            ntopics = Integer.parseInt(args[2]);
            niter = Integer.parseInt(args[3]);
            threshold = Double.parseDouble(args[4]);
            docsPerBatch = Integer.parseInt(args[5]);

            args[0]="";
        }



        SVB svb = new SVB();

        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+arrffName);

        PlateauLDAReduced plateauLDA = new PlateauLDAReduced(dataInstances.getAttributes(), "word", "count");
        plateauLDA.setNTopics(ntopics);
        plateauLDA.getVMP().setTestELBO(true);
        plateauLDA.getVMP().setMaxIter(niter);
        plateauLDA.getVMP().setOutput(true);
        plateauLDA.getVMP().setThreshold(threshold);

        svb.setPlateuStructure(plateauLDA);
        svb.setOutput(true);

        svb.initLearning();


        FileWriter fw = new FileWriter(dataPath+"SVBoutput_"+Arrays.toString(args)+"_.txt");

/*
        for (DataOnMemory<DataInstance> batch : BatchSpliteratorByID.iterableOverDocuments(dataInstances, docsPerBatch)) {
            System.out.println("Batch: " + batch.getNumberOfDataInstances());
            double log = svb.predictedLogLikelihood(batch);
            fw.write(log/nwords(batch)+"\t"+nwords(batch)+"\n");
            fw.flush();
            svb.updateModel(batch);
        }
*/


        List<DataOnMemory<DataInstance>> batches = BatchSpliteratorByID.streamOverDocuments(dataInstances,docsPerBatch).collect(Collectors.toList());

        for (int i = 0; i < batches.size(); i++) {

            System.out.println("Batch: " + batches.get(i).getNumberOfDataInstances());

            double log = 0;
            int nwords = 0;

            for (int j = i+1; j < (i+1+5) && j< batches.size(); j++) {
                log += svb.predictedLogLikelihood(batches.get(j));
                nwords += nwords(batches.get(j));
            }

            fw.write(log/nwords+"\t"+nwords+"\n");
            fw.flush();

            svb.updateModel(batches.get(i));

            System.out.println();
            System.out.println();
            RunDrift.printTopics(svb.getPlateuStructure().getPlateauNaturalParameterPosterior());
            System.out.println();
            System.out.println();

        }
    }
}
