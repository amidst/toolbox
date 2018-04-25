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
import eu.amidst.lda.core.MultiDriftLDAv1;
import eu.amidst.lda.core.MultiDriftLDAv2;

import static simulatedData.StaticMethods.*;

/**
 * Created by andresmasegosa on 10/11/16.
 */
public class MultinomialManyStates {


    public static void main(String[] args) {

        int nStates = 1000;

        Variables variables = new Variables();

        Variable multinomialVar = variables.newMultinomialVariable("N",nStates);

        BayesianNetwork bn = new BayesianNetwork(new DAG(variables));

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);


        BayesianParameterLearningAlgorithm svb = initMultiDriftLDAv2();

        svb.setDAG(bn.getDAG());

        svb.setOutput(false);

        svb.initLearning();

        svb.randomInitialize();

        double total = 0;

        Multinomial multinomialDist = bn.getConditionalDistribution(multinomialVar);
        for (int i = 0; i < nStates; i++) {
            multinomialDist.setProbabilityOfState(i,1.0/nStates);
        }

        System.out.println(bn);

        System.out.println("LogLikelihood\t RealParameter \t Learnt Parameter \t [Lambda(s)]");

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

                System.out.println(log+"\t"+multinomialDist.getProbabilityOfState(0)+"\t"+svb.getLearntBayesianNetwork().getConditionalDistribution(multinomialVar).getParameters()[0] +"\t"+((DriftSVB)svb).getLambdaMomentParameter());
                total+=log;

            }else if (svb.getClass().getName().compareTo("eu.amidst.core.learning.parametric.bayesian.MultiDriftSVB")==0){
                ((MultiDriftSVB)svb).updateModelWithConceptDrift(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                sampler.setSeed(10*i);

                double log=svb.predictedLogLikelihood(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                System.out.println(log+"\t"+multinomialDist.getProbabilityOfState(0)+"\t"+svb.getLearntBayesianNetwork().getConditionalDistribution(multinomialVar).getParameters()[0] +"\t"+((MultiDriftSVB)svb).getLambdaMomentParameters()[0]);//+"\t"+((MultiDriftSVB)svb).getLambdaMomentParameters()[1]);
                total+=log;

            }else if (svb.getClass().getName().compareTo("eu.amidst.lda.core.MultiDriftLDAv2")==0){
                ((MultiDriftLDAv2)svb).updateModelWithConceptDrift(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                sampler.setSeed(10*i);

                double log=svb.predictedLogLikelihood(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                System.out.println(log+"\t"+multinomialDist.getProbabilityOfState(0)+"\t"+svb.getLearntBayesianNetwork().getConditionalDistribution(multinomialVar).getParameters()[0] +"\t"+((MultiDriftLDAv2)svb).getLambdaMomentParameters()[0]);//+"\t"+((MultiDriftSVB)svb).getLambdaMomentParameters()[1]);
                total+=log;

            } else if (svb.getClass().getName().compareTo("eu.amidst.lda.core.MultiDriftLDAv1")==0){
                ((MultiDriftLDAv1)svb).updateModelWithConceptDrift(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                sampler.setSeed(10*i);

                double log=svb.predictedLogLikelihood(sampler.sampleToDataStream(sampleSize).toDataOnMemory());

                System.out.print(log+"\t"+multinomialDist.getProbabilityOfState(0)+"\t"+svb.getLearntBayesianNetwork().getConditionalDistribution(multinomialVar).getParameters()[0] +"\t");

                double[] lambdas = ((MultiDriftLDAv1)svb).getLambdaMomentParameters()[0];

                for (int j = 0; j < lambdas.length; j++) {
                    System.out.print(lambdas[j]+"\t");
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

        }

        System.out.println(total);

    }
}
