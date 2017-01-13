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
package eu.amidst.icdm2016.smoothing;



import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_Normal;
import eu.amidst.core.exponentialfamily.EF_NormalParameter;
import eu.amidst.core.exponentialfamily.EF_Normal_NormalParents;
import eu.amidst.core.learning.parametric.bayesian.PlateuStructure;
import eu.amidst.core.learning.parametric.bayesian.TransitionMethod;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.List;

/**
 * Created by andresmasegosa on 13/4/15.
 */
public class Smooth_GaussianHiddenTransitionMethod implements TransitionMethod, Serializable{
    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    List<Variable> localHiddenVars;

    double meanPH0=0;
    double varPH0 = 1e100;
    double transtionVariance=1;

    double meanPAlpha=0;
    double varPAlpha = 1e100;

    double meanPBeta=0;
    double varPBeta = 1e100;

    public Smooth_GaussianHiddenTransitionMethod(List<Variable> localHiddenVars_){
        this.localHiddenVars=localHiddenVars_;
    }

    public void setTransitionVariance(double noise) {
        this.transtionVariance = noise;
    }

    public void setPH0(double mean, double var){
        this.meanPH0=mean;
        this.varPH0=var;
    }

    public void setPAlpha(double mean, double var) {
        this.meanPAlpha=mean;
        this.varPAlpha=var;
    }

    public void setPBeta(double mean, double var) {
        this.meanPBeta=mean;
        this.varPBeta=var;
    }


    @Override
    public EF_LearningBayesianNetwork initModel(EF_LearningBayesianNetwork bayesianNetwork, PlateuStructure plateuStructure) {


        //Initialization for Beta0 and Betas
        for (Variable paramVariable : bayesianNetwork.getParametersVariables().getListOfParamaterVariables()){

            if (!paramVariable.isNormalParameter())
                continue;

            EF_NormalParameter prior = bayesianNetwork.getDistribution(paramVariable);

            double varPrior, precisionPrior, meanPrior;
            if(paramVariable.getName().contains("_INDICATOR = 0.000}")){
                precisionPrior = Double.MAX_VALUE;
                meanPrior = 0;
            }else if(paramVariable.getName().contains("_Beta_")){
                varPrior = varPBeta;
                precisionPrior = 1 / varPrior;
                meanPrior = meanPBeta;
            }else if(paramVariable.getName().contains("_Beta0_")){
                varPrior = varPAlpha;
                precisionPrior = 1 / varPrior;
                meanPrior = meanPAlpha;
            }else{
                throw new UnsupportedOperationException("ERROR");
            }

            prior.setNaturalWithMeanPrecision(meanPrior,precisionPrior);
            prior.fixNumericalInstability();
            prior.updateMomentFromNaturalParameters();

        }


        //Initialization for Hidden

        EF_Normal normal = bayesianNetwork.getDistribution(localHiddenVars.get(0));

        double meanPrior = meanPH0;
        double varPrior = varPH0;
        double precisionPrior = 1 / varPrior;

        normal.setNaturalWithMeanPrecision(meanPrior,precisionPrior);
        normal.fixNumericalInstability();
        normal.updateMomentFromNaturalParameters();

       for (int i = 1; i < this.localHiddenVars.size(); i++) {

            EF_Normal_NormalParents normal_normalParents = bayesianNetwork.getDistribution(localHiddenVars.get(i));

            normal_normalParents.setBeta0(0);
            normal_normalParents.setBetas(new double[]{1.0});
            normal_normalParents.setVariance(transtionVariance);

        }

        return bayesianNetwork;

    }

    @Override
    public EF_LearningBayesianNetwork transitionModel(EF_LearningBayesianNetwork bayesianNetwork, PlateuStructure plateuStructure) {

        return bayesianNetwork;
    }
}
