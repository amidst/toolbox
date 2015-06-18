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

package eu.amidst.scai2015;


import eu.amidst.corestatic.datastream.DataInstance;
import eu.amidst.corestatic.datastream.DataOnMemoryListContainer;
import eu.amidst.corestatic.datastream.DataStream;
import eu.amidst.corestatic.io.DataStreamLoader;
import eu.amidst.corestatic.io.DataStreamWriter;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.models.DAG;
import eu.amidst.corestatic.utils.BayesianNetworkSampler;
import eu.amidst.corestatic.variables.Variables;

import java.io.IOException;
import java.util.Random;

/**
 * Created by andresmasegosa on 20/5/15.
 */
public class DataGeneration {

    public static void dataGenerator(String[] args) throws IOException {

        int[][] connected = {{2,3,4}, {2,3,4}, {5,6,7}, {5,6,7}, {12, 13, 14}};

        for (int M = 0; M < 5; M++) {


            Variables vars = new Variables();

            vars.newGaussianVariable("SEQUENCE_ID");
            vars.newGaussianVariable("TIME_ID");

            for (int i = 0; i < 10; i++) {
                vars.newGaussianVariable("Gaussian_" + i);
            }


            for (int i = 0; i < 10; i++) {
                vars.newMultionomialVariable("Binary" + i, 2);
            }

            vars.newMultionomialVariable("Class", 2);

            DAG dag = new DAG(vars);

            for (int i = 0; i < connected[M].length; i++) {
                dag.getParentSet(vars.getVariableById(connected[M][i])).addParent(vars.getVariableByName("Class"));
            }

            BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

            bn.randomInitialization(new Random(0));

            System.out.println(bn.toString());

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);

            DataStream<DataInstance> data = sampler.sampleToDataStream(50000);

            DataOnMemoryListContainer<DataInstance> dataM = new DataOnMemoryListContainer<>(data.getAttributes());

            int cont = 0;
            Random rand = new Random(0);
            for (DataInstance instance : data) {

                instance.setValue(vars.getVariableById(0), cont++);
                instance.setValue(vars.getVariableById(1), M);

                if (rand.nextDouble()<0.2){
                    int classValue = (int)instance.getValue(vars.getVariableByName("Class"));
                    instance.setValue(vars.getVariableByName("Class"), (classValue+1)%2);
                }

                dataM.add(instance);
            }

            DataStreamWriter.writeDataToFile(dataM, "./scai2015/datasets/MPrime_"+M+".arff");
        }
    }



    public static void main(String[] args) throws IOException {
        //DataGeneration.dataGenerator(args);

        System.out.println(DataStreamLoader.openFromFile("./scai2015/datasets/BankArtificialDataSCAI2015.arff").stream().count());
    }
}
