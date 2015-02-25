package eu.amidst.huginlink.inference;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.distribution.DistributionBuilder;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.InferenceAlgorithmForDBN;
import eu.amidst.core.inference.InferenceEngineForDBN;
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
import java.util.List;

/**
 * Created by afa on 23/2/15.
 */
public class HuginInferenceForDBN implements InferenceAlgorithmForDBN {

    public DynamicBayesianNetwork amidstDBN;
    public Class huginDBN;
    public Domain domainObject;
    //For now we assume a time window equals to 1
    private static final int timeWindow = 1;
    private DynamicAssignment assignment = new HashMapAssignment(0);

    @Override
    public void runInference() {
        try {
            domainObject.uncompile();
            domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
            domainObject.compile();
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
    }


    public DynamicAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(DynamicAssignment assignment_) {
        this.assignment = assignment_;
    }

    public Domain getDomainObject() {
        return domainObject;
    }

    public void setDomainObject(Domain domainObject) {
        this.domainObject = domainObject;
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
    public DynamicBayesianNetwork getModel() {
        return this.amidstDBN;
    }

    @Override
    public void addDynamicEvidence(DynamicAssignment assignment_) {

        //Can I remove this?
        try {
            this.domainObject.retractFindings();
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
        //System.out.println("ENTERING DYNAMIC EVIDENCE: [ ");
        this.assignment = assignment_;

        List<Variable> dynamicVariables = amidstDBN.getDynamicVariables().getListOfDynamicVariables();

        for (Variable var : dynamicVariables) {
            if (var.getVarID() != 10) {
                if (var.isMultinomial()) {
                    //System.out.print("\n    " +var.getName() + ": " + assignment.getValue(var));
                    try {
                        LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T1." + var.getName());
                        node.selectState((long) assignment.getValue(var));
                    } catch (ExceptionHugin exceptionHugin) {
                        exceptionHugin.printStackTrace();
                    }
                } else if (var.isGaussian()) {
                    System.out.println("IMPLEMENT!!!!");
                }
            }
        }
    }

    @Override
    public void reset() {

    }

    @Override
    public <E extends UnivariateDistribution> E getFilteredPosterior(Variable var) {
        UnivariateDistribution posteriorDistribution = null;

        try {
            domainObject.computeDBNPredictions(1);
            LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T1." + var.getName());
            posteriorDistribution = DistributionBuilder.newDistribution(var, new ArrayList<>()).getUnivariateDistribution(null);

            double[] probabilities = new double[(int) node.getNumberOfStates()];
            for (int i = 0; i < node.getNumberOfStates(); i++) {
                probabilities[i] = node.getBelief(i);
            }
            ((Multinomial) posteriorDistribution).setProbabilities(probabilities);
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
        return ((E) posteriorDistribution);
    }

    @Override
    public <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead) {
        UnivariateDistribution posteriorDistribution = DistributionBuilder.newDistribution(var, new ArrayList<>()).getUnivariateDistribution(null);
        try {
            domainObject.moveDBNWindow(nTimesAhead);

            LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T" + nTimesAhead + "." + var.getName());
            posteriorDistribution = DistributionBuilder.newDistribution(var, new ArrayList<>()).getUnivariateDistribution(null);

            double[] probabilities = new double[(int) node.getNumberOfStates()];
            for (int i = 0; i < node.getNumberOfStates(); i++) {
                probabilities[i] = node.getBelief(i);
            }
            ((Multinomial) posteriorDistribution).setProbabilities(probabilities);

        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
        return (E) posteriorDistribution;
    }

    @Override
    public int getTimeIDOfLastEvidence() {
        return 0;
    }

    @Override
    public int getTimeIDOfPosterior() {
        return 0;
    }


    public static void main(String[] args) throws IOException, ClassNotFoundException, ExceptionHugin {

        //**************************************************************************************************************
        // LEARN A DYNAMIC BAYESIAN NETWORK
        //**************************************************************************************************************

       /* String file = "./datasets/bank_data_train.arff";
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
       // UnivariateDistribution distAhead = null;
        Variable defaultVar = amidstDBN.getDynamicVariables().getVariableByName("DEFAULT");

        for (DynamicDataInstance instance : data) {

           if (instance.getTimeID()==0 && dist != null) {
              // System.out.print("\nTIME_ID: " + instance.getTimeID() + " SEQUENCE_ID: " + instance.getSequenceID() + "\n");
               System.out.println(dist.toString());
               //System.out.println(distAhead.toString());
               InferenceEngineForDBN.reset();
           }
            instance.setValue(defaultVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            dist = InferenceEngineForDBN.getFilteredPosterior(defaultVar);
            //System.out.println(dist.toString());
           //distAhead = InferenceEngineForDBN.getPredictivePosterior(defaultVar,1);
            //System.out.println(distAhead.toString());

        }
    }
}
