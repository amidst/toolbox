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

package eu.amidst.lda.core;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.exponentialfamily.EF_Dirichlet;
import eu.amidst.core.exponentialfamily.EF_TruncatedExponential;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.exponentialfamily.ParameterVariables;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;
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
public class MultiDriftLDAv2 extends SVB{


    EF_TruncatedExponential ef_TExpP;
    EF_TruncatedExponential[] ef_TExpQ;

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

    public void setUpperInterval(double val) {
        this.ef_TExpP.setUpperInterval(val);
    }
    public void setLowerInterval(double val) {
        this.ef_TExpP.setLowerInterval(val);
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

        int size = prior.getNumberOfBaseVectors();
        this.ef_TExpQ = new EF_TruncatedExponential[size];
        for (int i = 0; i < size; i++) {
            this.ef_TExpQ[i] = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.getDelta());
        }
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
        this.ef_TExpP.getNaturalParameters().set(0,delta);
        this.ef_TExpP.updateMomentFromNaturalParameters();

        //Restart Truncated-Exp
        for (int i = 0; i < prior.getNumberOfBaseVectors(); i++) {
            this.ef_TExpQ[i] = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.getDelta());
            this.ef_TExpQ[i].setUpperInterval(this.ef_TExpP.getUpperInterval());
            this.ef_TExpQ[i].setLowerInterval(this.ef_TExpP.getLowerInterval());

        }

        boolean convergence = false;
        double elbo = Double.NaN;
        double niter=0;
        while(!convergence && niter<10) {

            //Messages for TExp to Theta
            double[] lambda = new double[prior.getNumberOfBaseVectors()];
            for (int i = 0; i < lambda.length; i++) {
                lambda[i] = this.ef_TExpQ[i].getMomentParameters().get(0);
            }
            CompoundVector newPrior = Serialization.deepCopy(prior);
            for (int i = 0; i < lambda.length; i++) {
                newPrior.getVectorByPosition(i).multiplyBy(1 - lambda[i]);
            }
            CompoundVector newPosterior = Serialization.deepCopy(posteriorT_1);
            for (int i = 0; i < lambda.length; i++) {
                newPosterior.getVectorByPosition(i).multiplyBy(lambda[i]);
            }
            newPrior.sum(newPosterior);
            this.plateuStructure.updateNaturalParameterPrior(newPrior);

            //if (niter==0)
            //    this.plateuStructure.resetQs();

            //Standard Messages
            //this.plateuStructure.getVMP().setMaxIter(10);
            this.plateuStructure.runInference();

            //Compute elbo
            double newELBO = this.plateuStructure.getLogProbabilityOfEvidence();


            double[] kl_q_p0 = new double[this.prior.getNumberOfBaseVectors()];
            int count = 0;
            //Messages to TExp
            this.plateuStructure.updateNaturalParameterPrior(this.prior);
            for (Node node : this.plateuStructure.getNonReplictedNodes().collect(Collectors.toList())) {
                Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                kl_q_p0[count] = local_kl((EF_Dirichlet)node.getQDist(),(EF_Dirichlet)node.getPDist());
                count++;
            }

            double[] kl_q_pt_1 = new double[this.prior.getNumberOfBaseVectors()];
            count = 0;
            //Messages to TExp
            this.plateuStructure.updateNaturalParameterPrior(this.posteriorT_1);
            for (Node node : this.plateuStructure.getNonReplictedNodes().collect(Collectors.toList())) {
                Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                kl_q_pt_1[count] = local_kl((EF_Dirichlet)node.getQDist(),(EF_Dirichlet)node.getPDist());
                count++;
            }

            for (int i = 0; i < ef_TExpQ.length; i++) {
                ef_TExpQ[i].getNaturalParameters().set(0,
                        - kl_q_pt_1[i] + kl_q_p0[i] +
                                this.ef_TExpP.getNaturalParameters().get(0));
                ef_TExpQ[i].fixNumericalInstability();
                ef_TExpQ[i].updateMomentFromNaturalParameters();


                //Elbo component assocaited to the truncated exponential.
                newELBO-=this.ef_TExpQ[i].kl(this.ef_TExpP.getNaturalParameters(),this.ef_TExpP.computeLogNormalizer());

            }

            if (!Double.isNaN(elbo) &&  newELBO<elbo){
                new IllegalStateException("Non increasing lower bound");
            }
            double percentageIncrease = 100*Math.abs((newELBO-elbo)/elbo);

            System.out.println("N Iter: " + niter + ", " + newELBO + ", "+ elbo + ", "+ percentageIncrease +", "+lambda[0]);

            if (!Double.isNaN(elbo) && percentageIncrease<this.plateuStructure.getVMP().getThreshold()){
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


    public static double  local_kl(EF_Dirichlet q, EF_Dirichlet p){

        double LIMIT = PlateauLDA.TOPIC_PRIOR;

        double kl = 0;
        int size = q.getNaturalParameters().size();
        int nstates = 0;
        for (int i = 0; i < size; i++) {
            double nqi = q.getNaturalParameters().get(i);
            double npi = p.getNaturalParameters().get(i);
            if (npi > (LIMIT+0.01) || nqi > (LIMIT+0.01)) {
                nstates++;
            }
        }

        ParameterVariables parameterVariables = new ParameterVariables(0);
        Variable var = parameterVariables.newDirichletParameter("Local",nstates);
        EF_Dirichlet localQ = new EF_Dirichlet(var);
        EF_Dirichlet localP = new EF_Dirichlet(var);


        int count =0;
        for (int i = 0; i < size; i++) {
            double nqi = q.getNaturalParameters().get(i);
            double npi = p.getNaturalParameters().get(i);

            if (npi > (LIMIT+0.01) || nqi > (LIMIT+0.01)) {
                localQ.getNaturalParameters().set(count,nqi);
                localP.getNaturalParameters().set(count,npi);
                count++;
            }

        }

        localP.fixNumericalInstability();
        localQ.fixNumericalInstability();
        localP.updateMomentFromNaturalParameters();
        localQ.updateMomentFromNaturalParameters();

        kl = localQ.kl(localP.getNaturalParameters(),localP.computeLogNormalizer());

        if (kl<0){
            System.out.println("ERROR");
        }

        return kl;
    }

    public double[] getLambdaMomentParameters(){

        double[] out = new double[this.prior.getNumberOfBaseVectors()];
        for (int i = 0; i < out.length; i++) {
            out[i] = this.ef_TExpQ[i].getMomentParameters().get(0);
        }

        return out;
    }

    public double[] getLambdaNaturalParameters(){

        double[] out = new double[this.prior.getNumberOfBaseVectors()];
        for (int i = 0; i < out.length; i++) {
            out[i] = this.ef_TExpQ[i].getNaturalParameters().get(0);
        }

        return out;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        BayesianNetwork oneNormalVarBN = BayesianNetworkLoader.loadFromFile("./networks/simulated/Normal.bn");

        System.out.println(oneNormalVarBN);
        int batchSize = 1000;


        MultiDriftLDAv2 svb = new MultiDriftLDAv2();
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
