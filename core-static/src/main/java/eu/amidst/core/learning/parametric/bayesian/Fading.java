/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.learning.parametric.bayesian;

import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.exponentialfamily.NaturalParameters;

/**
 * Created by andresmasegosa on 13/4/15.
 */
public class Fading implements TransitionMethod {

    double fading = 1.0;

    public Fading(double fading_){
        this.fading=fading_;
    }

    public double getFading() {
        return fading;
    }

    public void setFading(double fading) {
        this.fading = fading;
    }

    @Override
    public EF_LearningBayesianNetwork transitionModel(EF_LearningBayesianNetwork ef_extendedBN, PlateuStructure plateuStructure) {
        ef_extendedBN.getParametersVariables().getListOfVariables().stream().forEach(var -> {
            EF_UnivariateDistribution prior = (EF_UnivariateDistribution) ef_extendedBN.getDistribution(var);
            NaturalParameters naturalParameters = prior.getNaturalParameters();
            naturalParameters.multiplyBy(fading);
            prior.setNaturalParameters(naturalParameters);
            ef_extendedBN.setDistribution(var,prior);
            plateuStructure.getNodeOfVar(var,0).setPDist(prior);
        });

        return ef_extendedBN;
    }

}
