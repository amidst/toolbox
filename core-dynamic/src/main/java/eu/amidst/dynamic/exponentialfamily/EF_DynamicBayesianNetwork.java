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

package eu.amidst.dynamic.exponentialfamily;

import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.utils.Vector;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;

import java.util.stream.Collectors;

/**
 * This class extends the abstract class {@link EF_DynamicDistribution} and defines a {@link DynamicBayesianNetwork} as a
 * conjugate exponential family (EF) model, consisting of EF distributions in canonical form.
 */

public class EF_DynamicBayesianNetwork extends EF_DynamicDistribution {

    /** Represents an {@link EF_BayesianNetwork} object at Time 0. */
    EF_BayesianNetwork bayesianNetworkTime0;

    /** Represents an {@link EF_BayesianNetwork} object at Time T. */
    EF_BayesianNetwork bayesianNetworkTimeT;


    /**
     * Creates a new EF_BayesianNetwork object given a {@link DynamicDAG} object.
     * @param dag a {@link DynamicDAG} object.
     */
    public EF_DynamicBayesianNetwork(DynamicDAG dag) {
        this.bayesianNetworkTime0 = new EF_BayesianNetwork(dag.getParentSetsTime0());
        this.bayesianNetworkTimeT = new EF_BayesianNetwork(dag.getParentSetsTimeT());
    }

    /**
     * Creates a new EF_BayesianNetwork object given a {@link DynamicBayesianNetwork} object.
     * @param dbn a {@link DynamicBayesianNetwork} object.
     */
    public EF_DynamicBayesianNetwork(DynamicBayesianNetwork dbn){
        this.bayesianNetworkTime0 = new EF_BayesianNetwork();
        this.bayesianNetworkTimeT = new EF_BayesianNetwork();

        this.bayesianNetworkTime0.setDistributionList(dbn.getConditionalDistributionsTime0().stream().map(dist -> dist.<EF_ConditionalDistribution>toEFConditionalDistribution()).collect(Collectors.toList()));
        this.bayesianNetworkTimeT.setDistributionList(dbn.getConditionalDistributionsTimeT().stream().map(dist -> dist.<EF_ConditionalDistribution>toEFConditionalDistribution()).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNaturalFromMomentParameters() {

        DynamiceBNCompoundVector globalMomentsParam = (DynamiceBNCompoundVector)this.momentParameters;
        DynamiceBNCompoundVector vectorNatural = this.createEmtpyCompoundVector();

        globalMomentsParam.getVectorTime0().divideBy(globalMomentsParam.getIndicatorTime0());
        globalMomentsParam.getVectorTimeT().divideBy(globalMomentsParam.getIndicatorTimeT());

        this.bayesianNetworkTime0.setMomentParameters((MomentParameters)globalMomentsParam.getVectorTime0());
        this.bayesianNetworkTimeT.setMomentParameters((MomentParameters)globalMomentsParam.getVectorTimeT());

        vectorNatural.setVectorTime0(this.bayesianNetworkTime0.getNaturalParameters());
        vectorNatural.setVectorTimeT(this.bayesianNetworkTimeT.getNaturalParameters());

        this.naturalParameters=vectorNatural;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {
        DynamiceBNCompoundVector globalNaturalParam = (DynamiceBNCompoundVector)this.naturalParameters;
        DynamiceBNCompoundVector vectorMoments = this.createEmtpyCompoundVector();


        this.bayesianNetworkTime0.setNaturalParameters((NaturalParameters) globalNaturalParam.getVectorTime0());
        this.bayesianNetworkTimeT.setNaturalParameters((NaturalParameters) globalNaturalParam.getVectorTimeT());

        vectorMoments.setVectorTime0(this.bayesianNetworkTime0.getNaturalParameters());
        vectorMoments.setVectorTimeT(this.bayesianNetworkTimeT.getNaturalParameters());

        this.momentParameters=vectorMoments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(DynamicDataInstance data) {
        DynamiceBNCompoundVector vectorSS = this.createEmtpyCompoundVector();

        if (data.getTimeID()==0) {
            vectorSS.setIndicatorTime0(1.0);
            vectorSS.setVectorTime0(this.bayesianNetworkTime0.getSufficientStatistics(data));
        }else {
            vectorSS.setIndicatorTimeT(1.0);
            vectorSS.setVectorTimeT(this.bayesianNetworkTimeT.getSufficientStatistics(data));
        }

        return vectorSS;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int sizeOfSufficientStatistics() {
        return this.bayesianNetworkTimeT.sizeOfSufficientStatistics() + this.bayesianNetworkTime0.sizeOfSufficientStatistics();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(DynamicDataInstance dataInstance) {
        throw new UnsupportedOperationException("No make sense for dynamic BNs");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {
        throw new UnsupportedOperationException("No make sense for dynamic BNs");
    }

    @Override
    public double computeLogProbabilityOf(DynamicDataInstance dataInstance) {
        if (dataInstance.getTimeID()==0)
            return this.bayesianNetworkTime0.computeLogProbabilityOf(dataInstance);
        else
            return this.bayesianNetworkTimeT.computeLogProbabilityOf(dataInstance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector createZeroVector() {
        return this.createCompoundVector();
    }

    @Override
    public SufficientStatistics createInitSufficientStatistics() {
        DynamiceBNCompoundVector vectorSS = this.createEmtpyCompoundVector();

        vectorSS.setIndicatorTime0(1.0);
        vectorSS.setVectorTime0(this.bayesianNetworkTime0.createInitSufficientStatistics());

        vectorSS.setIndicatorTimeT(1.0);
        vectorSS.setVectorTimeT(this.bayesianNetworkTimeT.createInitSufficientStatistics());

        return vectorSS;
    }

    /**
     * Returns an empty zeroed parameter vector (i.e., a vector filled with zeros).
     * @return a {@link Vector} object.
     */
    public Vector createEmptyZeroedVector() {
        return this.createEmtpyCompoundVector();
    }

    /**
     * Returns an empty compound parameter vector.
     * @return a {@link DynamiceBNCompoundVector} object.
     */
    private DynamiceBNCompoundVector createEmtpyCompoundVector() {
        return new DynamiceBNCompoundVector(this.bayesianNetworkTime0.sizeOfSufficientStatistics() + this.bayesianNetworkTimeT.sizeOfSufficientStatistics());
    }

    /**
     * Returns a compound parameter vector.
     * @return a {@link DynamiceBNCompoundVector} object.
     */
    private DynamiceBNCompoundVector createCompoundVector() {
        return new DynamiceBNCompoundVector(this.bayesianNetworkTime0.createZeroVector(), this.bayesianNetworkTimeT.createZeroVector());
    }

    /**
     * Returns the {@link EF_BayesianNetwork} at Time 0 of this EF_DynamicBayesianNetwork.
     * @return an {@link EF_BayesianNetwork} object.
     */
    public EF_BayesianNetwork getBayesianNetworkTime0() {
        return bayesianNetworkTime0;
    }

    /**
     * Returns the {@link EF_BayesianNetwork} at Time T of this EF_DynamicBayesianNetwork.
     * @return an {@link EF_BayesianNetwork} object.
     */
    public EF_BayesianNetwork getBayesianNetworkTimeT() {
        return bayesianNetworkTimeT;
    }

    /**
     * Converts this EF_DynamicBayesianNetwork to an equivalent {@link DynamicBayesianNetwork} object.
     * @param dag a {@link DynamicDAG} object defining the graphical structure.
     * @return a {@link DynamicBayesianNetwork} object.
     */
    public DynamicBayesianNetwork toDynamicBayesianNetwork(DynamicDAG dag) {
        return new DynamicBayesianNetwork(dag,
                EF_BayesianNetwork.toConditionalDistribution(this.bayesianNetworkTime0.getDistributionList()),
                EF_BayesianNetwork.toConditionalDistribution(this.bayesianNetworkTimeT.getDistributionList()));
    }

    /**
     * The class CompoundVector implements the interfaces {@link SufficientStatistics}, {@link MomentParameters}, and {@link NaturalParameters},
     * and it handles some utility methods of compound parameter vector for EF_DynamicBayesianNetwork.
     */
    public static class DynamiceBNCompoundVector implements SufficientStatistics, MomentParameters, NaturalParameters {
        double indicatorTime0;
        double indicatorTimeT;
        Vector vectorTime0;
        Vector vectorTimeT;
        int totalVectorSize;

        public DynamiceBNCompoundVector(int totalVectorSize1){
            this.indicatorTime0=0;
            this.indicatorTimeT=0;
            vectorTime0=null;
            vectorTimeT=null;
            totalVectorSize =totalVectorSize1;
        }
        public DynamiceBNCompoundVector(Vector vectorTime0_1, Vector vectorTimeT_1) {
            this.indicatorTime0=0;
            this.indicatorTimeT=0;
            this.vectorTime0=vectorTime0_1;
            this.vectorTimeT=vectorTimeT_1;
            totalVectorSize = this.vectorTime0.size() + this.vectorTimeT.size();
        }
        public double getIndicatorTime0() {
            return indicatorTime0;
        }

        public void setIndicatorTime0(double indicatorTime0) {
            this.indicatorTime0 = indicatorTime0;
        }

        public double getIndicatorTimeT() {
            return indicatorTimeT;
        }

        public void setIndicatorTimeT(double indicatorTimeT) {
            this.indicatorTimeT = indicatorTimeT;
        }

        public Vector getVectorTime0() {
            return vectorTime0;
        }

        public void setVectorTime0(Vector vectorTime0) {
            this.vectorTime0 = vectorTime0;
        }

        public Vector getVectorTimeT() {
            return vectorTimeT;
        }

        public void setVectorTimeT(Vector vectorTimeT) {
            this.vectorTimeT = vectorTimeT;
        }

        @Override
        public double get(int i) {
            throw new UnsupportedOperationException("No get for this vector implementation");
        }

        @Override
        public void set(int i, double val) {
            throw new UnsupportedOperationException("No set for this vector implementation");
        }

        @Override
        public int size() {
            return this.totalVectorSize + 2;
        }

        @Override
        public void sum(Vector vector) {
            this.sum((DynamiceBNCompoundVector) vector);
        }

        @Override
        public void copy(Vector vector) {
            this.copy((DynamiceBNCompoundVector) vector);
        }

        @Override
        public void divideBy(double val) {
            this.indicatorTime0/=val;
            if (this.vectorTime0!=null) this.vectorTime0.divideBy(val);

            this.indicatorTimeT/=val;
            if (this.vectorTimeT!=null) this.vectorTimeT.divideBy(val);
        }

        @Override
        public double dotProduct(Vector vec) {
            return this.dotProduct((DynamiceBNCompoundVector) vec);
        }

        public double dotProduct(DynamiceBNCompoundVector vec) {
            if (vec.size() != this.size())
                throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

            double sum = 0;
            sum += this.getIndicatorTime0()*vec.getIndicatorTime0();
            sum += this.getIndicatorTimeT()*vec.getIndicatorTimeT();

            if (this.vectorTime0!=null && vec.getVectorTime0()!=null) sum += this.vectorTime0.dotProduct(vec.getVectorTime0());
            if (this.vectorTimeT!=null && vec.getVectorTimeT()!=null) sum += this.vectorTimeT.dotProduct(vec.getVectorTimeT());

            return sum;
        }

        public void copy(DynamiceBNCompoundVector vector) {

            if (vector.size() != this.size())
                throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

            this.setIndicatorTime0(vector.getIndicatorTime0());
            this.setIndicatorTimeT(vector.getIndicatorTimeT());

            if (this.vectorTime0==null)
                this.vectorTime0=vector.getVectorTime0();
            else if (vector.getVectorTime0()==null)
                this.vectorTime0=null;
            else
                this.vectorTime0.copy(vector.getVectorTime0());

            if (this.vectorTimeT==null)
                this.vectorTimeT=vector.getVectorTimeT();
            else if (vector.getVectorTimeT()==null)
                this.vectorTimeT=null;
            else
                this.vectorTimeT.copy(vector.getVectorTimeT());
        }

        public void sum(DynamiceBNCompoundVector vector) {
            if (vector.size() != this.size())
                throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

            this.setIndicatorTime0(this.getIndicatorTime0() + vector.getIndicatorTime0());
            this.setIndicatorTimeT(this.getIndicatorTimeT() + vector.getIndicatorTimeT());

            if (this.vectorTime0==null)
                this.vectorTime0=vector.getVectorTime0();
            else if (vector.getVectorTime0()!=null)
                this.vectorTime0.sum(vector.getVectorTime0());

            if (this.vectorTimeT==null)
                this.vectorTimeT=vector.getVectorTimeT();
            else if (vector.getVectorTimeT()!=null)
                this.vectorTimeT.sum(vector.getVectorTimeT());

        }
    }
}