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

package eu.amidst.huginlink.inference;
import eu.amidst.core.distribution.*;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by afa on 1/3/15.
 * Test to check the Importance Sampling algorithm against Hugin inference engine (without evidences!!)
 */
public class ImportanceSamplingHuginTest {

    @Before
    public void setUp() {

    }

    @Test
    public void test() throws IOException, ClassNotFoundException {

        BayesianNetwork model = BayesianNetworkLoader.loadFromFile("../networks/simulated/IS.bn");
        System.out.println(model.toString());
        //**************************************************************************************************************
        // MODEL DISTRIBUTIONS
        //**************************************************************************************************************
        //System.out.println("MODEL DISTRIBUTIONS");
        //model.getConditionalDistributions().stream().forEach(e-> {
        //     System.out.println(e.getVariable().getName());
        //     System.out.println(e);
        //});

        Variables variables = model.getVariables();
        Variable varA = variables.getVariableByName("A");
        Variable varB = variables.getVariableByName("B");
        Variable varC = variables.getVariableByName("C");
        Variable varD = variables.getVariableByName("D");
        Variable varE = variables.getVariableByName("E");

        HashMapAssignment evidence = new HashMapAssignment(0);
        //evidence.setValue(varA,1.0);

        //**************************************************************************************************************
        // HUGIN INFERENCE
        //**************************************************************************************************************

        HuginInference huginInferenceForBN = new HuginInference();
        huginInferenceForBN.setModel(model);
        huginInferenceForBN.setEvidence(evidence);
        huginInferenceForBN.runInference();

        //**************************************************************************************************************
        // IMPORTANCE SAMPLING INFERENCE
        //**************************************************************************************************************

        BayesianNetwork samplingModel = ImportanceSamplingHuginTest.getNoisyModel();
        //System.out.println("  SAMPLING DISTRIBUTIONS (MODEL DISTRIBUTIONS WITH NOISE) ");
        //samplingModel.getConditionalDistributions().stream().forEach(e-> {
        //    System.out.println(e.getVariable().getName());
        //    System.out.println(e);
        //});
        ImportanceSampling IS = new ImportanceSampling();
        IS.setModel(model);
        IS.setSamplingModel(samplingModel);
        IS.setSampleSize(500000);
        IS.setEvidence(evidence);
        IS.setParallelMode(true);
        IS.setKeepDataOnMemory(true);
        //**************************************************************************************************************

        double threshold = 0.01;

        /* runInference() method must be called each time we compute a posterior because the Stream of Weighted
           Assignments is closed (reduced) in method getPosterior(var).*/
        IS.runInference();
        System.out.println("Posterior IS: "+IS.getPosterior(varA).toString());
        System.out.println("Posterior Hugin: "+huginInferenceForBN.getPosterior(varA).toString());
        assertTrue(IS.getPosterior(varA).equalDist(huginInferenceForBN.getPosterior(varA),threshold));

        System.out.println("Posterior IS: "+IS.getPosterior(varB).toString());
        System.out.println("Posterior Hugin: "+huginInferenceForBN.getPosterior(varB).toString());
        assertTrue(IS.getPosterior(varB).equalDist(huginInferenceForBN.getPosterior(varB),threshold));

        System.out.println("Posterior IS: "+IS.getPosterior(varC).toString());
        System.out.println("Posterior Hugin: "+huginInferenceForBN.getPosterior(varC).toString());
        assertTrue(IS.getPosterior(varC).equalDist(huginInferenceForBN.getPosterior(varC),threshold));

        System.out.println("Posterior IS: "+IS.getPosterior(varD).toString());
        System.out.println("Posterior Hugin: "+huginInferenceForBN.getPosterior(varD).toString());
        assertTrue(IS.getPosterior(varD).equalDist(huginInferenceForBN.getPosterior(varD),threshold));

        System.out.println("Posterior IS: "+IS.getPosterior(varE).toString());
        System.out.println("Posterior Hugin: "+huginInferenceForBN.getPosterior(varE).toString());
        assertTrue(IS.getPosterior(varE).equalDist(huginInferenceForBN.getPosterior(varE),threshold));

        }

    private static BayesianNetwork getNoisyModel() throws IOException, ClassNotFoundException {

        BayesianNetwork samplingBN = BayesianNetworkLoader.loadFromFile("../networks/simulated/IS.bn");
        Variables variables = samplingBN.getVariables();
        Variable A = variables.getVariableByName("A");
        Variable B = variables.getVariableByName("B");
        Variable C = variables.getVariableByName("C");
        Variable D = variables.getVariableByName("D");
        Variable E = variables.getVariableByName("E");

        // Variable A
        Multinomial distA = samplingBN.getConditionalDistribution(A);
        distA.setProbabilities(new double[]{0.15, 0.85});

        // Variable B
        Multinomial_MultinomialParents distB = samplingBN.getConditionalDistribution(B);
        distB.getMultinomial(0).setProbabilities(new double[]{0.15,0.85});
        distB.getMultinomial(1).setProbabilities(new double[]{0.75,0.25});

        // Variable C
        Normal_MultinomialParents distC = samplingBN.getConditionalDistribution(C);
        distC.getNormal(0).setMean(3.1);
        distC.getNormal(0).setVariance(0.93320508059375);
        distC.getNormal(1).setMean(2.1);
        distC.getNormal(1).setVariance(0.720262834489);

        //Variable D
        Normal_MultinomialNormalParents distD = samplingBN.getConditionalDistribution(D);
        distD.getNormal_NormalParentsDistribution(0).setIntercept(2.1);
        //distD.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{2.1});
        distD.getNormal_NormalParentsDistribution(0).setCoeffForParent(C, 2.1);
        distD.getNormal_NormalParentsDistribution(0).setVariance(1.21);

        distD.getNormal_NormalParentsDistribution(1).setIntercept(0.6);
        //distD.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.6});
        distD.getNormal_NormalParentsDistribution(1).setCoeffForParent(C, 1.6);
        distD.getNormal_NormalParentsDistribution(1).setVariance(2.29280164);

        //Variable E
        ConditionalLinearGaussian distE  = samplingBN.getConditionalDistribution(E);
        distE.setIntercept(2.4);
        //distE.setCoeffParents(new double[]{4.1});
        distE.setCoeffForParent(C, 4.1);
        distE.setVariance(1.64660224);

        return(samplingBN);
    }

}