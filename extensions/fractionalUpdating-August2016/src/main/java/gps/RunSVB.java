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
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by andresmasegosa on 4/5/16.
 */
public class RunSVB {

    public static void main(String[] args) throws Exception{

        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/Geo/out/";
        int ntopics = 0;
        int niter = 100;
        double threshold = 0.1;
        int docsPerBatch = 1000;


        if (args.length>1){
            dataPath=args[0];
            ntopics= Integer.parseInt(args[1]);
            niter = Integer.parseInt(args[2]);
            threshold = Double.parseDouble(args[3]);
            docsPerBatch = Integer.parseInt(args[4]);

            args[0]="";
        }



        SVB svb = new SVB();

        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+
                Arrays.asList(new File(dataPath).list())
                        .stream()
                        .filter(string -> string.endsWith(".arff")).findFirst().get());

        svb.getPlateuStructure().getVMP().setTestELBO(true);
        svb.getPlateuStructure().getVMP().setMaxIter(niter);
        svb.getPlateuStructure().getVMP().setOutput(true);
        svb.getPlateuStructure().getVMP().setThreshold(threshold);

        svb.setWindowsSize(docsPerBatch);
        svb.setDAG(DAGsGeneration.getGPSFADAG(dataInstances.getAttributes(),ntopics));
        svb.setOutput(true);

        svb.initLearning();


        FileWriter fw = new FileWriter(dataPath+"SVBoutput_"+Arrays.toString(args)+"_.txt");


        int count=0;

//        Iterator<DataOnMemory<DataInstance>> iterator = Arrays.asList(new File(dataPath).list()).stream().map(string -> DataStreamLoader.loadDataOnMemoryFromFile(string)).iterator();

        final String path = dataPath;
        final int finalDocsPerBatch = docsPerBatch;
        Iterator<DataOnMemory<DataInstance>> iterator =
                Arrays.asList(new File(dataPath).list())
                        .stream()
                        .filter(string -> string.endsWith(".arff"))
                        .map(string -> DataStreamLoader.loadDataOnMemoryFromFile(path+string))
                        .flatMap(data -> data.streamOfBatches(finalDocsPerBatch))
                        .iterator();

        List<DataOnMemory<DataInstance>> list = new ArrayList<>();

        int eval = 1;

        for (int i = 0; i < (eval-1) && iterator.hasNext(); i++) {
            DataOnMemory<DataInstance> batch= iterator.next();
            if (batch.getNumberOfDataInstances()==0)
                continue;
            list.add(batch);        }

        while(iterator.hasNext()){

            DataOnMemory<DataInstance> batch= iterator.next();
            if (batch.getNumberOfDataInstances()==0)
                continue;

            list.add(batch);

            double log = 0;
            double inst =0;
            for (int i = 0; i < list.size(); i++) {
                log+=svb.predictedLogLikelihood(list.get(i));
                inst+=list.get(i).getNumberOfDataInstances();
            }
            fw.write((count++)+"\t"+log/inst+"\t"+inst+"\n");
            fw.flush();

            svb.updateModel(list.remove(0));
        }
    }
}
