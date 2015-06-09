/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

/**
 ******************* ISSUE LIST **************************************************************************************
 *
 * 1. Make SufficientStatics an static class to avoid the creation of an object in each call to getSuffStatistics();
 * 2. Make naturalParameters and momentParameters statics?
 * *******************************************************************************************************************
 */

package eu.amidst.corestatic.exponentialfamily;

import eu.amidst.corestatic.datastream.DynamicDataInstance;
import eu.amidst.corestatic.utils.Vector;
import eu.amidst.corestatic.variables.Variable;

/**
 * Created by andresmasegosa on 13/11/14.
 */
public abstract class EF_DynamicDistribution {
    /**
     * The variable of the distribution
     */
    protected Variable var;

    protected NaturalParameters naturalParameters;

    protected MomentParameters momentParameters;

    /**
     * Gets the variable of the distribution
     *
     * @return A <code>Variable</code> object.
     */
    public final Variable getVariable() {

        return this.var;
    }

    public final NaturalParameters getNaturalParameters() {

        return this.naturalParameters;
    }


    public final MomentParameters getMomentParameters() {

        return this.momentParameters;
    }

    public void setNaturalParameters(NaturalParameters parameters) {
        this.naturalParameters=parameters;//.copy(parameters);
        this.updateMomentFromNaturalParameters();
    }

    public void setMomentParameters(SufficientStatistics parameters) {
        this.momentParameters=(MomentParameters)parameters;
        this.updateNaturalFromMomentParameters();
    }

    public void setMomentParameters(MomentParameters parameters) {
        this.momentParameters=parameters;// .copy(parameters);
        this.updateNaturalFromMomentParameters();
    }

    public abstract void updateNaturalFromMomentParameters();

    public abstract void updateMomentFromNaturalParameters();

    public abstract SufficientStatistics getSufficientStatistics(DynamicDataInstance data);

    public abstract int sizeOfSufficientStatistics();

    public abstract double computeLogBaseMeasure(DynamicDataInstance dataInstance);

    public abstract double computeLogNormalizer();

    public double computeProbabilityOf(DynamicDataInstance dataInstance){
        return Math.exp(this.computeLogProbabilityOf(dataInstance));
    }

    //TODO: the logbasemeasure and the lognormalizer are positives or negatives terms (Andres)
    public double computeLogProbabilityOf(DynamicDataInstance dataInstance){
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(dataInstance)) + this.computeLogBaseMeasure(dataInstance) - this.computeLogNormalizer();
    }

    public abstract Vector createZeroedVector();

    public MomentParameters createZeroedMomentParameters(){
        return (MomentParameters)this.createZeroedVector();
    }

    public SufficientStatistics createZeroedSufficientStatistics(){
        return (SufficientStatistics)this.createZeroedVector();
    }

    public NaturalParameters createZeroedNaturalParameters(){
        return (NaturalParameters)this.createZeroedVector();
    }



}
