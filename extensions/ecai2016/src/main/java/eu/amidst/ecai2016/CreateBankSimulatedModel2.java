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

package eu.amidst.ecai2016;

import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by andresmasegosa on 31/3/16.
 */
public class CreateBankSimulatedModel2 {

    public static void main(String[] args) throws IOException {


        // VARIABLES:

        DynamicVariables dynamicVariables = new DynamicVariables();

        Variable personalSituation = dynamicVariables.newMultinomialDynamicVariable("PersonalFinancialSituation", Arrays.asList("Good","Bad"));

        Variable linking = dynamicVariables
                .newMultinomialDynamicVariable("Linking", Arrays.asList("Low", "High"));

        Variable unexpectedIncident = dynamicVariables
                .newMultinomialDynamicVariable("UnexpectedIncident", Arrays.asList("No", "Yes"));

        Variable overallIncomes = dynamicVariables.newGaussianDynamicVariable("OveralIncomes");

        Variable overallExpenses = dynamicVariables.newGaussianDynamicVariable("OveralExpenses");

        Variable localIncomes = dynamicVariables.newGaussianDynamicVariable("LocalIncomes");

        Variable localExpenses = dynamicVariables.newGaussianDynamicVariable("LocalExpenses");



        // GRAPH:

        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(personalSituation).addParent(personalSituation.getInterfaceVariable());

        dynamicDAG.getParentSetTimeT(linking).addParent(linking.getInterfaceVariable());


        dynamicDAG.getParentSetTimeT(overallIncomes).addParent(personalSituation);
        dynamicDAG.getParentSetTimeT(overallIncomes).addParent(overallIncomes.getInterfaceVariable());

        dynamicDAG.getParentSetTimeT(overallExpenses).addParent(personalSituation);
        dynamicDAG.getParentSetTimeT(overallExpenses).addParent(unexpectedIncident);
        dynamicDAG.getParentSetTimeT(overallExpenses).addParent(overallExpenses.getInterfaceVariable());

        dynamicDAG.getParentSetTimeT(localIncomes).addParent(linking);
        dynamicDAG.getParentSetTimeT(localIncomes).addParent(overallIncomes);

        dynamicDAG.getParentSetTimeT(localExpenses).addParent(linking);
        dynamicDAG.getParentSetTimeT(localExpenses).addParent(overallExpenses);



        // PROBABILITY DISTRIBUTIONS:

        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dynamicDAG);

        // Personal Financial Situation:
        Multinomial p0 = dbn.getConditionalDistributionTime0(personalSituation);
        p0.setProbabilities(new double[]{0.8,0.2});

        Multinomial_MultinomialParents p1 = dbn.getConditionalDistributionTimeT(personalSituation);
        p1.getMultinomial(0).setProbabilities(new double[]{0.9,0.1});
        p1.getMultinomial(1).setProbabilities(new double[]{0.1,0.9});

        // LINKAGE:
        Multinomial p2 = dbn.getConditionalDistributionTime0(linking);
        p2.setProbabilities(new double[]{0.5,0.5});

        Multinomial_MultinomialParents p3 = dbn.getConditionalDistributionTimeT(linking);
        p3.getMultinomial(0).setProbabilities(new double[]{0.9,0.1});
        p3.getMultinomial(1).setProbabilities(new double[]{0.1,0.9});

        // UNEXPECTED FINANCIAL EVENT:
        Multinomial p4 = dbn.getConditionalDistributionTime0(unexpectedIncident);
        p4.setProbabilities(new double[]{0.8,0.2});

        Multinomial p5 = dbn.getConditionalDistributionTimeT(unexpectedIncident);
        p5.setProbabilities(new double[]{0.8,0.2});


        // GLOBAL INCOMES:
        Normal_MultinomialParents p8 = dbn.getConditionalDistributionTime0(overallIncomes);
        p8.getNormal(0).setMean(10);
        p8.getNormal(0).setVariance(2);

        p8.getNormal(1).setMean(3);
        p8.getNormal(1).setVariance(1);


        Normal_MultinomialNormalParents p9 = dbn.getConditionalDistributionTimeT(overallIncomes);
        p9.getNormal_NormalParentsDistribution(0).setIntercept(0.1);
        p9.getNormal_NormalParentsDistribution(0).setCoeffForParent(overallIncomes.getInterfaceVariable(),1);
        p9.getNormal_NormalParentsDistribution(0).setVariance(0.5);

        p9.getNormal_NormalParentsDistribution(1).setIntercept(-0.1);
        p9.getNormal_NormalParentsDistribution(1).setCoeffForParent(overallIncomes.getInterfaceVariable(),1);
        p9.getNormal_NormalParentsDistribution(1).setVariance(0.5);


        // GLOBAL EXPENSES:
        Normal_MultinomialParents p10 = dbn.getConditionalDistributionTime0(overallExpenses);
        p10.getNormal(0).setMean(-7);
        p10.getNormal(0).setVariance(1);

        p10.getNormal(1).setMean(-3);
        p10.getNormal(1).setVariance(1);

        p10.getNormal(2).setMean(-9);
        p10.getNormal(2).setVariance(2);

        p10.getNormal(3).setMean(-5);
        p10.getNormal(3).setVariance(2);

        Normal_MultinomialNormalParents p11 = dbn.getConditionalDistributionTimeT(overallExpenses);
        p11.getNormal_NormalParentsDistribution(0).setIntercept(0.0);
        p11.getNormal_NormalParentsDistribution(0).setCoeffForParent(overallExpenses.getInterfaceVariable(),1);
        p11.getNormal_NormalParentsDistribution(0).setVariance(0.5);

        p11.getNormal_NormalParentsDistribution(1).setIntercept(-0.1);
        p11.getNormal_NormalParentsDistribution(1).setCoeffForParent(overallExpenses.getInterfaceVariable(),1);
        p11.getNormal_NormalParentsDistribution(1).setVariance(0.5);

        p11.getNormal_NormalParentsDistribution(2).setIntercept(-1);
        p11.getNormal_NormalParentsDistribution(2).setCoeffForParent(overallExpenses.getInterfaceVariable(),1);
        p11.getNormal_NormalParentsDistribution(2).setVariance(1);

        p11.getNormal_NormalParentsDistribution(3).setIntercept(-1);
        p11.getNormal_NormalParentsDistribution(3).setCoeffForParent(overallExpenses.getInterfaceVariable(),1);
        p11.getNormal_NormalParentsDistribution(3).setVariance(1);


        // LOCAL INCOMES:
        Normal_MultinomialNormalParents p12 = dbn.getConditionalDistributionTime0(localIncomes);
        p12.getNormal_NormalParentsDistribution(0).setIntercept(0);
        p12.getNormal_NormalParentsDistribution(0).setCoeffForParent(overallIncomes,0.2);
        p12.getNormal_NormalParentsDistribution(0).setVariance(0.5);

        p12.getNormal_NormalParentsDistribution(1).setIntercept(0);
        p12.getNormal_NormalParentsDistribution(1).setCoeffForParent(overallIncomes,0.8);
        p12.getNormal_NormalParentsDistribution(1).setVariance(0.5);

        Normal_MultinomialNormalParents p13 = dbn.getConditionalDistributionTimeT(localIncomes);
        p13.getNormal_NormalParentsDistribution(0).setIntercept(0);
        p13.getNormal_NormalParentsDistribution(0).setCoeffForParent(overallIncomes,0.2);
        p13.getNormal_NormalParentsDistribution(0).setVariance(0.5);

        p13.getNormal_NormalParentsDistribution(1).setIntercept(0);
        p13.getNormal_NormalParentsDistribution(1).setCoeffForParent(overallIncomes,0.8);
        p13.getNormal_NormalParentsDistribution(1).setVariance(0.5);


        // LOCAL EXPENSES:
        Normal_MultinomialNormalParents p14 = dbn.getConditionalDistributionTime0(localExpenses);
        p14.getNormal_NormalParentsDistribution(0).setIntercept(0);
        p14.getNormal_NormalParentsDistribution(0).setCoeffForParent(overallExpenses,0.2);
        p14.getNormal_NormalParentsDistribution(0).setVariance(0.5);

        p14.getNormal_NormalParentsDistribution(1).setIntercept(0);
        p14.getNormal_NormalParentsDistribution(1).setCoeffForParent(overallExpenses,0.8);
        p14.getNormal_NormalParentsDistribution(1).setVariance(0.5);

        Normal_MultinomialNormalParents p15 = dbn.getConditionalDistributionTimeT(localExpenses);
        p15.getNormal_NormalParentsDistribution(0).setIntercept(0);
        p15.getNormal_NormalParentsDistribution(0).setCoeffForParent(overallExpenses,0.2);
        p15.getNormal_NormalParentsDistribution(0).setVariance(0.5);

        p15.getNormal_NormalParentsDistribution(1).setIntercept(0);
        p15.getNormal_NormalParentsDistribution(1).setCoeffForParent(overallExpenses,0.8);
        p15.getNormal_NormalParentsDistribution(1).setVariance(0.5);

        DynamicBayesianNetworkWriter.saveToFile(dbn, "./networks/BankSimulatedNetwork2.dbn");

        System.out.println(dbn);
    }
}
