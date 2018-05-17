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

package textJournalTopWords;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.exponentialfamily.EF_TruncatedExponential;
import eu.amidst.core.exponentialfamily.EF_TruncatedNormal;
import eu.amidst.core.exponentialfamily.EF_TruncatedUnivariateDistribution;
import eu.amidst.core.exponentialfamily.MomentParameters;
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

import static eu.amidst.core.learning.parametric.bayesian.DriftSVB.TRUNCATED_EXPONENTIAL;
import static eu.amidst.core.learning.parametric.bayesian.DriftSVB.TRUNCATED_NORMAL;

/**
 * Created by andresmasegosa on 14/4/16.
 */
public class MultiDriftSVB_EB extends SVB {

    EF_TruncatedUnivariateDistribution[] ef_TExpP;
    EF_TruncatedUnivariateDistribution[] ef_TExpQ;

    Variable truncatedExpVar;

    boolean firstBatch=true;

    CompoundVector posteriorT_1=null;

    CompoundVector prior=null;

    double delta = 0.1;

    double[] hppVal = {-0.1};
    int type = TRUNCATED_EXPONENTIAL;

    //double[] hppVal = {0.1, 1};
    //int type = TRUNCATED_NORMAL;

    public void setPriorDistribution(int type, double[] val) {
        this.type = type;
        this.hppVal = val;
    }

    public double getDelta() {
        return delta;
    }

    public void setDelta(double delta) {
        this.delta = delta;
    }

    public void setUpperInterval(double val) {

        for (int i = 0; i < ef_TExpP.length; i++) {
            this.ef_TExpP[i].setUpperInterval(val);
        }

    }

    public void setLowerInterval(double val) {
        for (int i = 0; i < ef_TExpP.length; i++) {
            this.ef_TExpP[i].setLowerInterval(val);
        }
    }



    public void initHPP(int size){
        if (type == TRUNCATED_EXPONENTIAL) {
            truncatedExpVar = new Variables().newTruncatedExponential("TruncatedExponentialVar");
            this.ef_TExpP = new EF_TruncatedExponential[size];
            this.ef_TExpQ = new EF_TruncatedExponential[size];
            for (int i = 0; i < size; i++) {
                this.ef_TExpP[i] = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.hppVal[0]);
                this.ef_TExpQ[i] = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.hppVal[0]);
            }
        } else if (type == TRUNCATED_NORMAL) {
            truncatedExpVar = new Variables().newTruncatedNormal("TruncatedNormalVar");
            this.ef_TExpP = new EF_TruncatedUnivariateDistribution[size];
            this.ef_TExpQ = new EF_TruncatedUnivariateDistribution[size];
            for (int i = 0; i < size; i++) {
                this.ef_TExpP[i] = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.hppVal[0], this.hppVal[1]);
                this.ef_TExpQ[i] = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(this.hppVal[0], this.hppVal[1]);
            }
        } else {
            throw new IllegalArgumentException("No prior defined");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initLearning() {
        super.initLearning();

        prior = this.plateuStructure.getPlateauNaturalParameterPrior();

        this.initHPP(prior.getNumberOfBaseVectors());

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
        for (int i = 0; i < prior.getNumberOfBaseVectors(); i++) {
            for (int j = 0; j < this.hppVal.length; j++) {
                this.ef_TExpQ[i].getNaturalParameters().set(j,this.ef_TExpP[i].getNaturalParameters().get(j));
            }
            this.ef_TExpQ[i].setUpperInterval(this.ef_TExpP[i].getUpperInterval());
            this.ef_TExpQ[i].setLowerInterval(this.ef_TExpP[i].getLowerInterval());
            this.ef_TExpQ[i].updateMomentFromNaturalParameters();
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

            if (niter==0) {
                Random random = new Random(0);
                this.plateuStructure.getReplicatedNodes().filter(node -> !node.isObserved()).forEach(node -> node.resetQDist(random));
            }

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
                kl_q_p0[count] = node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                        node.getPDist().getExpectedLogNormalizer(momentParents));
                count++;
            }

            double[] kl_q_pt_1 = new double[this.prior.getNumberOfBaseVectors()];
            count = 0;
            //Messages to TExp
            this.plateuStructure.updateNaturalParameterPrior(this.posteriorT_1);
            for (Node node : this.plateuStructure.getNonReplictedNodes().collect(Collectors.toList())) {
                Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                kl_q_pt_1[count] = node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                        node.getPDist().getExpectedLogNormalizer(momentParents));
                count++;
            }

            for (int i = 0; i < ef_TExpQ.length; i++) {
                ef_TExpQ[i].getNaturalParameters().set(0,
                        - kl_q_pt_1[i] + kl_q_p0[i] +
                                this.ef_TExpP[i].getNaturalParameters().get(0));
                for (int j = 1; j < this.hppVal.length; j++) {
                    this.ef_TExpQ[i].getNaturalParameters().set(j,this.ef_TExpP[i].getNaturalParameters().get(j));
                }
                ef_TExpQ[i].fixNumericalInstability();
                ef_TExpQ[i].updateMomentFromNaturalParameters();


                //Elbo component assocaited to the truncated exponential.
                newELBO-=this.ef_TExpQ[i].kl(this.ef_TExpP[i].getNaturalParameters(),this.ef_TExpP[i].computeLogNormalizer());

            }

            if (!Double.isNaN(elbo) &&  newELBO<elbo){
                new IllegalStateException("Non increasing lower bound");
            }
            double percentageIncrease = 100*Math.abs((newELBO-elbo)/elbo);

            if (activateOutput) System.out.print("N Iter: " + niter + ", " + newELBO + ", "+ elbo + ", "+ percentageIncrease);
            if (activateOutput && this.type == TRUNCATED_NORMAL){
                for (int i = 0; i < lambda.length; i++) {
                    System.out.print(", "+ lambda[i] + " - " + ((EF_TruncatedNormal)ef_TExpQ[i]).getPrecision());
                }
            }

            if (activateOutput) System.out.println();
            if (activateOutput) System.out.print("KL: " + niter + ", " + newELBO + ", "+ elbo + ", "+ percentageIncrease);
            for (int i = 0; i < lambda.length; i++) {
                if (activateOutput) System.out.print(", "+ kl_q_p0[i] + " - " + kl_q_pt_1[i]);
            }
            if (activateOutput) System.out.println();
            if (!Double.isNaN(elbo) && percentageIncrease<this.plateuStructure.getVMP().getThreshold()){
                convergence=true;
            }

            elbo=newELBO;
            niter++;

            if (this.type == TRUNCATED_NORMAL) if (niter>0) learningP_Precision(kl_q_p0,kl_q_pt_1);

            convergence=false;
        }

        //System.out.println("end");

        posteriorT_1 = this.plateuStructure.getPlateauNaturalParameterPosterior();

        this.plateuStructure.updateNaturalParameterPrior(posteriorT_1);


        return elbo;
    }
    private void learningP_Natural(double[] kl_q_p0, double[] kl_q_pt_1){
        for (int i = 0; i < ef_TExpP.length; i++) {
            double learningRate=1;
            double mean = ((EF_TruncatedNormal)ef_TExpP[i]).getMean();
            double precision = ((EF_TruncatedNormal)ef_TExpP[i]).getPrecision();

            double kl = ef_TExpP[i].getNaturalParameters().dotProduct(ef_TExpQ[i].getMomentParameters()) - ef_TExpP[i].computeLogNormalizer();

            System.out.println(((EF_TruncatedNormal)ef_TExpQ[i]).getPrecision() + " : " + ef_TExpQ[i].getMomentParameters().get(0) +" : " + " : " + kl + " : " + learningRate);

            boolean local_convergence = false;
            for (int iter = 0; iter < 1000 && !local_convergence; iter++) {

                //double gradient =  -0.5*(ef_TExpQ[i].getMomentParameters().get(1) - ef_TExpP[i].getMomentParameters().get(1)) + mean*(ef_TExpQ[i].getMomentParameters().get(0) - ef_TExpP[i].getMomentParameters().get(0));
                double gradient = ef_TExpQ[i].getMomentParameters().get(1) - ef_TExpP[i].getMomentParameters().get(1);
                while (precision+learningRate*gradient<=0.0){
                    learningRate/=2;
                }
                precision += learningRate*gradient;
                ((EF_TruncatedNormal)ef_TExpP[i]).getNaturalParameters().set(1,precision);
                ef_TExpP[i].updateMomentFromNaturalParameters();

                double kl_new = ef_TExpP[i].getNaturalParameters().dotProduct(ef_TExpQ[i].getMomentParameters()) - ef_TExpP[i].computeLogNormalizer();



                if (learningRate<0.00001) {
                    local_convergence = true;
                }else if (kl_new<kl){
                    precision -= learningRate*gradient;
                    ((EF_TruncatedNormal)ef_TExpP[i]).setNaturalWithMeanPrecision(mean,precision);
                    ef_TExpP[i].updateMomentFromNaturalParameters();

                    learningRate/=2;
                }else {
                    kl = kl_new;
                }
            }
            ef_TExpQ[i].getNaturalParameters().set(0,
                    - kl_q_pt_1[i] + kl_q_p0[i] +
                            this.ef_TExpP[i].getNaturalParameters().get(0));
            ef_TExpQ[i].getNaturalParameters().set(1,this.ef_TExpP[i].getNaturalParameters().get(1));
            ef_TExpQ[i].fixNumericalInstability();
            ef_TExpQ[i].updateMomentFromNaturalParameters();
            System.out.println(((EF_TruncatedNormal)ef_TExpQ[i]).getPrecision() + " : " + ef_TExpQ[i].getMomentParameters().get(0) +" : " + " : " + kl + " : " + learningRate);

        }
    }

    private void learningP_Precision(double[] kl_q_p0, double[] kl_q_pt_1){
        for (int i = 0; i < ef_TExpP.length; i++) {
            double learningRate=1;
            double mean = ((EF_TruncatedNormal)ef_TExpP[i]).getMean();
            double precision = ((EF_TruncatedNormal)ef_TExpP[i]).getPrecision();

            double kl = ef_TExpP[i].getNaturalParameters().dotProduct(ef_TExpQ[i].getMomentParameters()) - ef_TExpP[i].computeLogNormalizer();

            if (activateOutput) System.out.println(((EF_TruncatedNormal)ef_TExpQ[i]).getPrecision() + " : " + ef_TExpQ[i].getMomentParameters().get(0) +" : " + " : " + kl + " : " + learningRate);

            boolean local_convergence = false;
            for (int iter = 0; iter < 1000 && !local_convergence; iter++) {

                double gradient =  -0.5*(ef_TExpQ[i].getMomentParameters().get(1) - ef_TExpP[i].getMomentParameters().get(1)) + mean*(ef_TExpQ[i].getMomentParameters().get(0) - ef_TExpP[i].getMomentParameters().get(0));
                while (precision+learningRate*gradient<=0.0){
                    learningRate/=2;
                }
                precision += learningRate*gradient;
                ((EF_TruncatedNormal)ef_TExpP[i]).setNaturalWithMeanPrecision(mean,precision);
                ef_TExpP[i].updateMomentFromNaturalParameters();

                double kl_new = ef_TExpP[i].getNaturalParameters().dotProduct(ef_TExpQ[i].getMomentParameters()) - ef_TExpP[i].computeLogNormalizer();



                if (learningRate<0.001) {
                    local_convergence = true;
                }else if (kl_new<kl){
                    precision -= learningRate*gradient;
                    ((EF_TruncatedNormal)ef_TExpP[i]).setNaturalWithMeanPrecision(mean,precision);
                    ef_TExpP[i].updateMomentFromNaturalParameters();

                    learningRate/=2;
                }else {
                    kl = kl_new;
                }
            }
            ef_TExpQ[i].getNaturalParameters().set(0,
                    - kl_q_pt_1[i] + kl_q_p0[i] +
                            this.ef_TExpP[i].getNaturalParameters().get(0));
            ef_TExpQ[i].getNaturalParameters().set(1,this.ef_TExpP[i].getNaturalParameters().get(1));
            ef_TExpQ[i].fixNumericalInstability();
            ef_TExpQ[i].updateMomentFromNaturalParameters();
            System.out.println(((EF_TruncatedNormal)ef_TExpQ[i]).getPrecision() + " : " + ef_TExpQ[i].getMomentParameters().get(0) +" : " + " : " + kl + " : " + learningRate);

        }
    }

    private void learningPandQ(double[] kl_q_p0, double[] kl_q_pt_1){
        for (int i = 0; i < ef_TExpP.length; i++) {
            double learningRate=100;
            double mean = ((EF_TruncatedNormal)ef_TExpP[i]).getMean();
            double precision = ((EF_TruncatedNormal)ef_TExpP[i]).getPrecision();

            double local_elbo = - ef_TExpQ[i].getMomentParameters().get(0)*kl_q_pt_1[i] - (1-ef_TExpQ[i].getMomentParameters().get(0))*kl_q_p0[i];
            double kl = ef_TExpQ[i].getNaturalParameters().dotProduct(ef_TExpQ[i].getMomentParameters())-ef_TExpQ[i].computeLogNormalizer() - ef_TExpP[i].getNaturalParameters().dotProduct(ef_TExpQ[i].getMomentParameters()) + ef_TExpP[i].computeLogNormalizer();
            local_elbo-=kl;

            boolean local_convergence = false;
            for (int iter = 0; iter < 1000 && !local_convergence; iter++) {
                double gradient =  -0.5*(ef_TExpQ[i].getMomentParameters().get(1) - ef_TExpP[i].getMomentParameters().get(1)) + mean*(ef_TExpQ[i].getMomentParameters().get(0) - ef_TExpP[i].getMomentParameters().get(0));
                precision += learningRate*gradient;
                ((EF_TruncatedNormal)ef_TExpP[i]).setNaturalWithMeanPrecision(mean,precision);
                ef_TExpP[i].updateMomentFromNaturalParameters();

                ef_TExpQ[i].getNaturalParameters().set(0,
                        - kl_q_pt_1[i] + kl_q_p0[i] +
                                this.ef_TExpP[i].getNaturalParameters().get(0));
                ef_TExpQ[i].getNaturalParameters().set(1,this.ef_TExpP[i].getNaturalParameters().get(1));
                ef_TExpQ[i].fixNumericalInstability();
                ef_TExpQ[i].updateMomentFromNaturalParameters();


                double local_elbo_new = - ef_TExpQ[i].getMomentParameters().get(0)*kl_q_pt_1[i] - (1-ef_TExpQ[i].getMomentParameters().get(0))*kl_q_p0[i];
                kl = ef_TExpQ[i].getNaturalParameters().dotProduct(ef_TExpQ[i].getMomentParameters())-ef_TExpQ[i].computeLogNormalizer() - ef_TExpP[i].getNaturalParameters().dotProduct(ef_TExpQ[i].getMomentParameters()) + ef_TExpP[i].computeLogNormalizer();
                local_elbo_new-=kl;

                System.out.println(precision + " : " + ef_TExpQ[i].getMomentParameters().get(0) +" : " + gradient + " : " + local_elbo_new + " : " + learningRate);

                if (Double.isInfinite(local_elbo_new)) {
                    System.out.println();
                    System.out.println(ef_TExpQ[i].computeLogNormalizer());
                }
                if (local_elbo_new<local_elbo){
                    precision -= learningRate*gradient;
                    ((EF_TruncatedNormal)ef_TExpP[i]).setNaturalWithMeanPrecision(mean,precision);
                    ef_TExpP[i].updateMomentFromNaturalParameters();
                    ef_TExpQ[i].getNaturalParameters().set(0,
                            - kl_q_pt_1[i] + kl_q_p0[i] +
                                    this.ef_TExpP[i].getNaturalParameters().get(0));
                    ef_TExpQ[i].getNaturalParameters().set(1,this.ef_TExpP[i].getNaturalParameters().get(1));
                    ef_TExpQ[i].fixNumericalInstability();
                    ef_TExpQ[i].updateMomentFromNaturalParameters();

                    learningRate/=2;
                }else if (Math.abs(gradient)<0.001) {
                    local_convergence = true;
                }

                local_elbo = local_elbo_new;
            }

        }
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


        MultiDriftSVB_EB svb = new MultiDriftSVB_EB();
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
