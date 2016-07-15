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

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.ConditionalLinearGaussian;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.FileNotFoundException;
import java.util.Random;

/**
 * Created by andresmasegosa on 14/7/16.
 */
public class BayesianRegressionExpVMP {

    static DAG dag;

    public static DAG getDAG(int atts) {
        Variables variables  = new Variables();

        for (int i = 0; i < atts; i++) {
            variables.newGaussianVariable("Att"+i);
        }

        Variable predVar = variables.newGaussianVariable("PRED");

        DAG dag = new DAG(variables);

        for (Variable variable : variables) {
            if (variable==predVar)
                continue;
            dag.getParentSet(predVar).addParent(variable);
        }

        return dag;
    }

    public static void generateData(int samples, String file) throws Exception {

        BayesianNetwork bn = new BayesianNetwork(dag);
        bn.randomInitialization(new Random(10));

        Normal dist0 =  bn.getConditionalDistribution(bn.getVariables().getVariableByName("LocalHidden_0"));
        dist0.setMean(10);
        dist0.setVariance(1);
        Normal dist1 =  bn.getConditionalDistribution(bn.getVariables().getVariableByName("LocalHidden_1"));
        dist1.setMean(10);
        dist1.setVariance(1);

        ConditionalLinearGaussian conditionalLinearGaussian = bn.getConditionalDistribution(bn.getVariables().getVariableByName("G0"));

        conditionalLinearGaussian.setIntercept(1.0);
        conditionalLinearGaussian.setCoeffParents(new double[]{1.0, 1.0});
        conditionalLinearGaussian.setVariance(1);

        System.out.println(bn.toString());


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);

        DataStreamWriter.writeDataToFile(sampler.sampleToDataStream(samples),file);

    }

    public static void learn(int batchsize, String dataFile) throws FileNotFoundException {

        DataStream<DataInstance> data = DataStreamLoader.open(dataFile);
        dag.getVariables().setAttributes(data.getAttributes());

        SVB svb = new SVB();
        svb.setDAG(dag);
        svb.getPlateuStructure().getVMP().setMaxIter(100);
        svb.getPlateuStructure().getVMP().setThreshold(1e-100);
        svb.setWindowsSize(batchsize);
        svb.setOutput(true);

        svb.initLearning();

        svb.updateModel(data);

        System.out.println(svb.getLearntBayesianNetwork().toString());

    }

    public static void main(String[] args) throws Exception {
        args = new String[]{"3", "1000", "1000"};

        int natts = Integer.parseInt(args[0]);
        int nsamples = Integer.parseInt(args[1]);
        int batchsize = Integer.parseInt(args[2]);

        dag = DAGsGeneration.getIDAMultiLocalGaussianDAG(1,2);

        generateData(nsamples, "./tmp.arff");
        learn(batchsize, "./tmp.arff");
    }
}
