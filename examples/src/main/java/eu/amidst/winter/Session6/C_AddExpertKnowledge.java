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

package eu.amidst.winter.Session6;

import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.io.DynamicBayesianNetworkLoader;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;

/**
 * Session 6. Set prior knowledge to complete those parts of the model which can not be learnt from data.
 * Created by andresmasegosa on 13/01/2018.
 */
public class C_AddExpertKnowledge {
    public static void main(String[] args) throws Exception {
        //Load the learnt model
        DynamicBayesianNetwork fireDetector = DynamicBayesianNetworkLoader.loadFromFile("./models/DynamicFireDetectorModel.dbn");

        //Access the variable of interest.
        Variable fire = fireDetector.getDynamicVariables().getVariableByName("Fire");
        Variable temperature = fireDetector.getDynamicVariables().getVariableByName("Temperature");
        Variable smoke = fireDetector.getDynamicVariables().getVariableByName("Smoke");

        /******* Parameters Time 0 ***********/
        //Modify the parameters of the model according to our prior knowledge.
        Multinomial fireprob = fireDetector.getConditionalDistributionTime0(fire);
        fireprob.setProbabilities(new double[]{0.999, 0.001});

        Normal_MultinomialParents tempprob = fireDetector.getConditionalDistributionTime0(temperature);
        tempprob.getNormal(1).setMean(tempprob.getNormal(0).getMean()+10);
        tempprob.getNormal(1).setVariance(tempprob.getNormal(0).getVariance());

        Multinomial_MultinomialParents smokeProb = fireDetector.getConditionalDistributionTime0(smoke);
        smokeProb.getMultinomial(1).setProbabilities(new double[]{0.001, 0.999});

        /******* Parameters Time T ***********/
        //Modify the parameters of the model according to our prior knowledge.
        Multinomial_MultinomialParents fireprobTimeT = fireDetector.getConditionalDistributionTimeT(fire);
        fireprobTimeT.getMultinomial(0).setProbabilities(new double[]{0.999, 0.001});
        fireprobTimeT.getMultinomial(1).setProbabilities(new double[]{0.01, 0.99});

        Multinomial_MultinomialParents smokeProbTimeT = fireDetector.getConditionalDistributionTimeT(smoke);
        smokeProbTimeT.getMultinomial(1).setProbabilities(new double[]{0.001, 0.999});
        smokeProbTimeT.getMultinomial(3).setProbabilities(new double[]{0.001, 0.999});

        Normal_MultinomialNormalParents tempTimeT = fireDetector.getConditionalDistributionTimeT(temperature);
        tempTimeT.getNormal_NormalParentsDistribution(1).setIntercept(tempprob.getNormal(0).getMean()+10);
        tempTimeT.getNormal_NormalParentsDistribution(1).setCoeffForParent(temperature.getInterfaceVariable(),0.0);
        tempTimeT.getNormal_NormalParentsDistribution(1).setVariance(tempprob.getNormal(0).getVariance());

        //Print the model
        System.out.println(fireDetector);

        //Save to disk the new model
        DynamicBayesianNetworkWriter.save(fireDetector,"./models/DynamicFireDetectorModel.dbn");

    }
}
