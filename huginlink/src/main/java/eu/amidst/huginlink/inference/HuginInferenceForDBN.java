package eu.amidst.huginlink.inference;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import COM.hugin.HAPI.Node;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.inference.InferenceAlgorithmForDBN;
import eu.amidst.core.inference.InferenceEngineForDBN;
import eu.amidst.core.inference.VMP_.*;
import eu.amidst.core.io.DynamicBayesianNetworkLoader;
import eu.amidst.core.io.DynamicBayesianNetworkWriter;
import eu.amidst.core.io.DynamicDataStreamLoader;
import eu.amidst.core.learning.DynamicNaiveBayesClassifier;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.DynamicAssignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.converters.DBNConverterToHugin;
import eu.amidst.huginlink.io.BNWriterToHugin;
import eu.amidst.huginlink.io.DBNWriterToHugin;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Created by afa on 23/2/15.
 */
public class HuginInferenceForDBN implements InferenceAlgorithmForDBN {

    public DynamicBayesianNetwork amidstDBN;
    public Class huginDBN;
    public Domain domainObject;
    //For now we assume a time window equals to 1
    public static final int timeWindow = 1;
    private DynamicAssignment assignment = new HashMapAssignment(0);

    int timeID;
    int sequenceID;

    public HuginInferenceForDBN() {
        this.timeID=-1;
    }

    @Override
    public DynamicBayesianNetwork getModel() {
        return this.amidstDBN;
    }

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

    @Override
    public void addDynamicEvidence(DynamicAssignment assignment_) {


        if (this.sequenceID != -1 && this.sequenceID != assignment_.getSequenceID())
            throw new IllegalArgumentException("The sequence ID does not match. If you want to change the sequence, invoke reset method");

        if (this.timeID >= assignment_.getTimeID())
            throw new IllegalArgumentException("The provided assignment is not posterior to the previous provided assignment.");

        this.assignment = assignment_;

    }
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


    public void setAssignmentToHuginModel(DynamicAssignment assignment, int time) {
        List<Variable> dynamicVariables = amidstDBN.getDynamicVariables().getListOfDynamicVariables();
        for (Variable var : dynamicVariables) {
            //Skip non-observed variables
            if(!Double.isNaN(assignment.getValue(var))){
                if (var.isMultinomial()) {
                    try {
                        LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T" + time + "." + var.getName());
                        node.selectState((long) assignment.getValue(var));
                    } catch (ExceptionHugin exceptionHugin) {
                        exceptionHugin.printStackTrace();
                    }
                }
            }
        }
    }

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

    @Override
    public int getTimeIDOfLastEvidence() {
        return this.assignment.getTimeID();
    }

    @Override
    public int getTimeIDOfPosterior() {
        return this.timeID;
    }
}
