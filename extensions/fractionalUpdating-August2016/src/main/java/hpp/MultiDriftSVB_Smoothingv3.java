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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 14/4/16.
 */
public class MultiDriftSVB_Smoothingv3 {

    List<DataOnMemory<DataInstance>> testBatches = new ArrayList();
    List<DataOnMemory<DataInstance>> trainBatches = new ArrayList();
    MultiDriftSVB multiDriftSVB = new MultiDriftSVB();
    List<List<EF_UnivariateDistribution>> lambdaPosteriors = new ArrayList();
    List<List<EF_UnivariateDistribution>> omegaPosteriorsFiltered = new ArrayList<>();
    List<List<EF_UnivariateDistribution>> omegaPosteriorsBackward = new ArrayList<>();

    List<EF_UnivariateDistribution> omegaPrior = new ArrayList<>();
    double learningRate = 0.1;
    int totalIter = 100;
    CompoundVector initialPrior = null;
    private boolean arcReversal = false;
    private boolean naturalGradient = false;

    public void initLearning() {
        multiDriftSVB.initLearning();
        multiDriftSVB.randomInitialize();
        initialPrior=multiDriftSVB.getPlateuStructure().getPlateauNaturalParameterPrior();
    }

    public void setNaturalGradient(boolean natural) {
        this.naturalGradient = natural;
    }

    public void setArcReversal(boolean arcReversal) {
        this.arcReversal = arcReversal;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    public void setTotalIter(int totalIter) {
        this.totalIter = totalIter;
    }

    public List<List<EF_UnivariateDistribution>> getLambdaPosteriors() {
        return lambdaPosteriors;
    }

    public List<List<EF_UnivariateDistribution>> getOmegaPosteriors() {
        return omegaPosteriorsFiltered;
    }

    public List<List<EF_UnivariateDistribution>> getOmegaPosteriorsFiltered() {
        return omegaPosteriorsFiltered;
    }

    public List<List<EF_UnivariateDistribution>> getOmegaPosteriorsBackward() {
        return omegaPosteriorsBackward;
    }


    public void aggregateTestBatches(DataOnMemory<DataInstance> batch) {
        testBatches.add(batch);
    }
    public void aggregateTrainBatches(DataOnMemory<DataInstance> batch){
        trainBatches.add(batch);
        multiDriftSVB.updateModelWithConceptDrift(batch);
        lambdaPosteriors.add(multiDriftSVB.getPlateuStructure().getQPosteriors());
        omegaPosteriorsFiltered.add(multiDriftSVB.getRhoPosterior());
        omegaPosteriorsBackward.add(multiDriftSVB.getRhoPosterior());
        omegaPrior.add(multiDriftSVB.getRhoPrior());

    }

    private void arcReversalv4(){
        multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(this.initialPrior);
        multiDriftSVB.setFirstBatch(true);

        for (int t = trainBatches.size() - 1; t >= 0; t--) {
            multiDriftSVB.updateModelWithConceptDrift(trainBatches.get(t));
            CompoundVector vector = multiDriftSVB.getPlateuStructure().getPlateauNaturalParameterPosterior();
            for (int i = 0; i < this.lambdaPosteriors.get(t).size(); i++) {
                this.lambdaPosteriors.get(t).get(i).getNaturalParameters().sum(vector.getVectorByPosition(i));
                this.lambdaPosteriors.get(t).get(i).getNaturalParameters().divideBy(2.0);
                this.lambdaPosteriors.get(t).get(i).updateMomentFromNaturalParameters();
            }
        }
    }

    private void arcReversal(){
        multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(this.initialPrior);
        multiDriftSVB.setFirstBatch(true);

        for (int t = trainBatches.size() - 1; t >= 0; t--) {
            multiDriftSVB.updateModelWithConceptDrift(trainBatches.get(t));
            CompoundVector vector = multiDriftSVB.getPlateuStructure().getPlateauNaturalParameterPosterior();
            if (t>0){
                for (int i = 0; i < this.lambdaPosteriors.get(t).size(); i++) {
                    this.lambdaPosteriors.get(t-1).get(i).getNaturalParameters().sum(vector.getVectorByPosition(i));
                    this.lambdaPosteriors.get(t-1).get(i).getNaturalParameters().substract(this.initialPrior.getVectorByPosition(i));
                    this.lambdaPosteriors.get(t-1).get(i).updateMomentFromNaturalParameters();
                }
            }else{
                for (int i = 0; i < this.lambdaPosteriors.get(t).size(); i++) {
                    this.lambdaPosteriors.get(t).get(i).getNaturalParameters().copy(vector.getVectorByPosition(i));
                    this.lambdaPosteriors.get(t).get(i).updateMomentFromNaturalParameters();
                }
            }

        }
    }

//    private void adam(){
//        DenseVector vector= new DenseVector(double[] array);
//
//
//    }

    private double[] computeFilteredELBO(){
        double[] elbo = new double[trainBatches.size()];

        for (int t = 0; t < trainBatches.size(); t++) {
            elbo[t]=0;

            CompoundVector priorT_1 = null;
            if (t > 0)
                priorT_1 = new CompoundVector(lambdaPosteriors.get(t - 1).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
            else
                priorT_1 = initialPrior;

            multiDriftSVB.getPlateuStructure().setEvidence(trainBatches.get(t).getList());

            //Compute L_t gradient
            if (t > 0) {
                double[] lambda = new double[priorT_1.getNumberOfBaseVectors()];
                for (int i = 0; i < lambda.length; i++) {
                    lambda[i] = this.omegaPosteriorsFiltered.get(t).get(i).getMomentParameters().get(0);
                }
                CompoundVector newPrior = Serialization.deepCopy(this.initialPrior);
                for (int i = 0; i < lambda.length; i++) {
                    newPrior.getVectorByPosition(i).multiplyBy(1 - lambda[i]);
                }
                CompoundVector newPosterior = Serialization.deepCopy(priorT_1);
                for (int i = 0; i < lambda.length; i++) {
                    newPosterior.getVectorByPosition(i).multiplyBy(lambda[i]);
                }
                newPrior.sum(newPosterior);

                multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(newPrior);
            } else {
                multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(priorT_1);
            }

            CompoundVector posterior = new CompoundVector(lambdaPosteriors.get(t).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
            multiDriftSVB.getPlateuStructure().updateNaturalParameterPosteriors(posterior);


            multiDriftSVB.getPlateuStructure().runInference();

            elbo[t] += multiDriftSVB.getPlateuStructure().getReplicatedNodes().filter(node -> node.isActive()).mapToDouble(node -> multiDriftSVB.getPlateuStructure().getVMP().computeELBO(node)).sum();
        }


        for (int t = 0; t < trainBatches.size(); t++) {

            CompoundVector priorT_1 = null;
            if (t > 0)
                priorT_1 = new CompoundVector(lambdaPosteriors.get(t - 1).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
            else
                priorT_1 = initialPrior;

            if (t > 0) {
                CompoundVector posterior = new CompoundVector(lambdaPosteriors.get(t).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
                multiDriftSVB.getPlateuStructure().updateNaturalParameterPosteriors(posterior);

                double[] kl_q_p0 = new double[this.initialPrior.getNumberOfBaseVectors()];
                int count = 0;
                //Messages to TExp
                this.multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(this.initialPrior);
                for (Node node : this.multiDriftSVB.getPlateuStructure().getNonReplictedNodes().collect(Collectors.toList())) {
                    Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                    kl_q_p0[count] = node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                            node.getPDist().getExpectedLogNormalizer(momentParents));
                    count++;
                }

                double[] kl_q_pt_1 = new double[this.initialPrior.getNumberOfBaseVectors()];
                count = 0;
                //Messages to TExp
                this.multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(priorT_1);
                for (Node node : this.multiDriftSVB.getPlateuStructure().getNonReplictedNodes().collect(Collectors.toList())) {
                    Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                    kl_q_pt_1[count] = node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                            node.getPDist().getExpectedLogNormalizer(momentParents));
                    count++;
                }

                for (int i = 0; i < this.omegaPosteriorsFiltered.get(t).size(); i++) {
                    elbo[t] -= this.omegaPosteriorsFiltered.get(t).get(i).getExpectedParameters().get(0) * kl_q_pt_1[i];
                    elbo[t] -= (1 - this.omegaPosteriorsFiltered.get(t).get(i).getExpectedParameters().get(0)) * kl_q_p0[i];
                    elbo[t] -= this.omegaPosteriorsFiltered.get(t).get(i).kl(this.omegaPrior.get(t).getNaturalParameters(),this.omegaPrior.get(t).computeLogNormalizer());
                }
            }else{
                CompoundVector posterior = new CompoundVector(lambdaPosteriors.get(t).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
                multiDriftSVB.getPlateuStructure().updateNaturalParameterPosteriors(posterior);
                double[] kl_q_p0 = new double[this.initialPrior.getNumberOfBaseVectors()];
                int count = 0;
                //Messages to TExp
                this.multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(this.initialPrior);
                for (Node node : this.multiDriftSVB.getPlateuStructure().getNonReplictedNodes().collect(Collectors.toList())) {
                    Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                    kl_q_p0[count] = node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                            node.getPDist().getExpectedLogNormalizer(momentParents));
                    count++;
                }
                for (int i = 0; i < this.omegaPosteriorsFiltered.get(t).size(); i++) {
                    elbo[t]-=kl_q_p0[i];
                }
            }
        }
        return elbo;
    }

    public List<CompoundVector> computeGradient(List<List<EF_UnivariateDistribution>> omegaPosteriors, double[] elbo, Function<Integer,Integer> previousT, Function<Integer,Integer>  futureT, Function<Integer,Boolean> initTimeStep, Function<Integer,Boolean> finalTimeStep){
        List<CompoundVector> gradientT = new ArrayList(trainBatches.size());
        for (int t = 0; t < trainBatches.size(); t++) {
            gradientT.add(null);
        }

        for (int currentT = 0; currentT < trainBatches.size(); currentT++) {

            CompoundVector priorT_1 = null;
            if (!initTimeStep.apply(currentT))
                priorT_1 = new CompoundVector(lambdaPosteriors.get(previousT.apply(currentT)).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
            else
                priorT_1 = initialPrior;

            multiDriftSVB.getPlateuStructure().setEvidence(trainBatches.get(currentT).getList());

            //Compute L_t gradient
            if (!initTimeStep.apply(currentT)) {
                double[] lambda = new double[priorT_1.getNumberOfBaseVectors()];
                for (int i = 0; i < lambda.length; i++) {
                    lambda[i] = omegaPosteriors.get(currentT).get(i).getMomentParameters().get(0);
                }
                CompoundVector newPrior = Serialization.deepCopy(this.initialPrior);
                for (int i = 0; i < lambda.length; i++) {
                    newPrior.getVectorByPosition(i).multiplyBy(1 - lambda[i]);
                }
                CompoundVector newPosterior = Serialization.deepCopy(priorT_1);
                for (int i = 0; i < lambda.length; i++) {
                    newPosterior.getVectorByPosition(i).multiplyBy(lambda[i]);
                }
                newPrior.sum(newPosterior);

                multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(newPrior);
            }else{
                multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(priorT_1);
            }

            CompoundVector posterior = new CompoundVector(lambdaPosteriors.get(currentT).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
            multiDriftSVB.getPlateuStructure().updateNaturalParameterPosteriors(posterior);


            multiDriftSVB.getPlateuStructure().runInference();

            elbo[currentT]+=multiDriftSVB.getPlateuStructure().getReplicatedNodes().filter(node -> node.isActive()).mapToDouble(node -> multiDriftSVB.getPlateuStructure().getVMP().computeELBO(node)).sum();

            //Compute E_q[] - \bmlambda_t
            gradientT.set(currentT,multiDriftSVB.getPlateuStructure().getPlateauNaturalParameterPosterior());
            gradientT.get(currentT).substract(posterior);

            if (!naturalGradient) {
                //Multiply by hessian
                for (int k = 0; k < lambdaPosteriors.get(currentT).size(); k++) {
                    this.lambdaPosteriors.get(currentT).get(k).perMultiplyHessian(gradientT.get(currentT).getVectorByPosition(k));
                }
            }

            if (!finalTimeStep.apply(currentT)) {
                //E[\rho]E_q[]
                CompoundVector gradientTplus1 = new CompoundVector(this.lambdaPosteriors.get(futureT.apply(currentT)).stream().map(q -> Serialization.deepCopy(q.getMomentParameters())).collect(Collectors.toList()));
                gradientTplus1.substract(new CompoundVector(this.lambdaPosteriors.get(currentT).stream().map(q -> Serialization.deepCopy(q.getMomentParameters())).collect(Collectors.toList())));
                for (int k = 0; k < gradientTplus1.getNumberOfBaseVectors(); k++) {
                    gradientTplus1.getVectorByPosition(k).multiplyBy(omegaPosteriors.get(futureT.apply(currentT)).get(k).getExpectedParameters().get(0));
                }

                if (naturalGradient) {
                    for (int k = 0; k < lambdaPosteriors.get(currentT).size(); k++) {
                        this.lambdaPosteriors.get(currentT).get(k).perMultiplyInverseHessian(gradientTplus1.getVectorByPosition(k));
                    }
                }

                gradientT.get(currentT).sum(gradientTplus1);
            }
        }
        return gradientT;
    }

    public void updateLambdaPosteriors(double learningRate, List<CompoundVector> gradientT ){
        for (int t = 0; t < trainBatches.size(); t++) {
            double[] learningRates = new double[gradientT.get(t).getNumberOfBaseVectors()];
            for (int k = 0; k < gradientT.get(t).getNumberOfBaseVectors(); k++) {
                learningRates[k] = this.lambdaPosteriors.get(t).get(k).checkGradient(learningRate, gradientT.get(t).getVectorByPosition(k));
            }

            for (int k = 0; k < gradientT.get(t).getNumberOfBaseVectors(); k++) {
                Vector localGradient = Serialization.deepCopy(gradientT.get(t).getVectorByPosition(k));
                localGradient.multiplyBy(learningRates[k]);
                this.lambdaPosteriors.get(t).get(k).getNaturalParameters().sum(localGradient);
                this.lambdaPosteriors.get(t).get(k).updateMomentFromNaturalParameters();
            }
        }
    }

    public double[] updateOmegaPosteriors(List<List<EF_UnivariateDistribution>> omegaPosteriors, double[][][] gradient, Function<Integer,Integer> previousT, Function<Integer,Integer>  futureT, Function<Integer,Boolean> initTimeStep, Function<Integer,Boolean> finalTimeStep) {

        double[] elbo = new double[trainBatches.size()];

        for (int currentT = 0; currentT < trainBatches.size(); currentT++) {
            if (!initTimeStep.apply(currentT)) {
                for (int i = 0; i < omegaPosteriors.get(currentT).size(); i++) {
                    omegaPosteriors.get(currentT).get(i).getNaturalParameters().set(0, -gradient[1][currentT][i] + gradient[0][currentT][i] + this.omegaPrior.get(currentT).getNaturalParameters().get(0));
                    for (int j = 1; j < omegaPosteriors.get(currentT).get(i).getNaturalParameters().size(); j++) {
                        omegaPosteriors.get(currentT).get(i).getNaturalParameters().set(j, this.omegaPrior.get(currentT).getNaturalParameters().get(j));
                    }
                    omegaPosteriors.get(currentT).get(i).fixNumericalInstability();
                    omegaPosteriors.get(currentT).get(i).updateMomentFromNaturalParameters();
                }

                for (int i = 0; i < omegaPosteriors.get(currentT).size(); i++) {
                    elbo[currentT] -= omegaPosteriors.get(currentT).get(i).getExpectedParameters().get(0) * gradient[1][currentT][i];
                    elbo[currentT] -= (1 - omegaPosteriors.get(currentT).get(i).getExpectedParameters().get(0)) * gradient[0][currentT][i];
                    elbo[currentT] -= omegaPosteriors.get(currentT).get(i).kl(this.omegaPrior.get(currentT).getNaturalParameters(),this.omegaPrior.get(currentT).computeLogNormalizer());
                }
            }else{
                CompoundVector posterior = new CompoundVector(lambdaPosteriors.get(currentT).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
                multiDriftSVB.getPlateuStructure().updateNaturalParameterPosteriors(posterior);
                double[] kl_q_p0 = new double[this.initialPrior.getNumberOfBaseVectors()];
                int count = 0;
                //Messages to TExp
                this.multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(this.initialPrior);
                for (Node node : this.multiDriftSVB.getPlateuStructure().getNonReplictedNodes().collect(Collectors.toList())) {
                    Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                    kl_q_p0[count] = node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                            node.getPDist().getExpectedLogNormalizer(momentParents));
                    count++;
                }
                for (int i = 0; i < omegaPosteriors.get(currentT).size(); i++) {
                    elbo[currentT]-=kl_q_p0[i];
                }
            }
        }

        return elbo;
    }

    public double[][][] gradientOmegaPosteriors(Function<Integer,Integer> previousT, Function<Integer,Integer>  futureT, Function<Integer,Boolean> initTimeStep, Function<Integer,Boolean> finalTimeStep){

        double[][][] gradient = new double[2][trainBatches.size()][];
        for (int currentT = 0; currentT < trainBatches.size(); currentT++) {
            CompoundVector priorT_1 = null;
            if (!initTimeStep.apply(currentT))
                priorT_1 = new CompoundVector(lambdaPosteriors.get(previousT.apply(currentT)).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
            else
                priorT_1 = initialPrior;

            if (!initTimeStep.apply(currentT)) {
                CompoundVector posterior = new CompoundVector(lambdaPosteriors.get(currentT).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
                multiDriftSVB.getPlateuStructure().updateNaturalParameterPosteriors(posterior);

                gradient[0][currentT]=new double[this.initialPrior.getNumberOfBaseVectors()];
                gradient[1][currentT]=new double[this.initialPrior.getNumberOfBaseVectors()];

                int count = 0;
                //Messages to TExp
                this.multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(this.initialPrior);
                for (Node node : this.multiDriftSVB.getPlateuStructure().getNonReplictedNodes().collect(Collectors.toList())) {
                    Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                    gradient[0][currentT][count] = node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                            node.getPDist().getExpectedLogNormalizer(momentParents));
                    count++;
                }

                count = 0;
                //Messages to TExp
                this.multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(priorT_1);
                for (Node node : this.multiDriftSVB.getPlateuStructure().getNonReplictedNodes().collect(Collectors.toList())) {
                    Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                    gradient[1][currentT][count] = node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                            node.getPDist().getExpectedLogNormalizer(momentParents));
                    count++;
                }

            }
        }

        return gradient;
    }

    public void smooth(){
        if (arcReversal)
            this.arcReversalv4();

        VMPLocalUpdates vmpLocalUpdates = new VMPLocalUpdates(multiDriftSVB.getPlateuStructure());

        double threshold = multiDriftSVB.getPlateuStructure().getVMP().getThreshold();
        int iterVMP = multiDriftSVB.getPlateuStructure().getVMP().getMaxIter();

        multiDriftSVB.getPlateuStructure().setVmp(vmpLocalUpdates);

        multiDriftSVB.getPlateuStructure().getVMP().setTestELBO(false);
        multiDriftSVB.getPlateuStructure().getVMP().setMaxIter(iterVMP);
        multiDriftSVB.getPlateuStructure().getVMP().setOutput(true);
        multiDriftSVB.getPlateuStructure().getVMP().setThreshold(threshold);



        multiDriftSVB.initLearning();


        double[] elbo = this.computeFilteredELBO();

        System.out.print("Iter, ELBO, ELBO DATA, ELBO PRIOR, ");
        for (int i = 0; i < elbo.length; i++) {
            System.out.print("elbo_"+i+", ");
        }
        System.out.println();


        System.out.print(0+", "+Utils.sum(elbo)+", "+0+", "+0);
        for (int i = 0; i < elbo.length; i++) {
            System.out.print(", "+elbo[i]);
        }
        System.out.println();
        double previousElbo = Double.NEGATIVE_INFINITY;

        double elboData = 0;
        double elboPrior = 0;

        for (int iter = 0; iter < totalIter; iter++) {

            double[] dataELBO = new double[elbo.length];

            List<CompoundVector> gradientTFiltered = this.computeGradient(this.omegaPosteriorsFiltered, dataELBO,(Integer t) -> (t-1), (Integer t) -> (t+1), (Integer t)-> t==0,(Integer t)-> t==trainBatches.size()-1);
            List<CompoundVector> gradientTBackward = this.computeGradient(this.omegaPosteriorsBackward, dataELBO,(Integer t) -> (t+1), (Integer t) -> (t-1), (Integer t)-> t==trainBatches.size()-1, (Integer t)-> t==0);

            for (int i = 0; i < gradientTFiltered.size(); i++) {
                gradientTFiltered.get(i).sum(gradientTBackward.get(i));
                gradientTFiltered.get(i).divideBy(2.0);
            }

            if (iter>0) {
                elboData = Utils.sum(dataELBO);
                for (int i = 0; i < dataELBO.length; i++) {
                    elbo[i] += dataELBO[i];
                }
            }

            if (iter>0) {
                System.out.print(iter+", "+Utils.sum(elbo)+", "+elboData+", "+elboPrior);
                for (int i = 0; i < elbo.length; i++) {
                    System.out.print(", "+elbo[i]);
                }
                System.out.println();
            }

            if (iter>0 && previousElbo>Utils.sum(elbo))
                throw new IllegalStateException("Non Increasing ELBO");
           else if (iter>0)
                previousElbo=Utils.sum(elbo);


            double normGradient = 0;

            for (int t = 0; t < trainBatches.size(); t++) {
                for (int k = 0; k < gradientTFiltered.get(t).getNumberOfBaseVectors(); k++) {
                    normGradient += gradientTFiltered.get(t).getVectorByPosition(k).norm2();
                }
                elbo[t]=0;
            }
            normGradient=Math.sqrt(normGradient);


            this.updateLambdaPosteriors(learningRate,gradientTFiltered);

            double[][][] gradientFiltered = this.gradientOmegaPosteriors((Integer t) -> (t-1), (Integer t) -> (t+1), (Integer t)-> t==0,(Integer t)-> t==trainBatches.size()-1);
            double[] elboFiltered = this.updateOmegaPosteriors(this.omegaPosteriorsFiltered, gradientFiltered, (Integer t) -> (t-1), (Integer t) -> (t+1), (Integer t)-> t==0,(Integer t)-> t==trainBatches.size()-1);

            double[][][] gradientBackward = this.gradientOmegaPosteriors((Integer t) -> (t+1), (Integer t) -> (t-1), (Integer t)-> t==trainBatches.size()-1, (Integer t)-> t==0);
            double[] elboBackward = this.updateOmegaPosteriors(this.omegaPosteriorsBackward, gradientBackward, (Integer t) -> (t+1), (Integer t) -> (t-1), (Integer t)-> t==trainBatches.size()-1, (Integer t)-> t==0);

            for (int i = 0; i < elbo.length; i++) {
                elbo[i]=elboFiltered[i]+elboBackward[i];
            }

            elboPrior = Utils.sum(elbo);

            System.out.println("Iter: "+iter+", Norm Gradient:"+normGradient);
        }

    }


    public double[] predictedLogLikelihood(){

        double[] testLL = new double[trainBatches.size()];
        for (int t = 0; t < testBatches.size(); t++) {
            CompoundVector posterior = new CompoundVector(lambdaPosteriors.get(t).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
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


        int nStates = 2;

        int timeSteps = 10;

        Variables variables = new Variables();

        Variable multinomialVar = variables.newMultinomialVariable("N", nStates);
        //Variable normal = variables.newGaussianVariable("N");
        //List<Variable> parents = IntStream.range(1,6).mapToObj(i ->variables.newGaussianVariable("N"+i)).collect(Collectors.toList());

        DAG dag = new DAG(variables);
        //parents.forEach(var -> dag.getParentSet(normal).addParent(var));

        BayesianNetwork bn = new BayesianNetwork(dag);
        bn.randomInitialization(new Random(0));

        //((ConditionalLinearGaussian)bn.getConditionalDistribution(normal)).setVariance(1);
        //parents.forEach(p -> {((Normal)bn.getConditionalDistribution(p)).setMean(0);((Normal)bn.getConditionalDistribution(p)).setVariance(1);});
        System.out.println(bn);
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);

        int batchSize = 50;


        MultiDriftSVB_Smoothingv3 svb = new MultiDriftSVB_Smoothingv3();
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setTestELBO(false);
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setMaxIter(100);
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setOutput(true);
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setThreshold(0.1);

        //svb.getMultiDriftSVB().setPriorDistribution(DriftSVB.TRUNCATED_EXPONENTIAL,new double[]{10000});

        svb.setNaturalGradient(true);
        svb.setArcReversal(true);
        svb.setLearningRate(0.01);
        svb.setTotalIter(100);
        svb.setWindowsSize(1000);

        svb.setDAG(bn.getDAG());

        svb.initLearning();

        Multinomial multinomialDist = bn.getConditionalDistribution(multinomialVar);
        multinomialDist.setProbabilityOfState(0,0.5);
        multinomialDist.setProbabilityOfState(1, 0.5);

        int k = 0;

        List<DataOnMemory<DataInstance>> testBatches = new ArrayList<>();
        Random rand = new Random(0);
        double[] preSmoothLog = new double[timeSteps];
        for (int i = 0; i < timeSteps; i++) {

            //sampler.setSeed(0);

            //multinomialDist = bn.getConditionalDistribution(multinomialVar);

            /*if (i>=5){
                multinomialDist.setProbabilityOfState(0,10.0/11.0);
                multinomialDist.setProbabilityOfState(1, 1.0/11.0);
            }
            if (i>=15) {
                multinomialDist.setProbabilityOfState(0,1.0/11.0);
                multinomialDist.setProbabilityOfState(1, 10.0/11.0);
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


            DataOnMemory<DataInstance> testBatch = sampler.sampleToDataStream(1000).toDataOnMemory();
            svb.aggregateTestBatches(testBatch);


            preSmoothLog[i]=svb.getMultiDriftSVB().predictedLogLikelihood(testBatch);

            System.out.println("Filter:\t" +i+ "\t" + preSmoothLog[i]+"\t"+((MultiDriftSVB)svb.getMultiDriftSVB()).getLambdaMomentParameters()[0]+"\t"+svb.getMultiDriftSVB().getPlateuStructure().getPlateauNaturalParameterPosterior().getVectorByPosition(0).get(0)+"\t"+svb.getMultiDriftSVB().getPlateuStructure().getPlateauNaturalParameterPosterior().getVectorByPosition(0).get(1));


        }

        svb.smooth();
        double[] testLL = svb.predictedLogLikelihood();
        for (int i = 0; i < timeSteps; i++) {
            System.out.println("Smoothed:\t" +i+ "\t" + testLL[i] +"\t"+"\t"+Double.NaN+"\t"+svb.getOmegaPosteriorsFiltered().get(i).get(0).getExpectedParameters().get(0)+"\t"+svb.getOmegaPosteriorsBackward().get(i).get(0).getExpectedParameters().get(0)+"\t"+svb.getLambdaPosteriors().get(i).get(0).getNaturalParameters().get(0)+"\t"+svb.getLambdaPosteriors().get(i).get(0).getNaturalParameters().get(1));//+"\t"+((MultiDriftSVB)svb).getLambdaMomentParameters()[1]);
        }

        System.out.println(Utils.sum(preSmoothLog));
        System.out.println(Utils.sum(testLL));
    }
}
