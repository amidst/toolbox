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

package eu.amidst.winter.Session3;

import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Variable;

/**
 * Session 3. Set prior knowledge to complete those parts of the model which can not be learnt from data.
 * Created by andresmasegosa on 13/01/2018.
 */
public class B_AddExpertKnowledge {
    public static void main(String[] args) throws Exception {
        //Load the learnt model
        BayesianNetwork fireDetector = BayesianNetworkLoader.loadFromFile("./models/LearntFireDetectorModel.bn");

        //Access the variable of interest.
        Variable fire = fireDetector.getVariables().getVariableByName("Fire");
        Variable temperature = fireDetector.getVariables().getVariableByName("Temperature");
        Variable smoke = fireDetector.getVariables().getVariableByName("Smoke");

        //Modify the parameters of the model according to our prior knowledge.
        Multinomial fireprob = fireDetector.getConditionalDistribution(fire);
        fireprob.setProbabilities(new double[]{0.999, 0.001});

        Normal_MultinomialParents tempprob = fireDetector.getConditionalDistribution(temperature);
        tempprob.getNormal(1).setMean(tempprob.getNormal(0).getMean()+10);
        tempprob.getNormal(1).setVariance(tempprob.getNormal(0).getVariance());

        Multinomial_MultinomialParents smokeProb = fireDetector.getConditionalDistribution(smoke);
        smokeProb.getMultinomial(1).setProbabilities(new double[]{0.001, 0.999});

        //Print the model
        System.out.println(fireDetector);

        //Save to disk the new model
        BayesianNetworkWriter.save(fireDetector,"./models/FireDetectorModel.bn");

    }
}
