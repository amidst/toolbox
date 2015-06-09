/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.ida2015;

import com.google.common.base.Stopwatch;
import eu.amidst.corestatic.distribution.*;
import eu.amidst.corestatic.datastream.Attribute;
import eu.amidst.corestatic.datastream.DataInstance;
import eu.amidst.corestatic.datastream.DataOnMemory;
import eu.amidst.corestatic.datastream.DataStream;
import eu.amidst.corestatic.inference.messagepassing.VMP;
import eu.amidst.corestatic.io.BayesianNetworkLoader;
import eu.amidst.corestatic.io.DataStreamLoader;
import eu.amidst.corestatic.io.DataStreamWriter;
import eu.amidst.corestatic.learning.parametric.*;
import eu.amidst.corestatic.learning.parametric.bayesian.Fading;
import eu.amidst.corestatic.learning.parametric.bayesian.StreamingVariationalBayesVMP;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.models.DAG;
import eu.amidst.corestatic.utils.BayesianNetworkSampler;
import eu.amidst.corestatic.variables.StaticVariables;
import eu.amidst.corestatic.variables.Variable;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

/**
 * Created by andresmasegosa on 07/04/15.
 */
public class LearningVMPTests {

    public static void testMixtureOfCLG() throws IOException, ClassNotFoundException {


        StaticVariables variables = new StaticVariables();

        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varB);
        dag.getParentSet(varA).addParent(varC);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        bn.randomInitialization(new Random(0));

        Normal_MultinomialNormalParents distA = bn.getConditionalDistribution(varA);

        distA.getNormal_NormalParentsDistribution(0).setIntercept(1.0);
        distA.getNormal_NormalParentsDistribution(1).setIntercept(2.0);



        System.out.println(bn.toString());

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        //sampler.setSeed(1);
        sampler.setHiddenVar(varC);
        DataStream<DataInstance> data = sampler.sampleToDataStream(1000);

        DataStreamWriter.writeDataToFile(data, "./datasets/tmp.arff");

        int windowSize = 10;

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        //svb.setSeed(1);
        //svb.setFading(0.9);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setWindowsSize(windowSize);
        svb.setDAG(bn.getDAG());
        svb.initLearning();

        int count=0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {
            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            Normal_MultinomialNormalParents dist = learntBN.getConditionalDistribution(varA);

            //System.out.println(count +"\t"+ dist.getNormal_NormalParentsDistribution(0).getIntercept() + "\t"+ dist.getNormal_NormalParentsDistribution(1).getIntercept());
            System.out.println(dist.getNormal_NormalParentsDistribution(0).getIntercept() + "\t"+ dist.getNormal_NormalParentsDistribution(1).getIntercept());

            count+=windowSize;
        }

    }



    public static void testMixtureOfCLGConceptDrift() throws IOException, ClassNotFoundException {


        StaticVariables variables = new StaticVariables();

        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varB);
        dag.getParentSet(varA).addParent(varC);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        bn.randomInitialization(new Random(0));

        Normal_MultinomialNormalParents distA = bn.getConditionalDistribution(varA);

        distA.getNormal_NormalParentsDistribution(0).setIntercept(1.0);
        distA.getNormal_NormalParentsDistribution(1).setIntercept(2.0);



        System.out.println(bn.toString());

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setHiddenVar(varC);
        DataStream<DataInstance> data = sampler.sampleToDataStream(1000);

        int windowSize = 10;

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        svb.setTransitionMethod(new Fading(0.9));
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setWindowsSize(windowSize);
        svb.setDAG(bn.getDAG());
        svb.initLearning();

        int count=0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {
            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            Normal_MultinomialNormalParents dist = learntBN.getConditionalDistribution(varA);

            System.out.println(count +"\t"+ dist.getNormal_NormalParentsDistribution(0).getIntercept() + "\t"+ dist.getNormal_NormalParentsDistribution(1).getIntercept());

            count+=windowSize;
        }

        /*************************************************/
        /********** CONCEPT DRIFT ***********************/
        /*************************************************/


        distA.getNormal_NormalParentsDistribution(0).setIntercept(-1.0);
        distA.getNormal_NormalParentsDistribution(1).setIntercept(-2.0);


        sampler = new BayesianNetworkSampler(bn);
        sampler.setHiddenVar(varC);
        data = sampler.sampleToDataStream(1000);


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {
            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            Normal_MultinomialNormalParents dist = learntBN.getConditionalDistribution(varA);

            System.out.println(count +"\t"+ dist.getNormal_NormalParentsDistribution(0).getIntercept() + "\t"+ dist.getNormal_NormalParentsDistribution(1).getIntercept());

            count+=windowSize;
        }

    }


    public static void testClusteringCLGConceptDrift() throws IOException, ClassNotFoundException {


        StaticVariables variables = new StaticVariables();

        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varB);
        dag.getParentSet(varA).addParent(varC);
        dag.getParentSet(varB).addParent(varC);


        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        bn.randomInitialization(new Random(0));

        Multinomial distC = bn.getConditionalDistribution(varC);
        distC.setProbabilityOfState(0,0.5);
        distC.setProbabilityOfState(1,0.5);

        Normal_MultinomialParents distB = bn.getConditionalDistribution(varB);
        distB.getNormal(0).setMean(-3.0);
        distB.getNormal(1).setMean(3.0);
        distB.getNormal(0).setVariance(1.0);
        distB.getNormal(1).setVariance(1.0);

        Normal_MultinomialNormalParents distA = bn.getConditionalDistribution(varA);

        //distA.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
        distA.getNormal_NormalParentsDistribution(0).setCoeffForParent(varB, 1.0);
        //distA.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{2.0});
        distA.getNormal_NormalParentsDistribution(1).setCoeffForParent(varB, 2.0);

        distA.getNormal_NormalParentsDistribution(0).setIntercept(0.0);
        distA.getNormal_NormalParentsDistribution(1).setIntercept(0.0);



        System.out.println(bn.toString());

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(10);
        //sampler.setHiddenVar(varC);
        DataStream<DataInstance> data = sampler.sampleToDataStream(1000);

        int windowSize = 200;

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(3);
        svb.setTransitionMethod(new Fading(0.1));
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setWindowsSize(windowSize);
        svb.setDAG(bn.getDAG());
        svb.initLearning();

        int count=0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {
            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            distB = learntBN.getConditionalDistribution(varB);
            double meanB0 =  distB.getNormal(0).getMean();
            double meanB1 =  distB.getNormal(1).getMean();

            distA = learntBN.getConditionalDistribution(varA);

            double meanA0  = distA.getNormal_NormalParentsDistribution(0).getIntercept()
                    + distA.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]*meanB0;

            double meanA1  = distA.getNormal_NormalParentsDistribution(1).getIntercept()
                    + distA.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]*meanB1;


            System.out.println(count + "\t" + meanB0 + "\t" + meanA0 + "\t" + meanB1 + "\t" + meanA1 + "\t" );


            count+=windowSize;
        }

        /*************************************************/
        /********** CONCEPT DRIFT ***********************/
        /*************************************************/

        distB = bn.getConditionalDistribution(varB);
        distB.getNormal(0).setMean(-5.0);
        distB.getNormal(1).setMean(5.0);


        distA = bn.getConditionalDistribution(varA);
        distA.getNormal_NormalParentsDistribution(0).setIntercept(0.0);
        distA.getNormal_NormalParentsDistribution(1).setIntercept(0.0);
        //distA.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{-1.0});
        distA.getNormal_NormalParentsDistribution(0).setCoeffForParent(varB, -1.0);
        //distA.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{-2.0});
        distA.getNormal_NormalParentsDistribution(1).setCoeffForParent(varB, -2.0);

        System.out.println(bn.toString());

        sampler = new BayesianNetworkSampler(bn);
        //sampler.setHiddenVar(varC);
        data = sampler.sampleToDataStream(10000);


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {
            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            distB = learntBN.getConditionalDistribution(varB);
            double meanB0 =  distB.getNormal(0).getMean();
            double meanB1 =  distB.getNormal(1).getMean();

            distA = learntBN.getConditionalDistribution(varA);

            double meanA0  = distA.getNormal_NormalParentsDistribution(0).getIntercept()
                    + distA.getNormal_NormalParentsDistribution(0).getCoeffParents()[0]*meanB0;

            double meanA1  = distA.getNormal_NormalParentsDistribution(1).getIntercept()
                    + distA.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]*meanB1;


            System.out.println(count + "\t" + meanB0 + "\t" + meanA0 + "\t" + meanB1 + "\t" + meanA1 + "\t" );

            count+=windowSize;
        }

    }



    public static void testCLGConceptDriftWithHiddenContinuous() throws IOException, ClassNotFoundException {


        StaticVariables variables = new StaticVariables();

        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varB);
        dag.getParentSet(varA).addParent(varC);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        bn.randomInitialization(new Random(0));

        Normal distB = bn.getConditionalDistribution(varB);
        distB.setMean(1.0);

        ConditionalLinearGaussian distA = bn.getConditionalDistribution(varA);

        distA.setIntercept(0);
        //distA.setCoeffParents(new double[]{1.0,0.0});
        distA.setCoeffForParent(varB, 1.0);
        distA.setCoeffForParent(varC, 0.0);


        System.out.println(bn.toString());
        int windowSize = 10;
        int sampleSize = 1000;


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(1);
        sampler.setHiddenVar(varC);
        DataStream<DataInstance> data = sampler.sampleToDataStream(sampleSize);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        svb.setTransitionMethod(new Fading(0.95));
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setWindowsSize(windowSize);
        svb.setDAG(bn.getDAG());
        svb.initLearning();

        int count=0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {
            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            Normal distC = learntBN.getConditionalDistribution(varC);

            distB = learntBN.getConditionalDistribution(varB);

            distA = learntBN.getConditionalDistribution(varA);

            double meanA = distA.getIntercept() + distB.getMean()*distA.getCoeffParents()[0] + distC.getMean()*distA.getCoeffParents()[1];

            System.out.println(count +"\t"+ meanA);

            count+=windowSize;
        }

        /*************************************************/
        /********** CONCEPT DRIFT ***********************/
        /*************************************************/



        distA = bn.getConditionalDistribution(varA);
        distA.setIntercept(0);
        distA.setCoeffParents(new double[]{2.0, 0.0});


        sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(1);
        sampler.setHiddenVar(varC);
        data = sampler.sampleToDataStream(sampleSize);


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {
            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            Normal distC = learntBN.getConditionalDistribution(varC);

            distB = learntBN.getConditionalDistribution(varB);

            distA = learntBN.getConditionalDistribution(varA);

            double meanA = distA.getIntercept() + distB.getMean()*distA.getCoeffParents()[0] + distC.getMean()*distA.getCoeffParents()[1];

            System.out.println(count +"\t"+ meanA);

            count+=windowSize;
        }

    }

    public static void testCLGConceptDriftHiddenMultinomial() throws IOException, ClassNotFoundException {


        StaticVariables variables = new StaticVariables();

        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newMultionomialVariable("C",2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varB);
        dag.getParentSet(varA).addParent(varC);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        bn.randomInitialization(new Random(0));

        Normal distB = bn.getConditionalDistribution(varB);
        distB.setMean(1.0);

        Normal_MultinomialNormalParents distA = bn.getConditionalDistribution(varA);

        distA.getNormal_NormalParentsDistribution(0).setIntercept(0);
        distA.getNormal_NormalParentsDistribution(1).setIntercept(0);

        //distA.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
        distA.getNormal_NormalParentsDistribution(0).setCoeffForParent(varB, 1.0);
        //distA.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
        distA.getNormal_NormalParentsDistribution(1).setCoeffForParent(varB, 1.0);


        System.out.println(bn.toString());
        int windowSize = 10;
        int sampleSize = 1000;


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(1);
        sampler.setHiddenVar(varC);
        DataStream<DataInstance> data = sampler.sampleToDataStream(sampleSize);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        svb.setTransitionMethod(new Fading(0.95));
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setWindowsSize(windowSize);
        svb.setDAG(bn.getDAG());
        svb.initLearning();

        int count=0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {
            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            Multinomial distC = learntBN.getConditionalDistribution(varC);

            distB = learntBN.getConditionalDistribution(varB);

            distA = learntBN.getConditionalDistribution(varA);

            double meanA = (distA.getNormal_NormalParentsDistribution(0).getIntercept() + distB.getMean()*distA.getNormal_NormalParentsDistribution(0).getCoeffParents()[0])*distC.getProbabilityOfState(0);
            meanA += (distA.getNormal_NormalParentsDistribution(1).getIntercept() + distB.getMean()*distA.getNormal_NormalParentsDistribution(1).getCoeffParents()[0])*distC.getProbabilityOfState(1);


            System.out.println(count +"\t"+ meanA);

            count+=windowSize;
        }

        /*************************************************/
        /********** CONCEPT DRIFT ***********************/
        /*************************************************/



        distA = bn.getConditionalDistribution(varA);
        distA.getNormal_NormalParentsDistribution(0).setIntercept(0);
        distA.getNormal_NormalParentsDistribution(1).setIntercept(0);

        //distA.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{2.0});
        distA.getNormal_NormalParentsDistribution(0).setCoeffForParent(varB, 2.0);
        //distA.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{2.0});
        distA.getNormal_NormalParentsDistribution(1).setCoeffForParent(varB, 2.0);


        sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(1);
        sampler.setHiddenVar(varC);
        data = sampler.sampleToDataStream(sampleSize);


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {
            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            Multinomial distC = learntBN.getConditionalDistribution(varC);

            distB = learntBN.getConditionalDistribution(varB);

            distA = learntBN.getConditionalDistribution(varA);

            double meanA = (distA.getNormal_NormalParentsDistribution(0).getIntercept() + distB.getMean()*distA.getNormal_NormalParentsDistribution(0).getCoeffParents()[0])*distC.getProbabilityOfState(0);
            meanA += (distA.getNormal_NormalParentsDistribution(1).getIntercept() + distB.getMean()*distA.getNormal_NormalParentsDistribution(1).getCoeffParents()[0])*distC.getProbabilityOfState(1);

            System.out.println(count +"\t"+ meanA);

            count+=windowSize;
        }

    }

    public static void testCompareBatchSizes() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        for (int i = 0; i < 1; i++) {

            System.out.println("\nNormal|Normal variable network \n ");

            normalVarBN.randomInitialization(new Random(i));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(i);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

            Attribute attVarA = data.getAttributes().getAttributeByName("A");
            Attribute attVarB = data.getAttributes().getAttributeByName("B");

            Variable varA = normalVarBN.getStaticVariables().getVariableByName("A");
            Variable varB = normalVarBN.getStaticVariables().getVariableByName("B");


            BayesianNetwork learntNormalVarBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);
            String beta0fromML = Double.toString(((ConditionalLinearGaussian) learntNormalVarBN.
                    getConditionalDistribution(varA)).getIntercept());
            String beta1fromML = Double.toString(((ConditionalLinearGaussian) learntNormalVarBN.
                    getConditionalDistribution(varA)).getCoeffParents()[0]);

            /**
             * Incremental sample mean
             */
            String sampleMeanB = "";
            double incrementalMeanB = 0;
            double index = 1;
            for (DataInstance dataInstance : data) {
                incrementalMeanB = incrementalMeanB + (dataInstance.getValue(attVarB) - incrementalMeanB) / index;
                sampleMeanB += incrementalMeanB + ", ";
                index++;
            }

            /**
             * Streaming Variational Bayes for batches of 1 sample
             */
            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            svb.setDAG(normalVarBN.getDAG());
            svb.setDataStream(data);


            String varA_Beta0output = "Variable A beta0 (CLG)\n" + beta0fromML + "\n",
                    varA_Beta1output = "Variable A beta1 (CLG)\n" + beta1fromML + "\n",
                    varBoutput = "Variable B mean (univ. normal)\n" + sampleMeanB + "\n";
            int[] windowsSizes = {1, 10, 100, 1000};
            for (int j = 0; j < windowsSizes.length; j++) {
                svb.setWindowsSize(windowsSizes[j]);
                svb.initLearning();
                String svbBeta0A = "", svbBeta1A = "", svbMeanB = "";
                Iterator<DataOnMemory<DataInstance>> batchIterator = data.streamOfBatches(windowsSizes[j]).iterator();
                while (batchIterator.hasNext()) {
                    DataOnMemory<DataInstance> batch = batchIterator.next();
                    svb.updateModel(batch);
                    ConditionalLinearGaussian distAsample = svb.getLearntBayesianNetwork().
                            getConditionalDistribution(varA);
                    double beta0A = distAsample.getIntercept();
                    double beta1A = distAsample.getCoeffParents()[0];
                    svbBeta0A += beta0A + ", ";
                    svbBeta1A += beta1A + ", ";
                    Normal distBsample = (Normal) ((BaseDistribution_MultinomialParents) svb.getLearntBayesianNetwork().
                            getConditionalDistribution(varB)).getBaseDistribution(0);
                    svbMeanB += distBsample.getMean() + ", ";
                }
                varA_Beta0output += svbBeta0A + "\n";
                varA_Beta1output += svbBeta1A + "\n";
                varBoutput += svbMeanB + "\n";
            }

            System.out.println(varA_Beta0output);
            System.out.println(varA_Beta1output);
            System.out.println(varBoutput);
        }

    }

    public static void testCompareBatchSizesParallelMode() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        for (int i = 0; i < 1; i++) {

            System.out.println("\nNormal|Normal variable network \n ");

            normalVarBN.randomInitialization(new Random(i));


            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(i);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

            Attribute attVarA = data.getAttributes().getAttributeByName("A");
            Attribute attVarB = data.getAttributes().getAttributeByName("B");

            Variable varA = normalVarBN.getStaticVariables().getVariableByName("A");
            Variable varB = normalVarBN.getStaticVariables().getVariableByName("B");


            BayesianNetwork learntNormalVarBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);

            //System.out.println(learntNormalVarBN.toString());


            String meanfromML = Double.toString(((Normal) ((BaseDistribution_MultinomialParents) learntNormalVarBN.
                    getConditionalDistribution(varB)).getBaseDistribution(0)).getMean());
            String varianceBfromML = Double.toString(((Normal) ((BaseDistribution_MultinomialParents) learntNormalVarBN.
                    getConditionalDistribution(varB)).getBaseDistribution(0)).getVariance());
            String beta0fromML = Double.toString(((ConditionalLinearGaussian) learntNormalVarBN.
                    getConditionalDistribution(varA)).getIntercept());
            String beta1fromML = Double.toString(((ConditionalLinearGaussian) learntNormalVarBN.
                    getConditionalDistribution(varA)).getCoeffParents()[0]);
            String varianceAfromML = Double.toString(((ConditionalLinearGaussian) learntNormalVarBN.
                    getConditionalDistribution(varA)).getVariance());


            /**
             * Incremental sample mean
             */
            String sampleMeanB = "";
            double incrementalMeanB = 0;
            double index = 1;
            for (DataInstance dataInstance : data) {
                incrementalMeanB = incrementalMeanB + (dataInstance.getValue(attVarB) - incrementalMeanB) / index;
                sampleMeanB += incrementalMeanB + ", ";
                index++;
            }

            /**
             * Streaming Variational Bayes for batches of different sizes
             */
            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setParallelMode(false);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setOutput(false);
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            svb.setDAG(normalVarBN.getDAG());
            svb.setDataStream(data);


            int[] windowsSizes = {1, 2, 10, 100, 1000};
            //[serial, ParallelSeqQ, ParallelRandQ][mean, beta0, beta1, varianceB, varianceA,nIter][windowSizes.length]
            String[][][] outputPerWindowSize = new String[3][6][windowsSizes.length];
            boolean[] parallelMode = {false, true, true};
            boolean[] randomRestart = {false, false, true};
            boolean[] outputMode = {false, false, false};

            for (int e = 0; e < parallelMode.length; e++) {
                vmp.setOutput(outputMode[e]);
                svb.setParallelMode(parallelMode[e]);
                svb.setRandomRestart(randomRestart[e]);
                for (int j = 0; j < windowsSizes.length; j++) {
                    //System.out.println("Window: "+windowsSizes[j]);
                    svb.setWindowsSize(windowsSizes[j]);
                    svb.initLearning();
                    svb.runLearning();
                    BayesianNetwork svbSerial = svb.getLearntBayesianNetwork();

                    outputPerWindowSize[e][0][j] = Double.toString(((Normal) ((BaseDistribution_MultinomialParents) svbSerial.
                            getConditionalDistribution(varB)).getBaseDistribution(0)).getMean());
                    outputPerWindowSize[e][1][j] = Double.toString(((ConditionalLinearGaussian) svbSerial.
                            getConditionalDistribution(varA)).getIntercept());
                    outputPerWindowSize[e][2][j] = Double.toString(((ConditionalLinearGaussian) svbSerial.
                            getConditionalDistribution(varA)).getCoeffParents()[0]);
                    outputPerWindowSize[e][3][j] = Double.toString(((Normal) ((BaseDistribution_MultinomialParents) svbSerial.
                            getConditionalDistribution(varB)).getBaseDistribution(0)).getVariance());
                    outputPerWindowSize[e][4][j] = Double.toString(((ConditionalLinearGaussian) svbSerial.
                            getConditionalDistribution(varA)).getVariance());
                    outputPerWindowSize[e][5][j] = Double.toString(svb.getAverageNumOfIterations());
                }
            }


            System.out.println("Mean of B");
            System.out.println("WindowSize \t ML \t Serial \t ParallelSeqQ \t ParallelRandQ");
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(windowsSizes[j] + "\t" + meanfromML + "\t" + outputPerWindowSize[0][0][j] + "\t" +
                        outputPerWindowSize[1][0][j] + "\t" + outputPerWindowSize[2][0][j]);
            }

            System.out.println("Beta0 of A");
            System.out.println("WindowSize\t ML \t Serial \t ParallelSeqQ \t ParallelRandQ");
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(windowsSizes[j] + "\t" + beta0fromML + "\t" + outputPerWindowSize[0][1][j] + "\t" +
                        outputPerWindowSize[1][1][j] + "\t" + outputPerWindowSize[2][1][j]);
            }

            System.out.println("Beta1 of A");
            System.out.println("WindowSize\t ML \t Serial \t ParallelSeqQ \t ParallelRandQ");
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(windowsSizes[j] + "\t" + beta1fromML + "\t" + outputPerWindowSize[0][2][j] + "\t" +
                        outputPerWindowSize[1][2][j] + "\t" + outputPerWindowSize[2][2][j]);
            }

            System.out.println("Variance of B");
            System.out.println("WindowSize\t ML \t Serial \t ParallelSeqQ \t ParallelRandQ");
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(windowsSizes[j] + "\t" + varianceBfromML + "\t" + outputPerWindowSize[0][3][j] + "\t" +
                        outputPerWindowSize[1][3][j] + "\t" + outputPerWindowSize[2][3][j]);
            }

            System.out.println("Variance of A");
            System.out.println("WindowSize\t ML \t Serial \t ParallelSeqQ \t ParallelRandQ");
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(windowsSizes[j] + "\t" + varianceAfromML + "\t" + outputPerWindowSize[0][4][j] + "\t" +
                        outputPerWindowSize[1][4][j] + "\t" + outputPerWindowSize[2][4][j]);
            }

            System.out.println("Average #iterations");
            System.out.println("WindowSize \t Serial \t ParallelSeqQ \t ParallelRandQ");
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(windowsSizes[j] + "\t" + outputPerWindowSize[0][5][j] + "\t" +
                        outputPerWindowSize[1][5][j] + "\t" + outputPerWindowSize[2][5][j]);
            }

            //svb.runLearningOnParallelForDifferentBatchWindows(windowsSizes, beta0fromML, beta1fromML, sampleMeanB);
        }

    }

    public static void testCompareBatchSizesFadingVMP() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        for (int i = 0; i < 1; i++) {

            System.out.println("\nNormal|Normal variable network \n ");

            normalVarBN.randomInitialization(new Random(i));


            Variable varA = normalVarBN.getStaticVariables().getVariableByName("A");
            Variable varB = normalVarBN.getStaticVariables().getVariableByName("B");

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(0);
            //sampler.setMARVar(varB,0.5);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

            DataStreamWriter.writeDataToFile(data, "./datasets/tmp.arff");

            data = DataStreamLoader.loadFromFile("./datasets/tmp.arff");

            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setParallelMode(false);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setOutput(false);
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            svb.setDAG(normalVarBN.getDAG());
            svb.setDataStream(data);


            int[] windowsSizes = {1, 2, 10, 100, 1000};
            double[] fadingFactor = {1.0, 0.9999, 0.999, 0.99, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2};
            //[mean, beta0, beta1, varianceB, varianceA][windowSizes.length]
            String[][] outputPerWindowSize = new String[5][windowsSizes.length];

            for (int j = 0; j < windowsSizes.length; j++) {
                outputPerWindowSize[0][j] = windowsSizes[j] + "\t";
                outputPerWindowSize[1][j] = windowsSizes[j] + "\t";
                outputPerWindowSize[2][j] = windowsSizes[j] + "\t";
                outputPerWindowSize[3][j] = windowsSizes[j] + "\t";
                outputPerWindowSize[4][j] = windowsSizes[j] + "\t";
                for (int f = 0; f < fadingFactor.length; f++) {
                    //System.out.println("Window: "+windowsSizes[j]);
                    svb.setParallelMode(false);
                    svb.setWindowsSize(windowsSizes[j]);
                    svb.setTransitionMethod(new Fading(fadingFactor[f]));
                    svb.initLearning();
                    svb.runLearning();
                    BayesianNetwork VMPlearnBN = svb.getLearntBayesianNetwork();
                    outputPerWindowSize[0][j] += Double.toString(((Normal) ((BaseDistribution_MultinomialParents) VMPlearnBN.
                            getConditionalDistribution(varB)).getBaseDistribution(0)).getMean()) + "\t";
                    outputPerWindowSize[1][j] += Double.toString(((ConditionalLinearGaussian) VMPlearnBN.
                            getConditionalDistribution(varA)).getIntercept()) + "\t";
                    outputPerWindowSize[2][j] += Double.toString(((ConditionalLinearGaussian) VMPlearnBN.
                            getConditionalDistribution(varA)).getCoeffParents()[0]) + "\t";
                    outputPerWindowSize[3][j] += Double.toString(((Normal) ((BaseDistribution_MultinomialParents) VMPlearnBN.
                            getConditionalDistribution(varB)).getBaseDistribution(0)).getVariance()) + "\t";
                    outputPerWindowSize[4][j] += Double.toString(((ConditionalLinearGaussian) VMPlearnBN.
                            getConditionalDistribution(varA)).getVariance()) + "\t";
                }

            }

            String fadingOutput = "";
            for (int j = 0; j < fadingFactor.length; j++) {
                fadingOutput += fadingFactor[j] + "\t";
            }

            System.out.println("Mean of B");
            System.out.println("WindowSize \t" + fadingOutput);
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(outputPerWindowSize[0][j]);
            }

            System.out.println("Beta0 of A");
            System.out.println("WindowSize \t" + fadingOutput);
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(outputPerWindowSize[1][j]);
            }

            System.out.println("Beta1 of A");
            System.out.println("WindowSize \t" + fadingOutput);
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(outputPerWindowSize[2][j]);
            }

            System.out.println("Variance of B");
            System.out.println("WindowSize \t" + fadingOutput);
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(outputPerWindowSize[3][j]);
            }

            System.out.println("Variance of A");
            System.out.println("WindowSize \t" + fadingOutput);
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(outputPerWindowSize[4][j]);
            }

        }

    }

    public static void testCompareBatchSizesFadingML() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        for (int i = 0; i < 1; i++) {

            System.out.println("\nNormal|Normal variable network \n ");

            normalVarBN.randomInitialization(new Random(i));


            Variable varA = normalVarBN.getStaticVariables().getVariableByName("A");
            Variable varB = normalVarBN.getStaticVariables().getVariableByName("B");

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(0);
            //sampler.setMARVar(varB,0.5);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

            DataStreamWriter.writeDataToFile(data, "./datasets/tmp.arff");

            data = DataStreamLoader.loadFromFile("./datasets/tmp.arff");


            int[] windowsSizes = {1, 2, 10, 100, 1000};
            double[] fadingFactor = {1.0, 0.9999, 0.999, 0.99, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2};
            //[mean, beta0, beta1, varianceB, varianceA][windowSizes.length]
            String[][] outputPerWindowSize = new String[5][windowsSizes.length];

            for (int j = 0; j < windowsSizes.length; j++) {
                outputPerWindowSize[0][j] = windowsSizes[j] + "\t";
                outputPerWindowSize[1][j] = windowsSizes[j] + "\t";
                outputPerWindowSize[2][j] = windowsSizes[j] + "\t";
                outputPerWindowSize[3][j] = windowsSizes[j] + "\t";
                outputPerWindowSize[4][j] = windowsSizes[j] + "\t";
                MaximumLikelihoodFading likelihoodFading = new MaximumLikelihoodFading();

                for (int f = 0; f < fadingFactor.length; f++) {
                    likelihoodFading.setFadingFactor(fadingFactor[f]);
                    likelihoodFading.setWindowSize(windowsSizes[j]);
                    LearningEngineForBN.setParameterLearningAlgorithm(likelihoodFading);

                    BayesianNetwork MLlearntBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);
                    outputPerWindowSize[0][j] += Double.toString(((Normal) ((BaseDistribution_MultinomialParents) MLlearntBN.
                            getConditionalDistribution(varB)).getBaseDistribution(0)).getMean()) + "\t";
                    outputPerWindowSize[1][j] += Double.toString(((ConditionalLinearGaussian) MLlearntBN.
                            getConditionalDistribution(varA)).getIntercept()) + "\t";
                    outputPerWindowSize[2][j] += Double.toString(((ConditionalLinearGaussian) MLlearntBN.
                            getConditionalDistribution(varA)).getCoeffParents()[0]) + "\t";
                    outputPerWindowSize[3][j] += Double.toString(((Normal) ((BaseDistribution_MultinomialParents) MLlearntBN.
                            getConditionalDistribution(varB)).getBaseDistribution(0)).getVariance()) + "\t";
                    outputPerWindowSize[4][j] += Double.toString(((ConditionalLinearGaussian) MLlearntBN.
                            getConditionalDistribution(varA)).getVariance()) + "\t";
                }

            }

            String fadingOutput = "";
            for (int j = 0; j < fadingFactor.length; j++) {
                fadingOutput += fadingFactor[j] + "\t";
            }

            System.out.println("Mean of B");
            System.out.println("WindowSize \t" + fadingOutput);
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(outputPerWindowSize[0][j]);
            }

            System.out.println("Beta0 of A");
            System.out.println("WindowSize \t" + fadingOutput);
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(outputPerWindowSize[1][j]);
            }

            System.out.println("Beta1 of A");
            System.out.println("WindowSize \t" + fadingOutput);
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(outputPerWindowSize[2][j]);
            }

            System.out.println("Variance of B");
            System.out.println("WindowSize \t" + fadingOutput);
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(outputPerWindowSize[3][j]);
            }

            System.out.println("Variance of A");
            System.out.println("WindowSize \t" + fadingOutput);
            for (int j = 0; j < windowsSizes.length; j++) {
                System.out.println(outputPerWindowSize[4][j]);
            }

        }

    }

    public static void testLogProbOfEvidenceForDiffBatches_WasteIncinerator() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/WasteIncinerator.bn");

        System.out.println("\nWaste Incinerator - \n ");

        for (int j = 0; j < 1; j++) {

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(j);
            DataStream<DataInstance> data = sampler.sampleToDataStream(100000);

            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setSeed(j);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);

            svb.setDAG(normalVarBN.getDAG());
            svb.setDataStream(data);

            String fadingOutput = "\t";
            String header = "Window Size";
            int[] windowsSizes = {1, 2, 10, 50, 100, 1000, 5000, 10000};
            //int[] windowsSizes = {10, 50, 100, 1000, 5000, 10000};
            //double[] fadingFactor = {1.0, 0.9999, 0.999, 0.99, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2};
            double[] fadingFactor = {0.99, 0.9, 0.8};
            for (int i = 0; i < fadingFactor.length; i++) {
                header += " \t logProg(D) \t Time \t nIter";
                fadingOutput += fadingFactor[i] + "\t\t\t";
            }
            System.out.println(fadingOutput + "\n" + header);
            for (int i = 0; i < windowsSizes.length; i++) {
                System.out.println("window: " + windowsSizes[i]);
                svb.setWindowsSize(windowsSizes[i]);
                String output = windowsSizes[i] + "\t";
                for (int f = 0; f < fadingFactor.length; f++) {
                    System.out.println("  fading: " + fadingFactor[f]);
                    svb.initLearning();
                    svb.setTransitionMethod(new Fading(fadingFactor[f]));
                    Stopwatch watch = Stopwatch.createStarted();
                    double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
                    output += logProbOfEv_Batch1 + "\t" + watch.stop() + "\t" + svb.getAverageNumOfIterations() + "\t";
                }
                System.out.println(output);
            }
        }
    }

    public static void testLogProbOfEvidenceForDiffBatches_WasteIncineratorWithLatentVars() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/WasteIncinerator.bn");

        System.out.println("\nWaste Incinerator + latent vars - \n ");


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        sampler.setHiddenVar(normalVarBN.getStaticVariables().getVariableByName("Mout"));
        //sampler.setHiddenVar(normalVarBN.getStaticVariables().getVariableByName("D"));
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(normalVarBN.getDAG());
        svb.setDataStream(data);

        String fadingOutput = "\t";
        String header = "Window Size";
        int[] windowsSizes = {1, 2};
        //int[] windowsSizes = {1,2, 10, 50, 100, 1000, 5000, 10000};
        //double[] fadingFactor = {1.0, 0.9999, 0.999, 0.99, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2};
        double[] fadingFactor = {1.0, 0.9999, 0.999, 0.99, 0.9, 0.8, 0.7};
        for (int i = 0; i < fadingFactor.length; i++) {
            header += " \t logProg(D) \t Time \t nIter";
            fadingOutput += fadingFactor[i] + "\t\t\t";
        }
        System.out.println(fadingOutput + "\n" + header);
        for (int i = 0; i < windowsSizes.length; i++) {
            System.out.println("window: " + windowsSizes[i]);
            svb.setWindowsSize(windowsSizes[i]);
            String output = windowsSizes[i] + "\t";
            for (int f = 0; f < fadingFactor.length; f++) {
                System.out.println("  fading: " + fadingFactor[f]);
                svb.initLearning();
                svb.setTransitionMethod(new Fading(fadingFactor[f]));
                Stopwatch watch = Stopwatch.createStarted();
                double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
                output += logProbOfEv_Batch1 + "\t" + watch.stop() + "\t" + svb.getAverageNumOfIterations() + "\t";
            }
            System.out.println(output);
        }
    }

    public static void testLogProbOfEvidenceForDiffBatches_Normal1Normal() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        normalVarBN.randomInitialization(new Random(0));
        System.out.println("\nNormal_1NormalParent - \n ");


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(normalVarBN.getDAG());
        svb.setDataStream(data);

        String fadingOutput = "\t";
        String header = "Window Size";
        //int[] windowsSizes = {1, 2};
        int[] windowsSizes = {1, 2, 10, 50, 100, 1000, 5000, 10000};
        double[] fadingFactor = {1.0, 0.9999, 0.999, 0.99, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2};
        //double[] fadingFactor = {1.0, 0.9999, 0.999, 0.99, 0.9, 0.8, 0.7};
        for (int i = 0; i < fadingFactor.length; i++) {
            header += " \t logProg(D) \t Time \t nIter";
            fadingOutput += fadingFactor[i] + "\t\t\t";
        }
        System.out.println(fadingOutput + "\n" + header);
        for (int i = 0; i < windowsSizes.length; i++) {
            //System.out.println("window: " + windowsSizes[i]);
            svb.setWindowsSize(windowsSizes[i]);
            String output = windowsSizes[i] + "\t";
            for (int f = 0; f < fadingFactor.length; f++) {
                //System.out.println("  fading: "+fadingFactor[f]);
                svb.initLearning();
                svb.setTransitionMethod(new Fading(fadingFactor[f]));
                Stopwatch watch = Stopwatch.createStarted();
                double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
                output += logProbOfEv_Batch1 + "\t" + watch.stop() + "\t" + svb.getAverageNumOfIterations() + "\t";
            }
            System.out.println(output);
        }
    }

    public static void testLogProbOfEvidenceForDiffBatches_Normal1Normal_LatentVariable() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        normalVarBN.randomInitialization(new Random(0));
        System.out.println("\nNormal_1NormalParent - \n ");


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        sampler.setHiddenVar(normalVarBN.getStaticVariables().getVariableByName("A"));
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(normalVarBN.getDAG());
        svb.setDataStream(data);

        System.out.println("Window Size \t logProg(D) \t Time");
        int[] windowsSizes = {1, 50, 100, 1000, 5000, 10000};
        for (int i = 0; i < windowsSizes.length; i++) {
            svb.setWindowsSize(windowsSizes[i]);
            svb.initLearning();
            Stopwatch watch = Stopwatch.createStarted();
            double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
            System.out.println(windowsSizes[i] + "\t" + logProbOfEv_Batch1 + "\t" + watch.stop());
        }
    }

    public static void testLogProbOfEvidenceForDiffBatches_Asia() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/asia.bn");

        System.out.println("\nAsia - \n ");


        normalVarBN.randomInitialization(new Random(0));
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(normalVarBN.getDAG());
        svb.setDataStream(data);

        System.out.println("Window Size \t logProg(D) \t Time");
        int[] windowsSizes = {1, 50, 100, 1000, 5000, 10000};
        for (int i = 0; i < windowsSizes.length; i++) {
            svb.setWindowsSize(windowsSizes[i]);
            svb.initLearning();
            Stopwatch watch = Stopwatch.createStarted();
            double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
            System.out.println(windowsSizes[i] + "\t" + logProbOfEv_Batch1 + "\t" + watch.stop());
        }
    }

    public static void testLogProbOfEvidenceForDiffBatches_AsiaLatentVars() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/asia.bn");

        System.out.println("\nAsia - \n ");


        normalVarBN.randomInitialization(new Random(0));
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        //sampler.setHiddenVar(normalVarBN.getStaticVariables().getVariableByName("D"));
        sampler.setHiddenVar(normalVarBN.getStaticVariables().getVariableByName("E"));
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(normalVarBN.getDAG());
        svb.setDataStream(data);

        System.out.println("Window Size \t logProg(D) \t Time");
        int[] windowsSizes = {1, 50, 100, 1000, 5000, 10000};
        for (int i = 0; i < windowsSizes.length; i++) {
            svb.setWindowsSize(windowsSizes[i]);
            svb.initLearning();
            Stopwatch watch = Stopwatch.createStarted();
            double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
            System.out.println(windowsSizes[i] + "\t" + logProbOfEv_Batch1 + "\t" + watch.stop());
        }
    }

    public static void testParametersForDiffBatchesAndFading_WasteIncinerator() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/WasteIncinerator.bn");

        System.out.println("\nWaste Incinerator - \n ");

        for (int j = 0; j < 1; j++) {

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(j);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

            Variable varMout = normalVarBN.getStaticVariables().getVariableByName("Mout");

            //BayesianNetwork MLlearntBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);


            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setSeed(j);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);

            svb.setDAG(normalVarBN.getDAG());
            svb.setDataStream(data);

            String fadingOutput = "\t";
            String header = "Window Size";
            int[] windowsSizes = {1, 2, 10, 50, 100, 1000, 5000, 10000};
            //int[] windowsSizes = {10, 50, 100, 1000, 5000, 10000};
            //double[] fadingFactor = {1.0, 0.9999, 0.999, 0.99, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2};
            double[] fadingFactor = {0.99, 0.9, 0.8};
            for (int i = 0; i < fadingFactor.length; i++) {
                header += " \t beta0(Mout) \t beta1(Mout) \t variance(Mout)";
                fadingOutput += fadingFactor[i] + "\t\t\t";
            }
            System.out.println(fadingOutput + "\n" + header);
            for (int i = 0; i < windowsSizes.length; i++) {
                //System.out.println("window: "+windowsSizes[i]);
                svb.setWindowsSize(windowsSizes[i]);
                String output = windowsSizes[i] + "\t";
                for (int f = 0; f < fadingFactor.length; f++) {
                    //System.out.println("  fading: "+fadingFactor[f]);
                    svb.initLearning();
                    svb.setTransitionMethod(new Fading(fadingFactor[f]));
                    //double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
                    BayesianNetwork bn = svb.getLearntBayesianNetwork();
                    ConditionalLinearGaussian distMout = ((ConditionalLinearGaussian) bn.
                            getConditionalDistribution(varMout));
                    output += distMout.getIntercept() + "\t" + distMout.getCoeffParents()[1] + "\t" + distMout.getVariance() + "\t";
                }
                System.out.println(output);
            }
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        LearningVMPTests.testClusteringCLGConceptDrift();

    }
}
