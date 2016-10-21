/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.flinklink.core.learning.parametric;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.DAGGenerator;
import eu.amidst.core.variables.Variable;
import eu.amidst.sparklink.core.data.DataSpark;
import eu.amidst.sparklink.core.io.DataSparkLoader;
import eu.amidst.sparklink.core.learning.ParallelMaximumLikelihood;
import junit.framework.TestCase;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.SQLContext;
import org.junit.Assert;

import java.io.IOException;

/**
 * Created by andresmasegosa on 2/9/15.
 */
public class ParallelMaximumLikelihoodTest extends TestCase {

    public void testingMLParallelWI() throws Exception {

        SparkConf conf = new SparkConf().setAppName("SparkLink!").setMaster("local");;
        SparkContext sc = new SparkContext(conf);
        SQLContext sqlContext = new SQLContext(sc);

        //Path to dataset
        String path ="datasets/simulated/WI_samples.json";

        //Create an AMIDST object for managing the data
        DataSpark dataSpark = DataSparkLoader.open(sqlContext, path);

        //Learning algorithm
        ParallelMaximumLikelihood parameterLearningAlgorithm = new ParallelMaximumLikelihood();


        //We fix the BN structure
        DAG dag = DAGGenerator.getNaiveBayesStructure(dataSpark.getAttributes(), "W");

        parameterLearningAlgorithm.setDAG(dag);

        //We set the batch size which will be employed to learn the model in parallel
        parameterLearningAlgorithm.setBatchSize(100);
        //We set the data which is going to be used for leaning the parameters
        parameterLearningAlgorithm.setDataSpark(dataSpark);
        //We perform the learning
        parameterLearningAlgorithm.runLearning();
        //And we get the model
        BayesianNetwork bn = parameterLearningAlgorithm.getLearntBayesianNetwork();

        System.out.println(bn);

        Multinomial dist = bn.getConditionalDistribution(bn.getVariables().getVariableByName("W"));

        double[] p = dist.getProbabilities();

        Assert.assertTrue(p[0] == 0.6998001998001998);
        Assert.assertTrue(p[1] == 0.3001998001998002);

    }


}