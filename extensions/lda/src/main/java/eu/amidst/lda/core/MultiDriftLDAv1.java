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
import eu.amidst.core.exponentialfamily.ParameterVariables;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import org.apache.commons.math3.special.Gamma;

import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 14/4/16.
 */
public class MultiDriftLDAv1 extends SVB{


    EF_TruncatedExponential ef_TExpP;
    EF_TruncatedExponential[][] ef_TExpQ;

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
    int maxsize = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initLearning() {
        super.initLearning();
        truncatedExpVar = new Variables().newTruncatedExponential("TruncatedExponentialVar");
        this.ef_TExpP = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.getDelta());

        prior = this.plateuStructure.getPlateauNaturalParameterPrior();

        for (Variable variable : this.plateuStructure.getNonReplicatedVariables()) {
            if (variable.getNumberOfStates()>maxsize)
                maxsize=variable.getNumberOfStates();
        }

        this.ef_TExpQ = new EF_TruncatedExponential[prior.getNumberOfBaseVectors()][maxsize];
        for (int i = 0; i < this.ef_TExpQ.length; i++) {
            for (int j = 0; j < this.ef_TExpQ[i].length; j++) {
                this.ef_TExpQ[i][j] = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.getDelta());
            }
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
        for (int i = 0; i < this.ef_TExpQ.length; i++) {
            for (int j = 0; j < this.ef_TExpQ[i].length; j++) {
                this.ef_TExpQ[i][j] = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.getDelta());
                this.ef_TExpQ[i][j].setUpperInterval(this.ef_TExpP.getUpperInterval());
                this.ef_TExpQ[i][j].setLowerInterval(this.ef_TExpP.getLowerInterval());
            }
        }

        boolean convergence = false;
        double elbo = Double.NaN;
        double niter=0;
        while(!convergence && niter<10) {

            //Messages for TExp to Theta
            double[][] lambda = new double[this.ef_TExpQ.length][maxsize];
            for (int i = 0; i < lambda.length; i++) {
                for (int j = 0; j < lambda[i].length; j++) {
                    lambda[i][j] = this.ef_TExpQ[i][j].getMomentParameters().get(0);
                }
                /*int minIndex = Utils.minIndex(lambda[i]);
                for (int j = 0; j < lambda[i].length; j++) {
                    lambda[i][j] = lambda[i][minIndex];
                }*/
            }

            CompoundVector newPrior = Serialization.deepCopy(prior);
            for (int i = 0; i < lambda.length; i++) {
                for (int j = 0; j < lambda[i].length; j++) {
                    newPrior.getVectorByPosition(i).set(j,newPrior.getVectorByPosition(i).get(j)*(1-lambda[i][j]));
                }
            }
            CompoundVector newPosterior = Serialization.deepCopy(posteriorT_1);

            for (int i = 0; i < lambda.length; i++) {
                for (int j = 0; j < lambda[i].length; j++) {
                    newPosterior.getVectorByPosition(i).set(j,newPosterior.getVectorByPosition(i).get(j)*(lambda[i][j]));
                }
            }

            newPrior.sum(newPosterior);
            this.plateuStructure.updateNaturalParameterPrior(newPrior);

            if (niter==0)
                this.plateuStructure.resetQs();

            //Standard Messages
            //this.plateuStructure.getVMP().setMaxIter(10);
            this.plateuStructure.runInference();

            //Compute elbo
            double newELBO = this.plateuStructure.getLogProbabilityOfEvidence();


            double[][] kl_q_p0 = new double[this.ef_TExpQ.length][maxsize];
            int count = 0;
            //Messages to TExp
            this.plateuStructure.updateNaturalParameterPrior(this.prior);
            for (Node node : this.plateuStructure.getNonReplictedNodes().collect(Collectors.toList())) {

                EF_Dirichlet dirichletQ = (EF_Dirichlet)node.getQDist();
                EF_Dirichlet dirichletP = (EF_Dirichlet)node.getPDist();
                double[] localKL = computeLocalKLDirichletBinary(dirichletQ,dirichletP);

                for (int i = 0; i < localKL.length; i++) {
                    kl_q_p0[count][i] = localKL[i];
                }
                count++;
            }

            double[][] kl_q_pt_1 = new double[this.ef_TExpQ.length][maxsize];
            count = 0;
            //Messages to TExp
            this.plateuStructure.updateNaturalParameterPrior(this.posteriorT_1);
            for (Node node : this.plateuStructure.getNonReplictedNodes().collect(Collectors.toList())) {

                EF_Dirichlet dirichletQ = (EF_Dirichlet)node.getQDist();
                EF_Dirichlet dirichletP = (EF_Dirichlet)node.getPDist();
                double[] localKL = computeLocalKLDirichletBinary(dirichletQ,dirichletP);

                for (int i = 0; i < localKL.length; i++) {
                    kl_q_pt_1[count][i] = localKL[i];
                }
                count++;
            }

            for (int i = 0; i < ef_TExpQ.length; i++) {
                for (int j = 0; j < ef_TExpQ[i].length; j++) {
                    ef_TExpQ[i][j].getNaturalParameters().set(0,
                            - kl_q_pt_1[i][j] + kl_q_p0[i][j] +
                                    this.ef_TExpP.getNaturalParameters().get(0));
                    ef_TExpQ[i][j].fixNumericalInstability();
                    ef_TExpQ[i][j].updateMomentFromNaturalParameters();


                    //Elbo component assocaited to the truncated exponential.
                    newELBO-=this.ef_TExpQ[i][j].kl(this.ef_TExpP.getNaturalParameters(),this.ef_TExpP.computeLogNormalizer());
                }
            }

            if (!Double.isNaN(elbo) &&  newELBO<elbo){
                new IllegalStateException("Non increasing lower bound");
            }
            double percentageIncrease = 100*Math.abs((newELBO-elbo)/elbo);

            System.out.println("N Iter: " + niter + ", " + newELBO + ", "+ elbo + ", "+ percentageIncrease +", "+lambda[0][0]);

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

    public static double[] computeLocalKLDirichlet(EF_Dirichlet dirichletQ, EF_Dirichlet dirichletP) {

        double[] localKL = new double[dirichletP.sizeOfSufficientStatistics()];

        int nstates = localKL.length;
        double sumCountsQ = dirichletQ.getNaturalParameters().sum();
        double sumCountsP = dirichletP.getNaturalParameters().sum();

        for (int i = 0; i < localKL.length; i++) {
            double alphaQ = dirichletQ.getNaturalParameters().get(i);
            double alphaP = dirichletP.getNaturalParameters().get(i);

            double kl =  alphaQ - alphaP;
            kl*=dirichletQ.getMomentParameters().get(i);

            kl-=(Gamma.logGamma(alphaQ) - Gamma.logGamma(sumCountsQ)/nstates);
            kl+=(Gamma.logGamma(alphaP) - Gamma.logGamma(sumCountsP)/nstates);

            localKL[i] = kl;
        }


        return localKL;
    }

    public static double[] computeLocalKLDirichletBinary(EF_Dirichlet dirichletQ, EF_Dirichlet dirichletP) {

        double[] localKL = new double[dirichletP.sizeOfSufficientStatistics()];

        int nstates = localKL.length;
        double sumCountsQ = dirichletQ.getNaturalParameters().sum();
        double sumCountsP = dirichletP.getNaturalParameters().sum();

        ParameterVariables parameterVariables = new ParameterVariables(0);
        Variable binary = parameterVariables.newDirichletParameter("Local",2);
        EF_Dirichlet localQ = new EF_Dirichlet(binary);
        EF_Dirichlet localP = new EF_Dirichlet(binary);

        for (int i = 0; i < localKL.length; i++) {


            double alphaQ = dirichletQ.getNaturalParameters().get(i);
            double alphaP = dirichletP.getNaturalParameters().get(i);

            localQ.getNaturalParameters().set(0,alphaQ);
            localQ.getNaturalParameters().set(1,sumCountsQ-alphaQ);
            localQ.fixNumericalInstability();
            localQ.updateMomentFromNaturalParameters();

            localP.getNaturalParameters().set(0,alphaP);
            localP.getNaturalParameters().set(1,sumCountsP-alphaP);
            localP.fixNumericalInstability();
            localP.updateMomentFromNaturalParameters();

            localKL[i] = localQ.kl(localP.getNaturalParameters(),localP.computeLogNormalizer());

        }


        return localKL;
    }

    public double[][] getLambdaMomentParameters(){

        double[][] out = new double[this.ef_TExpQ.length][maxsize];
        for (int i = 0; i < out.length; i++) {
            for (int j = 0; j < out[i].length; j++) {
                out[i][j] = this.ef_TExpQ[i][j].getMomentParameters().get(0);
            }
        }

        return out;
    }

    public double[][] getLambdaNaturalParameters(){

        double[][] out = new double[this.ef_TExpQ.length][maxsize];
        for (int i = 0; i < out.length; i++) {
            for (int j = 0; j < out[i].length; j++) {
                out[i][j] = this.ef_TExpQ[i][j].getNaturalParameters().get(0);
            }
        }

        return out;
    }

}
