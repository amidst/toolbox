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
package eu.amidst.core.conceptdrift.utils;



import eu.amidst.core.distribution.Normal;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuStructure;
import eu.amidst.core.learning.parametric.bayesian.utils.TransitionMethod;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.List;

/**
 * Created by andresmasegosa on 13/4/15.
 */
public class GaussianHiddenTransitionMethod implements TransitionMethod, Serializable{
    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    List<Variable> localHiddenVars;
    double meanStart;
    double transtionVariance;
    double fading = 1.0;

    public void setTransitionVariance(double noise) {
        this.transtionVariance = noise;
    }

    public void setFading(double fading) {
        this.fading = fading;
    }

    public GaussianHiddenTransitionMethod(List<Variable> localHiddenVars_, double meanStart_, double transtionVariance_){
        this.localHiddenVars=localHiddenVars_;
        this.meanStart = meanStart_;
        this.transtionVariance = transtionVariance_;
    }

    @Override
    public EF_LearningBayesianNetwork initModel(EF_LearningBayesianNetwork bayesianNetwork, PlateuStructure plateuStructure) {


        for (Variable paramVariable : bayesianNetwork.getParametersVariables().getListOfParamaterVariables()){

            if (!paramVariable.isNormalParameter())
                continue;

            EF_NormalParameter prior = bayesianNetwork.getDistribution(paramVariable);

            double varPrior = 1;
            double precisionPrior = 1/varPrior;
            double meanPrior = 0;

            prior.setNaturalWithMeanPrecision(meanPrior,precisionPrior);
            prior.fixNumericalInstability();
            prior.updateMomentFromNaturalParameters();

        }


        for (Variable localVar : this.localHiddenVars) {

            EF_Normal normal = bayesianNetwork.getDistribution(localVar);


            double mean = meanStart;
            double var = 1;

            normal.setNaturalWithMeanPrecision(mean,1/var);
            normal.fixNumericalInstability();
            normal.updateMomentFromNaturalParameters();

        }



        return bayesianNetwork;

    }

    @Override
    public EF_LearningBayesianNetwork transitionModel(EF_LearningBayesianNetwork bayesianNetwork, PlateuStructure plateuStructure) {

        for (Variable localVar : this.localHiddenVars) {
            Normal normalGlobalHiddenPreviousTimeStep = plateuStructure.getEFParameterPosterior(localVar).toUnivariateDistribution();

            EF_Normal normal = bayesianNetwork.getDistribution(localVar);

            double variance = normalGlobalHiddenPreviousTimeStep.getVariance() + this.transtionVariance;
            double mean = normalGlobalHiddenPreviousTimeStep.getMean();

            normal.setNaturalWithMeanPrecision(mean,1/variance);
            normal.fixNumericalInstability();
            normal.updateMomentFromNaturalParameters();
        }

        /***** FADING ****/


        if (fading<1.0) {
            bayesianNetwork.getParametersVariables().getListOfParamaterVariables().stream().forEach(var -> {
                EF_BaseDistribution_MultinomialParents dist = bayesianNetwork.getDistribution(var);
                EF_UnivariateDistribution prior = dist.getBaseEFUnivariateDistribution(0);
                NaturalParameters naturalParameters = prior.getNaturalParameters();
                naturalParameters.multiplyBy(fading);
                prior.setNaturalParameters(naturalParameters);
                dist.setBaseEFDistribution(0, prior);
            });
        }







        return bayesianNetwork;
    }
}
