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

/**
 * Created by andresmasegosa on 14/4/16.
 */
public class DriftSVB extends SVB{


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

        prior = this.plateuStructure.getPlateauNaturalParameterPrior();

        this.ef_TExpQ = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.getDelta());
        firstBatch=true;
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

        //System.out.println("SAMPLE:" + this.plateuStructure.getPosteriorSampleSize());

        this.plateuStructure.setEvidence(batch.getList());

        if (firstBatch){
            firstBatch=false;
            this.plateuStructure.runInference();

            posteriorT_1 = this.plateuStructure.getPlateauNaturalParameterPosterior();
            this.plateuStructure.updateNaturalParameterPrior(posteriorT_1);
            return this.plateuStructure.getLogProbabilityOfEvidence();
        }

        //Restart Truncated-Exp
        double delta = this.getDelta();//-this.plateuStructure.getPosteriorSampleSize()*0.1;
        this.ef_TExpP.getNaturalParameters().set(0,-delta);
        this.ef_TExpP.updateMomentFromNaturalParameters();

        this.ef_TExpQ.getNaturalParameters().set(0,-delta);
        this.ef_TExpQ.updateMomentFromNaturalParameters();

        boolean convergence = false;
        double elbo = Double.NaN;
        double niter=0;
        while(!convergence && niter<10) {

            //Messages for TExp to Theta
            double lambda = this.ef_TExpQ.getMomentParameters().get(0);



            CompoundVector newPrior = Serialization.deepCopy(prior);
            newPrior.multiplyBy(1 - lambda);


            CompoundVector newPosterior = Serialization.deepCopy(posteriorT_1);
            newPosterior.multiplyBy(lambda);


            newPrior.sum(newPosterior);
            this.plateuStructure.updateNaturalParameterPrior(newPrior);

            if (niter==0)
                this.plateuStructure.resetQs();

            //Standard Messages
            //this.plateuStructure.getVMP().setMaxIter(10);
            this.plateuStructure.runInference();

            //Compute elbo
            double newELBO = this.plateuStructure.getLogProbabilityOfEvidence();

            //Messages to TExp
            this.plateuStructure.updateNaturalParameterPrior(this.prior);
            double kl_q_p0 = this.plateuStructure.getNonReplictedNodes().mapToDouble(node-> {
                Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                return node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                        node.getPDist().getExpectedLogNormalizer(momentParents));
            }).sum();

            this.plateuStructure.updateNaturalParameterPrior(this.posteriorT_1);
            double kl_q_pt_1 = this.plateuStructure.getNonReplictedNodes().mapToDouble(node-> {
                Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                return node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                        node.getPDist().getExpectedLogNormalizer(momentParents));
            }).sum();

            //System.out.println("DIFF: " + (- kl_q_pt_1 + kl_q_p0));
            //System.out.println("DIFF2: " +  ef_TExpQ.getNaturalParameters().get(0));

            ef_TExpQ.getNaturalParameters().set(0,
                    - kl_q_pt_1 + kl_q_p0 +
                    this.ef_TExpP.getNaturalParameters().get(0));
            ef_TExpQ.fixNumericalInstability();
            ef_TExpQ.updateMomentFromNaturalParameters();

            //Elbo component assocaited to the truncated exponential.
            newELBO-=this.ef_TExpQ.kl(this.ef_TExpP.getNaturalParameters(),this.ef_TExpP.computeLogNormalizer());

            if (!Double.isNaN(elbo) &&  newELBO<elbo){
                new IllegalStateException("Non increasing lower bound: " + newELBO + " < " + elbo);
            }
            double percentageIncrease = 100*Math.abs((newELBO-elbo)/elbo);

            //System.out.println("N Iter: " + niter + ", " + newELBO + ", "+ elbo + ", "+ percentageIncrease +", "+lambda);

            if (!Double.isNaN(elbo) && percentageIncrease<0.0001){//this.plateuStructure.getVMP().getThreshold()){
                convergence=true;
            }

            elbo=newELBO;
            niter++;
        }


        //System.out.println("end");


        posteriorT_1 = this.plateuStructure.getPlateauNaturalParameterPosterior();

        this.plateuStructure.updateNaturalParameterPrior(posteriorT_1);


        return elbo;
    }


    public double getLambdaMomentParameter(){
        return this.ef_TExpQ.getMomentParameters().get(0);
    }

    public double getLambdaNaturalParameter(){
        return this.ef_TExpQ.getNaturalParameters().get(0);
    }
    
    public static void main(String[] args) throws IOException, ClassNotFoundException {

        BayesianNetwork oneNormalVarBN = BayesianNetworkLoader.loadFromFile("./networks/simulated/Normal.bn");

        System.out.println(oneNormalVarBN);
        int batchSize = 1000;


        DriftSVB svb = new DriftSVB();
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
