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

package eu.amidst.tutorial.usingAmidst.examples;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.latentvariablemodels.staticmodels.LDA;
import eu.amidst.latentvariablemodels.staticmodels.Model;
import eu.amidst.tutorial.usingAmidst.Main;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.io.IOException;

/**
 * Created by rcabanas on 23/05/16.
 */
public class LDAModelLearningFlink {
    public static void main(String[] args) throws  IOException {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        env.getConfig().disableSysoutLogging();
        env.setParallelism(Main.PARALLELISM);

        //Load the data
        String filename = "datasets/simulated/docs.nips.distributed.arff";
        DataFlink<DataInstance> data = DataFlinkLoader.loadDataFromFolder(env, filename, false);

        //Learn the model
        Model model = new LDA(data.getAttributes());
        model.updateModel(data);
        BayesianNetwork bn = model.getModel();

        System.out.println(bn);

        // Save with .bn format
        BayesianNetworkWriter.save(bn, "networks/simulated/exampleBN.bn");
    }

}
