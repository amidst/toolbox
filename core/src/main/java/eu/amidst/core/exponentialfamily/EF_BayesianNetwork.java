/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public class EF_BayesianNetwork extends EF_Distribution {

    List<EF_ConditionalDistribution> distributionList;
    int sizeSS;

    public EF_BayesianNetwork() {
        distributionList = new ArrayList<>();
        sizeSS=0;
        this.naturalParameters = this.createEmtpyCompoundVector();
        this.momentParameters = null;
    }

    public EF_BayesianNetwork(BayesianNetwork network){
        distributionList = new ArrayList(network.getNumberOfVars());

        sizeSS=0;
        for (ConditionalDistribution dist: network.getConditionalDistributions()){
            EF_ConditionalDistribution ef_dist = dist.toEFConditionalDistribution();
            distributionList.add(ef_dist.getVariable().getVarID(), ef_dist);
            sizeSS+=ef_dist.sizeOfSufficientStatistics();
        }

        CompoundVector vectorNatural = this.createEmtpyCompoundVector();

        for (EF_ConditionalDistribution dist : distributionList){
            vectorNatural.setVectorByPosition(dist.getVariable().getVarID(), dist.getNaturalParameters());
        }
        this.naturalParameters = vectorNatural;
        this.momentParameters = null;
    }

    public void setDistributionList(List<EF_ConditionalDistribution> distributionList_) {

        distributionList = distributionList_;

        sizeSS=0;
        for (EF_ConditionalDistribution ef_dist: distributionList){
            sizeSS+=ef_dist.sizeOfSufficientStatistics();
        }

        CompoundVector vectorNatural = this.createEmtpyCompoundVector();

        for (EF_ConditionalDistribution dist : distributionList){
            vectorNatural.setVectorByPosition(dist.getVariable().getVarID(), dist.getNaturalParameters());
        }
        this.naturalParameters = vectorNatural;
        this.momentParameters = null;
    }

    public EF_BayesianNetwork(DAG  dag){
        this(dag.getParentSets());
    }


    public EF_BayesianNetwork(List<ParentSet>  parentSets){
        distributionList = new ArrayList(parentSets.size());

        sizeSS=0;
        for (ParentSet parentSet: parentSets){
            //ConditionalDistribution dist = parentSet.getMainVar().newConditionalDistribution(parentSet.getParents());
            //dist.randomInitialization(new Random(0));
            EF_ConditionalDistribution ef_dist = parentSet.getMainVar().getDistributionType().newEFConditionalDistribution(parentSet.getParents());
            distributionList.add(ef_dist.getVariable().getVarID(), ef_dist);
            sizeSS+=ef_dist.sizeOfSufficientStatistics();
        }
        CompoundVector vectorNatural = this.createEmtpyCompoundVector();

        for (EF_ConditionalDistribution dist : distributionList){
            vectorNatural.setVectorByPosition(dist.getVariable().getVarID(), dist.getNaturalParameters());
        }
        this.naturalParameters = vectorNatural;
        this.momentParameters = null;
    }

    public BayesianNetwork toBayesianNetwork(DAG dag){
        return BayesianNetwork.newBayesianNetwork(dag, toConditionalDistribution(this.distributionList));
    }

    public static List<ConditionalDistribution> toConditionalDistribution(List<EF_ConditionalDistribution> ef_dists){
        ConditionalDistribution[] dists = new ConditionalDistribution[ef_dists.size()];
        ef_dists.stream().forEach(dist -> dists[dist.getVariable().getVarID()] = dist.toConditionalDistribution());
        return Arrays.asList(dists);
    }

    public List<EF_ConditionalDistribution> getDistributionList() {
        return distributionList;
    }


    public EF_ConditionalDistribution getDistribution(Variable var) {
        return distributionList.get(var.getVarID());
    }

    @Override
    public void updateNaturalFromMomentParameters() {

        CompoundVector globalMomentsParam = (CompoundVector)this.momentParameters;
        CompoundVector vectorNatural = this.createEmtpyCompoundVector();

        this.distributionList.stream().forEach(w -> {
            MomentParameters localMomentParam = (MomentParameters) globalMomentsParam.getVectorByPosition(w.getVariable().getVarID());
            w.setMomentParameters(localMomentParam);
            vectorNatural.setVectorByPosition(w.getVariable().getVarID(),w.getNaturalParameters());
        });

        this.naturalParameters=vectorNatural;
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        CompoundVector vectorSS = this.createEmtpyCompoundVector();//.createCompoundVector();

        this.distributionList.stream().forEach(w -> {
            vectorSS.setVectorByPosition(w.getVariable().getVarID(), w.getSufficientStatistics(data));
        });

        return vectorSS;
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return sizeSS;
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return this.distributionList.stream().mapToDouble(w -> w.computeLogBaseMeasure(dataInstance)).sum();
    }

    @Override
    public double computeLogNormalizer() {
        return this.distributionList.stream().mapToDouble(w -> w.computeLogNormalizer()).sum();
    }

    @Override
    public Vector createZeroedVector() {
        return createCompoundVector();
    }

    private CompoundVector createCompoundVector(){
        return new CompoundVector(this.distributionList.stream().map(w-> w.createZeroedVector()).collect(Collectors.toList()));
    }

    private CompoundVector createEmtpyCompoundVector() {
        return new CompoundVector(this.distributionList.size(), this.sizeOfSufficientStatistics());
    }

    public boolean equal_efBN(EF_BayesianNetwork ef_bayesianNetwork, double threshold){

        for (EF_ConditionalDistribution this_dist: this.getDistributionList()){
            EF_ConditionalDistribution ef_dist = ef_bayesianNetwork.getDistribution(this_dist.getVariable());
            if(!this_dist.getClass().getName().equals(ef_dist.getClass().getName()))
                return false;
            List<Variable> this_Vars = this_dist.getConditioningVariables();
            List<Variable> ef_Vars = ef_dist.getConditioningVariables();
            if(this_Vars.size()!=ef_Vars.size())
                return false;
            for(Variable var: this_Vars){
                if(!ef_Vars.contains(var))
                    return false;
            }
        }

        return this.getNaturalParameters().equalsVector(ef_bayesianNetwork.getNaturalParameters(), threshold);

    }

}
