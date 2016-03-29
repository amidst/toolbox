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

import COM.hugin.HAPI.Class;
import COM.hugin.HAPI.*;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The DBNConverterToAmidst class converts a Dynamic Bayesian network model from Hugin to AMIDST.
 * It works only for multinomial distributions.
 */
public class DBNConverterToAmidst {

    /** Represents the Dynamic Bayesian network model in AMIDST format. */
    private DynamicBayesianNetwork amidstDBN;

    /** Represents the Dynamic Bayesian network model in Hugin format. */
    private Class huginDBN;

    /**
     * Class constructor.
     * @param huginDBN_ the Hugin model to be converted.
     */
    public DBNConverterToAmidst(Class huginDBN_){
        this.huginDBN = huginDBN_;
    }

    /**
     * Sets the AMIDST model structure (nodes and parents) from the Hugin model.
     * @throws ExceptionHugin
     */
    private void setNodesAndParents() throws ExceptionHugin {

        List<Attribute> listOfAttributes = new ArrayList<>();
        NodeList huginNodes = this.huginDBN.getNodes();
        int numNodes = huginNodes.size();

        for(int i=0;i<numNodes;i++){

            Node n = (Node)huginNodes.get(i);
            if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) != 0)
                throw new IllegalArgumentException("Only multinomial distributions are allowed.");

            //Only temporal master nodes
            if (n.getTemporalMaster()==null) {
                int numStates = (int) ((DiscreteChanceNode) n).getNumberOfStates();
                listOfAttributes.add(new Attribute(i, n.getName(), new FiniteStateSpace(numStates)));
            }
        }

        Attributes attributes = new Attributes(listOfAttributes);
        DynamicVariables dynamicVariables = new DynamicVariables(attributes);
        DynamicDAG dynamicDAG  = new DynamicDAG(dynamicVariables);

        // Set the ParentSet at time T. ParentSet at time 0 are automatically created at the same time.
        for(int i=0;i<numNodes;i++){
            Node huginChild = (Node)huginNodes.get(i);
            if(huginChild.getTemporalMaster()==null){ //Only master nodes
                Variable amidstChild = dynamicVariables.getVariableByName(huginChild.getName());
                NodeList huginParents = huginChild.getParents();

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
                    Node huginParent = (Node) huginParents.get(parentsIndexes.get(j));
                    if(huginParent.getTemporalMaster()==null){
                        Variable amidstParent = dynamicVariables.getVariableByName(huginParent.getName());
                        dynamicDAG.getParentSetTimeT(amidstChild).addParent(amidstParent);
                    }
                    else {
                        Variable amidstClone = dynamicVariables.getInterfaceVariable(amidstChild);
                        dynamicDAG.getParentSetTimeT(amidstChild).addParent(amidstClone);
                    }
                }
            }
        }
        this.amidstDBN = new DynamicBayesianNetwork(dynamicDAG);
    }

    /**
     * Sets the distributions for all the variables in the AMIDST model from the distributions in the Hugin model.
     * Both distributions at time t and time 0 are converted for each variable.
     * @throws ExceptionHugin
     */
    private void setDistributions() throws ExceptionHugin {

        List<Variable> amidstVars = amidstDBN.getDynamicVariables().getListOfDynamicVariables();

        for (Variable amidstVar : amidstVars) {

            Node huginVar = this.huginDBN.getNodeByName(amidstVar.getName());
            Node huginTemporalClone = huginVar.getTemporalClone();

            //************************************ TIME T *****************************************************
            double[] huginProbabilitiesTimeT = huginVar.getTable().getData();
            List<Variable> parentsTimeT = amidstDBN.getDynamicDAG().getParentSetTimeT(amidstVar).getParents();
            if (parentsTimeT.size()==0){
                Multinomial dist_TimeT = amidstDBN.getConditionalDistributionTimeT(amidstVar);
                int numStates = amidstVar.getNumberOfStates();
                double[] amidstProbabilities = new double[numStates];
                for (int k = 0; k < numStates; k++) {
                    amidstProbabilities[k] = huginProbabilitiesTimeT[k];
                }
                dist_TimeT.setProbabilities(amidstProbabilities);
            }else {
                Multinomial_MultinomialParents dist_TimeT = amidstDBN.getConditionalDistributionTimeT(amidstVar);
                int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(parentsTimeT);
                int numStates = amidstVar.getNumberOfStates();
                for (int i = 0; i < numParentAssignments; i++) {
                    double[] amidstProbabilities = new double[numStates];
                    for (int k = 0; k < numStates; k++) {
                        amidstProbabilities[k] = huginProbabilitiesTimeT[i * numStates + k];
                    }
                    dist_TimeT.getMultinomial(i).setProbabilities(amidstProbabilities);
                }
            }
            //************************************ TIME 0 *****************************************************
            double[] huginProbabilitiesTime0 = huginTemporalClone.getTable().getData();
            List<Variable> parentsTime0 = amidstDBN.getDynamicDAG().getParentSetTime0(amidstVar).getParents();
            if (parentsTime0.size()==0) {
                Multinomial dist_Time0 = amidstDBN.getConditionalDistributionTime0(amidstVar);
                int numStates = amidstVar.getNumberOfStates();
                double[] amidstProbabilities = new double[numStates];
                    for (int k = 0; k < numStates; k++) {
                        amidstProbabilities[k] = huginProbabilitiesTime0[k];
                    }
                    dist_Time0.setProbabilities(amidstProbabilities);
            } else {
                Multinomial_MultinomialParents dist_Time0 = amidstDBN.getConditionalDistributionTime0(amidstVar);
                int numParentAssignmentsTime0 = MultinomialIndex.getNumberOfPossibleAssignments(parentsTime0);
                int numStates = amidstVar.getNumberOfStates();
                for (int i = 0; i < numParentAssignmentsTime0; i++) {
                    double[] amidstProbabilities = new double[numStates];
                    for (int k = 0; k < numStates; k++) {
                        amidstProbabilities[k] = huginProbabilitiesTime0[i * numStates + k];
                    }
                    dist_Time0.getMultinomial(i).setProbabilities(amidstProbabilities);
                }
            }
        }
    }

    /**
     * Converts a Dynamic Bayesian network from Hugin to AMIDST format.
     * @param huginDBN the Hugin Dynamic Bayesian network to be converted.
     * @return the converted AMIDST Dynamic Bayesian network.
     * @throws ExceptionHugin
     */
    public static DynamicBayesianNetwork convertToAmidst(Class huginDBN) throws ExceptionHugin {

        DBNConverterToAmidst DBNconverterToAMIDST = new DBNConverterToAmidst(huginDBN);
        DBNconverterToAMIDST.setNodesAndParents();
        DBNconverterToAMIDST.setDistributions();
        return DBNconverterToAMIDST.amidstDBN;
    }
}
