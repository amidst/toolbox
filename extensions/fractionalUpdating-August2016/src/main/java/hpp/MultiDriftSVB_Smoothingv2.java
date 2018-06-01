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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 14/4/16.
 */
public class MultiDriftSVB_Smoothingv2 {

    List<DataOnMemory<DataInstance>> testBatches = new ArrayList();
    List<DataOnMemory<DataInstance>> trainBatches = new ArrayList();
    MultiDriftSVB multiDriftSVB = new MultiDriftSVB();
    List<List<EF_UnivariateDistribution>> lambdaPosteriors = new ArrayList();
    List<List<EF_UnivariateDistribution>> omegaPosteriors = new ArrayList<>();
    List<EF_UnivariateDistribution> omegaPrior = new ArrayList<>();
    double learningRate = 0.1;
    int totalIter = 100;
    CompoundVector initialPrior = null;
    private boolean arcReversal = true;
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
        return omegaPosteriors;
    }

    public void aggregateTestBatches(DataOnMemory<DataInstance> batch) {
        testBatches.add(batch);
    }
    public void aggregateTrainBatches(DataOnMemory<DataInstance> batch){
        trainBatches.add(batch);
        multiDriftSVB.updateModelWithConceptDrift(batch);
        lambdaPosteriors.add(multiDriftSVB.getPlateuStructure().getQPosteriors());
        omegaPosteriors.add(multiDriftSVB.getRhoPosterior());
        omegaPrior.add(multiDriftSVB.getRhoPrior());

    }

    private void arcReversal(){
        multiDriftSVB.getPlateuStructure().updateNaturalParameterPrior(this.initialPrior);
        multiDriftSVB.setFirstBatch(true);
        multiDriftSVB.updateModelWithConceptDrift(trainBatches.get(trainBatches.size() - 1));

        for (int t = trainBatches.size() - 2; t >= 0; t--) {
            multiDriftSVB.updateModelWithConceptDrift(trainBatches.get(t));
            CompoundVector vector = multiDriftSVB.getPlateuStructure().getPlateauNaturalParameterPosterior();
            if (t>0){
                for (int i = 0; i < this.lambdaPosteriors.get(t).size(); i++) {
                    this.lambdaPosteriors.get(t).get(i).getNaturalParameters().sum(vector.getVectorByPosition(i));
                    this.lambdaPosteriors.get(t).get(i).getNaturalParameters().substract(this.initialPrior.getVectorByPosition(i));
                    this.lambdaPosteriors.get(t).get(i).updateMomentFromNaturalParameters();

                    this.omegaPosteriors.get(t).get(i).getNaturalParameters().sum(multiDriftSVB.getRhoPosterior().get(i).getNaturalParameters());
                    this.omegaPosteriors.get(t).get(i).getNaturalParameters().substract(multiDriftSVB.getRhoPrior().getNaturalParameters());
                    this.omegaPosteriors.get(t).get(i).updateMomentFromNaturalParameters();
                }
            }else{
                for (int i = 0; i < this.lambdaPosteriors.get(t).size(); i++) {
                    this.lambdaPosteriors.get(t).get(i).getNaturalParameters().copy(vector.getVectorByPosition(i));
                    this.lambdaPosteriors.get(t).get(i).updateMomentFromNaturalParameters();

                    this.omegaPosteriors.get(t).get(i).getNaturalParameters().copy(multiDriftSVB.getRhoPosterior().get(i).getNaturalParameters());
                    this.omegaPosteriors.get(t).get(i).updateMomentFromNaturalParameters();
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
                    lambda[i] = this.omegaPosteriors.get(t).get(i).getMomentParameters().get(0);
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

                for (int i = 0; i < this.omegaPosteriors.get(t).size(); i++) {
                    elbo[t] -= this.omegaPosteriors.get(t).get(i).getExpectedParameters().get(0) * kl_q_pt_1[i];
                    elbo[t] -= (1 - this.omegaPosteriors.get(t).get(i).getExpectedParameters().get(0)) * kl_q_p0[i];
                    elbo[t] -= this.omegaPosteriors.get(t).get(i).kl(this.omegaPrior.get(t).getNaturalParameters(),this.omegaPrior.get(t).computeLogNormalizer());
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
                for (int i = 0; i < this.omegaPosteriors.get(t).size(); i++) {
                    elbo[t]-=kl_q_p0[i];
                }
            }
        }
        return elbo;
    }

    public void smooth(){
        if (arcReversal)
            this.arcReversal();

        VMPLocalUpdates vmpLocalUpdates = new VMPLocalUpdates(multiDriftSVB.getPlateuStructure());

        double threshold = multiDriftSVB.getPlateuStructure().getVMP().getThreshold();
        int iterVMP = multiDriftSVB.getPlateuStructure().getVMP().getMaxIter();

        multiDriftSVB.getPlateuStructure().setVmp(vmpLocalUpdates);

        multiDriftSVB.getPlateuStructure().getVMP().setTestELBO(false);
        multiDriftSVB.getPlateuStructure().getVMP().setMaxIter(iterVMP);
        multiDriftSVB.getPlateuStructure().getVMP().setOutput(true);
        multiDriftSVB.getPlateuStructure().getVMP().setThreshold(threshold);



        multiDriftSVB.initLearning();
        List<CompoundVector> gradientT = new ArrayList(trainBatches.size());
        for (int t = 0; t < trainBatches.size(); t++) {
            gradientT.add(null);
        }


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
        double previousElbo = Utils.sum(elbo);

        double elboData = 0;
        double elboPrior = 0;

        for (int iter = 0; iter < totalIter; iter++) {

            for (int t = 0; t < trainBatches.size(); t++) {

                CompoundVector priorT_1 = null;
                if (t>0)
                    priorT_1 = new CompoundVector(lambdaPosteriors.get(t-1).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
                else
                    priorT_1 = initialPrior;

                multiDriftSVB.getPlateuStructure().setEvidence(trainBatches.get(t).getList());

                //Compute L_t gradient
                if (t>0) {
                    double[] lambda = new double[priorT_1.getNumberOfBaseVectors()];
                    for (int i = 0; i < lambda.length; i++) {
                        lambda[i] = this.omegaPosteriors.get(t).get(i).getMomentParameters().get(0);
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

                CompoundVector posterior = new CompoundVector(lambdaPosteriors.get(t).stream().map(q -> q.getNaturalParameters()).collect(Collectors.toList()));
                multiDriftSVB.getPlateuStructure().updateNaturalParameterPosteriors(posterior);


                multiDriftSVB.getPlateuStructure().runInference();

                if (iter>0){
                    elbo[t]+=multiDriftSVB.getPlateuStructure().getReplicatedNodes().filter(node -> node.isActive()).mapToDouble(node -> multiDriftSVB.getPlateuStructure().getVMP().computeELBO(node)).sum();
                    elboData+=multiDriftSVB.getPlateuStructure().getReplicatedNodes().filter(node -> node.isActive()).mapToDouble(node -> multiDriftSVB.getPlateuStructure().getVMP().computeELBO(node)).sum();
                }

                //Compute E_q[] - \bmlambda_t
                gradientT.set(t,multiDriftSVB.getPlateuStructure().getPlateauNaturalParameterPosterior());
                gradientT.get(t).substract(posterior);

                if (!naturalGradient) {
                    //Multiply by hessian
                    for (int k = 0; k < lambdaPosteriors.get(t).size(); k++) {
                        this.lambdaPosteriors.get(t).get(k).perMultiplyHessian(gradientT.get(t).getVectorByPosition(k));
                    }
                }

                if (t<trainBatches.size()-1) {
                    //E[\rho]E_q[]
                    CompoundVector gradientTplus1 = new CompoundVector(this.lambdaPosteriors.get(t + 1).stream().map(q -> Serialization.deepCopy(q.getMomentParameters())).collect(Collectors.toList()));
                    gradientTplus1.substract(new CompoundVector(this.lambdaPosteriors.get(t).stream().map(q -> Serialization.deepCopy(q.getMomentParameters())).collect(Collectors.toList())));
                    for (int k = 0; k < gradientTplus1.getNumberOfBaseVectors(); k++) {
                        gradientTplus1.getVectorByPosition(k).multiplyBy(this.omegaPosteriors.get(t + 1).get(k).getExpectedParameters().get(0));
                    }

                    if (naturalGradient) {
                        for (int k = 0; k < lambdaPosteriors.get(t).size(); k++) {
                            this.lambdaPosteriors.get(t).get(k).perMultiplyInverseHessian(gradientTplus1.getVectorByPosition(k));
                        }
                    }

                    gradientT.get(t).sum(gradientTplus1);
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
            else
                previousElbo=Utils.sum(elbo);


            elboData=0;
            elboPrior=0;
            double normGradient = 0;
            for (int t = 0; t < trainBatches.size(); t++) {
                elbo[t]=0;
                double[] learningRates = new double[gradientT.get(t).getNumberOfBaseVectors()];
                for (int k = 0; k < gradientT.get(t).getNumberOfBaseVectors(); k++) {
                    learningRates[k] = this.lambdaPosteriors.get(t).get(k).checkGradient(learningRate, gradientT.get(t).getVectorByPosition(k));
                }

                for (int k = 0; k < gradientT.get(t).getNumberOfBaseVectors(); k++) {
                    Vector localGradient = Serialization.deepCopy(gradientT.get(t).getVectorByPosition(k));
                    normGradient += localGradient.norm2();
                    localGradient.multiplyBy(learningRates[k]);
                    this.lambdaPosteriors.get(t).get(k).getNaturalParameters().sum(localGradient);
                    this.lambdaPosteriors.get(t).get(k).updateMomentFromNaturalParameters();
                }

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

                    for (int i = 0; i < this.omegaPosteriors.get(t).size(); i++) {
                        this.omegaPosteriors.get(t).get(i).getNaturalParameters().set(0,
                                -kl_q_pt_1[i] + kl_q_p0[i] +
                                        this.omegaPrior.get(t).getNaturalParameters().get(0));
                        for (int j = 1; j < this.omegaPosteriors.get(t).get(i).getNaturalParameters().size(); j++) {
                            this.omegaPosteriors.get(t).get(i).getNaturalParameters().set(j, this.omegaPrior.get(t).getNaturalParameters().get(j));
                        }
                        this.omegaPosteriors.get(t).get(i).fixNumericalInstability();
                        this.omegaPosteriors.get(t).get(i).updateMomentFromNaturalParameters();
                    }

                    for (int i = 0; i < this.omegaPosteriors.get(t).size(); i++) {
                        elbo[t] -= this.omegaPosteriors.get(t).get(i).getExpectedParameters().get(0) * kl_q_pt_1[i];
                        elbo[t] -= (1 - this.omegaPosteriors.get(t).get(i).getExpectedParameters().get(0)) * kl_q_p0[i];
                        elbo[t] -= this.omegaPosteriors.get(t).get(i).kl(this.omegaPrior.get(t).getNaturalParameters(),this.omegaPrior.get(t).computeLogNormalizer());
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
                    for (int i = 0; i < this.omegaPosteriors.get(t).size(); i++) {
                        elbo[t]-=kl_q_p0[i];
                    }
                }
                elboPrior+=elbo[t];
            }

            normGradient=Math.sqrt(normGradient);

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


        int nStates = 100;

        int timeSteps = 10;

        Variables variables = new Variables();

        //Variable multinomialVar = variables.newMultinomialVariable("N", nStates);
        Variable normal = variables.newGaussianVariable("N");
        List<Variable> parents = IntStream.range(1,6).mapToObj(i ->variables.newGaussianVariable("N"+i)).collect(Collectors.toList());

        DAG dag = new DAG(variables);
        parents.forEach(var -> dag.getParentSet(normal).addParent(var));

        BayesianNetwork bn = new BayesianNetwork(dag);
        bn.randomInitialization(new Random(0));

        //((ConditionalLinearGaussian)bn.getConditionalDistribution(normal)).setVariance(1);
        //parents.forEach(p -> {((Normal)bn.getConditionalDistribution(p)).setMean(0);((Normal)bn.getConditionalDistribution(p)).setVariance(1);});
        System.out.println(bn);
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);

        int batchSize = 10;


        MultiDriftSVB_Smoothingv2 svb = new MultiDriftSVB_Smoothingv2();
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setTestELBO(false);
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setMaxIter(100);
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setOutput(true);
        svb.getMultiDriftSVB().getPlateuStructure().getVMP().setThreshold(0.1);

        //svb.getMultiDriftSVB().setPriorDistribution(DriftSVB.TRUNCATED_EXPONENTIAL,new double[]{10000});

        svb.setNaturalGradient(true);
        svb.setArcReversal(false);
        svb.setLearningRate(0.1);
        svb.setTotalIter(100);
        svb.setWindowsSize(1000);

        svb.setDAG(bn.getDAG());

        svb.initLearning();

        //Multinomial multinomialDist = bn.getConditionalDistribution(multinomialVar);
        //multinomialDist.setProbabilityOfState(0,0.5);
        //multinomialDist.setProbabilityOfState(1, 0.5);

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
            System.out.println("Smoothed:\t" +i+ "\t" + testLL[i] +"\t"+"\t"+Double.NaN+"\t"+svb.getOmegaPosteriors().get(i).get(0).getExpectedParameters().get(0)+"\t"+svb.getLambdaPosteriors().get(i).get(0).getNaturalParameters().get(0)+"\t"+svb.getLambdaPosteriors().get(i).get(0).getNaturalParameters().get(1));//+"\t"+((MultiDriftSVB)svb).getLambdaMomentParameters()[1]);
        }

        System.out.println(Utils.sum(preSmoothLog));
        System.out.println(Utils.sum(testLL));
    }
}
