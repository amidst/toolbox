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

/**
 * This class extends the abstract class {@link UnivariateDistribution} and defines the Uniform distribution.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 *
 */
public class Uniform extends UnivariateDistribution {

    /**
     * Creates a new Uniform distribution for a given variable.
     * @param var1 a {@link Variable} object.
     */
    public Uniform(Variable var1) {
        this.var = var1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogProbability(double value) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double sample(Random rand) {
        return 0;
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
        //Discrete uniform needs 1 parameter (number of states of the variable)
        //Continuous uniform needs 2 parameters (minimum and maximum interval)
        return 0; //TODO Check????
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String label(){ return "Uniform"; }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialization(Random random) {
          //TODO Empty? Check!
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof Uniform)
            return this.equalDist((Uniform)dist,threshold);
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
     * Tests if a given Uniform distribution is equal to this Uniform distribution.
     * @param dist a given Uniform distribution.
     * @param threshold a threshold.
     * @return true if the two Uniform distributions are equal, false otherwise.
     */
    public boolean equalDist(Uniform dist, double threshold) {
        if (dist.getVariable()!=dist.getVariable())
            return false;

        return true;
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
        return new Uniform(variable);
    }
}
