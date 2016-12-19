/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.core.inference;

import eu.amidst.core.distribution.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import junit.framework.TestCase;

public class ImportanceSamplingCLG_newTest extends TestCase {


    // A -> B
    public static void test1() {

        Variables variables = new Variables();
        Variable varA = variables.newMultionomialVariable("A", 2);
        Variable varB = variables.newMultionomialVariable("B", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = new BayesianNetwork(dag);

        Multinomial distA = bn.getConditionalDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getConditionalDistribution(varB);

        distA.setProbabilities(new double[]{0.9, 0.1});
        distB.getMultinomial(0).setProbabilities(new double[]{0.75, 0.25});
        distB.getMultinomial(1).setProbabilities(new double[]{0.25, 0.75});

        //bn.randomInitialization(new Random(0));

        double[] pA = distA.getProbabilities();
        double[][] pB = new double[2][];
        pB[0] = distB.getMultinomial(0).getProbabilities();
        pB[1] = distB.getMultinomial(1).getProbabilities();

        System.out.println(bn.toString());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varB, 1.0);


        ImportanceSamplingCLG_new importanceSampling = new ImportanceSamplingCLG_new();
        importanceSampling.setSampleSize(100000);
        importanceSampling.setModel(bn);


        importanceSampling.runInference();

        Multinomial postA = importanceSampling.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Multinomial postB = importanceSampling.getPosterior(varB);
        System.out.println("P(B) = " + postB.toString());


        assertEquals(postA.getProbabilities()[0], 0.9, 0.01);
        assertEquals(postB.getProbabilities()[0], 0.7, 0.01);

    }

    // A -> (B=0.0)
    public static void test2() {

        Variables variables = new Variables();
        Variable varA = variables.newMultionomialVariable("A", 2);
        Variable varB = variables.newMultionomialVariable("B", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = new BayesianNetwork(dag);

        Multinomial distA = bn.getConditionalDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getConditionalDistribution(varB);

        distA.setProbabilities(new double[]{0.5, 0.5});
        distB.getMultinomial(0).setProbabilities(new double[]{0.75, 0.25});
        distB.getMultinomial(1).setProbabilities(new double[]{0.25, 0.75});

        System.out.println(bn.toString());

        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varB, 0.0);


        ImportanceSamplingCLG_new importanceSampling = new ImportanceSamplingCLG_new();
        importanceSampling.setSampleSize(100000);
        importanceSampling.setModel(bn);

        importanceSampling.setEvidence(assignment);

        importanceSampling.runInference();


        Multinomial postA = importanceSampling.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());

        assertEquals(0.75, postA.getProbabilities()[0], 0.01);

    }


    // (A,B) -> C
    public static void test3() {

        Variables variables = new Variables();
        Variable varA = variables.newMultionomialVariable("A", 2);
        Variable varB = variables.newMultionomialVariable("B", 2);
        Variable varC = variables.newMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        BayesianNetwork bn = new BayesianNetwork(dag);

        Multinomial distA = bn.getConditionalDistribution(varA);
        Multinomial distB = bn.getConditionalDistribution(varB);
        Multinomial_MultinomialParents distC = bn.getConditionalDistribution(varC);

        distA.setProbabilities(new double[]{0.5, 0.5});
        distB.setProbabilities(new double[]{0.5, 0.5});

        distC.getMultinomial(0).setProbabilities(new double[]{0.6, 0.4});
        distC.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});
        distC.getMultinomial(2).setProbabilities(new double[]{0.9, 0.1});
        distC.getMultinomial(3).setProbabilities(new double[]{0.7, 0.3});

        //bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        ImportanceSamplingCLG_new importanceSampling = new ImportanceSamplingCLG_new();
        importanceSampling.setSampleSize(100000);
        importanceSampling.setModel(bn);

        importanceSampling.runInference();


        Multinomial postA = importanceSampling.getPosterior(varA);
        Multinomial postB = importanceSampling.getPosterior(varB);
        Multinomial postC = importanceSampling.getPosterior(varC);


        System.out.println("P(A) = " + postA.toString());
        System.out.println("P(B) = " + postB.toString());
        System.out.println("P(C) = " + postC.toString());


        assertEquals(postA.getProbabilities()[0], 0.5, 0.01);
        assertEquals(postB.getProbabilities()[0], 0.5, 0.01);
        assertEquals(postC.getProbabilities()[0], 0.6, 0.01);

    }

    // (A,B) -> (C==0)
    public static void test4() {

        Variables variables = new Variables();
        Variable varA = variables.newMultionomialVariable("A", 2);
        Variable varB = variables.newMultionomialVariable("B", 2);
        Variable varC = variables.newMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        BayesianNetwork bn = new BayesianNetwork(dag);

        Multinomial distA = bn.getConditionalDistribution(varA);
        Multinomial distB = bn.getConditionalDistribution(varB);
        Multinomial_MultinomialParents distC = bn.getConditionalDistribution(varC);

        distA.setProbabilities(new double[]{0.5, 0.5});
        distB.setProbabilities(new double[]{0.5, 0.5});

        distC.getMultinomial(0).setProbabilities(new double[]{0.6, 0.4});
        distC.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});
        distC.getMultinomial(2).setProbabilities(new double[]{0.9, 0.1});
        distC.getMultinomial(3).setProbabilities(new double[]{0.7, 0.3});

        //bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varC, 0.0);

        ImportanceSamplingCLG_new importanceSampling = new ImportanceSamplingCLG_new();
        importanceSampling.setSampleSize(100000);
        importanceSampling.setModel(bn);

        importanceSampling.setEvidence(assignment);

        importanceSampling.runInference();


        Multinomial postA = importanceSampling.getPosterior(varA);
        Multinomial postB = importanceSampling.getPosterior(varB);
        Multinomial postC = importanceSampling.getPosterior(varC);


        System.out.println("P(A) = " + postA.toString());
        System.out.println("P(B) = " + postB.toString());
        System.out.println("P(C) = " + postC.toString());


        assertEquals(postA.getProbabilities()[0], 0.625, 0.01);
        assertEquals(postB.getProbabilities()[0], 0.333, 0.01);
        assertEquals(postC.getProbabilities()[0], 1, 0.001);

    }


    // A multinomial -> B Gaussian
    public static void test5() {

        Variables variables = new Variables();
        Variable varA = variables.newMultionomialVariable("A", 2);
        Variable varB = variables.newGaussianVariable("B");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = new BayesianNetwork(dag);

        Multinomial distA = bn.getConditionalDistribution(varA);
        Normal_MultinomialParents distB = bn.getConditionalDistribution(varB);

        distA.setProbabilities(new double[]{0.5, 0.5});

        Normal normal0 = new Normal(varB);
        Normal normal1 = new Normal(varB);

        normal0.setMean(0);
        normal0.setVariance(1);

        normal1.setMean(5);
        normal1.setVariance(1);

        distB.setNormal(0, normal0);
        distB.setNormal(1, normal1);


        //bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        ImportanceSamplingCLG_new importanceSampling = new ImportanceSamplingCLG_new();
        importanceSampling.setSampleSize(100000);
        importanceSampling.setModel(bn);
        importanceSampling.setGaussianMixturePosteriors(false);

        importanceSampling.runInference();


        Multinomial postA = importanceSampling.getPosterior(varA);
        Normal postB = importanceSampling.getPosterior(varB);


        System.out.println("P(A) = " + postA.toString());
        System.out.println("P(B) = " + postB.toString());

        assertEquals(0.5, postA.getProbabilities()[0], 0.01);
        assertEquals(2.5, postB.getMean(), 0.02);
        assertEquals(7.2, postB.getVariance(),  0.02);


    }

    // A  multinomial -> B  Gaussian
    public static void test6() {

        Variables variables = new Variables();
        Variable varA = variables.newMultionomialVariable("A", 2);
        Variable varB = variables.newGaussianVariable("B");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = new BayesianNetwork(dag);

        Multinomial distA = bn.getConditionalDistribution(varA);
        Normal_MultinomialParents distB = bn.getConditionalDistribution(varB);

        distA.setProbabilities(new double[]{0.333, 0.667});

        Normal normal0 = new Normal(varB);
        normal0.setMean(0);
        normal0.setVariance(2);

        Normal normal1 = new Normal(varB);
        normal1.setMean(0);
        normal1.setVariance(3);

        distB.setNormal(0, normal0);
        distB.setNormal(1, normal1);


        //bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        ImportanceSamplingCLG_new importanceSampling = new ImportanceSamplingCLG_new();
        importanceSampling.setSampleSize(100000);
        importanceSampling.setModel(bn);
        importanceSampling.setGaussianMixturePosteriors(false);

        importanceSampling.runInference();


        Multinomial postA = importanceSampling.getPosterior(varA);
        Normal postB = importanceSampling.getPosterior(varB);


        System.out.println("P(A) = " + postA.toString());
        System.out.println("P(B) = " + postB.toString());

        assertEquals(0.333, postA.getProbabilities()[0], 0.01);
        assertEquals(0, postB.getMean(), 0.02);
        assertEquals(2.8, postB.getVariance(), 0.5);

    }


    // A  multinomial -> B  mixture of Gaussians
    public static void test7() {

        Variables variables = new Variables();
        Variable varA = variables.newMultionomialVariable("A", 2);
        Variable varB = variables.newGaussianVariable("B");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = new BayesianNetwork(dag);

        Multinomial distA = bn.getConditionalDistribution(varA);
        Normal_MultinomialParents distB = bn.getConditionalDistribution(varB);

        distA.setProbabilities(new double[]{0.333, 0.667});

        Normal normal0 = new Normal(varB);
        Normal normal1 = new Normal(varB);

        normal0.setMean(0);
        normal0.setVariance(1);

        normal1.setMean(10);
        normal1.setVariance(1);

        distB.setNormal(0, normal0);
        distB.setNormal(1, normal1);


        //bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        ImportanceSamplingCLG_new importanceSampling = new ImportanceSamplingCLG_new();
        importanceSampling.setSampleSize(100000);
        importanceSampling.setModel(bn);
        importanceSampling.setGaussianMixturePosteriors(true);
        importanceSampling.setMixtureOfGaussiansInitialVariance(3);
        //importanceSampling.setMixtureOfGaussiansNoveltyRate(0.0001);


        importanceSampling.runInference();


        Multinomial postA = importanceSampling.getPosterior(varA);
        GaussianMixture postB = importanceSampling.getPosterior(varB);


        System.out.println("P(A) = " + postA.toString());
        System.out.println("P(B) = " + postB.toString());

        assertEquals(0.333, postA.getProbabilities()[0], 0.02);
        assertEquals(0.666, postB.getParameters()[0], 0.02);
        assertEquals(10, postB.getParameters()[1], 0.02);
        assertEquals(1, postB.getParameters()[2], 0.02);
        assertEquals(0.333, postB.getParameters()[3], 0.02);
        assertEquals(0, postB.getParameters()[4], 0.02);
        assertEquals(1, postB.getParameters()[5], 0.02);

    }

}