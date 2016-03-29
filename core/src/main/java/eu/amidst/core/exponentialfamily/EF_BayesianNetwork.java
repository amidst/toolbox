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
 * This class extends the abstract class {@link EF_Distribution} and defines a {@link BayesianNetwork} as a
 * conjugate exponential family (EF) model, consisting of EF distributions in canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>)
 * </p>
 */
public class EF_BayesianNetwork extends EF_Distribution {

    /** Represents the list of {@link EF_ConditionalDistribution} objects in this EF_BayesianNetwork. */
    List<EF_ConditionalDistribution> distributionList;

    /** Represents the size of the sufficient statistics of the EF distributions. */
    int sizeSS;

    /**
     * Creates a new EF_BayesianNetwork object.
     */
    public EF_BayesianNetwork() {
        distributionList = new ArrayList<>();
        sizeSS=0;
        this.naturalParameters = this.createEmtpyCompoundVector();
        this.momentParameters = null;
    }

    /**
     * Creates a new EF_BayesianNetwork object from a {@link BayesianNetwork} object.
     * @param network a {@link BayesianNetwork} object.
     */
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

    /**
     * Creates a new EF_BayesianNetwork object from a {@link DAG} object.
     * @param dag a {@link DAG} object.
     */
    public EF_BayesianNetwork(DAG  dag){
        this(dag.getParentSets());
    }

    /**
     * Creates a new EF_BayesianNetwork object from a list of {@link ParentSet} objects.
     * @param parentSets a list of {@link ParentSet} objects.
     */
    public EF_BayesianNetwork(List<ParentSet> parentSets){
        distributionList = new ArrayList(parentSets.size());

        sizeSS=0;
        for (ParentSet parentSet: parentSets){
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

    /**
     * Converts this EF_BayesianNetwork to an equivalent {@link BayesianNetwork} object.
     * @param dag a {@link DAG} object defining the graphical structure.
     * @return a {@link BayesianNetwork} object.
     */
    public BayesianNetwork toBayesianNetwork(DAG dag){
        return new BayesianNetwork(dag, toConditionalDistribution(this.distributionList));
    }

    /**
     * Converts a given list of {@link EF_ConditionalDistribution} to a list of equivalent {@link ConditionalDistribution} objects.
     * I.e., both represent the same conditional probability distributions.
     * @param ef_dists a list of {@link EF_ConditionalDistribution} objects.
     * @return a list of {@link ConditionalDistribution}objects.
     */
    public static List<ConditionalDistribution> toConditionalDistribution(List<EF_ConditionalDistribution> ef_dists){
        ConditionalDistribution[] dists = new ConditionalDistribution[ef_dists.size()];
        ef_dists.stream().forEach(dist -> dists[dist.getVariable().getVarID()] = dist.toConditionalDistribution());
        return Arrays.asList(dists);
    }

    /**
     * Sets the list of EF_ConditionalDistribution objects for this EF_BayesianNetwork.
     * @param distributionList_ a list of {@link EF_ConditionalDistribution} objects.
     */
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

    /**
     * Returns the list of EF_Distribution objects of this EF_BayesianNetwork.
     * @return a list of {@link EF_ConditionalDistribution} objects.
     */
    public List<EF_ConditionalDistribution> getDistributionList() {
        return distributionList;
    }

    /**
     * Returns the EF Conditional Distribution associated to a given variable.
     * @param var a {@link Variable} object.
     * @return an {@link EF_ConditionalDistribution} object.
     */
    public EF_ConditionalDistribution getDistribution(Variable var) {
        return distributionList.get(var.getVarID());
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("This method does not apply in this case!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        CompoundVector vectorSS = this.createEmtpyCompoundVector();//.createZeroCompoundVector();

        this.distributionList.stream().forEach(w -> {
            vectorSS.setVectorByPosition(w.getVariable().getVarID(), w.getSufficientStatistics(data));
        });

        return vectorSS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sizeOfSufficientStatistics() {
        return sizeSS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return this.distributionList.stream().mapToDouble(w -> w.computeLogBaseMeasure(dataInstance)).sum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {
        return this.distributionList.stream().mapToDouble(w -> w.computeLogNormalizer()).sum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector createZeroVector() {
        return createZeroCompoundVector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics createInitSufficientStatistics(){
        return new CompoundVector(this.distributionList.stream().map(w-> w.createInitSufficientStatistics()).collect(Collectors.toList()));
    }

    /**
     * Creates a zero compound parameter vector (i.e., a vector filled with zeros).
     * @return a {@link CompoundVector} object.
     */
    private CompoundVector createZeroCompoundVector(){
        return new CompoundVector(this.distributionList.stream().map(w-> w.createZeroVector()).collect(Collectors.toList()));
    }

    /**
     * Creates an empty compound parameter vector.
     * @return a {@link CompoundVector} object.
     */
    private CompoundVector createEmtpyCompoundVector() {
        return new CompoundVector(this.distributionList.size(), this.sizeOfSufficientStatistics());
    }

    /**
     * Tests whether a given EF_BayesianNetwork model is equal to this EF_BayesianNetwork model.
     * @param ef_bayesianNetwork a given input EF_BayesianNetwork object.
     * @param threshold a {@code double} value that represents the threshold or maximum allowed difference.
     * @return {@code true} if the two models are equal, {@code false} otherwise.
     */
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
