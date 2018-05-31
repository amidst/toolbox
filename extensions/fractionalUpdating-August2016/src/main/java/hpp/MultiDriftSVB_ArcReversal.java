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
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.learning.parametric.bayesian.MultiDriftSVB;
import eu.amidst.core.learning.parametric.bayesian.utils.VMPLocalUpdates;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import weka.core.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 14/4/16.
 */
public class MultiDriftSVB_ArcReversal {

    List<DataOnMemory<DataInstance>> testBatches = new ArrayList();
    List<DataOnMemory<DataInstance>> trainBatches = new ArrayList();
    MultiDriftSVB multiDriftSVB = new MultiDriftSVB();
    List<List<EF_UnivariateDistribution>> lambdaPosteriors = new ArrayList();
    List<List<EF_UnivariateDistribution>> omegaPosteriors = new ArrayList<>();
    List<EF_UnivariateDistribution> omegaPrior = new ArrayList<>();
    CompoundVector initialPrior = null;
    public void initLearning() {
        multiDriftSVB.initLearning();
        multiDriftSVB.randomInitialize();
        initialPrior=multiDriftSVB.getPlateuStructure().getPlateauNaturalParameterPrior();
    }

    public List<List<EF_UnivariateDistribution>> getOmegaPosteriors() {
        return omegaPosteriors;
    }

    public void aggregateTestBatches(DataOnMemory<DataInstance> batch) {
        testBatches.add(batch);
    }
    public void aggregateTrainBatches(DataOnMemory<DataInstance> batch){
        trainBatches.add(batch);
        multiDriftSVB.updateModelWithConceptDrift(batch);
        lambdaPosteriors.add(multiDriftSVB.getPlateuStructure().getQPosteriors());
        omegaPosteriors.add(multiDriftSVB.getRhoPosterior());
        omegaPrior.add(multiDriftSVB.getRhoPrior());

    }


    public void smooth() {

        multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(this.initialPrior);

        multiDriftSVB.updateModelWithConceptDrift(trainBatches.get(trainBatches.size() - 1));

        for (int t = trainBatches.size() - 2; t >= 0; t--) {
            multiDriftSVB.updateModelWithConceptDrift(trainBatches.get(t));
            CompoundVector vector = multiDriftSVB.getPlateuStructure().getPlateauNaturalParameterPosterior();
            if (t>0){
                for (int i = 0; i < this.lambdaPosteriors.get(t).size(); i++) {
                    this.lambdaPosteriors.get(t).get(i).getNaturalParameters().sum(vector.getVectorByPosition(i));
                    this.lambdaPosteriors.get(t).get(i).getNaturalParameters().substract(this.initialPrior.getVectorByPosition(i));
                    this.lambdaPosteriors.get(t).get(i).updateMomentFromNaturalParameters();

                    this.omegaPosteriors.get(t).get(i).getNaturalParameters().sum(multiDriftSVB.getRhoPosterior().get(i).getNaturalParameters());
                    this.omegaPosteriors.get(t).get(i).getNaturalParameters().substract(multiDriftSVB.getRhoPrior().getNaturalParameters());
                    this.omegaPosteriors.get(t).get(i).updateMomentFromNaturalParameters();
                }
            }else{
                for (int i = 0; i < this.lambdaPosteriors.get(t).size(); i++) {
                    this.lambdaPosteriors.get(t).get(i).getNaturalParameters().copy(vector.getVectorByPosition(i));
                    this.lambdaPosteriors.get(t).get(i).updateMomentFromNaturalParameters();

                    this.omegaPosteriors.get(t).get(i).getNaturalParameters().copy(multiDriftSVB.getRhoPosterior().get(i).getNaturalParameters());
                    this.omegaPosteriors.get(t).get(i).updateMomentFromNaturalParameters();
                }
            }

        }
    }
    public double[] predictedLogLikelihood(){

        double[] testLL = new double[trainBatches.size()];
        for (int t = 0; t < testBatches.size(); t++) {
            CompoundVector prior = null;
            if (t > 0)
                prior = new CompoundVector(lambdaPosteriors.get(t - 1).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
            else
                prior = initialPrior;

            CompoundVector posterior = new CompoundVector(lambdaPosteriors.get(t).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
            multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(prior);
            multiDriftSVB.getPlateuStructure().updateNaturalParameterPosteriors(posterior);
            testLL[t]=multiDriftSVB.predictedLogLikelihood(testBatches.get(t));
        }
        return testLL;
    }

    public void setDAG(DAG dag) {
        this.multiDriftSVB.setDAG(dag);
    }

    public void setWindowsSize(int windowsSize) {
        this.multiDriftSVB.setWindowsSize(windowsSize);
    }

    public MultiDriftSVB getMultiDriftSVB() {
        return multiDriftSVB;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {


        int nStates = 100;

        int timeSteps = 2;

        Variables variables = new Variables();

        Variable multinomialVar = variables.newMultinomialVariable("N", nStates);

        BayesianNetwork bn = new BayesianNetwork(new DAG(variables));

        bn.randomInitialization(new Random(0));
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);


        int batchSize = 100;


        MultiDriftSVB_ArcReversal svb = new MultiDriftSVB_ArcReversal();
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setTestELBO(false);
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setMaxIter(100);
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setOutput(true);
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setThreshold(0.1);

        svb.setWindowsSize(batchSize);

        svb.setDAG(bn.getDAG());

        svb.initLearning();

        Multinomial multinomialDist = bn.getConditionalDistribution(multinomialVar);

        int k = 0;

        List<DataOnMemory<DataInstance>> testBatches = new ArrayList<>();
        Random rand = new Random(0);
        double preSmoothLog = 0;
        for (int i = 0; i < timeSteps; i++) {

            //sampler.setSeed(i);

            multinomialDist = bn.getConditionalDistribution(multinomialVar);

            /*if (i>=30){
                multinomialDist.setProbabilityOfState(1,0.5);
                multinomialDist.setProbabilityOfState(0, 0.5);
            } if (i>=60) {
                multinomialDist.setProbabilityOfState(1,0.2);
                multinomialDist.setProbabilityOfState(0, 0.8);
            }*/

            /*if (i%5==1) {
                System.out.println("CHANGE!!");
                double m = 100*rand.nextDouble();

                k = i%nStates;
                multinomialDist.setProbabilityOfState(k,m/(m+nStates));
                for (int j = 0; j < nStates; j++) {
                    if (k==j)
                        continue;
                    multinomialDist.setProbabilityOfState(j,1.0/(m+nStates));
                }
            }*/

            /*if (i%5==1) {
                System.out.println("CHANGE!!");
                double m = 10*rand.nextDouble()+1;

                k = 0;
                multinomialDist.setProbabilityOfState(k,m/(m+nStates));
                for (int j = 0; j < nStates; j++) {
                    if (k==j)
                        continue;
                    multinomialDist.setProbabilityOfState(j,1.0/(m+nStates));
                }
            }*/

            svb.aggregateTrainBatches(sampler.sampleToDataStream(batchSize).toDataOnMemory());


            DataOnMemory<DataInstance> testBatch = sampler.sampleToDataStream(batchSize).toDataOnMemory();
            svb.aggregateTestBatches(testBatch);


            double log=svb.getMultiDriftSVB().predictedLogLikelihood(testBatch);
            preSmoothLog+=log;

            System.out.println("Filter:\t" +i+ "\t" + log+"\t"+multinomialDist.getProbabilityOfState(k)+"\t"+svb.getMultiDriftSVB().getLearntBayesianNetwork().getConditionalDistribution(multinomialVar).getParameters()[k] +"\t"+((MultiDriftSVB)svb.getMultiDriftSVB()).getLambdaMomentParameters()[0]);//+"\t"+((MultiDriftSVB)svb).getLambdaMomentParameters()[1]);


        }

        svb.smooth();
        double[] testLL = svb.predictedLogLikelihood();
        for (int i = 0; i < timeSteps; i++) {
            System.out.println("Smoothed:\t" +i+ "\t" + testLL[i] +"\t"+multinomialDist.getProbabilityOfState(k)+"\t"+svb.getMultiDriftSVB().getLearntBayesianNetwork().getConditionalDistribution(multinomialVar).getParameters()[k] +"\t"+svb.getOmegaPosteriors().get(i).get(0).getExpectedParameters().get(0));//+"\t"+((MultiDriftSVB)svb).getLambdaMomentParameters()[1]);
        }

        System.out.println(preSmoothLog);
        System.out.println(Utils.sum(testLL));
    }
}
