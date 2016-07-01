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

package eu.amidst.core.learning.parametric.bayesian;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.exponentialfamily.EF_TruncatedExponential;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 14/4/16.
 */
public class MultiDriftSVB extends SVB{


    EF_TruncatedExponential ef_TExpP;
    EF_TruncatedExponential ef_TExpQ;

    Variable truncatedExpVar;

    boolean firstBatch=true;

    CompoundVector posteriorT_1=null;

    CompoundVector prior=null;

    double delta = 0.1;

    public double getDelta() {
        return delta;
    }

    public void setDelta(double delta) {
        this.delta = delta;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initLearning() {
        super.initLearning();
        truncatedExpVar = new Variables().newTruncatedExponential("TruncatedExponentialVar");
        this.ef_TExpP = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.getDelta());
        this.ef_TExpQ = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.getDelta());
        firstBatch=true;
        prior = this.plateuStructure.getPlateauNaturalParameterPrior();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianNetwork getLearntBayesianNetwork() {

        CompoundVector prior = this.plateuStructure.getPlateauNaturalParameterPrior();

        this.updateNaturalParameterPrior(this.plateuStructure.getPlateauNaturalParameterPosterior());

        BayesianNetwork learntBN =  new BayesianNetwork(this.dag, ef_extendedBN.toConditionalDistribution());

        this.updateNaturalParameterPrior(prior);

        return learntBN;

    }

    public double updateModelWithConceptDrift(DataOnMemory<DataInstance> batch) {

        this.plateuStructure.setEvidence(batch.getList());

        if (firstBatch){
            firstBatch=false;
            this.plateuStructure.runInference();

            posteriorT_1 = this.plateuStructure.getPlateauNaturalParameterPosterior();
            return this.plateuStructure.getLogProbabilityOfEvidence();
        }

        //Restart Truncated-Exp
        this.ef_TExpQ = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.getDelta());

        boolean convergence = false;
        double elbo = Double.NaN;
        double niter=0;
        while(!convergence && niter<100) {

            //Messages for TExp to Theta
            double lambda = this.ef_TExpQ.getMomentParameters().get(0);
            CompoundVector newPrior = Serialization.deepCopy(prior);
            newPrior.multiplyBy(1 - lambda);
            CompoundVector newPosterior = Serialization.deepCopy(posteriorT_1);
            newPosterior.multiplyBy(lambda);
            newPrior.sum(newPosterior);
            this.plateuStructure.updateNaturalParameterPrior(newPrior);

            //Standard Messages
            //this.plateuStructure.getVMP().setMaxIter(10);
            this.plateuStructure.runInference();

            //Compute elbo
            double newELBO = this.plateuStructure.getLogProbabilityOfEvidence();

            double[] kl_q_p0_vals = new double[(int)this.plateuStructure.getNonReplictedNodes().count()];
            double[] kl_q_pt_1_vals = new double[(int)this.plateuStructure.getNonReplictedNodes().count()];



            double kl_q_p0 =0;
            int count = 0;
            //Messages to TExp
            this.plateuStructure.updateNaturalParameterPrior(this.prior);
            for (Node node : this.plateuStructure.getNonReplictedNodes().collect(Collectors.toList())) {
                Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                kl_q_p0_vals[count] = node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                        node.getPDist().getExpectedLogNormalizer(momentParents));
                kl_q_p0+=kl_q_p0_vals[count];
                count++;
            }

//            double kl_q_p0 = this.plateuStructure.getNonReplictedNodes().mapToDouble(node-> {
//                Map<Variable, MomentParameters> momentParents = node.getMomentParents();
//                return node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
//                        node.getPDist().getExpectedLogNormalizer(momentParents));
//            }).sum();

            double kl_q_pt_1 =0;
            count = 0;
            //Messages to TExp
            this.plateuStructure.updateNaturalParameterPrior(this.posteriorT_1);
            for (Node node : this.plateuStructure.getNonReplictedNodes().collect(Collectors.toList())) {
                Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                kl_q_pt_1_vals[count] = node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                        node.getPDist().getExpectedLogNormalizer(momentParents));
                kl_q_pt_1+=kl_q_pt_1_vals[count];
                count++;
            }

//            this.plateuStructure.updateNaturalParameterPrior(this.posteriorT_1);
//            double kl_q_pt_1 = this.plateuStructure.getNonReplictedNodes().mapToDouble(node-> {
//                Map<Variable, MomentParameters> momentParents = node.getMomentParents();
//                return node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
//                        node.getPDist().getExpectedLogNormalizer(momentParents));
//            }).sum();

            ef_TExpQ.getNaturalParameters().set(0,
                    - kl_q_pt_1 + kl_q_p0 +
                    this.ef_TExpP.getNaturalParameters().get(0));
            ef_TExpQ.fixNumericalInstability();
            ef_TExpQ.updateMomentFromNaturalParameters();


            //Elbo component assocaited to the truncated exponential.
            newELBO-=this.ef_TExpQ.kl(this.ef_TExpP.getNaturalParameters(),this.ef_TExpP.computeLogNormalizer());

            if (!Double.isNaN(elbo) &&  newELBO<elbo){
                new IllegalStateException("Non increasing lower bound");
            }
            double percentageIncrease = 100*Math.abs((newELBO-elbo)/elbo);

            System.out.print("Delta: " + niter + ", " + newELBO + ", "+ elbo + ", "+ percentageIncrease +", "+lambda+ ", " + (- kl_q_pt_1 + kl_q_p0));

            for (int i = 0; i < kl_q_p0_vals.length; i++) {
                System.out.print(", "+ (- kl_q_pt_1_vals[i] + kl_q_p0_vals[i]));
            }
            System.out.println();

            if (!Double.isNaN(elbo) && percentageIncrease<this.plateuStructure.getVMP().getThreshold()){
                convergence=true;
            }

            elbo=newELBO;
            niter++;
        }

        posteriorT_1 = this.plateuStructure.getPlateauNaturalParameterPosterior();


        return elbo;
    }


    public double getLambdaValue(){
        return this.ef_TExpQ.getMomentParameters().get(0);
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
