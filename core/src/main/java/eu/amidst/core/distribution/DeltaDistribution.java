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

import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.variables.Variable;
import java.util.Random;

//TODO finish the toString() method

/**
 * This class extends the abstract class {@link UnivariateDistribution} and defines the Delta distribution.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 *
 */
public class DeltaDistribution extends UnivariateDistribution {

    /** Represents the delta value of this DeltaDistribution. */
    double deltaValue;

    /**
     * Creates a new DeltaDistribution for a given variable and delta value.
     * @param var1 a {@link Variable} object.
     * @param deltaValue1 a delta value.
     */
    public DeltaDistribution(Variable var1, double deltaValue1){
        this.var=var1;
        this.deltaValue=deltaValue1;
    }

    /**
     * Returns the delta value of this DeltaDistribution.
     * @return the delta value of this DeltaDistribution.
     */
    public double getDeltaValue() {
        return deltaValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogProbability(double value) {
        return (deltaValue==value)? 1.0 : 0.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double sample(Random rand) {
        return deltaValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends EF_UnivariateDistribution> E toEFUnivariateDistribution() {
        throw new UnsupportedOperationException("This distribution is not supported yet in exponential form");
    }

    @Override
    public UnivariateDistribution deepCopy(Variable variable) {
        return new DeltaDistribution(variable, this.getDeltaValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getParameters() {
        return new double[1];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfParameters() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String label() {
        return "Delta of " + this.deltaValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialization(Random random) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof DeltaDistribution)
            return this.equalDist((DeltaDistribution)dist,threshold);
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
     * Tests if a given delta distribution is equal to this DeltaDistribution.
     * Two delta distributions are equal if they are defined for the same variable and have the same delta values.
     * @param dist a given delta distribution.
     * @return true if the two delta distributions are equal, false otherwise.
     */
    public boolean equalDist(DeltaDistribution dist) {
        if (dist.getVariable()!=dist.getVariable())
            return false;

        if (deltaValue!=dist.getDeltaValue())
            return false;

        return true;
    }
}
