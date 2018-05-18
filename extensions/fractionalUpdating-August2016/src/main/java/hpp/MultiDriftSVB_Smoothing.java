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

package hpp;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.learning.parametric.bayesian.MultiDriftSVB;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.utils.VMPLocalUpdates;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static eu.amidst.core.learning.parametric.bayesian.DriftSVB.TRUNCATED_EXPONENTIAL;
import static eu.amidst.core.learning.parametric.bayesian.DriftSVB.TRUNCATED_NORMAL;

/**
 * Created by andresmasegosa on 14/4/16.
 */
public class MultiDriftSVB_Smoothing  {

    List<DataOnMemory<DataInstance>> testBatches = new ArrayList();
    List<DataOnMemory<DataInstance>> trainBatches = new ArrayList();
    MultiDriftSVB multiDriftSVB = new MultiDriftSVB();
    List<List<EF_UnivariateDistribution>> lambdaPosteriors = new ArrayList();
    List<List<EF_UnivariateDistribution>> omegaPosteriors = new ArrayList<>();
    CompoundVector initialPrior = null;
    public void initLearning() {
        multiDriftSVB.initLearning();
        initialPrior=multiDriftSVB.getPlateuStructure().getPlateauNaturalParameterPrior();
    }

    public void aggregateTestBatches(DataOnMemory<DataInstance> batch) {
        testBatches.add(batch);
    }
    public void aggregateTrainBatches(DataOnMemory<DataInstance> batch){
        trainBatches.add(batch);
        multiDriftSVB.updateModelWithConceptDrift(batch);
        lambdaPosteriors.add(multiDriftSVB.getPlateuStructure().getQPosteriors());
        omegaPosteriors.add(multiDriftSVB.getRhoPosterior());
    }

    public void smooth(){
        VMPLocalUpdates vmpLocalUpdates = new VMPLocalUpdates(multiDriftSVB.getPlateuStructure());
        multiDriftSVB.getPlateuStructure().setVmp(vmpLocalUpdates);
        multiDriftSVB.initLearning();

        double learningRate = 0.1;
        for (int t = 0; t < trainBatches.size(); t++) {
            CompoundVector prior = null;
            if (t==0)
                prior = new CompoundVector(lambdaPosteriors.get(t-1).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
            else
                prior = initialPrior;

            CompoundVector posterior = new CompoundVector(lambdaPosteriors.get(t).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
            multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(prior);
            multiDriftSVB.getPlateuStructure().updateNaturalParameterPosteriors(posterior);

            multiDriftSVB.getPlateuStructure().setEvidence(trainBatches.get(t).getList());
                for (int iter = 0; iter < 100; iter++) {
                    multiDriftSVB.updateModelOnBatchParallel(trainBatches.get(t));

                    //Compute E_q[] - \bmlambda_t
                    CompoundVector gradientT = multiDriftSVB.getPlateuStructure().getPlateauNaturalParameterPosterior();
                    gradientT.substract(posterior);

                    //Multiply by hessian
                    for (int k = 0; k < lambdaPosteriors.get(0).size(); k++) {
                        //this.lambdaPosteriors.get(t).get(k).perMultiplyHessian(gradientT.getVectorByPosition(k));
                    }

                    //E[\rho]E_q[]
                    CompoundVector gradientTplus1 = new CompoundVector(this.lambdaPosteriors.get(t+1).stream().map(q -> q.getMomentParameters()).collect(Collectors.toList()));
                    gradientTplus1.substract(new CompoundVector(this.lambdaPosteriors.get(t).stream().map(q -> q.getMomentParameters()).collect(Collectors.toList())));
                    for (int k = 0; k < gradientTplus1.getNumberOfBaseVectors(); k++) {
                        gradientTplus1.getVectorByPosition(k).multiplyBy(this.omegaPosteriors.get(t+1).get(k).getExpectedParameters().get(0));
                    }
                    gradientT.sum(gradientTplus1);
                    gradientT.multiplyBy(learningRate);
                    for (int k = 0; k < gradientTplus1.getNumberOfBaseVectors(); k++) {
                        this.lambdaPosteriors.get(t).get(k).getNaturalParameters().sum(gradientT.getVectorByPosition(k));
                    }
                }
        }

    }

    double predictedLogLikelihood(List<DataOnMemory<DataInstance>> batch){

        double testLL = 0;
        for (int t = 0; t < testBatches.size(); t++) {
            CompoundVector prior = null;
            if (t == 0)
                prior = new CompoundVector(lambdaPosteriors.get(t - 1).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
            else
                prior = initialPrior;

            CompoundVector posterior = new CompoundVector(lambdaPosteriors.get(t).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
            multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(prior);
            multiDriftSVB.getPlateuStructure().updateNaturalParameterPosteriors(posterior);
            testLL+=multiDriftSVB.predictedLogLikelihood(batch.get(t));
        }
        return testLL;
    }



    public static void main(String[] args) throws IOException, ClassNotFoundException {

        BayesianNetwork oneNormalVarBN = BayesianNetworkLoader.loadFromFile("./networks/simulated/Normal.bn");

        System.out.println(oneNormalVarBN);
        int batchSize = 1000;


        MultiDriftSVB svb = new MultiDriftSVB();
        svb.setWindowsSize(batchSize);
        svb.setSeed(0);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(oneNormalVarBN.getDAG());

        svb.initLearning();

        double pred = 0;
        for (int i = 0; i < 10; i++) {

            if (i%3==0) {
                oneNormalVarBN.randomInitialization(new Random(i));
                System.out.println(oneNormalVarBN);
            }

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(oneNormalVarBN);
            sampler.setSeed(i);
            DataOnMemory<DataInstance> batch = sampler.sampleToDataStream(batchSize).toDataOnMemory();

            if (i>0)
                pred+=svb.predictedLogLikelihood(batch);

            svb.updateModelWithConceptDrift(batch);


            System.out.println(svb.getLogMarginalProbability());
            System.out.println(svb.getLearntBayesianNetwork());

        }

        System.out.println(pred);

    }
}
