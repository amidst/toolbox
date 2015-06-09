/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.exponentialfamily;

import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.corestatic.exponentialfamily.*;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.corestatic.utils.Vector;

import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 13/01/15.
 */
public class EF_DynamicBayesianNetwork extends EF_DynamicDistribution {

    EF_BayesianNetwork bayesianNetworkTime0;

    EF_BayesianNetwork bayesianNetworkTimeT;


    public EF_DynamicBayesianNetwork(DynamicDAG dag) {
        this.bayesianNetworkTime0 = new EF_BayesianNetwork(dag.getParentSetsTime0());
        this.bayesianNetworkTimeT = new EF_BayesianNetwork(dag.getParentSetsTimeT());
    }

    public EF_DynamicBayesianNetwork(DynamicBayesianNetwork dbn){
        this.bayesianNetworkTime0 = new EF_BayesianNetwork();
        this.bayesianNetworkTimeT = new EF_BayesianNetwork();

        this.bayesianNetworkTime0.setDistributionList(dbn.getConditionalDistributionsTime0().stream().map(dist -> dist.<EF_ConditionalDistribution>toEFConditionalDistribution()).collect(Collectors.toList()));
        this.bayesianNetworkTimeT.setDistributionList(dbn.getConditionalDistributionsTimeT().stream().map(dist -> dist.<EF_ConditionalDistribution>toEFConditionalDistribution()).collect(Collectors.toList()));


    }

    @Override
    public void updateNaturalFromMomentParameters() {

        CompoundVector globalMomentsParam = (CompoundVector)this.momentParameters;
        CompoundVector vectorNatural = this.createEmtpyCompoundVector();

        globalMomentsParam.getVectorTime0().divideBy(globalMomentsParam.getIndicatorTime0());
        globalMomentsParam.getVectorTimeT().divideBy(globalMomentsParam.getIndicatorTimeT());

        this.bayesianNetworkTime0.setMomentParameters((MomentParameters)globalMomentsParam.getVectorTime0());
        this.bayesianNetworkTimeT.setMomentParameters((MomentParameters)globalMomentsParam.getVectorTimeT());

        vectorNatural.setVectorTime0(this.bayesianNetworkTime0.getNaturalParameters());
        vectorNatural.setVectorTimeT(this.bayesianNetworkTimeT.getNaturalParameters());

        this.naturalParameters=vectorNatural;
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        CompoundVector globalNaturalParam = (CompoundVector)this.naturalParameters;
        CompoundVector vectorMoments = this.createEmtpyCompoundVector();


        this.bayesianNetworkTime0.setNaturalParameters((NaturalParameters) globalNaturalParam.getVectorTime0());
        this.bayesianNetworkTimeT.setNaturalParameters((NaturalParameters) globalNaturalParam.getVectorTimeT());

        vectorMoments.setVectorTime0(this.bayesianNetworkTime0.getNaturalParameters());
        vectorMoments.setVectorTimeT(this.bayesianNetworkTimeT.getNaturalParameters());

        this.momentParameters=vectorMoments;
    }

    @Override
    public SufficientStatistics getSufficientStatistics(DynamicDataInstance data) {
        CompoundVector vectorSS = this.createEmtpyCompoundVector();

        if (data.getTimeID()==0) {
            vectorSS.setIndicatorTime0(1.0);
            vectorSS.setVectorTime0(this.bayesianNetworkTime0.getSufficientStatistics(data));
        }else {
            vectorSS.setIndicatorTimeT(1.0);
            vectorSS.setVectorTimeT(this.bayesianNetworkTimeT.getSufficientStatistics(data));
        }

        return vectorSS;
    }


    @Override
    public int sizeOfSufficientStatistics() {
        return this.bayesianNetworkTimeT.sizeOfSufficientStatistics() + this.bayesianNetworkTime0.sizeOfSufficientStatistics();
    }

    @Override
    public double computeLogBaseMeasure(DynamicDataInstance dataInstance) {
        throw new UnsupportedOperationException("No make sense for dynamic BNs");
    }

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

    @Override
    public Vector createZeroedVector() {
        return this.createCompoundVector();
    }

    public Vector createEmptyZeroedVector() {
        return this.createEmtpyCompoundVector();
    }

    private CompoundVector createEmtpyCompoundVector() {
        return new CompoundVector(this.bayesianNetworkTime0.sizeOfSufficientStatistics() + this.bayesianNetworkTimeT.sizeOfSufficientStatistics());
    }

    private CompoundVector createCompoundVector() {
        return new CompoundVector(this.bayesianNetworkTime0.createZeroedVector(), this.bayesianNetworkTimeT.createZeroedVector());
    }

    public EF_BayesianNetwork getBayesianNetworkTime0() {
        return bayesianNetworkTime0;
    }

    public EF_BayesianNetwork getBayesianNetworkTimeT() {
        return bayesianNetworkTimeT;
    }

    public DynamicBayesianNetwork toDynamicBayesianNetwork(DynamicDAG dag) {
        return DynamicBayesianNetwork.newDynamicBayesianNetwork(dag,
                EF_BayesianNetwork.toConditionalDistribution(this.bayesianNetworkTime0.getDistributionList()),
                EF_BayesianNetwork.toConditionalDistribution(this.bayesianNetworkTimeT.getDistributionList()));
    }

    class CompoundVector implements SufficientStatistics, MomentParameters, NaturalParameters {
        double indicatorTime0;
        double indicatorTimeT;
        Vector vectorTime0;
        Vector vectorTimeT;
        int totalVectorSize;

        public CompoundVector(int totalVectorSize1){
            this.indicatorTime0=0;
            this.indicatorTimeT=0;
            vectorTime0=null;
            vectorTimeT=null;
            totalVectorSize =totalVectorSize1;
        }
        public CompoundVector( Vector vectorTime0_1,  Vector vectorTimeT_1) {
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
            this.sum((CompoundVector) vector);
        }

        @Override
        public void copy(Vector vector) {
            this.copy((CompoundVector) vector);
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
            return this.dotProduct((CompoundVector) vec);
        }

        public double dotProduct(CompoundVector vec) {
            if (vec.size() != this.size())
                throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

            double sum = 0;
            sum += this.getIndicatorTime0()*vec.getIndicatorTime0();
            sum += this.getIndicatorTimeT()*vec.getIndicatorTimeT();

            if (this.vectorTime0!=null && vec.getVectorTime0()!=null) sum += this.vectorTime0.dotProduct(vec.getVectorTime0());
            if (this.vectorTimeT!=null && vec.getVectorTimeT()!=null) sum += this.vectorTimeT.dotProduct(vec.getVectorTimeT());

            return sum;
        }

        public void copy(CompoundVector vector) {

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

        public void sum(CompoundVector vector) {
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