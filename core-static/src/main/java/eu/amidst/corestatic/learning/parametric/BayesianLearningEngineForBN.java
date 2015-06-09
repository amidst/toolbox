/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.corestatic.learning.parametric;

import eu.amidst.corestatic.datastream.DataInstance;
import eu.amidst.corestatic.datastream.DataOnMemory;
import eu.amidst.corestatic.datastream.DataStream;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.models.DAG;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public final class BayesianLearningEngineForBN {

    private static BayesianLearningAlgorithmForBN bayesianLearningAlgorithmForBN = new StreamingVariationalBayesVMP();

    public static void setBayesianLearningAlgorithmForBN(BayesianLearningAlgorithmForBN bayesianLearningAlgorithmForBN) {
        BayesianLearningEngineForBN.bayesianLearningAlgorithmForBN = bayesianLearningAlgorithmForBN;
    }

    public static double updateModel(DataOnMemory<DataInstance> batch){
        return bayesianLearningAlgorithmForBN.updateModel(batch);
    }

    public static void runLearning() {
        bayesianLearningAlgorithmForBN.runLearning();
    }

    public static double getLogMarginalProbability(){
        return bayesianLearningAlgorithmForBN.getLogMarginalProbability();
    }


    public static void setDataStream(DataStream<DataInstance> data){
        bayesianLearningAlgorithmForBN.setDataStream(data);
    }

    public void setParallelMode(boolean parallelMode) {
        bayesianLearningAlgorithmForBN.setParallelMode(parallelMode);
    }

    public static void setDAG(DAG dag){
        bayesianLearningAlgorithmForBN.setDAG(dag);
    }

    public static BayesianNetwork getLearntBayesianNetwork(){
        return bayesianLearningAlgorithmForBN.getLearntBayesianNetwork();
    }

    public static void main(String[] args) throws Exception{

    }
}
