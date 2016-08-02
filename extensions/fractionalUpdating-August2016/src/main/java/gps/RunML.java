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

package gps;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by andresmasegosa on 4/5/16.
 */
public class RunML {

    public static void main(String[] args) throws Exception{

        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/gps/Mixture_1_100.arff";
        int ntopics = 2;
        int niter = 100;
        double threshold = 0.1;
        int docsPerBatch = 100;

        if (args.length>1){
            dataPath=args[0];
            ntopics= Integer.parseInt(args[1]);
            niter = Integer.parseInt(args[2]);
            threshold = Double.parseDouble(args[3]);
            docsPerBatch = Integer.parseInt(args[4]);

            args[0]="";
        }


        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath);



        FileWriter fw = new FileWriter(dataPath+"MLoutput_"+Arrays.toString(args)+"_.txt");


        Iterator<DataOnMemory<DataInstance>> iterator = Arrays.asList(new File(dataPath).list()).stream().map(string -> DataStreamLoader.loadDataOnMemoryFromFile(string)).iterator();

//        Iterator<DataOnMemory<DataInstance>> iterator = dataInstances.iterableOverBatches(docsPerBatch).iterator();

        ParallelMaximumLikelihood svb = new ParallelMaximumLikelihood();


        svb.setWindowsSize(docsPerBatch);
        svb.setDAG(DAGsGeneration.getGPSFADAG(dataInstances.getAttributes(),0));
        svb.setOutput(true);

        svb.initLearning();

        while(iterator.hasNext()){

            DataOnMemory<DataInstance> batch= iterator.next();
            if (batch.getNumberOfDataInstances()==0)
                continue;

            svb.updateModel(batch);

            BayesianNetwork bn = svb.getLearntBayesianNetwork();
            fw.write(((Normal)bn.getConditionalDistributions().get(0)).getMean()+"\t"+((Normal)bn.getConditionalDistributions().get(1)).getMean()+"\n");
            fw.flush();

        }
        fw.close();
    }
}
