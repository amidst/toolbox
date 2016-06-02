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

import eu.amidst.core.distribution.*;
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
public class CreateBankSimulatedModel {

    public static void main(String[] args) throws IOException {


        // VARIABLES:

        DynamicVariables dynamicVariables = new DynamicVariables();

        Variable defaulter = dynamicVariables.newMultinomialDynamicVariable("Defaulter", Arrays.asList("No","Yes"));

        Variable globalSituation = dynamicVariables
                .newMultinomialDynamicVariable("GlobalFinancialSituation", Arrays.asList("Good", "Neutral", "Bad"));

        Variable incident = dynamicVariables
                .newMultinomialDynamicVariable("UnexpectedIncident", Arrays.asList("No", "Yes"));

        Variable job = dynamicVariables
                .newMultinomialDynamicVariable("Job", Arrays.asList("No", "Yes"));

        Variable balance = dynamicVariables.newGaussianDynamicVariable("AccountBalance");

        Variable amortization = dynamicVariables.newGaussianDynamicVariable("CreditRepayment");



        // GRAPH:

        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(defaulter).addParent(defaulter.getInterfaceVariable());

        dynamicDAG.getParentSetTimeT(globalSituation).addParent(globalSituation.getInterfaceVariable());

        dynamicDAG.getParentSetTimeT(job).addParent(globalSituation);
        dynamicDAG.getParentSetTimeT(job).addParent(job.getInterfaceVariable());



        dynamicDAG.getParentSetTimeT(balance).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(balance).addParent(job);
        dynamicDAG.getParentSetTimeT(balance).addParent(balance.getInterfaceVariable());

        dynamicDAG.getParentSetTimeT(amortization).addParent(balance);
        dynamicDAG.getParentSetTimeT(amortization).addParent(incident);
        dynamicDAG.getParentSetTimeT(amortization).addParent(job);




        // PROBABILITY DISTRIBUTIONS:

        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dynamicDAG);

        // DEFAULTER:
        Multinomial p0 = dbn.getConditionalDistributionTime0(defaulter);
        p0.setProbabilities(new double[]{0.9,0.1});

        Multinomial_MultinomialParents p1 = dbn.getConditionalDistributionTimeT(defaulter);
        p1.getMultinomial(0).setProbabilities(new double[]{0.9,0.1});
        p1.getMultinomial(1).setProbabilities(new double[]{0.1,0.9});

        // GLOBAL FINANCIAL SITUATION:
        Multinomial p2 = dbn.getConditionalDistributionTime0(globalSituation);
        p2.setProbabilities(new double[]{0.3,0.4,0.3});

        Multinomial_MultinomialParents p3 = dbn.getConditionalDistributionTimeT(globalSituation);
        p3.getMultinomial(0).setProbabilities(new double[]{0.7,0.2,0.1});
        p3.getMultinomial(1).setProbabilities(new double[]{0.3,0.4,0.3});
        p3.getMultinomial(2).setProbabilities(new double[]{0.1,0.2,0.7});

        // UNEXPECTED FINANCIAL EVENT:
        Multinomial p4 = dbn.getConditionalDistributionTime0(incident);
        p4.setProbabilities(new double[]{0.8,0.2});

        Multinomial p5 = dbn.getConditionalDistributionTimeT(incident);
        p5.setProbabilities(new double[]{0.8,0.2});

        // JOB:
        Multinomial_MultinomialParents p6 = dbn.getConditionalDistributionTime0(job);
        p6.getMultinomial(0).setProbabilities(new double[]{0.1, 0.9});
        p6.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});
        p6.getMultinomial(2).setProbabilities(new double[]{0.3, 0.7});

        Multinomial_MultinomialParents p7 = dbn.getConditionalDistributionTimeT(job);
        p7.getMultinomial(0).setProbabilities(new double[]{0.3, 0.7});
        p7.getMultinomial(1).setProbabilities(new double[]{0.5, 0.5});
        p7.getMultinomial(2).setProbabilities(new double[]{0.6, 0.4});
        p7.getMultinomial(3).setProbabilities(new double[]{0.05, 0.95});
        p7.getMultinomial(4).setProbabilities(new double[]{0.1, 0.9});
        p7.getMultinomial(5).setProbabilities(new double[]{0.2, 0.8});


        // ACCOUNT BALANCE:
        Normal_MultinomialParents p8 = dbn.getConditionalDistributionTime0(balance);
        p8.getNormal(0).setMean(3);
        p8.getNormal(0).setVariance(1);

        p8.getNormal(1).setMean(0.5);
        p8.getNormal(1).setVariance(0.2);

        p8.getNormal(2).setMean(8);
        p8.getNormal(2).setVariance(1);

        p8.getNormal(3).setMean(4);
        p8.getNormal(3).setVariance(1);

        Normal_MultinomialNormalParents p9 = dbn.getConditionalDistributionTimeT(balance);
        p9.getNormal_NormalParentsDistribution(0).setIntercept(0);
        p9.getNormal_NormalParentsDistribution(0).setCoeffForParent(balance.getInterfaceVariable(),0.8);
        p9.getNormal_NormalParentsDistribution(0).setVariance(0.2);

        p9.getNormal_NormalParentsDistribution(1).setIntercept(0);
        p9.getNormal_NormalParentsDistribution(1).setCoeffForParent(balance.getInterfaceVariable(),0.6);
        p9.getNormal_NormalParentsDistribution(1).setVariance(0.2);

        p9.getNormal_NormalParentsDistribution(2).setIntercept(1);
        p9.getNormal_NormalParentsDistribution(2).setCoeffForParent(balance.getInterfaceVariable(),1);
        p9.getNormal_NormalParentsDistribution(2).setVariance(0.2);

        p9.getNormal_NormalParentsDistribution(3).setIntercept(1);
        p9.getNormal_NormalParentsDistribution(3).setCoeffForParent(balance.getInterfaceVariable(),0.7);
        p9.getNormal_NormalParentsDistribution(3).setVariance(0.2);


        // CREDIT REPAYMENT (OR EXPANSION)
        Normal_MultinomialNormalParents p10 = dbn.getConditionalDistributionTime0(amortization);
        p10.getNormal_NormalParentsDistribution(0).setIntercept(0);
        p10.getNormal_NormalParentsDistribution(0).setCoeffForParent(balance,0.2);
        p10.getNormal_NormalParentsDistribution(0).setVariance(0.5);

        p10.getNormal_NormalParentsDistribution(1).setIntercept(-1);
        p10.getNormal_NormalParentsDistribution(1).setCoeffForParent(balance,0.0);
        p10.getNormal_NormalParentsDistribution(1).setVariance(0.5);

        p10.getNormal_NormalParentsDistribution(2).setIntercept(2);
        p10.getNormal_NormalParentsDistribution(2).setCoeffForParent(balance,0.4);
        p10.getNormal_NormalParentsDistribution(2).setVariance(0.5);

        p10.getNormal_NormalParentsDistribution(3).setIntercept(1);
        p10.getNormal_NormalParentsDistribution(3).setCoeffForParent(balance,0.2);
        p10.getNormal_NormalParentsDistribution(3).setVariance(0.5);


        Normal_MultinomialNormalParents p11 = dbn.getConditionalDistributionTimeT(amortization);
        p11.getNormal_NormalParentsDistribution(0).setIntercept(0);
        p11.getNormal_NormalParentsDistribution(0).setCoeffForParent(balance,0.2);
        p11.getNormal_NormalParentsDistribution(0).setVariance(0.5);

        p11.getNormal_NormalParentsDistribution(1).setIntercept(-1);
        p11.getNormal_NormalParentsDistribution(1).setCoeffForParent(balance,0.0);
        p11.getNormal_NormalParentsDistribution(1).setVariance(0.5);

        p11.getNormal_NormalParentsDistribution(2).setIntercept(2);
        p11.getNormal_NormalParentsDistribution(2).setCoeffForParent(balance,0.4);
        p11.getNormal_NormalParentsDistribution(2).setVariance(0.5);

        p11.getNormal_NormalParentsDistribution(3).setIntercept(1);
        p11.getNormal_NormalParentsDistribution(3).setCoeffForParent(balance,0.2);
        p11.getNormal_NormalParentsDistribution(3).setVariance(0.5);


        DynamicBayesianNetworkWriter.save(dbn, "./networks/BankSimulatedNetwork.dbn");

        System.out.println(dbn);
    }
}
