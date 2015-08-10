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
 *
 * This class represents a Bayesian network as a conjugate exponential family model. It inherits from the
 * {@link EF_Distribution} class, because the Bayesian network model defines an exponential family distribution in canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox look at the following paper </p>
 * <p> <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>)
 * </p>
 *
 */
public class EF_BayesianNetwork extends EF_Distribution {

    /** A list of {@link EF_ConditionalDistribution} objects defining the Bayesian network. */
    List<EF_ConditionalDistribution> distributionList;

    /** A field storing the size of the sufficient statistics of the exponential family distributioned defined by the BN model. */
    int sizeSS;

    /**
     * A empty builder.
     */
    public EF_BayesianNetwork() {
        distributionList = new ArrayList<>();
        sizeSS=0;
        this.naturalParameters = this.createEmtpyCompoundVector();
        this.momentParameters = null;
    }

    /**
     * Create a new EF_BayesianNetwork object from a {@link BayesianNetwork} object.
     * @param network, a <code>BayesianNetwork</code> object
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
     * Create a new EF_BayesianNetwork object from a {@link DAG} object.
     * @param dag, a <code>DAG</code> object
     */
    public EF_BayesianNetwork(DAG  dag){
        this(dag.getParentSets());
    }

    /**
     * Create a new EF_BayesianNetwork object from a list of {@link ParentSet} objects.
     * @param parentSets, a list of <code>ParentSet</code> objects
     */
    public EF_BayesianNetwork(List<ParentSet>  parentSets){
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
     * Gets a new {@link BayesianNetwork} equivalent to the current {@link EF_BayesianNetwork} object (ie both
     * objects represent the same Bayesian network model). A {@link DAG} object
     * defining the graphical structure must be supplied.
     * @param dag
     * @return
     */
    public BayesianNetwork toBayesianNetwork(DAG dag){
        return BayesianNetwork.newBayesianNetwork(dag, toConditionalDistribution(this.distributionList));
    }

    /**
     * This static method converts a list of {@link EF_ConditionalDistribution} to a list of equivalent
     * {@link ConditionalDistribution} objects  (ie both represent the same conditional probability distributions).
     *
     * @param ef_dists, a list of <code>EF_ConditionalDistribution</code> objects.
     * @return A list of <code>ConditionalDistribution</code> objects.
     */
    public static List<ConditionalDistribution> toConditionalDistribution(List<EF_ConditionalDistribution> ef_dists){
        ConditionalDistribution[] dists = new ConditionalDistribution[ef_dists.size()];
        ef_dists.stream().forEach(dist -> dists[dist.getVariable().getVarID()] = dist.toConditionalDistribution());
        return Arrays.asList(dists);
    }

    /**
     * Set the EF_Distribution objects defining the EF_BayesianNetwork.
     * @param distributionList_, a list of <code>EF_ConditionalDistribution</code> objects
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
     * Get the list of EF_Distribution objects defining the EF_BayesianNetwork.
     * @return A list of <code>EF_ConditionalDistribution</code> objects
     */
    public List<EF_ConditionalDistribution> getDistributionList() {
        return distributionList;
    }


    /**
     * Get the EF_Distribution object associated to a given variable.
     * @param var, a <code>Variable</code> object.
     * @return A <code>EF_Distribution</code> object.
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
        throw new UnsupportedOperationException("Method not implemented yet!");
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
     * Creates a zero compound parameter vector (ie a vector fills with zeros).
     * @return A <code>CompoundVector</code> object
     */
    private CompoundVector createZeroCompoundVector(){
        return new CompoundVector(this.distributionList.stream().map(w-> w.createZeroVector()).collect(Collectors.toList()));
    }

    /**
     * Creates an empty compound parameter vector.
     * @return A <code>CompoundVector</code> object
     */
    private CompoundVector createEmtpyCompoundVector() {
        return new CompoundVector(this.distributionList.size(), this.sizeOfSufficientStatistics());
    }

    /**
     * Test whether a given EF-BN model is equal to the **this** EF-BN model.
     * @param ef_bayesianNetwork, a <code>EF_BayesianNetwork</code> object.
     * @param threshold, a double value which defines the maximum difference that makes to parameter values to be equal.
     * @return A true/false value indicating whether the models are equal or not.
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
