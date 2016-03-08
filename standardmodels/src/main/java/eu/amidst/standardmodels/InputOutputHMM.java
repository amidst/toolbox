package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicDAG;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 08/03/16.
 */
public class InputOutputHMM  extends DynamicModel {

    private boolean diagonal = true;
    private int numStates = 2;

    private List<Attribute> inputAtts;
    private List<Attribute> outputAtts;

    public boolean isDiagonal() {
        return diagonal;
    }

    public void setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
    }

    public int getNumStates() {
        return numStates;
    }

    public void setNumStates(int numStates) {
        this.numStates = numStates;
    }

    public InputOutputHMM(Attributes attributes, List<Attribute> inputs,List<Attribute> outputs ) {
        super(attributes);
        this.inputAtts = inputs;
        this.outputAtts = outputs;
    }

    @Override
    protected void buildDAG() {

        List<Variable> inputVars = this.variables.getVariablesForListOfAttributes(inputAtts);
        List<Variable> outputVars = this.variables.getVariablesForListOfAttributes(outputAtts);
        Variable discreteHiddenVar = this.variables.newMultinomialDynamicVariable("discreteHiddenVar", getNumStates());


        dynamicDAG = new DynamicDAG(this.variables);

        for (Variable inputVar : inputVars) {
            if (inputVar.isNormal())
                throw new UnsupportedOperationException("Invalid configuration: all the input variables must be " +
                        "discrete");
            dynamicDAG.getParentSetTimeT(discreteHiddenVar).addParent(inputVar);
            for (Variable outputVar : outputVars) {
                dynamicDAG.getParentSetTimeT(outputVar).addParent(inputVar);
            }
        }

        for (Variable outputVar : outputVars) {
            dynamicDAG.getParentSetTimeT(outputVar).addParent(discreteHiddenVar);
        }

        System.out.println(dynamicDAG);
        dynamicDAG.getParentSetTimeT(discreteHiddenVar).addParent(discreteHiddenVar.getInterfaceVariable());


        /*
         * Learn full covariance matrix
         */
        if(!isDiagonal()) {

            for (int i=0; i<inputVars.size()-1; i++){
                for(int j=i+1; j<inputVars.size(); j++) {
                    dynamicDAG.getParentSetTime0(inputVars.get(i)).addParent(inputVars.get(j));
                    dynamicDAG.getParentSetTimeT(inputVars.get(i)).addParent(inputVars.get(j));
                }
            }

            for (int i=0; i<outputVars.size()-1; i++){
                for(int j=i+1; j<outputVars.size(); j++) {
                    dynamicDAG.getParentSetTime0(outputVars.get(i)).addParent(outputVars.get(j));
                    dynamicDAG.getParentSetTimeT(outputVars.get(i)).addParent(outputVars.get(j));
                }
            }
        }
    }

    @Override
    public void isValidConfiguration() {
    }

    public static void main(String[] args) {


        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader
                .loadFromFile("datasets/WasteIncineratorSample.arff");

        Attributes dataAttributes = data.getAttributes();

        List<Attribute> inputAtts = new ArrayList<>();
        inputAtts.add(dataAttributes.getAttributeByName("B"));
        inputAtts.add(dataAttributes.getAttributeByName("F"));
        inputAtts.add(dataAttributes.getAttributeByName("W"));
        List<Attribute> outputAtts = new ArrayList<>();
        outputAtts.add(dataAttributes.getAttributeByName("Mout"));
        outputAtts.add(dataAttributes.getAttributeByName("C"));
        outputAtts.add(dataAttributes.getAttributeByName("L"));

        System.out.println("------------------Input-Output HMM (diagonal matrix) from streaming------------------");
        InputOutputHMM IOHMM = new InputOutputHMM(dataAttributes,inputAtts,outputAtts);
        IOHMM.setNumStates(2);
        System.out.println(IOHMM.getDynamicDAG());
        IOHMM.learnModel(data);
        System.out.println(IOHMM.getModel());

        System.out.println("------------------Input-Output HMM (full cov. matrix) from streaming------------------");
        IOHMM = new InputOutputHMM(dataAttributes,inputAtts,outputAtts);
        IOHMM.setDiagonal(false);
        System.out.println(IOHMM.getDynamicDAG());
        IOHMM.learnModel(data);
        System.out.println(IOHMM.getModel());

        System.out.println("------------------Input-Output HMM (diagonal matrix) from batches------------------");
        IOHMM = new InputOutputHMM(dataAttributes,inputAtts,outputAtts);
        System.out.println(IOHMM.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : data.iterableOverBatches(100)) {
            IOHMM.updateModel(batch);
        }
        System.out.println(IOHMM.getModel());

    }
}
