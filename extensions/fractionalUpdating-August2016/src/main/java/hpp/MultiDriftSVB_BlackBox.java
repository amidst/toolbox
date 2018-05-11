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

import com.sun.javafx.util.Utils;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.random.Well19937a;

import static eu.amidst.core.learning.parametric.bayesian.DriftSVB.TRUNCATED_EXPONENTIAL;
import static eu.amidst.core.learning.parametric.bayesian.DriftSVB.TRUNCATED_NORMAL;

/**
 * Created by andresmasegosa on 14/4/16.
 */
public class MultiDriftSVB_BlackBox extends SVB {

    public static int BETA  = 2;

    private static final int NSAMPLES = 100;
    EF_UnivariateDistribution[] ef_TExpP;
    EF_UnivariateDistribution[] ef_TExpQ;

    Variable truncatedExpVar;

    boolean firstBatch=true;

    CompoundVector posteriorT_1=null;

    CompoundVector prior=null;

    double delta = 0.1;

    double[] hppVal = {10};
    int type = TRUNCATED_EXPONENTIAL;

    //double[] hppVal = {0.1, 1};
    //int type = TRUNCATED_NORMAL;

    Random random = new Random(0);
    Well19937a randomBeta = new Well19937a(0);

    private boolean scoreGradient = true;

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
        } else if (type == BETA) {
            truncatedExpVar = new Variables().newBeta("BetaVar");
            this.ef_TExpP = new EF_UnivariateDistribution[size];
            this.ef_TExpQ = new EF_UnivariateDistribution[size];
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


    private List<EF_UnivariateDistribution> getBetaQPosteriors(){
        final int[] count = new int[1];
        count[0] = 0;

        List<EF_UnivariateDistribution> distributionList = this.plateuStructure.getEFLearningBN().getDistributionList().stream()
                .map(dist -> dist.getVariable())
                .filter(var -> this.plateuStructure.isNonReplicatedVar(var))
                .map(var -> ((EF_UnivariateDistribution)this.plateuStructure.getNodeOfNonReplicatedVar(var).getQDist()).deepCopy())
                .collect(Collectors.toList());

        return distributionList;
    }

    private double[] sample(EF_UnivariateDistribution sampler, int nsamples){
        double[] samples = new double[nsamples];

        if (sampler.getClass().isAssignableFrom(EF_TruncatedExponential.class)){
            EF_TruncatedExponential q = (EF_TruncatedExponential)sampler;
            for (int i = 0; i < samples.length; i++) {
                samples[i]=q.inverserCumulativeDistribution(random.nextDouble());
            }
        }else if (sampler.getClass().isAssignableFrom(EF_Dirichlet.class)){
            BetaDistribution q = new BetaDistribution(randomBeta,sampler.getNaturalParameters().get(0)+1,sampler.getNaturalParameters().get(1)+1);
            samples=q.sample(nsamples);
            for (int i = 0; i < samples.length; i++) {
                if (samples[i]==1.0)
                    samples[i]=0.9999999;
                if (samples[i]==0.0)
                    samples[i]=0.0000001;
            }
        }else{
            throw new IllegalArgumentException("Non implemented sampling method");
        }

        return samples;
    }

    private double naturalGradient(EF_UnivariateDistribution qModel){

        double naturalGradient = 0;
        if (qModel.getClass().isAssignableFrom(EF_TruncatedExponential.class)){
            double lambda = qModel.getNaturalParameters().get(0);

            naturalGradient = -Math.exp(-lambda)/Math.pow(1-Math.exp(-lambda),2) + Math.pow(lambda,-2);

        }else{
            throw new IllegalArgumentException("Non implemented natural gradient method");
        }

        return naturalGradient;
    }

    private Vector estimateGradientLowerBoundRho(int index, int nSamples, EF_UnivariateDistribution qRhoSampler, EF_UnivariateDistribution qBetaPosterior){
        EF_UnivariateDistribution qRhoModel = this.ef_TExpQ[index];
        EF_UnivariateDistribution pRhoModel = this.ef_TExpP[index];

        EF_UnivariateDistribution pBetaPrior = Serialization.deepCopy(qBetaPosterior);
        Vector gradientVector  = Serialization.deepCopy(qRhoSampler.getNaturalParameters());

        double rho_t = qRhoModel.getMomentParameters().get(0);
        Vector newPrior = Serialization.deepCopy(prior.getVectorByPosition(index));
        Vector newposteriorT_1 = Serialization.deepCopy(posteriorT_1.getVectorByPosition(index));

        //Compute the E_q[ln\hat{p}(\beta_t|\bmlambda_{t-1},\rho_t)
        newPrior.copy(prior.getVectorByPosition(index));
        newPrior.multiplyBy(1 - rho_t);
        newposteriorT_1.copy(posteriorT_1.getVectorByPosition(index));
        newposteriorT_1.multiplyBy(rho_t);
        newPrior.sum(newposteriorT_1);
        pBetaPrior.getNaturalParameters().copy(newPrior);
        pBetaPrior.fixNumericalInstability();
        pBetaPrior.updateMomentFromNaturalParameters();

        newposteriorT_1.copy(posteriorT_1.getVectorByPosition(index));
        newposteriorT_1.substract(prior.getVectorByPosition(index));

        Vector moments = Serialization.deepCopy(qBetaPosterior.getMomentParameters());
        moments.substract(pBetaPrior.getMomentParameters());
        double gradient = newposteriorT_1.dotProduct(moments) + pRhoModel.getNaturalParameters().get(0);

        gradientVector.set(0,gradient);

        return gradientVector;
    }


    private Vector estimateGradientLowerBoundTExpPathWise(int index, int nSamples, EF_UnivariateDistribution qRhoSampler, EF_UnivariateDistribution qBetaPosterior){
        EF_TruncatedExponential qRhoModel = (EF_TruncatedExponential)this.ef_TExpQ[index];
        EF_TruncatedExponential pRhoModel = (EF_TruncatedExponential)this.ef_TExpP[index];

        EF_UnivariateDistribution pBetaPrior = Serialization.deepCopy(qBetaPosterior);

        Vector newPrior = Serialization.deepCopy(prior.getVectorByPosition(index));
        Vector newposteriorT_1 = Serialization.deepCopy(posteriorT_1.getVectorByPosition(index));

        double lambda = qRhoModel.getNaturalParameters().get(0);


        double finalGradient = 0;
        for (int i = 0; i < nSamples; i++) {
            double uniform = random.nextDouble();
            double rho_t = qRhoModel.inverserCumulativeDistribution(uniform);

            newPrior.copy(prior.getVectorByPosition(index));
            newPrior.multiplyBy(1 - rho_t);
            newposteriorT_1.copy(posteriorT_1.getVectorByPosition(index));
            newposteriorT_1.multiplyBy(rho_t);
            newPrior.sum(newposteriorT_1);
            pBetaPrior.getNaturalParameters().copy(newPrior);
            pBetaPrior.fixNumericalInstability();
            pBetaPrior.updateMomentFromNaturalParameters();

            newposteriorT_1.copy(posteriorT_1.getVectorByPosition(index));
            newposteriorT_1.substract(prior.getVectorByPosition(index));

            Vector moments = Serialization.deepCopy(qBetaPosterior.getMomentParameters());
            moments.substract(pBetaPrior.getMomentParameters());
            double gradientRho = newposteriorT_1.dotProduct(moments) + pRhoModel.getNaturalParameters().get(0);

            double gradientF_1 = qRhoModel.gradientInverserCumulativeDistribution(uniform);

            double localFinalGradientRho = gradientRho*gradientF_1;
            double localFinalGradientEntropy = qRhoModel.getNaturalParameters().get(0)*gradientF_1 + (rho_t - qRhoModel.getMomentParameters().get(0));
            finalGradient+=localFinalGradientRho-localFinalGradientEntropy;

        }

        finalGradient/=nSamples;
        Vector gradient = Serialization.deepCopy(qRhoModel.getNaturalParameters());
        gradient.set(0,finalGradient);

        double naturalGradient = this.naturalGradient(qRhoModel);

        //gradient.divideBy(naturalGradient);

        return gradient;
    }


    private Vector estimateGradientLowerBound(int index, int nSamples, EF_UnivariateDistribution qRhoSampler, EF_UnivariateDistribution qBetaPosterior){
        EF_UnivariateDistribution qRhoModel = this.ef_TExpQ[index];
        EF_UnivariateDistribution pRhoModel = this.ef_TExpP[index];

        EF_UnivariateDistribution pBetaPrior = Serialization.deepCopy(qBetaPosterior);

        double[] rho_t = sample(qRhoSampler,nSamples);
        Vector newPrior = Serialization.deepCopy(prior.getVectorByPosition(index));
        Vector newposteriorT_1 = Serialization.deepCopy(posteriorT_1.getVectorByPosition(index));
        Vector naturalWeights = Serialization.deepCopy(qRhoSampler.getNaturalParameters());


        Vector gradient = Serialization.deepCopy(qRhoModel.getNaturalParameters());
        for (int i = 0; i < rho_t.length; i++) {
            double partialLowerBound = 0;
            //Importance Sampling Weight
            naturalWeights.copy(qRhoModel.getNaturalParameters());
            naturalWeights.substract(qRhoSampler.getNaturalParameters());
            double logWeight=naturalWeights.dotProduct(qRhoSampler.getSufficientStatistics(rho_t[i]));
            logWeight+=(qRhoSampler.computeLogNormalizer()-qRhoModel.computeLogNormalizer());

            //Compute the E_q[ln\hat{p}(\beta_t|\bmlambda_{t-1},\rho_t)
            newPrior.copy(prior.getVectorByPosition(index));
            newPrior.multiplyBy(1 - rho_t[i]);
            newposteriorT_1.copy(posteriorT_1.getVectorByPosition(index));
            newposteriorT_1.multiplyBy(rho_t[i]);
            newPrior.sum(newposteriorT_1);
            pBetaPrior.getNaturalParameters().copy(newPrior);
            pBetaPrior.fixNumericalInstability();
            pBetaPrior.updateMomentFromNaturalParameters();
            partialLowerBound+=pBetaPrior.getNaturalParameters().dotProduct(qBetaPosterior.getMomentParameters())-pBetaPrior.computeLogNormalizer();

            //Compute E_q[\ln p(rho_t|\gamma))]
            partialLowerBound+=pRhoModel.getNaturalParameters().dotProduct(pRhoModel.getSufficientStatistics(rho_t[i])) - pRhoModel.computeLogNormalizer();

            //Compute -E_q[\ln q(rho_t|\gamma))]
            partialLowerBound-=(qRhoModel.getNaturalParameters().dotProduct(qRhoModel.getSufficientStatistics(rho_t[i])) - qRhoModel.computeLogNormalizer());


            //Compute (t(\rho_t) - E_q[t(\rho_t)])*f(\rho_t)
            Vector localGradient = qRhoModel.getSufficientStatistics(rho_t[i]);
            localGradient.substract(qRhoModel.getMomentParameters());

            localGradient.multiplyBy(Math.exp(logWeight)*partialLowerBound);

            gradient.sum(localGradient);
        }
        gradient.divideBy(rho_t.length);

        //double naturalGradient = this.naturalGradient(qRhoModel);

        //gradient.divideBy(naturalGradient);

        return gradient;
    }

    private double estimateLowerBound(int index, int nSamples, EF_UnivariateDistribution qRhoSampler, EF_UnivariateDistribution qBetaPosterior){
        EF_UnivariateDistribution qRhoModel = this.ef_TExpQ[index];
        EF_UnivariateDistribution pRhoModel = this.ef_TExpP[index];

        EF_UnivariateDistribution pBetaPrior = Serialization.deepCopy(qBetaPosterior);


        double[] rho_t = sample(qRhoSampler,nSamples);
        Vector newPrior = Serialization.deepCopy(prior.getVectorByPosition(index));
        Vector newposteriorT_1 = Serialization.deepCopy(posteriorT_1.getVectorByPosition(index));
        Vector naturalWeights = Serialization.deepCopy(qRhoSampler.getNaturalParameters());


        double[] lowerbound = new double[nSamples];
        for (int i = 0; i < rho_t.length; i++) {
            double partialLowerBound = 0;
            //Importance Sampling Weight
            naturalWeights.copy(qRhoModel.getNaturalParameters());
            naturalWeights.substract(qRhoSampler.getNaturalParameters());
            double logWeight=naturalWeights.dotProduct(qRhoSampler.getSufficientStatistics(rho_t[i]));
            logWeight+=(qRhoSampler.computeLogNormalizer()-qRhoModel.computeLogNormalizer());

            //Compute the E_q[ln\hat{p}(\beta_t|\bmlambda_{t-1},\rho_t)
            newPrior.copy(prior.getVectorByPosition(index));
            newPrior.multiplyBy(1 - rho_t[i]);
            newposteriorT_1.copy(posteriorT_1.getVectorByPosition(index));
            newposteriorT_1.multiplyBy(rho_t[i]);
            newPrior.sum(newposteriorT_1);
            pBetaPrior.getNaturalParameters().copy(newPrior);
            pBetaPrior.fixNumericalInstability();
            pBetaPrior.updateMomentFromNaturalParameters();
            partialLowerBound+=pBetaPrior.getNaturalParameters().dotProduct(qBetaPosterior.getMomentParameters())-pBetaPrior.computeLogNormalizer();

            //Compute E_q[\ln p(rho_t|\gamma))]
            partialLowerBound+=pRhoModel.getNaturalParameters().dotProduct(pRhoModel.getSufficientStatistics(rho_t[i])) - pRhoModel.computeLogNormalizer();

            //Compute -E_q[\ln q(rho_t|\gamma))]
            partialLowerBound-=(qRhoModel.getNaturalParameters().dotProduct(qRhoModel.getSufficientStatistics(rho_t[i])) - qRhoModel.computeLogNormalizer());

            lowerbound[i]=Math.exp(logWeight)*partialLowerBound;
        }

        return Utils.sum(lowerbound)/rho_t.length;
    }

    private double estimateLowerBoundRho(int index, int nSamples, EF_UnivariateDistribution qRhoSampler, EF_UnivariateDistribution qBetaPosterior){
        EF_UnivariateDistribution qRhoModel = this.ef_TExpQ[index];
        EF_UnivariateDistribution pRhoModel = this.ef_TExpP[index];

        EF_UnivariateDistribution pBetaPrior = Serialization.deepCopy(qBetaPosterior);


        double rho_t = qRhoModel.getMomentParameters().get(0);
        Vector newPrior = Serialization.deepCopy(prior.getVectorByPosition(index));
        Vector newposteriorT_1 = Serialization.deepCopy(posteriorT_1.getVectorByPosition(index));
        Vector naturalWeights = Serialization.deepCopy(qRhoSampler.getNaturalParameters());


        double partialLowerBound = 0;

        //Compute the E_q[ln\hat{p}(\beta_t|\bmlambda_{t-1},\rho_t)
        newPrior.copy(prior.getVectorByPosition(index));
        newPrior.multiplyBy(1 - rho_t);
        newposteriorT_1.copy(posteriorT_1.getVectorByPosition(index));
        newposteriorT_1.multiplyBy(rho_t);
        newPrior.sum(newposteriorT_1);
        pBetaPrior.getNaturalParameters().copy(newPrior);
        pBetaPrior.fixNumericalInstability();
        pBetaPrior.updateMomentFromNaturalParameters();
        partialLowerBound+=pBetaPrior.getNaturalParameters().dotProduct(qBetaPosterior.getMomentParameters())-pBetaPrior.computeLogNormalizer();

        //Compute E_q[\ln p(rho_t|\gamma))]
        partialLowerBound+=pRhoModel.getNaturalParameters().dotProduct(pRhoModel.getSufficientStatistics(rho_t)) - pRhoModel.computeLogNormalizer();

        return partialLowerBound;
    }

    private double estimateQRhoBackTracking(int index, int nSamples, EF_UnivariateDistribution qBetaPosterior){

        EF_UnivariateDistribution samplerQ = Serialization.deepCopy(this.ef_TExpQ[index]);
        double lowerBound = this.estimateLowerBound(index, nSamples, samplerQ,qBetaPosterior);

        NaturalParameters naturalParametersQRhoModel = Serialization.deepCopy(this.ef_TExpQ[index].getNaturalParameters());
        double learningRate=1;
        boolean local_convergence = false;
        for (int iter = 0; iter < 1000 && !local_convergence; iter++) {
            Vector gradient = this.estimateGradientLowerBound(index,nSamples,samplerQ,qBetaPosterior);
            learningRate = this.checkGradient(naturalParametersQRhoModel, gradient, learningRate);
            for (int i = 0; i < naturalParametersQRhoModel.size(); i++) {
                naturalParametersQRhoModel.set(i,naturalParametersQRhoModel.get(i)+learningRate*gradient.get(i));
            }
            this.ef_TExpQ[index].getNaturalParameters().copy(naturalParametersQRhoModel);
            this.ef_TExpQ[index].updateMomentFromNaturalParameters();


            samplerQ.getNaturalParameters().copy(naturalParametersQRhoModel);
            samplerQ.updateMomentFromNaturalParameters();
            double newlowerBound = this.estimateLowerBound(index,nSamples,samplerQ,qBetaPosterior);

            if (learningRate<0.01) {
                local_convergence = true;
            }else if (newlowerBound<lowerBound){
                for (int i = 0; i < naturalParametersQRhoModel.size(); i++) {
                    naturalParametersQRhoModel.set(i,naturalParametersQRhoModel.get(i)-learningRate*gradient.get(i));
                }
                this.ef_TExpQ[index].getNaturalParameters().copy(naturalParametersQRhoModel);
                this.ef_TExpQ[index].updateMomentFromNaturalParameters();
                samplerQ.getNaturalParameters().copy(naturalParametersQRhoModel);
                samplerQ.updateMomentFromNaturalParameters();
                learningRate/=2;
            }else {
                samplerQ.getNaturalParameters().copy(naturalParametersQRhoModel);
                samplerQ.updateMomentFromNaturalParameters();
                lowerBound = this.estimateLowerBound(index, nSamples, samplerQ,qBetaPosterior);
            }
        }

        //checkMaximum(index, nSamples, qBetaPosterior, lowerBound);

        return lowerBound;
    }

    private void checkMaximumRho(int index, int nSamples, EF_UnivariateDistribution qBetaPosterior, double lowerBoundMax){
        EF_UnivariateDistribution tmpQ = Serialization.deepCopy(this.ef_TExpQ[index]);
        double[] lowerBounds = new double[20];
        int count = 0;
        double range = 1-tmpQ.getMomentParameters().get(0);
        for (int i = 0; i < 10; i++) {
            this.ef_TExpQ[index].getMomentParameters().set(0, tmpQ.getMomentParameters().get(0)+(i+1)*range/10);
            lowerBounds[count++] = this.estimateLowerBoundRho(index, nSamples, this.ef_TExpQ[index], qBetaPosterior);
        }
        range = tmpQ.getMomentParameters().get(0)-0;
        for (int i = 0; i < 10; i++) {
            this.ef_TExpQ[index].getMomentParameters().set(0, tmpQ.getMomentParameters().get(0)-(i+1)*range/10);
            lowerBounds[count++] = this.estimateLowerBoundRho(index, nSamples, this.ef_TExpQ[index], qBetaPosterior);
        }

        OptionalDouble highest = Arrays.stream(lowerBounds).max();

        if (highest.getAsDouble()>lowerBoundMax)
            System.out.println("ERROR!!");

        this.ef_TExpQ[index] = tmpQ;

    }

    private void checkMaximum(int index, int nSamples, EF_UnivariateDistribution qBetaPosterior, double lowerBoundMax){
        EF_UnivariateDistribution tmpQ = Serialization.deepCopy(this.ef_TExpQ[index]);
        double[] lowerBounds = new double[20];
        int count = 0;
        for (int i = 0; i < 10; i++) {
            this.ef_TExpQ[index].getNaturalParameters().set(0, tmpQ.getNaturalParameters().get(0)+(i+1)*10);
            this.ef_TExpQ[index].updateMomentFromNaturalParameters();
            lowerBounds[count++] = this.estimateLowerBound(index, nSamples, this.ef_TExpQ[index], qBetaPosterior);
        }
        for (int i = 0; i < 10; i++) {
            this.ef_TExpQ[index].getNaturalParameters().set(0, tmpQ.getNaturalParameters().get(0)-(i+1)*10);
            this.ef_TExpQ[index].updateMomentFromNaturalParameters();
            lowerBounds[count++] = this.estimateLowerBound(index, nSamples, this.ef_TExpQ[index], qBetaPosterior);
        }

        OptionalDouble highest = Arrays.stream(lowerBounds).max();

        if (highest.getAsDouble()>lowerBoundMax)
            System.out.println("ERROR!!");

        this.ef_TExpQ[index] = tmpQ;

    }

    private double checkGradient(NaturalParameters naturalParametersQRhoModel, Vector gradient, double learningRate) {
        if (type==BETA){
            for (int i = 0; i < naturalParametersQRhoModel.size(); i++) {
                while (naturalParametersQRhoModel.get(i)+learningRate*gradient.get(i)<=0)
                    learningRate/=2;
            }
        }
        return learningRate;
    }

    private double estimateRhoSGA(int index, int nSamples, EF_UnivariateDistribution qBetaPosterior){

        EF_UnivariateDistribution samplerQ = Serialization.deepCopy(this.ef_TExpQ[index]);
        NaturalParameters naturalParametersQRhoModel = Serialization.deepCopy(this.ef_TExpQ[index].getNaturalParameters());
        naturalParametersQRhoModel.set(0,samplerQ.getMomentParameters().get(0));
        double learningRate=0.01;
        boolean local_convergence = false;
        for (int iter = 0; iter < 100 && !local_convergence; iter++) {
            learningRate = Math.pow(2+iter,-0.75);
            Vector gradient = this.estimateGradientLowerBoundRho(index,nSamples,samplerQ,qBetaPosterior);
            while (naturalParametersQRhoModel.get(0)+learningRate*gradient.get(0)>1.0 || naturalParametersQRhoModel.get(0)+learningRate*gradient.get(0)<0.0)
                learningRate/=2;
            for (int i = 0; i < naturalParametersQRhoModel.size(); i++) {
                naturalParametersQRhoModel.set(i,naturalParametersQRhoModel.get(i)+learningRate*gradient.get(i));
            }
            this.ef_TExpQ[index].getMomentParameters().set(0,naturalParametersQRhoModel.get(0));

        }

        double lowerBound = this.estimateLowerBoundRho(index,nSamples,samplerQ,qBetaPosterior);
        //checkMaximumRho(index, nSamples, qBetaPosterior, lowerBound);


        return lowerBound;
    }

    private double estimateQRhoSGA(int index, int nSamples, EF_UnivariateDistribution qBetaPosterior){

        EF_UnivariateDistribution samplerQ = Serialization.deepCopy(this.ef_TExpQ[index]);
        NaturalParameters naturalParametersQRhoModel = Serialization.deepCopy(this.ef_TExpQ[index].getNaturalParameters());
        double learningRate=0.01;
        boolean local_convergence = false;
        for (int iter = 0; iter < 50 && !local_convergence; iter++) {
            learningRate = Math.pow(2+iter,-0.75);
            Vector gradient = null;
            if (scoreGradient)
                gradient = this.estimateGradientLowerBound(index,nSamples,samplerQ,qBetaPosterior);
            else
                gradient = this.estimateGradientLowerBoundTExpPathWise(index,nSamples,samplerQ,qBetaPosterior);

            learningRate = this.checkGradient(naturalParametersQRhoModel, gradient, learningRate);

            for (int i = 0; i < naturalParametersQRhoModel.size(); i++) {
                naturalParametersQRhoModel.set(i,naturalParametersQRhoModel.get(i)+learningRate*gradient.get(i));
            }
            this.ef_TExpQ[index].getNaturalParameters().copy(naturalParametersQRhoModel);
            this.ef_TExpQ[index].updateMomentFromNaturalParameters();


            samplerQ.getNaturalParameters().copy(naturalParametersQRhoModel);
            samplerQ.updateMomentFromNaturalParameters();
        }
        double lowerBound = this.estimateLowerBound(index,nSamples,samplerQ,qBetaPosterior);
        //checkMaximum(index, nSamples, qBetaPosterior, lowerBound);

        return lowerBound;
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
            this.ef_TExpQ[i].updateMomentFromNaturalParameters();
        }

        boolean convergence = false;
        double elbo = Double.NaN;
        double niter=0;
        while(!convergence && niter<10) {

            //Messages for TExp to Theta
            double[] lambda = new double[prior.getNumberOfBaseVectors()];
            for (int i = 0; i < lambda.length; i++) {
                lambda[i] = this.ef_TExpQ[i].getExpectedParameters().get(0);
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


            //Estimate the new q(\rho_t|\omega_t) posteriors
            List<EF_UnivariateDistribution> qBetaPosterior = this.getBetaQPosteriors();

            for (int i = 0; i < this.ef_TExpQ.length; i++) {
                newELBO+=this.estimateQRhoSGA(i,NSAMPLES,qBetaPosterior.get(i));
            }



            if (!Double.isNaN(elbo) &&  newELBO<elbo){
                new IllegalStateException("Non increasing lower bound");
            }
            double percentageIncrease = 100*Math.abs((newELBO-elbo)/elbo);

/*            System.out.print("N Iter: " + niter + ", " + newELBO + ", "+ elbo + ", "+ percentageIncrease);
            if (this.type == TRUNCATED_NORMAL){
                for (int i = 0; i < lambda.length; i++) {
                    System.out.print(", "+ lambda[i] + " - " + ((EF_TruncatedNormal)ef_TExpQ[i]).getPrecision());
                }
            }

            System.out.println();
            System.out.print("KL: " + niter + ", " + newELBO + ", "+ elbo + ", "+ percentageIncrease);

            System.out.println();
            */
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

    public double[] getLambdaMomentParameters(){

        double[] out = new double[this.prior.getNumberOfBaseVectors()];
        for (int i = 0; i < out.length; i++) {
            out[i] = this.ef_TExpQ[i].getExpectedParameters().get(0);
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


        MultiDriftSVB_BlackBox svb = new MultiDriftSVB_BlackBox();
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
