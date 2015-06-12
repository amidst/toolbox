/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.ida2015;

import eu.amidst.corestatic.exponentialfamily.EF_BaseDistribution_MultinomialParents;
import eu.amidst.corestatic.exponentialfamily.EF_Dirichlet;
import eu.amidst.corestatic.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.corestatic.exponentialfamily.EF_Multinomial_Dirichlet;
import eu.amidst.corestatic.learning.parametric.bayesian.PlateuStructure;
import eu.amidst.corestatic.learning.parametric.bayesian.TransitionMethod;
import eu.amidst.corestatic.variables.Variable;

import java.util.List;

/**
 * Created by andresmasegosa on 13/4/15.
 */
public class MultinomialHiddenTransitionMethod implements TransitionMethod{

    List<Variable> localHiddenVars;
    double transitionProb;

    public MultinomialHiddenTransitionMethod(List<Variable> localHiddenVars_, double transitionProb_){
        this.localHiddenVars=localHiddenVars_;
        this.transitionProb = transitionProb_;
    }

    @Override
    public EF_LearningBayesianNetwork initModel(EF_LearningBayesianNetwork bayesianNetwork, PlateuStructure plateuStructure) {

        for (Variable localVar : this.localHiddenVars) {

            EF_Multinomial_Dirichlet multinomial_dirichlet = (EF_Multinomial_Dirichlet) bayesianNetwork.getDistribution(localVar);

            Variable dirichletVariable = multinomial_dirichlet.getDirichletVariable();

            EF_Dirichlet dirichlet = ((EF_BaseDistribution_MultinomialParents<EF_Dirichlet>) bayesianNetwork.getDistribution(dirichletVariable)).getBaseEFDistribution(0);

            dirichlet.getNaturalParameters().set(0,1);
            for (int i = 1; i < localVar.getNumberOfStates(); i++) {
                dirichlet.getNaturalParameters().set(i,1);
            }

            dirichlet.updateMomentFromNaturalParameters();

            //
            /*
            for (int i = 0; i < plateuStructure.getNumberOfReplications(); i++) {
                ((EF_Multinomial)plateuStructure.getNodeOfVar(localVar,i).getQDist()).getMomentParameters().set(0,1.0);
                for (int j = 1; j < localVar.getNumberOfStates(); j++) {
                    ((EF_Multinomial)plateuStructure.getNodeOfVar(localVar,i).getQDist()).getMomentParameters().set(j,0);
                }
                ((EF_Multinomial)plateuStructure.getNodeOfVar(localVar,i).getQDist()).updateNaturalFromMomentParameters();
            }*/

        }

        return bayesianNetwork;
    }

    @Override
    public EF_LearningBayesianNetwork transitionModel(EF_LearningBayesianNetwork bayesianNetwork, PlateuStructure plateuStructure) {
/*
        for (Variable localVar : this.localHiddenVars) {
            Multinomial multiPreviousTimeSep = plateuStructure.getEFVariablePosterior(localVar, 0).toUnivariateDistribution();

            EF_Multinomial_Dirichlet multinomial_dirichlet = (EF_Multinomial_Dirichlet) bayesianNetwork.getDistribution(localVar);

            Variable dirichletVariable = multinomial_dirichlet.getDirichletVariable();

            EF_Dirichlet dirichlet = ((EF_BaseDistribution_MultinomialParents<EF_Dirichlet>) bayesianNetwork.getDistribution(dirichletVariable)).getBaseEFDistribution(0);


            double[] newprobs = new double[localVar.getNumberOfStates()];

            newprobs[0] = multiPreviousTimeSep.getProbabilityOfState(0) * (1-transitionProb);

            for (int i = 1; i < localVar.getNumberOfStates(); i++) {
                newprobs[i] =   multiPreviousTimeSep.getProbabilityOfState(i) * (1-transitionProb) +
                                multiPreviousTimeSep.getProbabilityOfState(i - 1) * transitionProb;
            }

            Utils.normalize(newprobs);

            for (int i = 0; i < localVar.getNumberOfStates(); i++) {
                dirichlet.getNaturalParameters().set(i,newprobs[i]*1000+1);
            }


            //
            //
            for (int i = 0; i < plateuStructure.getNumberOfReplications(); i++) {
                for (int j = 0; j < localVar.getNumberOfStates(); j++) {
                    ((EF_Multinomial)plateuStructure.getNodeOfVar(localVar,i).getQDist()).getMomentParameters().set(j,newprobs[j]);
                }
                ((EF_Multinomial)plateuStructure.getNodeOfVar(localVar,i).getQDist()).updateNaturalFromMomentParameters();
            }
        }
        */
        /***** FADING ****/
        /*
        double fading = 0.9;

        bayesianNetwork.getParametersVariables().getListOfVariables().stream().forEach(var -> {
            EF_BaseDistribution_MultinomialParents dist = (EF_BaseDistribution_MultinomialParents) bayesianNetwork.getDistribution(var);
            EF_UnivariateDistribution prior = dist.getBaseEFUnivariateDistribution(0);
            NaturalParameters naturalParameters = prior.getNaturalParameters();
            naturalParameters.multiplyBy(fading);
            prior.setNaturalParameters(naturalParameters);
            dist.setBaseEFDistribution(0, prior);
        });

        */
        return bayesianNetwork;
    }
}
