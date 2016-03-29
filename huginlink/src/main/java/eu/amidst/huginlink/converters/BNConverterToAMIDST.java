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

package eu.amidst.huginlink.converters;

import COM.hugin.HAPI.*;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.distribution.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The BNConverterToAMIDST class converts a Bayesian network model from Hugin to AMIDST format.
 */
public class BNConverterToAMIDST {

    /** Represents the {@link BayesianNetwork} model in AMIDST format. */
    private BayesianNetwork amidstBN;

    /** The Bayesian network model in Hugin format. */
    private Domain huginBN;

    /**
     * Class constructor.
     * @param huginBN_ the Hugin model to be converted.
     */
    public BNConverterToAMIDST(Domain huginBN_){
        this.huginBN = huginBN_;
    }

    /**
     * Sets the AMIDST model structure (nodes and parents) from the Hugin model.
     * @throws ExceptionHugin
     */
    private void setNodesAndParents() throws ExceptionHugin {

        List<Attribute> attributes = new ArrayList<>();

        NodeList huginNodes = this.huginBN.getNodes();
        int numNodes = huginNodes.size();

        for(int i=0;i<numNodes;i++){
            Node n = (Node)huginNodes.get(i);
            if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                int numStates = (int)((DiscreteChanceNode)n).getNumberOfStates();
                attributes.add(new Attribute(i, n.getName(), new FiniteStateSpace(numStates)));
            }
            else if (n.getKind().compareTo(NetworkModel.H_KIND_CONTINUOUS) == 0) {
                attributes.add(new Attribute(i, n.getName(), new RealStateSpace()));
            }
        }
        Variables variables = new Variables(new Attributes(attributes));
        DAG dag = new DAG(variables);

        try {
            dag.setName(Paths.get(huginBN.getFileName()).getFileName().toString());
        }catch(Exception e){
            dag.setName("DAG");
        }

        Variables amidstVariables = variables;

        for(int i=0;i<numNodes;i++){
            Node huginChild = huginNodes.get(i);
            NodeList huginParents = huginChild.getParents();
            Variable amidstChild = amidstVariables.getVariableByName(huginChild.getName());

            // Only multinomial parents are indexed in reverse order in Hugin
            //-----------------------------------------------------------------------------
            ArrayList<Integer> multinomialParentsIndexes = new ArrayList();
            for (int j=0;j<huginParents.size();j++) {
                Node huginParent = huginParents.get(j);
                if (huginParent.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                    multinomialParentsIndexes.add(j);
                }
            }
            Collections.reverse(multinomialParentsIndexes);
            ArrayList<Integer> parentsIndexes = new ArrayList();
            for (int j=0;j<huginParents.size();j++) {
                Node huginParent = huginParents.get(j);
                if (huginParent.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                    parentsIndexes.add(multinomialParentsIndexes.get(0));
                    multinomialParentsIndexes.remove(0);
                }
                else {
                    parentsIndexes.add(j);
                }
            }
            //-----------------------------------------------------------------------------
            for(int j=0;j<huginParents.size();j++) {
                Node huginParent = huginParents.get(parentsIndexes.get(j));
                Variable amidstParent = amidstVariables.getVariableByName(huginParent.getName());
                dag.getParentSet(amidstChild).addParent(amidstParent);
            }
        }
        this.amidstBN = new BayesianNetwork(dag);
        try {
            amidstBN.setName(Paths.get(huginBN.getFileName()).getFileName().toString());
        }catch(Exception e){
            amidstBN.setName("BN");
        }
    }

    /**
     * Sets the distribution of a multinomial variable with no parents in the AMIDST model
     * from the corresponding distribution in the Hugin model.
     * @param huginVar the Hugin variable with the distribution to be converted.
     * @throws ExceptionHugin
     */
    private void setMultinomial(Node huginVar) throws ExceptionHugin {
        int indexNode = this.huginBN.getNodes().indexOf(huginVar);
        Variable amidstVar = this.amidstBN.getVariables().getVariableById(indexNode);
        int numStates = amidstVar.getNumberOfStates();

        double[] huginProbabilities = huginVar.getTable().getData();
        double[] amidstProbabilities = new double[numStates];
        for (int k = 0; k < numStates; k++) {
            amidstProbabilities[k] = huginProbabilities[k];
        }
        Multinomial dist = this.amidstBN.getConditionalDistribution(amidstVar);
        dist.setProbabilities(amidstProbabilities);
    }

    /**
     * Sets the distribution of a multinomial variable with multinomial parents in the AMIDST model
     * from the corresponding distribution in the Hugin model.
     * @param huginVar the Hugin variable with the distribution to be converted.
     * @throws ExceptionHugin
     */
    private void setMultinomial_MultinomialParents(Node huginVar) throws ExceptionHugin {

        int indexNode = this.huginBN.getNodes().indexOf(huginVar);
        Variable amidstVar = this.amidstBN.getVariables().getVariableById(indexNode);
        int numStates = amidstVar.getNumberOfStates();

        double[] huginProbabilities = huginVar.getTable().getData();

        List<Variable> parents = this.amidstBN.getDAG().getParentSet(amidstVar).getParents();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(parents);

        for (int i = 0; i < numParentAssignments; i++) {

            double[] amidstProbabilities = new double[numStates];
            for (int k = 0; k < numStates; k++) {
                amidstProbabilities[k] = huginProbabilities[i * numStates + k];
            }
            Multinomial_MultinomialParents dist = this.amidstBN.getConditionalDistribution(amidstVar);
            dist.getMultinomial(i).setProbabilities(amidstProbabilities);
        }
    }

    /**
     * Sets the distribution of a normal variable with normal parents in the AMIDST model
     * from the corresponding distribution in the Hugin model.
     * @param huginVar the Hugin variable with the distribution to be converted.
     * @throws ExceptionHugin
     */
    private void setNormal_NormalParents(Node huginVar) throws ExceptionHugin {

        int indexNode = this.huginBN.getNodes().indexOf(huginVar);
        Variable amidstVar = this.amidstBN.getVariables().getVariableById(indexNode);
        ConditionalLinearGaussian dist = this.amidstBN.getConditionalDistribution(amidstVar);

        double huginIntercept = ((ContinuousChanceNode)huginVar).getAlpha(0);
        dist.setIntercept(huginIntercept);

        NodeList huginParents = huginVar.getParents();
        int numParents = huginParents.size();
        double[] coefficients = new double[numParents];

        for(int i=0;i<numParents;i++){
            ContinuousChanceNode huginParent = (ContinuousChanceNode)huginParents.get(i);
            coefficients[i]= ((ContinuousChanceNode)huginVar).getBeta(huginParent,0);
        }
        dist.setCoeffParents(coefficients);

        double huginVariance = ((ContinuousChanceNode)huginVar).getGamma(0);
        dist.setVariance(huginVariance);
    }

    /**
     * Sets the distribution of a normal variable in the AMIDST model from the ith-normal distribution in the list
     * of multinomial parent assignments for this variable in the Hugin model.
     * @param huginVar the Hugin variable with the distribution to be converted.
     * @param normal the <code>Normal</code> distribution to be modified.
     * @param i the position in which an assignment of the multinomial parents is indexed in Hugin. This is needed to
     *          obtain the mean and variance of the normal distribution associated to the assignment.
     *          Note that <code>i</code> is equal to 0 when the variable has no multinomial parents.
     * @throws ExceptionHugin
     */
    private void setNormal(Node huginVar, Normal normal, int i) throws ExceptionHugin {

        double huginMean  = ((ContinuousChanceNode)huginVar).getAlpha(i);
        double huginVariance  = ((ContinuousChanceNode)huginVar).getGamma(i);
        normal.setMean(huginMean);
        normal.setVariance(huginVariance);
    }

    /**
     * Sets the distribution of a normal variable in the AMIDST model.
     * @param huginVar the Hugin variable with the corresponding distribution to be converted.
     * @throws ExceptionHugin
     */
    private void setNormal(Node huginVar) throws ExceptionHugin {
        int indexNode = this.huginBN.getNodes().indexOf(huginVar);
        Variable amidstVar = this.amidstBN.getVariables().getVariableById(indexNode);
        Normal dist = this.amidstBN.getConditionalDistribution(amidstVar);
        this.setNormal(huginVar, dist, 0);
    }

    /**
     * Sets the distribution of a normal variable with multinomial parents in the AMIDST model from the corresponding
     * distribution in the Hugin model.
     * For each assignment of the multinomial parents, a univariate normal is set.
     * @param huginVar the Hugin variable with the distribution to be converted.
     * @throws ExceptionHugin
     */
    private void setNormal_MultinomialParents(Node huginVar) throws ExceptionHugin {

        int indexNode = this.huginBN.getNodes().indexOf(huginVar);
        Variable amidstVar = this.amidstBN.getVariables().getVariableById(indexNode);

        List<Variable> conditioningVariables = this.amidstBN.getDAG().getParentSet(amidstVar).getParents();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);
        Normal_MultinomialParents dist = this.amidstBN.getConditionalDistribution(amidstVar);
        for (int i = 0; i < numParentAssignments; i++) {
            Normal normal = dist.getNormal(i);
            this.setNormal(huginVar, normal, i);
        }
    }

    /**
     * Sets the distribution of a normal variable with normal and multinomial parents in the AMIDST model from the
     * corresponding distribution in the Hugin model.
     * For each assignment of the multinomial parents, a CLG distribution is set.
     * @param huginVar the Hugin variable with the distribution to be converted.
     * @throws ExceptionHugin
     */
    private void setNormal_MultinomialNormalParents(Node huginVar) throws ExceptionHugin {

        int indexNode = this.huginBN.getNodes().indexOf(huginVar);
        Variable amidstVar = this.amidstBN.getVariables().getVariableById(indexNode);
        Normal_MultinomialNormalParents dist = this.amidstBN.getConditionalDistribution(amidstVar);

        List<Variable> multinomialParents = dist.getMultinomialParents();

        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);

        for(int i=0;i<numParentAssignments;i++) {

            ConditionalLinearGaussian normalNormal = dist.getNormal_NormalParentsDistribution(i);

            double huginIntercept = ((ContinuousChanceNode)huginVar).getAlpha(i);
            normalNormal.setIntercept(huginIntercept);

            List<Variable> normalParents = dist.getNormalParents();
            int numParents = normalParents.size();
            double[] coefficients = new double[numParents];

            for(int j=0;j<numParents;j++){
                String nameAmidstNormalParent = normalParents.get(j).getName();
                ContinuousChanceNode huginParent =  (ContinuousChanceNode)this.huginBN.getNodeByName(nameAmidstNormalParent);
                coefficients[j]= ((ContinuousChanceNode)huginVar).getBeta(huginParent,i);
            }
            normalNormal.setCoeffParents(coefficients);

            double huginVariance = ((ContinuousChanceNode)huginVar).getGamma(i);
            normalNormal.setVariance(huginVariance);
        }
    }

    /**
     * Sets the distributions for all the variables in the AMIDST network from the distributions in the Hugin model.
     * For each variable, the distribution type is determined and the corresponding conversion is carried out.
     * @throws ExceptionHugin
     */
    private void setDistributions() throws ExceptionHugin {

        NodeList huginNodes = this.huginBN.getNodes();
        Variables amidstVariables = this.amidstBN.getVariables();

        for (int i = 0; i < huginNodes.size(); i++) {

            Variable amidstVar = amidstVariables.getVariableById(i);
            Node huginVar = (Node)huginNodes.get(i);

            int type = Utils.getConditionalDistributionType(amidstVar, amidstBN);

            switch (type) {
                case 0:
                    this.setMultinomial_MultinomialParents(huginVar);
                    break;
                case 1:
                    this.setNormal_NormalParents(huginVar);
                    break;
                case 2:
                    this.setNormal_MultinomialParents(huginVar);
                    break;
                case 3:
                    this.setNormal_MultinomialNormalParents(huginVar);
                    break;
                case 4:
                    this.setMultinomial(huginVar);
                    break;
                case 5:
                    this.setNormal(huginVar);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized DistributionType. ");
            }
        }
    }

    /**
     * Converts a Bayesian network from Hugin to AMIDST format.
     * @param huginBN the Hugin Bayesian network to be converted.
     * @return the converted AMIDST BayesianNetwork.
     * @throws ExceptionHugin
     */
    public static BayesianNetwork convertToAmidst(Domain huginBN) throws ExceptionHugin {

        BNConverterToAMIDST converterToAMIDST = new BNConverterToAMIDST(huginBN);
        converterToAMIDST.setNodesAndParents();
        converterToAMIDST.setDistributions();

        return converterToAMIDST.amidstBN;
    }
}

