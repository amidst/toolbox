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

package eu.amidst.core.distribution;

import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import java.util.ArrayList;
import java.util.Random;

//TODO: Check getNumberOfParameters()! I'm not sure about how to compute this!
//TODO" toString() method!

/**
 * This class extends the abstract class {@link ConditionalDistribution} and defines the Indicator distribution.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 *
 */
public class IndicatorDistribution extends ConditionalDistribution {

    /** Represents the conditional distribution {@link ConditionalDistribution}. */
    private ConditionalDistribution conditionalDistribution;

    /** Represents the delta distribution {@link DeltaDistribution}. */
    private DeltaDistribution deltaDist;

    /** Represents the indicator {@link Variable}. */
    private Variable indicatorVar;

    /**
     * Creates a new IndicatorDistribution for a given indicator variable and a conditional distribution.
     * @param indicatorVar1 a given indicator variable.
     * @param conditionalDistribution1 a conditional distribution.
     */
    public IndicatorDistribution(Variable indicatorVar1, ConditionalDistribution conditionalDistribution1) {
        //if (!indicatorVar1.isIndicator()) {
        //    throw new IllegalArgumentException("IndicatorVar_ should be of indicator type");
        //}
        this.var = conditionalDistribution1.getVariable();
        this.parents = new ArrayList<>();
        for (Variable var: conditionalDistribution1.getConditioningVariables()){
            this.parents.add(var);
        }

        this.parents.add(indicatorVar1);
        this.conditionalDistribution=conditionalDistribution1;
        this.indicatorVar = indicatorVar1;
        this.deltaDist = new DeltaDistribution(this.getVariable(), 0.0);
    }

    /**
     * Returns the {@link ConditionalDistribution} of this IndicatorDistribution.
     * @return a {@link ConditionalDistribution} object.
     */
    public ConditionalDistribution getConditionalDistribution() {
        return conditionalDistribution;
    }

    /**
     * Returns the indicator variable of this IndicatorDistribution.
     * @return the indicator variable of this IndicatorDistribution.
     */
    public Variable getIndicatorVar() {
        return indicatorVar;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getParameters() {
        return new double[this.getNumberOfParameters()];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfParameters() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogConditionalProbability(Assignment assignment) {
        if (assignment.getValue(this.indicatorVar)==0.0) {
            // Both the indicator and main var have, by definition, the same value.
            return 0.0;
        }else {
            return this.conditionalDistribution.getLogConditionalProbability(assignment);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        if (assignment.getValue(this.indicatorVar)==0.0) {
            return this.deltaDist;
        }else{
            return this.conditionalDistribution.getUnivariateDistribution(assignment);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String label(){
        return "IndicatorDistribution of "+this.getConditionalDistribution().label();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialization(Random random) {
        this.conditionalDistribution.randomInitialization(random);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof IndicatorDistribution)
            return this.equalDist((IndicatorDistribution)dist,threshold);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return null;
    }

    /**
     * Tests if a given indicator distribution is equal to this IndicatorDistribution.
     * @param dist a given indicator distribution.
     * @param threshold a threshold.
     * @return true if the two delta distributions are equal, false otherwise.
     */
    public boolean equalDist(IndicatorDistribution dist, double threshold) {
        return this.getConditionalDistribution().equalDist(dist.getConditionalDistribution(),threshold);
    }
}
