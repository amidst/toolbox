package eu.amidst.huginlink.inference;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.InferenceAlgorithmForDBN;
import eu.amidst.core.inference.InferenceEngineForDBN;
import eu.amidst.core.io.DynamicBayesianNetworkLoader;
import eu.amidst.core.io.DynamicDataStreamLoader;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.DynamicAssignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.huginlink.converters.DBNConverterToHugin;


import java.io.IOException;
import java.util.List;

/**
 * Created by afa on 23/2/15.
 */
public class HuginInferenceForDBN implements InferenceAlgorithmForDBN {

    public DynamicBayesianNetwork amidstDBN;
    public Class huginDBN;
    public Domain domainObject;
    //For now we assume a time window equals to 1
    private final int timeWindow = 1;
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
            if (var.getVarID() != 10) {
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
//        //Can I remove this?
//        try {
//            this.domainObject.retractFindings();
//        } catch (ExceptionHugin exceptionHugin) {
//            exceptionHugin.printStackTrace();
//        }
        //System.out.println("ENTERING DYNAMIC EVIDENCE: [ ");
        this.timeID = -1;
        this.sequenceID=-1;
    }

    @Override
    public <E extends UnivariateDistribution> E getFilteredPosterior(Variable var) {
        UnivariateDistribution posteriorDistribution = null;

        String targetVariableName = (getTimeIDOfPosterior()==0)? "T0." : "T"+this.timeWindow+".";
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

            LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T1."+ var.getName());


            //Only works for discrete nodes in Hugin!!!
            this.domainObject.computeDBNPredictions(nTimesAhead+1);


            posteriorDistribution = var.newUnivariateDistribution();
            double[] probabilities = new double[(int) node.getNumberOfStates()];
            for (int i = 0; i < node.getNumberOfStates(); i++) {
                probabilities[i] = node.getPredictedBelief(i,nTimesAhead);
            }
            ((Multinomial) posteriorDistribution).setProbabilities(probabilities);


        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }

        //Mirar Hugin API computeDBNPredictions, getBeliefAhead etc

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

    public static void main(String[] args) throws IOException, ClassNotFoundException, ExceptionHugin {

        //**************************************************************************************************************
        // LEARN A DYNAMIC BAYESIAN NETWORK
        //**************************************************************************************************************
/*
        String file = "./datasets/bank_data_train.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(file);

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 3);//We set -3 to account for time id and seq_id
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork amidstDBN = model.getDynamicBNModel();
        DynamicBayesianNetworkWriter.saveToFile(amidstDBN,"networks/CajamarDBN.dbn");
*/

        DynamicBayesianNetwork amidstDBN = DynamicBayesianNetworkLoader.loadFromFile("networks/CajamarDBN.dbn");


        //**************************************************************************************************************
        // DATA TO PREDICT
        //**************************************************************************************************************
        String filePredict = "./datasets/bank_data_predict.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(filePredict);


        //**************************************************************************************************************
        // INFERENCE
        //**************************************************************************************************************
        HuginInferenceForDBN huginInferenceForDBN = new HuginInferenceForDBN();
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(huginInferenceForDBN);
        InferenceEngineForDBN.setModel(amidstDBN);


        // Save the compiled model in Hugin (note that this model is considered a static BN)
        //Domain domainObject = huginInferenceForDBN.getDomainObject();
        //BNWriterToHugin.saveToHuginFile(BNConverterToAMIDST.convertToAmidst(domainObject),"networks/CajamarDomainTimeWindow"+HuginInferenceForDBN.timeWindow +".net");

        UnivariateDistribution dist = null;
        UnivariateDistribution distAhead = null;

        Variable defaultVar = amidstDBN.getDynamicVariables().getVariableByName("DEFAULT");

        for (DynamicDataInstance instance : data) {


            if (instance.getTimeID()==0 && dist != null) {
                System.out.println(dist.toString());
                //System.out.println(distAhead.toString());
                InferenceEngineForDBN.reset();
            }
            System.out.print("TIME_ID: " + instance.getTimeID() + " - SEQUENCE_ID: " + instance.getSequenceID() + "\n");

            instance.setValue(defaultVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            dist = InferenceEngineForDBN.getFilteredPosterior(defaultVar);
           // distAhead = InferenceEngineForDBN.getPredictivePosterior(defaultVar,1);

        }
    }
}
