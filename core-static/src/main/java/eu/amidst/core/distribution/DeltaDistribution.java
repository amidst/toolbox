/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.variables.Variable;

import java.util.Random;

/**
 * Created by andresmasegosa on 13/01/15.
 */
public class DeltaDistribution extends UnivariateDistribution {

    double deltaValue;

    public DeltaDistribution(Variable var1, double deltaValue1){
        this.var=var1;
        this.deltaValue=deltaValue1;
    }

    public double getDeltaValue() {
        return deltaValue;
    }

    @Override
    public double getLogProbability(double value) {
        return (deltaValue==value)? 1.0 : 0.0;
    }

    @Override
    public double sample(Random rand) {
        return deltaValue;
    }

    @Override
    public <E extends EF_UnivariateDistribution> E toEFUnivariateDistribution() {
        throw new UnsupportedOperationException("This distribution is not supported yet in exponential form");
    }

    @Override
    public double[] getParameters() {
        return new double[1];
    }

    @Override
    public int getNumberOfParameters() {
        return 1;
    }

    @Override
    public String label() {
        return "Delta of " + this.deltaValue;
    }

    @Override
    public void randomInitialization(Random random) {

    }

    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof DeltaDistribution)
            return this.equalDist((DeltaDistribution)dist,threshold);
        return false;
    }

    @Override
    //TODO
    public String toString() {
        return null;
    }

    public boolean equalDist(DeltaDistribution dist, double threshold) {
        if (dist.getVariable()!=dist.getVariable())
            return false;

        if (deltaValue!=dist.getDeltaValue())
            return false;

        return true;
    }
}
