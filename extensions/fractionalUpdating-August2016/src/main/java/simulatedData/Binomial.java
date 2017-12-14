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
import eu.amidst.core.learning.parametric.bayesian.BayesianParameterLearningAlgorithm;
import eu.amidst.core.learning.parametric.bayesian.DriftSVB;
import eu.amidst.core.learning.parametric.bayesian.MultiDriftSVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.util.Random;

import static simulatedData.StaticMethods.*;

/**
 * Created by andresmasegosa on 10/11/16.
 */
public class Binomial {


    public static void main(String[] args) {

        int nStates = 2;

        Variables variables = new Variables();

        Variable multinomialVar = variables.newMultinomialVariable("N",nStates);

        BayesianNetwork bn = new BayesianNetwork(new DAG(variables));

        bn.randomInitialization(new Random(0));
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);


        BayesianParameterLearningAlgorithm svb = initDrift();//initPopulation(0.01,100);

        svb.setDAG(bn.getDAG());

        svb.setOutput(false);

        svb.initLearning();

        svb.randomInitialize();

        double total = 0;

        Multinomial multinomialDist = bn.getConditionalDistribution(multinomialVar);
        multinomialDist.setProbabilityOfState(1,0.8);
        multinomialDist.setProbabilityOfState(0, 0.2);

        System.out.println(bn);

        System.out.println("LogLikelihood\t RealParameter \t Learnt Parameter \t [Lambda(s)]");

        for (int i = 0; i < totalITER; i++) {
            sampler.setSeed(i);

            multinomialDist = bn.getConditionalDistribution(multinomialVar);

            if (i>=30){
                multinomialDist.setProbabilityOfState(1,0.5);
                multinomialDist.setProbabilityOfState(0, 0.5);
            } if (i>=60) {
                multinomialDist.setProbabilityOfState(1,0.2);
                multinomialDist.setProbabilityOfState(0, 0.8);
            }

            /*if (i%5==1) {
                double m = i+1;
                multinomialDist.setProbabilityOfState(0,m/(m+nStates));
                for (int j = 1; j < nStates; j++) {
                    multinomialDist.setProbabilityOfState(j,1.0/(m+nStates));
                }
            }*/

            if (svb.getClass().getName().compareTo("eu.amidst.core.learning.parametric.bayesian.DriftSVB")==0){
//                if (i<10){
//                    ((DriftSVB)svb).updateModel(sampler.sampleToDataStream(sampleSize).toDataOnMemory());
//                    CompoundVector prior = ((DriftSVB) svb).getPlateuStructure().getPlateauNaturalParameterPrior();
//                    prior.multiplyBy(10);
//                    ((DriftSVB) svb).updateNaturalParameterPrior(prior);
//                }else{
//                    ((DriftSVB)svb).updateModelWithConceptDrift(sampler.sampleToDataStream(sampleSize).toDataOnMemory());
//                }
                ((DriftSVB)svb).updateModelWithConceptDrift(sampler.sampleToDataStream(sampleSize).toDataOnMemory());


                sampler.setSeed(10*i);

                double log=svb.predictedLogLikelihood(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                System.out.println(log+"\t"+multinomialDist.getProbabilityOfState(0)+"\t"+svb.getLearntBayesianNetwork().getConditionalDistribution(multinomialVar).getParameters()[0] +"\t"+((DriftSVB)svb).getLambdaMomentParameter());
                //System.out.println(((DriftSVB)svb).getPlateuStructure().getPlateauNaturalParameterPrior().sum());

                total+=log;

            }else if (svb.getClass().getName().compareTo("eu.amidst.core.learning.parametric.bayesian.MultiDriftSVB")==0){
                ((MultiDriftSVB)svb).updateModelWithConceptDrift(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                sampler.setSeed(10*i);

                double log=svb.predictedLogLikelihood(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                System.out.println(log+"\t"+multinomialDist.getProbabilityOfState(0)+"\t"+svb.getLearntBayesianNetwork().getConditionalDistribution(multinomialVar).getParameters()[0] +"\t"+((MultiDriftSVB)svb).getLambdaMomentParameters()[0]);//+"\t"+((MultiDriftSVB)svb).getLambdaMomentParameters()[1]);
                total+=log;

            }else{
                svb.updateModel(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                sampler.setSeed(10*i);

                double log=svb.predictedLogLikelihood(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                System.out.println(log+"\t"+multinomialDist.getProbabilityOfState(0)+"\t"+svb.getLearntBayesianNetwork().getConditionalDistribution(multinomialVar).getParameters()[0]+"\t");

                //System.out.println(((SVBFading)svb).getSVB().getPlateuStructure().getPlateauNaturalParameterPosterior().sum());
                total+=log;
            }

        }

        System.out.println(total);

    }
}
