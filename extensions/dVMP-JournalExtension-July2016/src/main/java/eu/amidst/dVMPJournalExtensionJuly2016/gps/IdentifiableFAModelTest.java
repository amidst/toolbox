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

package eu.amidst.dVMPJournalExtensionJuly2016.gps;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.dVMPJournalExtensionJuly2016.DAGsGeneration;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.io.FileNotFoundException;

/**
 * Created by andresmasegosa on 27/7/16.
 */
public class IdentifiableFAModelTest {

    public static void main(String[] args) throws FileNotFoundException {

        int nStates = 2;

        IdentifiableFAModel model = new IdentifiableFAModel(nStates);

        // set up the execution environment
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 16000);
        conf.setInteger("taskmanager.numberOfTaskSlots",1);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        env.setParallelism(1);
        env.getConfig().disableSysoutLogging();

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "/Users/andresmasegosa/Dropbox/Amidst/datasets/gps/FA_small2_train.arff", false);

        DAG dag = DAGsGeneration.getGPSFADAG(dataFlink.getAttributes().subList(0,4),nStates);


        for (int i = 0; i < 10; i++) {
            System.out.println("ITER:" + i);
            for (Variable variable : dag.getVariables().getListOfVariables()) {
                if (model.isActiveAtEpoch(variable, i))
                    System.out.print(variable.getName()+",");
            }
            System.out.println();
        }

    }
}