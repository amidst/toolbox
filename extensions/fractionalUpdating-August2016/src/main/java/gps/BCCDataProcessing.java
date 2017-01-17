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
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 2/8/16.
 */
public class BCCDataProcessing {
    final static int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};

    public static void removePeakMonths(String[] args) throws IOException {
        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/";
        String dataPathOut = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWekaNoPeakMonths/";

        String[] strings = new File(dataPath).list();
        Arrays.sort(strings);
        for (String string : strings) {
            if (!string.endsWith(".arff"))
                continue;
            int currentMonth = Integer.parseInt(string.substring(8, 8 + 2));

            if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                continue;

            FileUtils.copyFile(new File(dataPath + string), new File(dataPathOut + string));

        }

    }

    public static void createMonths(String[] args) throws IOException {

        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/datosWekaRemoveOutliersExtreme.arff";

        String dataOutput = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWekaNoOutliers/";



        for (int i = 0; i < 84; i++) {
            String path;
            if (i<10){
                path = dataOutput+"dataWeka0"+i+".arff";
            }else{
                path = dataOutput+"dataWeka"+i+".arff";
            }
            FileWriter fileWriter = new FileWriter(path);

            Path pathFile = Paths.get(dataPath);

            Iterator<String> iter = Files.lines(pathFile).iterator();

            while(iter.hasNext()){
                String line = iter.next();
                if (line.startsWith("@")){
                    fileWriter.write(line+"\n");
                }else if (!line.isEmpty() && line.split(",")[0].compareTo(i+"")==0){
                    fileWriter.write(line+"\n");
                }
            }

            fileWriter.close();

        }


    }

    public static void printMeanAndVariance(String[] args) {

        String dataOutput = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWekaNoPeakMonths/";



        for (int i = 0; i < 84; i++) {
            String path;
            if (i<10){
                path = dataOutput+"dataWeka0"+i+".arff";
            }else{
                path = dataOutput+"dataWeka"+i+".arff";
            }

            try {
                DataStream<DataInstance> batch = DataStreamLoader.loadDataOnMemoryFromFile(path);


            DAG dag = DAGsGeneration.getBCCNB(batch.getAttributes());

            SVB svb = new SVB();

            svb.setDAG(dag);

            svb.initLearning();

            svb.updateModel(batch);


            BayesianNetwork net = svb.getLearntBayesianNetwork();

            for (Variable variable : dag.getVariables()) {
                if (!variable.isNormal())
                    continue;

                Normal dist = net.getConditionalDistribution(variable);

                System.out.print(dist.getMean()+"\t");
            }

            System.out.println();

            }catch(Exception ex){
                continue;
            }

        }
    }

    public static void computSize(String[] args) {

        String dataOutput = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWekaNoPeakMonths/";
        //String dataOutput = "/Users/andresmasegosa/Dropbox/Amidst/datasets/Geo/out_month_10/";



        double totalLog = 0;
        String[] strings = new File(dataOutput).list();
        Arrays.sort(strings);
        for (String string : strings) {

            if (!string.endsWith(".arff"))
                continue;

            DataOnMemory<DataInstance> batch = DataStreamLoader.loadDataOnMemoryFromFile(dataOutput+string);

            System.out.println(batch.getNumberOfDataInstances());

            totalLog+=batch.getNumberOfDataInstances();
        }

        System.out.println(totalLog);
    }

    public static void main(String[] args) {
        BCCDataProcessing.computSize(args);
    }

}