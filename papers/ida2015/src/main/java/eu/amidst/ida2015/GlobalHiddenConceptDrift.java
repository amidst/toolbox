/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.ida2015;

import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.*;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.Fading;
import eu.amidst.core.learning.parametric.bayesian.PlateuIIDReplication;
import eu.amidst.core.learning.parametric.bayesian.StreamingVariationalBayesVMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 13/4/15.
 */
public class GlobalHiddenConceptDrift {

    private static double getRealMeanRandomConceptDrift(BayesianNetwork bn) {

        Variable classVariable = bn.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = bn.getStaticVariables().getVariableByName("GaussianVar0");

        Multinomial distClass = bn.getConditionalDistribution(classVariable);
        Normal_MultinomialNormalParents distGV0 = bn.getConditionalDistribution(gaussianVar0);

        double inter0 = distGV0.getNormal_NormalParentsDistribution(0).getIntercept();
        double inter1 = distGV0.getNormal_NormalParentsDistribution(1).getIntercept();

        double mean = distClass.getProbabilityOfState(0) * inter0;
        mean += distClass.getProbabilityOfState(1) * inter1;

        return mean;
    }


    private static double getRealMeanSmoothConceptDrift(BayesianNetwork bn) {

        Variable classVariable = bn.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = bn.getStaticVariables().getVariableByName("GaussianVar0");
        Variable globalHidden = bn.getStaticVariables().getVariableByName("Global");

        Multinomial distClass = bn.getConditionalDistribution(classVariable);
        Normal_MultinomialNormalParents distGV0 = bn.getConditionalDistribution(gaussianVar0);
        Normal distGlobal = bn.getConditionalDistribution(globalHidden);

        double inter0 = distGV0.getNormal_NormalParentsDistribution(0).getIntercept();
        double inter1 = distGV0.getNormal_NormalParentsDistribution(1).getIntercept();

        double[] coef0 = distGV0.getNormal_NormalParentsDistribution(0).getCoeffParents();
        double[] coef1 = distGV0.getNormal_NormalParentsDistribution(1).getCoeffParents();

        double mean = distClass.getProbabilityOfState(0) * (inter0 + coef0[0] * distGlobal.getMean());
        mean += distClass.getProbabilityOfState(1) * (inter1 + coef1[0] * distGlobal.getMean());

        return mean;
    }


    private static double getLearntMean(BayesianNetwork bn, double globalMean) {

        Variable globalHidden = bn.getStaticVariables().getVariableByName("Global");
        Variable classVariable = bn.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = bn.getStaticVariables().getVariableByName("GaussianVar0");


        Normal distGlobal = bn.getConditionalDistribution(globalHidden);
        Multinomial distClass = bn.getConditionalDistribution(classVariable);
        Normal_MultinomialNormalParents distGV0 = bn.getConditionalDistribution(gaussianVar0);

        double inter0 = distGV0.getNormal_NormalParentsDistribution(0).getIntercept();
        double inter1 = distGV0.getNormal_NormalParentsDistribution(1).getIntercept();

        double[] coef0 = distGV0.getNormal_NormalParentsDistribution(0).getCoeffParents();
        double[] coef1 = distGV0.getNormal_NormalParentsDistribution(1).getCoeffParents();

        double mean = distClass.getProbabilityOfState(0) * (inter0 + coef0[0] * globalMean);
        mean += distClass.getProbabilityOfState(1) * (inter1 + coef1[0] * globalMean);

        return mean;
    }

    public static void conceptDriftWithRandomChangesMissingLabels(String[] args) {

        BayesianNetworkGenerator.setNumberOfGaussianVars(10);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(0, 2);
        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayesWithGlobalHiddenVar(2, "Global");

        naiveBayes.randomInitialization(new Random(0));

        Variable globalHidden = naiveBayes.getStaticVariables().getVariableByName("Global");
        Variable classVariable = naiveBayes.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = naiveBayes.getStaticVariables().getVariableByName("GaussianVar0");

        for (ConditionalDistribution dist : naiveBayes.getConditionalDistributions()) {
            if (dist.getVariable().equals(classVariable) || dist.getVariable().equals(globalHidden))
                continue;
            Normal_MultinomialNormalParents newdist = naiveBayes.getConditionalDistribution(dist.getVariable());
            //newdist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{0.0});
            newdist.getNormal_NormalParentsDistribution(0).setCoeffForParent(globalHidden, 0.0);
            //newdist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{0.0});
            newdist.getNormal_NormalParentsDistribution(1).setCoeffForParent(globalHidden, 0.0);
        }

        System.out.println(naiveBayes.toString());

        int windowSize =100;
        int sampleSize = 5000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setHiddenVar(globalHidden);
        DataStream<DataInstance> data = sampler.sampleToDataStream(sampleSize);
        int count = windowSize;

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setParallelMode(false);
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(Arrays.asList(globalHidden),true));
        svb.setTransitionMethod(new GaussianHiddenTransitionMethod(Arrays.asList(globalHidden), 0, 10));
        svb.setWindowsSize(windowSize);
        svb.setDAG(naiveBayes.getDAG());
        svb.initLearning();


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            //System.out.println("****************");
            //System.out.println(learntBN.toString());
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
            //System.out.println("Global Hidden: " + normal.getMean() +", " + normal.getVariance());
            //System.out.println("****************");

            Normal_MultinomialNormalParents distA = learntBN.getConditionalDistribution(gaussianVar0);

            Normal_MultinomialNormalParents distATrue = naiveBayes.getConditionalDistribution(gaussianVar0);


            System.out.print(count + "\t" + normal.getMean() + "\t" + getLearntMean(learntBN, normal.getMean())+"\t" + getRealMeanRandomConceptDrift(naiveBayes));
            //System.out.print("\t" + distA.getNormal_NormalParentsDistribution(0).getIntercept() +"\t" + distA.getNormal_NormalParentsDistribution(1).getIntercept());
            //System.out.print("\t" + distA.getNormal_NormalParentsDistribution(0).getCoeffParents()[0] + "\t" + distA.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
            //System.out.print("\t" + distATrue.getNormal_NormalParentsDistribution(0).getIntercept() + "\t" + distATrue.getNormal_NormalParentsDistribution(1).getIntercept());
            System.out.println();

            count += windowSize;
        }


        for (int K = 1; K < 10; K++) {
            //System.out.println("******************************** CONCEPT DRIFT ********************************");
            naiveBayes.randomInitialization(new Random(K+1));

            for (ConditionalDistribution dist : naiveBayes.getConditionalDistributions()) {
                if (dist.getVariable().equals(classVariable) || dist.getVariable().equals(globalHidden))
                    continue;
                Normal_MultinomialNormalParents newdist = naiveBayes.getConditionalDistribution(dist.getVariable());
                newdist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{0.0});
                newdist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{0.0});
            }

            //System.out.println(naiveBayes.toString());

            sampler = new BayesianNetworkSampler(naiveBayes);
            sampler.setHiddenVar(globalHidden);
            data = sampler.sampleToDataStream(sampleSize);
            Random random = new Random(0);

            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

                /*
                double innerCount = 0;
                for (DataInstance instance : batch){
                    //if (random.nextDouble()<0.9)
                    //    instance.setValue(classVariable, Utils.missingValue());
                    if (innerCount%10!=0)
                        instance.setValue(classVariable, Utils.missingValue());

                    innerCount++;
                }*/


                svb.updateModel(batch);
                BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

                //System.out.println("****************");
                //System.out.println(learntBN.toString());
                Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
                //System.out.println("Global Hidden: " + normal.getMean() + ", " + normal.getVariance());
                //System.out.println("****************");


                Normal_MultinomialNormalParents distA = learntBN.getConditionalDistribution(gaussianVar0);
                Normal_MultinomialNormalParents distATrue = naiveBayes.getConditionalDistribution(gaussianVar0);


                System.out.print(count + "\t" + normal.getMean() + "\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMeanRandomConceptDrift(naiveBayes));
                //System.out.print("\t" + distA.getNormal_NormalParentsDistribution(0).getIntercept() +"\t" + distA.getNormal_NormalParentsDistribution(1).getIntercept());
                //System.out.print("\t" + distA.getNormal_NormalParentsDistribution(0).getCoeffParents()[0] + "\t" + distA.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
                //System.out.print("\t" + distATrue.getNormal_NormalParentsDistribution(0).getIntercept() + "\t" + distATrue.getNormal_NormalParentsDistribution(1).getIntercept());
                System.out.println();

                count += windowSize;
            }
        }
    }

    public static void conceptDriftWithRandomChanges(String[] args) {

        BayesianNetworkGenerator.setNumberOfGaussianVars(10);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(0, 2);
        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayesWithGlobalHiddenVar(2, "Global");

        naiveBayes.randomInitialization(new Random(0));

        Variable globalHidden = naiveBayes.getStaticVariables().getVariableByName("Global");
        Variable classVariable = naiveBayes.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = naiveBayes.getStaticVariables().getVariableByName("GaussianVar0");

        for (ConditionalDistribution dist : naiveBayes.getConditionalDistributions()) {
            if (dist.getVariable().equals(classVariable) || dist.getVariable().equals(globalHidden))
                continue;
            Normal_MultinomialNormalParents newdist = naiveBayes.getConditionalDistribution(dist.getVariable());
            //newdist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{0.0});
            newdist.getNormal_NormalParentsDistribution(0).setCoeffForParent(globalHidden, 0.0);
            //newdist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{0.0});
            newdist.getNormal_NormalParentsDistribution(1).setCoeffForParent(globalHidden, 0.0);
        }

        System.out.println(naiveBayes.toString());

        int windowSize =100;
        int sampleSize = 5000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setHiddenVar(globalHidden);
        DataStream<DataInstance> data = sampler.sampleToDataStream(sampleSize);
        int count = windowSize;

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(Arrays.asList(globalHidden),true));
        svb.setTransitionMethod(new GaussianHiddenTransitionMethod(Arrays.asList(globalHidden), 1, 5));
        svb.setWindowsSize(windowSize);
        svb.setDAG(naiveBayes.getDAG());
        svb.initLearning();


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            //System.out.println("****************");
            //System.out.println(learntBN.toString());
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
            //System.out.println("Global Hidden: " + normal.getMean() +", " + normal.getVariance());
            //System.out.println("****************");

            Normal_MultinomialNormalParents distA = learntBN.getConditionalDistribution(gaussianVar0);

            Normal_MultinomialNormalParents distATrue = naiveBayes.getConditionalDistribution(gaussianVar0);


            System.out.print(count + "\t" + normal.getMean() + "\t" + getLearntMean(learntBN, normal.getMean())+"\t" + getRealMeanRandomConceptDrift(naiveBayes));
            //System.out.print("\t" + distA.getNormal_NormalParentsDistribution(0).getIntercept() +"\t" + distA.getNormal_NormalParentsDistribution(1).getIntercept());
            //System.out.print("\t" + distA.getNormal_NormalParentsDistribution(0).getCoeffParents()[0] + "\t" + distA.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
            //System.out.print("\t" + distATrue.getNormal_NormalParentsDistribution(0).getIntercept() + "\t" + distATrue.getNormal_NormalParentsDistribution(1).getIntercept());
            System.out.println();

            count += windowSize;
        }


        for (int K = 1; K < 10; K++) {
            //System.out.println("******************************** CONCEPT DRIFT ********************************");
            naiveBayes.randomInitialization(new Random(K+1));

            for (ConditionalDistribution dist : naiveBayes.getConditionalDistributions()) {
                if (dist.getVariable().equals(classVariable) || dist.getVariable().equals(globalHidden))
                    continue;
                Normal_MultinomialNormalParents newdist = naiveBayes.getConditionalDistribution(dist.getVariable());
                //newdist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{0.0});
                newdist.getNormal_NormalParentsDistribution(0).setCoeffForParent(globalHidden, 0.0);
                //newdist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{0.0});
                newdist.getNormal_NormalParentsDistribution(1).setCoeffForParent(globalHidden, 0.0);
            }

            //System.out.println(naiveBayes.toString());

            sampler = new BayesianNetworkSampler(naiveBayes);
            sampler.setHiddenVar(globalHidden);
            data = sampler.sampleToDataStream(sampleSize);

            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

                svb.updateModel(batch);
                BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

                //System.out.println("****************");
                //System.out.println(learntBN.toString());
                Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
                //System.out.println("Global Hidden: " + normal.getMean() + ", " + normal.getVariance());
                //System.out.println("****************");


                Normal_MultinomialNormalParents distA = learntBN.getConditionalDistribution(gaussianVar0);
                Normal_MultinomialNormalParents distATrue = naiveBayes.getConditionalDistribution(gaussianVar0);


                System.out.print(count + "\t" + normal.getMean() + "\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMeanRandomConceptDrift(naiveBayes));
                //System.out.print("\t" + distA.getNormal_NormalParentsDistribution(0).getIntercept() +"\t" + distA.getNormal_NormalParentsDistribution(1).getIntercept());
                //System.out.print("\t" + distA.getNormal_NormalParentsDistribution(0).getCoeffParents()[0] + "\t" + distA.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
                //System.out.print("\t" + distATrue.getNormal_NormalParentsDistribution(0).getIntercept() + "\t" + distATrue.getNormal_NormalParentsDistribution(1).getIntercept());
                System.out.println();

                count += windowSize;
            }
        }
    }


    public static void conceptDriftSmoothChanges(String[] args) {

        BayesianNetworkGenerator.setNumberOfGaussianVars(10);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(0, 2);
        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayesWithGlobalHiddenVar(2, "Global");

        naiveBayes.randomInitialization(new Random(1));

        Variable globalHidden = naiveBayes.getStaticVariables().getVariableByName("Global");
        Variable classVariable = naiveBayes.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = naiveBayes.getStaticVariables().getVariableByName("GaussianVar0");


        naiveBayes.getConditionalDistribution(globalHidden).randomInitialization(new Random(1));

        System.out.println(naiveBayes.toString());

        int windowSize = 100;
        int sampleSize = 5000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setHiddenVar(globalHidden);
        DataStream<DataInstance> data = sampler.sampleToDataStream(sampleSize);
        int count = windowSize;

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(Arrays.asList(globalHidden), true));
        svb.setTransitionMethod(new GaussianHiddenTransitionMethod(Arrays.asList(globalHidden), 1, 5));
        svb.setWindowsSize(windowSize);
        svb.setDAG(naiveBayes.getDAG());
        svb.initLearning();


        double acumLL = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            //BayesianNetwork learntBN = svb.getLearntBayesianNetwork();
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
            //System.out.println("H_prior:"+ normal.getMean() + "\t" + normal.getVariance());

            acumLL += svb.updateModel(batch);
            //System.out.println(count + "\t" +  acumLL/count);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            //System.out.println("****************");
            //System.out.println(learntBN.toString());
            normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
            //System.out.println("Global Hidden: " + normal.getMean() +", " + normal.getVariance());
            //System.out.println("****************");

            Normal distGH = naiveBayes.getConditionalDistribution(globalHidden);
            //System.out.println(count + "\t" + normal.getMean() + "\t" + normal.getVariance() +  "\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMeanSmoothConceptDrift(naiveBayes));
            System.out.print(count + "\t" + normal.getMean() + "\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMeanSmoothConceptDrift(naiveBayes));
            //System.out.print("\t" + distGH.getMean());

            System.out.println();
            count += windowSize;
        }


        for (int K = 1; K < 10; K++) {
            //System.out.println("******************************** CONCEPT DRIFT ********************************");
            naiveBayes.getConditionalDistribution(globalHidden).randomInitialization(new Random(K + 2));
            //((Normal)naiveBayes.getDistribution(globalHidden)).setVariance(0.1);

            //System.out.println(((Normal)naiveBayes.getDistribution(globalHidden)).getMean() +"\t" + ((Normal)naiveBayes.getDistribution(globalHidden)).getVariance());

            sampler = new BayesianNetworkSampler(naiveBayes);
            sampler.setHiddenVar(globalHidden);
            data = sampler.sampleToDataStream(sampleSize);

            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

                acumLL += svb.updateModel(batch);
                //System.out.println(count + "\t" +  acumLL/count);
                BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

                //System.out.println("****************");
                //System.out.println(learntBN.toString());
                Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
                //System.out.println("Global Hidden: " + normal.getMean() + ", " + normal.getVariance());
                //System.out.println("****************");

                Normal distGH = naiveBayes.getConditionalDistribution(globalHidden);
                //System.out.println(count + "\t" + normal.getMean() + "\t" + normal.getVariance() +  "\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMeanSmoothConceptDrift(naiveBayes));
                System.out.print(count + "\t" + normal.getMean() + "\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMeanSmoothConceptDrift(naiveBayes));
                //System.out.print("\t" + distGH.getMean());
                System.out.println();
                count += windowSize;
            }
        }
    }
    public static void conceptDriftSeaLevelMultipleGlobalHidden(String[] args) throws IOException {

        DataStream<DataInstance> data = DataStreamLoader.openFromFile("./IDA2015/DriftSets/hyperplane9.arff");


        int windowSizeModel = 100;
        int windowSizeData = 100;
        int count = windowSizeModel;
        double avACC = 0;



        NaiveBayesGaussianHiddenConceptDrift nb = new NaiveBayesGaussianHiddenConceptDrift();
        nb.setConceptDriftDetector(NaiveBayesGaussianHiddenConceptDrift.DriftDetector.GLOBAL);
        nb.setGlobalHidden(true);
        nb.setNumberOfGlobalVars(1);
        nb.setTransitionVariance(0.1);
        nb.setWindowsSize(windowSizeModel);
        nb.setData(data);

        nb.initLearning();




        Variable classVariable = nb.getClassVariable();


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSizeData)) {

            BayesianNetwork initBN = nb.getLearntBayesianNetwork();
            double accuracy = computeAccuracy(initBN, batch, classVariable);

            if (true) {
                double innerCount = 0;
                for (DataInstance instance : batch) {
                    //if (random.nextDouble()<0.9)
                    //    instance.setValue(classVariable, Utils.missingValue());
                    if (innerCount % 2 != 0)
                        instance.setValue(classVariable, Utils.missingValue());

                    innerCount++;
                }
            }

            if (false) {
                DataOnMemoryListContainer<DataInstance> newbatch = new DataOnMemoryListContainer(data.getAttributes());


                double innerCount = 0;
                for (DataInstance instance : batch) {
                    //if (random.nextDouble()<0.9)
                    //    instance.setValue(classVariable, Utils.missingValue());
                    if (innerCount % 2 == 0)
                        newbatch.add(instance);

                    innerCount++;
                }

                batch = newbatch;
            }

            nb.updateModel(batch);


            for (Variable hiddenVar :  nb.getHiddenVars()) {
                Normal normal = nb.getSvb().getPlateuStructure().getEFVariablePosterior(hiddenVar, 0).toUnivariateDistribution();
                System.out.print(count + "\t" + normal.getMean());
            }

//            BayesianNetwork learntBN = nb.getLearntBayesianNetwork();
//            Normal_MultinomialNormalParents dist1 = learntBN.getDistribution(at1);
//            Normal_MultinomialNormalParents dist2 = learntBN.getDistribution(at2);
//            Normal_MultinomialNormalParents dist3 = learntBN.getDistribution(at3);
//
//            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(0).getIntercept());
//            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(1).getIntercept());
//            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(0).getIntercept());
//            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(1).getIntercept());
//            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(0).getIntercept());
//            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(1).getIntercept());
//
//            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
//            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
//            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
//            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
//            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
//            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);

            System.out.print("\t" + accuracy);
            System.out.println();

            count += windowSizeData;
            avACC+= accuracy;

        }

        System.out.println(avACC/(count/windowSizeData));


    }

    public static void randomChangesMultipleGlobalHidden(String[] args) throws IOException {

        BayesianNetworkGenerator.setNumberOfGaussianVars(10);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(0, 2);
        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayesWithGlobalHiddenVar(2, "Global");

        naiveBayes.randomInitialization(new Random(0));

        Variable globalHidden = naiveBayes.getStaticVariables().getVariableByName("Global");
        Variable classVariable = naiveBayes.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = naiveBayes.getStaticVariables().getVariableByName("GaussianVar0");

        for (ConditionalDistribution dist : naiveBayes.getConditionalDistributions()) {
            if (dist.getVariable().equals(classVariable) || dist.getVariable().equals(globalHidden))
                continue;
            Normal_MultinomialNormalParents newdist = naiveBayes.getConditionalDistribution(dist.getVariable());
            //newdist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{0.0});
            newdist.getNormal_NormalParentsDistribution(0).setCoeffForParent(globalHidden, 0.0);
            //newdist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{0.0});
            newdist.getNormal_NormalParentsDistribution(1).setCoeffForParent(globalHidden, 0.0);
        }

        System.out.println(naiveBayes.toString());

        int windowSize =100;
        int sampleSize = 5000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setHiddenVar(globalHidden);
        DataStream<DataInstance> data = sampler.sampleToDataStream(sampleSize);
        int count = windowSize;
        double avACC = 0;



        NaiveBayesGaussianHiddenConceptDrift nb = new NaiveBayesGaussianHiddenConceptDrift();
        nb.setConceptDriftDetector(NaiveBayesGaussianHiddenConceptDrift.DriftDetector.GLOBAL);
        nb.setGlobalHidden(true);
        nb.setNumberOfGlobalVars(2);
        nb.setTransitionVariance(0.1);
        nb.setWindowsSize(windowSize);
        nb.setData(data);

        nb.initLearning();


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            BayesianNetwork initBN = nb.getLearntBayesianNetwork();
            double accuracy = computeAccuracy(initBN, batch, classVariable);

            if (false) {
                double innerCount = 0;
                for (DataInstance instance : batch) {
                    //if (random.nextDouble()<0.9)
                    //    instance.setValue(classVariable, Utils.missingValue());
                    if (count > 1000 && innerCount % 10 != 0)
                        instance.setValue(classVariable, Utils.missingValue());

                    innerCount++;
                }
            }

             nb.updateModel(batch);


            for (Variable hiddenVar :  nb.getHiddenVars()) {
                Normal normal = nb.getSvb().getPlateuStructure().getEFVariablePosterior(hiddenVar, 0).toUnivariateDistribution();
                System.out.print(count + "\t" + normal.getMean());
            }

//            BayesianNetwork learntBN = nb.getLearntBayesianNetwork();
//            Normal_MultinomialNormalParents dist1 = learntBN.getDistribution(at1);
//            Normal_MultinomialNormalParents dist2 = learntBN.getDistribution(at2);
//            Normal_MultinomialNormalParents dist3 = learntBN.getDistribution(at3);
//
//            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(0).getIntercept());
//            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(1).getIntercept());
//            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(0).getIntercept());
//            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(1).getIntercept());
//            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(0).getIntercept());
//            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(1).getIntercept());
//
//            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
//            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
//            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
//            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
//            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
//            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);

            System.out.print("\t" + accuracy);
            System.out.println();

            count += windowSize;
            avACC+= accuracy;

        }



        for (int K = 1; K < 10; K++) {
            //System.out.println("******************************** CONCEPT DRIFT ********************************");
            naiveBayes.randomInitialization(new Random(K + 1));

            for (ConditionalDistribution dist : naiveBayes.getConditionalDistributions()) {
                if (dist.getVariable().equals(classVariable) || dist.getVariable().equals(globalHidden))
                    continue;
                Normal_MultinomialNormalParents newdist = naiveBayes.getConditionalDistribution(dist.getVariable());
                newdist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{0.0});
                newdist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{0.0});
            }

            //System.out.println(naiveBayes.toString());

            sampler = new BayesianNetworkSampler(naiveBayes);
            sampler.setHiddenVar(globalHidden);
            data = sampler.sampleToDataStream(sampleSize);

            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

                BayesianNetwork initBN = nb.getLearntBayesianNetwork();
                double accuracy = computeAccuracy(initBN, batch, classVariable);

                if (false) {
                    double innerCount = 0;
                    for (DataInstance instance : batch) {
                        //if (random.nextDouble()<0.9)
                        //    instance.setValue(classVariable, Utils.missingValue());
                        if (count > 1000 && innerCount % 10 != 0)
                            instance.setValue(classVariable, Utils.missingValue());

                        innerCount++;
                    }
                }

                nb.updateModel(batch);


                for (Variable hiddenVar :  nb.getHiddenVars()) {
                    Normal normal = nb.getSvb().getPlateuStructure().getEFVariablePosterior(hiddenVar, 0).toUnivariateDistribution();
                    System.out.print(count + "\t" + normal.getMean());
                }

//            BayesianNetwork learntBN = nb.getLearntBayesianNetwork();
//            Normal_MultinomialNormalParents dist1 = learntBN.getDistribution(at1);
//            Normal_MultinomialNormalParents dist2 = learntBN.getDistribution(at2);
//            Normal_MultinomialNormalParents dist3 = learntBN.getDistribution(at3);
//
//            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(0).getIntercept());
//            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(1).getIntercept());
//            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(0).getIntercept());
//            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(1).getIntercept());
//            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(0).getIntercept());
//            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(1).getIntercept());
//
//            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
//            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
//            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
//            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
//            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
//            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);

                System.out.print("\t" + accuracy);
                System.out.println();

                count += windowSize;
                avACC+= accuracy;

            }

        }


        System.out.println(avACC/(count/windowSize));


    }


    public static void conceptDriftSeaLevel(String[] args) throws IOException {

        DataStream<DataInstance> data = DataStreamLoader.openFromFile("./IDA2015/DriftSets/sea.arff");

        Variables variables = new Variables(data.getAttributes());
        Variable globalHidden = variables.newGaussianVariable("Global");
        Variable classVariable = variables.getVariableByName("cl");
        Variable at1 = variables.getVariableByName("at1");
        Variable at2 = variables.getVariableByName("at2");
        Variable at3 = variables.getVariableByName("at3");

        DAG dag = new DAG(variables);
        dag.getParentSet(at1).addParent(classVariable);
        dag.getParentSet(at1).addParent(globalHidden);

        dag.getParentSet(at2).addParent(classVariable);
        dag.getParentSet(at2).addParent(globalHidden);

        dag.getParentSet(at3).addParent(classVariable);
        dag.getParentSet(at3).addParent(globalHidden);

        System.out.println(dag.toString());

        int windowSize = 500;
        int count = windowSize;
        int windowSizeData = 1000;


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setParallelMode(false);
        svb.setRandomRestart(false);
        svb.setSeed(1);
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(Arrays.asList(globalHidden), true));
        svb.setTransitionMethod(new GaussianHiddenTransitionMethod(Arrays.asList(globalHidden), 0, 0.000001));
        svb.setWindowsSize(windowSize);
        svb.setDAG(dag);
        svb.initLearning();

        //System.out.println(svb.getLearntBayesianNetwork().toString());

        Random random  = new Random(0);
        double acumLL = 0;
        double avACC = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSizeData)) {

            BayesianNetwork initBN = svb.getLearntBayesianNetwork();
            double accuracy = computeAccuracy(initBN, batch, classVariable);

            if (false) {
                double innerCount = 0;
                for (DataInstance instance : batch) {
                    //if (random.nextDouble()<0.9)
                    //    instance.setValue(classVariable, Utils.missingValue());
                    if (innerCount % 2 != 0)
                        instance.setValue(classVariable, Utils.missingValue());

                    innerCount++;
                }
            }

            acumLL += svb.updateModel(batch);

            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            //System.out.println(learntBN.toString());
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();

            Normal_MultinomialNormalParents dist1 = learntBN.getConditionalDistribution(at1);
            Normal_MultinomialNormalParents dist2 = learntBN.getConditionalDistribution(at2);
            Normal_MultinomialNormalParents dist3 = learntBN.getConditionalDistribution(at3);

            System.out.print(count + "\t" + normal.getMean());

            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(0).getIntercept());
            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(1).getIntercept());
            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(0).getIntercept());
            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(1).getIntercept());
            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(0).getIntercept());
            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(1).getIntercept());

            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);

            System.out.print("\t" + accuracy);
            System.out.println();

            count += windowSize;
            avACC+= accuracy;

        }

        System.out.println(avACC/(count/windowSize));

    }

    public static void localConceptDriftSeaLevel(String[] args) {

        DataStream<DataInstance> data = DataStreamLoader.openFromFile("./IDA2015/DriftSets/sea.arff");

        Variables variables = new Variables(data.getAttributes());

        List<Variable> localHidden = new ArrayList<Variable>();
        for (Attribute att : data.getAttributes()){
            if (att.getName().equals("cl"))
                continue;

            localHidden.add(variables.newGaussianVariable("Local_"+att.getName()));
        }

        Variable classVariable = variables.getVariableByName("cl");

        DAG dag = new DAG(variables);

        for (Attribute att : data.getAttributes()) {
            if (att.getName().equals("cl"))
                continue;

            Variable variable = variables.getVariableByName(att.getName());
            dag.getParentSet(variable).addParent(classVariable);
            dag.getParentSet(variable).addParent(variables.getVariableByName("Local_"+att.getName()));
        }


        System.out.println(dag.toString());

        int windowSize = 100;
        int count = windowSize;


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(localHidden, true));
        svb.setTransitionMethod(new GaussianHiddenTransitionMethod(localHidden, 0, 10));
        svb.setWindowsSize(windowSize);
        svb.setDAG(dag);
        svb.initLearning();

        //System.out.println(svb.getLearntBayesianNetwork().toString());

        Random random  = new Random(0);
        double acumLL = 0;
        double avACC = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            double accuracy = computeAccuracy(svb.getLearntBayesianNetwork(), batch, classVariable);

            for (DataInstance instance : batch){
                if (random.nextDouble()<0.0)
                    instance.setValue(classVariable, Utils.missingValue());
            }

            acumLL += svb.updateModel(batch);

            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            //System.out.println(learntBN.toString());
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(localHidden.get(1), 0).toUnivariateDistribution();
            System.out.print(count + "\t" + normal.getMean());

            normal = svb.getPlateuStructure().getEFVariablePosterior(localHidden.get(2), 0).toUnivariateDistribution();
            System.out.print("\t" + normal.getMean());

/*
            Normal_MultinomialNormalParents dist1 = learntBN.getDistribution(at1);
            Normal_MultinomialNormalParents dist2 = learntBN.getDistribution(at2);
            Normal_MultinomialNormalParents dist3 = learntBN.getDistribution(at3);

           System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(0).getIntercept());
            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(1).getIntercept());
            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(0).getIntercept());
            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(1).getIntercept());
            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(0).getIntercept());
            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(1).getIntercept());

            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]);
            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
*/
            System.out.print("\t" + accuracy);
            System.out.println();

            count += windowSize;
            avACC+= accuracy;

        }

        System.out.println(avACC/(count/windowSize));

    }

    public static double computeAccuracy(BayesianNetwork bn, DataOnMemory<DataInstance> data, Variable classVariable){

        double predictions = 0;
        VMP vmp = new VMP();
        vmp.setModel(bn);
        for (DataInstance instance : data) {
            double realValue = instance.getValue(classVariable);
            instance.setValue(classVariable, Utils.missingValue());
            vmp.setEvidence(instance);
            vmp.runInference();
            Multinomial posterior = vmp.getPosterior(classVariable);
            if (Utils.maxIndex(posterior.getProbabilities())==realValue)
                predictions++;

            instance.setValue(classVariable, realValue);
        }

        return predictions/data.getNumberOfDataInstances();
    }


    public static void conceptDriftHyperplane(String[] args) {

        DataStream<DataInstance> data = DataStreamLoader.openFromFile("./IDA2015/DriftSets/hyperplane9.arff");

        Variables variables = new Variables(data.getAttributes());
        Variable globalHidden = variables.newGaussianVariable("Global");
        Variable classVariable = variables.getVariableByName("output");

        DAG dag = new DAG(variables);

        for (int i = 0; i < 10; i++) {
            Variable att = variables.getVariableByName("attr"+i);
            dag.getParentSet(att).addParent(classVariable);
            dag.getParentSet(att).addParent(globalHidden);

        }

        System.out.println(dag.toString());

        int windowSize = 10;
        int count = windowSize;


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setParallelMode(true);
        svb.setSeed(0);
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(Arrays.asList(globalHidden), true));
        svb.setTransitionMethod(new GaussianHiddenTransitionMethod(Arrays.asList(globalHidden), 0, 0.1));
        svb.setWindowsSize(windowSize);
        svb.setDAG(dag);
        svb.initLearning();

        //System.out.println(svb.getLearntBayesianNetwork().toString());

        Random random = new Random(0);
        double acumLL = 0;
        double avAcc = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            double accuracy = computeAccuracy(svb.getLearntBayesianNetwork(), batch, classVariable);

            /*
            for (DataInstance instance : batch){
                if (random.nextDouble()<0.5)
                    instance.setValue(classVariable, Utils.missingValue());
            }*/

            acumLL += svb.updateModel(batch);

            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();
            //System.out.println(learntBN.toString());
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
            normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();

            System.out.println(count + "\t" + normal.getMean() +"\t" + accuracy);

            count += windowSize;
            avAcc += accuracy;
        }

        System.out.println(avAcc/(count/windowSize));

    }

    public static void conceptDriftElectricyt(String[] args) {

        DataStream<DataInstance> data = DataStreamLoader.openFromFile("./IDA2015/DriftSets/electricityOriginal.arff");

        Variables variables = new Variables(data.getAttributes());
        Variable globalHidden = variables.newGaussianVariable("Global");
        Variable classVariable = variables.getVariableByName("class");
        //Variable localVariable = variables.newMultionomialVariable("local",2);

        DAG dag = new DAG(variables);

        for (int i = 1; i < 3; i++) {
            Variable att = variables.getVariableById(i);
            dag.getParentSet(att).addParent(classVariable);
            dag.getParentSet(att).addParent(globalHidden);
            //dag.getParentSet(att).addParent(localVariable);
        }
        //dag.getParentSet(localVariable).addParent(classVariable);

        System.out.println(dag.toString());

        int windowSize = 1460;
        int count = windowSize;


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(Arrays.asList(globalHidden), true));
        svb.setTransitionMethod(new GaussianHiddenTransitionMethod(Arrays.asList(globalHidden), 0, 0.1));
        svb.setWindowsSize(windowSize);
        svb.setDAG(dag);
        svb.initLearning();

        //System.out.println(svb.getLearntBayesianNetwork().toString());

        int countMonth = 0;
        Random random = new Random(0);
        double acumLL = 0;
        double avAcc = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            //if (countMonth<12) {
            //    countMonth++;
            //    continue;
            //}


            double accuracy = computeAccuracy(svb.getLearntBayesianNetwork(), batch, classVariable);

            for (DataInstance instance : batch){
                if (random.nextDouble()<0.0)
                    instance.setValue(classVariable, Utils.missingValue());
            }

            acumLL += svb.updateModel(batch);

            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();
            //System.out.println(learntBN.toString());
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
            normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();

            System.out.println(count + "\t" + normal.getMean() +"\t" + accuracy);

            count += windowSize;
            avAcc += accuracy;
        }

        System.out.println(svb.getLearntBayesianNetwork().toString());
        System.out.println(avAcc/(count/windowSize));

    }
    public static void conceptDriftHyperplaneLocal(String[] args) {

        DataStream<DataInstance> data = DataStreamLoader.openFromFile("./IDA2015/DriftSets/hyperplane9.arff");

        Variables variables = new Variables(data.getAttributes());
        Variable globalHidden = variables.newGaussianVariable("Global");
        Variable localHidden = variables.newMultionomialVariable("Local", 2);

        Variable classVariable = variables.getVariableByName("output");

        DAG dag = new DAG(variables);

        for (int i = 0; i < 10; i++) {
            Variable att = variables.getVariableByName("attr"+i);
            dag.getParentSet(att).addParent(classVariable);
            dag.getParentSet(att).addParent(globalHidden);
            dag.getParentSet(att).addParent(localHidden);
        }
        dag.getParentSet(localHidden).addParent(classVariable);

        System.out.println(dag.toString());

        int windowSize = 1000;
        int count = windowSize;


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(Arrays.asList(globalHidden), true));
        svb.setTransitionMethod(new GaussianHiddenTransitionMethod(Arrays.asList(globalHidden), 1, 5));
        svb.setWindowsSize(windowSize);
        svb.setDAG(dag);
        svb.initLearning();

        //System.out.println(svb.getLearntBayesianNetwork().toString());

        double acumLL = 0;
        double avAcc = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            double accuracy = computeAccuracy(svb.getLearntBayesianNetwork(), batch, classVariable);

            acumLL += svb.updateModel(batch);

            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();
            //System.out.println(learntBN.toString());
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
            normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();

            System.out.println(count + "\t" + normal.getMean() +"\t" + accuracy);

            count += windowSize;
            avAcc += accuracy;
        }

        System.out.println(avAcc/(count/windowSize));

    }

    public static void conceptDriftSeaLevelFading(String[] args) {

        DataStream<DataInstance> data = DataStreamLoader.openFromFile("./IDA2015/DriftSets/sea.arff");

            Variables variables = new Variables(data.getAttributes());
        Variable classVariable = variables.getVariableByName("cl");
        Variable at1 = variables.getVariableByName("at1");
        Variable at2 = variables.getVariableByName("at2");
        Variable at3 = variables.getVariableByName("at3");

        DAG dag = new DAG(variables);
        dag.getParentSet(at1).addParent(classVariable);

        dag.getParentSet(at2).addParent(classVariable);

        dag.getParentSet(at3).addParent(classVariable);

        System.out.println(dag.toString());

        int windowSize = 1000;
        int count = windowSize;


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        svb.setPlateuStructure(new PlateuIIDReplication());
        svb.setTransitionMethod(new Fading(0.0001));
        svb.setWindowsSize(windowSize);
        svb.setDAG(dag);
        svb.initLearning();

        //System.out.println(svb.getLearntBayesianNetwork().toString());

        double acumLL = 0;
        double avACC = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            double accuracy = computeAccuracy(svb.getLearntBayesianNetwork(), batch, classVariable);

            acumLL += svb.updateModel(batch);

            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            Normal_MultinomialParents dist1 = learntBN.getConditionalDistribution(at1);
            Normal_MultinomialParents dist2 = learntBN.getConditionalDistribution(at2);
            Normal_MultinomialParents dist3 = learntBN.getConditionalDistribution(at3);
            System.out.print(count);
            System.out.print("\t" + dist1.getNormal(0).getMean());
            System.out.print("\t" + dist1.getNormal(1).getMean());
            System.out.print("\t" + dist2.getNormal(0).getMean());
            System.out.print("\t" + dist2.getNormal(1).getMean());
            System.out.print("\t" + dist3.getNormal(0).getMean());
            System.out.print("\t" + dist3.getNormal(1).getMean());
            System.out.print("\t" + accuracy);
            System.out.println();

            count += windowSize;
            avACC+= accuracy;
        }

        System.out.println(svb.getLearntBayesianNetwork().toString());
        System.out.println(avACC/(count/windowSize));
    }




    public static void conceptDriftIDA2015(String[] args) throws IOException {

        DataStream<DataInstance> data = DataStreamLoader.openFromFile(args[0]);
        //DataStream<DataInstance> data = DataStreamLoader.openFromFile("./IDA2015/DriftSets/hyperplane9.arff");
        //DataStream<DataInstance> data = DataStreamLoader.openFromFile("./IDA2015/IDA2015 Artificial Data/HYP1.arff");

        int windowsSize = Integer.parseInt(args[1]);
        int windowSizeModel = windowsSize;
        int windowSizeData = windowsSize;
        int count = windowSizeModel;
        double avACC = 0;

        boolean isNB = Integer.parseInt(args[2])==1;

        NaiveBayesGaussianHiddenConceptDrift nb = new NaiveBayesGaussianHiddenConceptDrift();
        nb.setConceptDriftDetector(NaiveBayesGaussianHiddenConceptDrift.DriftDetector.GLOBAL);
        nb.setGlobalHidden(!isNB);
        nb.setNumberOfGlobalVars(1);
        nb.setTransitionVariance(0.1);
        nb.setWindowsSize(windowSizeModel);
        nb.setData(data);

        nb.initLearning();




        Variable classVariable = nb.getClassVariable();


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSizeData)) {

            BayesianNetwork initBN = nb.getLearntBayesianNetwork();
            double accuracy = computeAccuracy(initBN, batch, classVariable);

            if (false) {
                double innerCount = 0;
                for (DataInstance instance : batch) {
                    //if (random.nextDouble()<0.9)
                    //    instance.setValue(classVariable, Utils.missingValue());
                    if (innerCount % 2 != 0)
                        instance.setValue(classVariable, Utils.missingValue());

                    innerCount++;
                }
            }

            if (false) {
                DataOnMemoryListContainer<DataInstance> newbatch = new DataOnMemoryListContainer(data.getAttributes());


                double innerCount = 0;
                for (DataInstance instance : batch) {
                    //if (random.nextDouble()<0.9)
                    //    instance.setValue(classVariable, Utils.missingValue());
                    if (innerCount % 2 == 0)
                        newbatch.add(instance);

                    innerCount++;
                }

                batch = newbatch;
            }

            nb.updateModel(batch);


            for (Variable hiddenVar :  nb.getHiddenVars()) {
                Normal normal = nb.getSvb().getPlateuStructure().getEFVariablePosterior(hiddenVar, 0).toUnivariateDistribution();
                System.out.print(count + "\t" + normal.getMean());
            }

            System.out.print("\t" + accuracy);

            double[] param = nb.getLearntBayesianNetwork().getParameters();
            for (int i = 0; i < param.length; i++) {
                System.out.print("\t"+param[i]);
            }


            System.out.println();

            count += windowSizeData;
            avACC+= accuracy;

        }

        System.out.println(avACC/(count/windowSizeData));


    }


    public static void main(String[] args) throws IOException {
        //GlobalHiddenConceptDrift.conceptDriftWithRandomChangesMissingLabels(args);
        //GlobalHiddenConceptDrift.randomChangesMultipleGlobalHidden(args);
        //GlobalHiddenConceptDrift.conceptDriftSeaLevelMultipleGlobalHidden(args);
        //GlobalHiddenConceptDrift.conceptDriftHyperplane(args);
        //GlobalHiddenConceptDrift.conceptDriftElectricyt(args);
        GlobalHiddenConceptDrift.conceptDriftIDA2015(args);
        //GlobalHiddenConceptDrift.conceptDriftSeaLevelFading(args);

    }
}