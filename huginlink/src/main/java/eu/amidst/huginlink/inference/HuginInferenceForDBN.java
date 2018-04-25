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

package eu.amidst.huginlink.inference;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.dynamic.inference.InferenceAlgorithmForDBN;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.dynamic.variables.HashMapDynamicAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.huginlink.converters.DBNConverterToHugin;

import java.util.List;

/**
 * This class provides an interface to perform inference over Dynamic Bayesian networks using the Hugin inference
 * engine.
 */
public class HuginInferenceForDBN implements InferenceAlgorithmForDBN {

    /** Represents the Dynamic Bayesian network model in AMIDST format. */
    public DynamicBayesianNetwork amidstDBN;

    /** Represents the Dynamic Bayesian network model in Hugin format. */
    public Class huginDBN;

    /** Represnts the expanded dynamic model over which the inference is performed. */
    public Domain domainObject;

    /** Represnts the number of time slices in which the model is expanded. For now, we assume a <code>timeWindow</code> equals to 1.
     */
    public static final int timeWindow = 1;

    /** Represents the AMIDST assignment to be evidenced into the Hugin model. */
    private DynamicAssignment assignment = new HashMapDynamicAssignment(0);

    /** Represents the time ID of the current assignment being processed. */
    long timeID;

    /**
     * Represents the sequence ID of the current assignment being processed.
     * For the Cajamar data set, this corresponds to the client ID.
     */
    long sequenceID;

    /**
     * Class constructor.
     */
    public HuginInferenceForDBN() {
        this.timeID=-1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DynamicBayesianNetwork getOriginalModel() {
        return this.amidstDBN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModel(DynamicBayesianNetwork model) {

        this.amidstDBN = model;

        try {
            this.huginDBN = DBNConverterToHugin.convertToHugin(model);
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }

        try {
            this.domainObject = this.huginDBN.createDBNDomain(timeWindow);
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDynamicEvidence(DynamicAssignment assignment_) {


        if (this.sequenceID != -1 && this.sequenceID != assignment_.getSequenceID())
            throw new IllegalArgumentException("The sequence ID does not match. If you want to change the sequence, invoke reset method");

        if (this.timeID >= assignment_.getTimeID())
            throw new IllegalArgumentException("The provided assignment is not posterior to the previous provided assignment.");

        this.assignment = assignment_;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runInference() {
        try {
            domainObject.uncompile();
            if (assignment.getTimeID()==0) {
                this.setAssignmentToHuginModel(this.assignment, 0);
                this.timeID = 0;
            } else{
                this.timeID = this.getTimeIDOfLastEvidence();
                this.setAssignmentToHuginModel(this.assignment, 1);
            }

            domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
            domainObject.compile();

        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
    }

    /**
     * Sets the AMIDST evidence into a given time slice of the expanded Hugin model.
     * @param assignment the evidence to be propagated.
     * @param time the time slice in which the evidence is entered.
     */
    public void setAssignmentToHuginModel(DynamicAssignment assignment, int time) {
        List<Variable> dynamicVariables = amidstDBN.getDynamicVariables().getListOfDynamicVariables();
        for (Variable var : dynamicVariables) {
            //Skip non-observed variables
            if(!Double.isNaN(assignment.getValue(var))){
                if (var.isMultinomial()) {
                    try {
                        LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T" + time + "." + var.getName());
                        node.selectState((int)assignment.getValue(var));
                    } catch (ExceptionHugin exceptionHugin) {
                        exceptionHugin.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {

        // Retract evidence for all nodes of domain, move the time window back to its initial position,
        // and establish the initial state of the inference engine.
        //--------------------------------------------------------------------------------------------------------------
        try {
            this.domainObject.initializeDBNWindow();
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
        //--------------------------------------------------------------------------------------------------------------

        this.timeID = -1;
        this.sequenceID=-1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends UnivariateDistribution> E getFilteredPosterior(Variable var) {
        UnivariateDistribution posteriorDistribution = null;

        String targetVariableName = (this.timeID==0)? "T0." : "T"+this.timeWindow+".";
        try {
            LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName(targetVariableName + var.getName());
            posteriorDistribution = var.newUnivariateDistribution();

            double[] probabilities = new double[(int) node.getNumberOfStates()];
            for (int i = 0; i < node.getNumberOfStates(); i++) {
                probabilities[i] = node.getBelief(i);
            }
            ((Multinomial) posteriorDistribution).setProbabilities(probabilities);
            domainObject.moveDBNWindow(1);

        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
        return ((E) posteriorDistribution);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead) {

        UnivariateDistribution posteriorDistribution = null;

        try {
            LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T"+this.timeWindow+"." + var.getName());
            this.domainObject.computeDBNPredictions(nTimesAhead);
            posteriorDistribution = var.newUnivariateDistribution();

            double[] probabilities = new double[(int) node.getNumberOfStates()];
            for (int i = 0; i < node.getNumberOfStates(); i++) {
                probabilities[i] = node.getPredictedBelief(i,nTimesAhead-1);
            }

            ((Multinomial) posteriorDistribution).setProbabilities(probabilities);
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
        return (E)posteriorDistribution;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeIDOfLastEvidence() {
        return this.assignment.getTimeID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeIDOfPosterior() {
        return this.timeID;
    }
}
