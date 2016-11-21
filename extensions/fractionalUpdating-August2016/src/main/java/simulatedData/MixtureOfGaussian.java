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

package simulatedData;

import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.learning.parametric.bayesian.BayesianParameterLearningAlgorithm;
import eu.amidst.core.learning.parametric.bayesian.DriftSVB;
import eu.amidst.core.learning.parametric.bayesian.MultiDriftSVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import static simulatedData.StaticMethods.*;

/**
 * Created by andresmasegosa on 10/11/16.
 */
public class MixtureOfGaussian {


    public static void main(String[] args) {

        int nStates = 2;

        Variables variables = new Variables();

        Variable multinomialVar = variables.newMultinomialVariable("M",nStates);

        Variable normal = variables.newGaussianVariable("G");
        DAG dag = new DAG(variables);

        dag.getParentSet(normal).addParent(multinomialVar);

        BayesianNetwork bn = new BayesianNetwork(dag);


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);

        BayesianParameterLearningAlgorithm svb = initDrift();
        svb.setDAG(bn.getDAG());

        svb.setOutput(false);

        svb.setSeed(3);
        svb.initLearning();




        double total = 0;

        Multinomial multinomialDist = bn.getConditionalDistribution(multinomialVar);
        //multinomialDist.setProbabilityOfState(0,0.9);
        //multinomialDist.setProbabilityOfState(1,0.1);

        for (int i = 0; i < nStates; i++) {
            multinomialDist.setProbabilityOfState(i,1.0/nStates);

            Normal_MultinomialParents dist = bn.getConditionalDistribution(normal);

            dist.getNormal(i).setMean(i*5);
            dist.getNormal(i).setVariance(2);

        }

        System.out.println(bn);

        svb.updateModel(sampler.sampleToDataStream(sampleSize).toDataOnMemory());


        sampler.setHiddenVar(multinomialVar);


        System.out.println(svb.getLearntBayesianNetwork());


        for (int i = 0; i < totalITER; i++) {
            sampler.setSeed(i);

            multinomialDist = bn.getConditionalDistribution(multinomialVar);

            if (i%3==1) {
                double m = i+1;
                multinomialDist.setProbabilityOfState(0,m/(m+nStates));
                for (int j = 1; j < nStates; j++) {
                    multinomialDist.setProbabilityOfState(j,1.0/(m+nStates));
                }
            }


            if (svb.getClass().getName().compareTo("eu.amidst.core.learning.parametric.bayesian.DriftSVB")==0){
                ((DriftSVB)svb).updateModelWithConceptDrift(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                sampler.setSeed(10*i);

                double log=svb.predictedLogLikelihood(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                System.out.println(log+"\t"+multinomialDist.getProbabilityOfState(0)+"\t"+svb.getLearntBayesianNetwork().getConditionalDistribution(multinomialVar).getParameters()[0] +"\t"+((DriftSVB)svb).getLambdaValue());
                total+=log;

            }else if (svb.getClass().getName().compareTo("eu.amidst.core.learning.parametric.bayesian.MultiDriftSVB")==0){
                ((MultiDriftSVB)svb).updateModelWithConceptDrift(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                sampler.setSeed(10*i);

                double log=svb.predictedLogLikelihood(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                System.out.print(log+"\t"+multinomialDist.getProbabilityOfState(0)+"\t"+svb.getLearntBayesianNetwork().getConditionalDistribution(multinomialVar).getParameters()[0] +"\t");

                double[] labmdaValues = ((MultiDriftSVB)svb).getLambdaValues();
                for (int j = 0; j < labmdaValues.length; j++) {
                    System.out.print(labmdaValues[j]+"\t");
                }

                System.out.println();
                total+=log;
            }else{
                svb.updateModel(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                sampler.setSeed(10*i);

                double log=svb.predictedLogLikelihood(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                System.out.println(log+"\t"+multinomialDist.getProbabilityOfState(0)+"\t"+svb.getLearntBayesianNetwork().getConditionalDistribution(multinomialVar).getParameters()[0]);
                total+=log;
            }

            //System.out.println(svb.getLearntBayesianNetwork());

        }

        System.out.println(total);

    }
}
