package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ana@cs.aau.dk on 04/03/16.
 */
public class HiddenMarkovModel extends DynamicModel{

    private int numStates = 2;
    private boolean diagonal = true;

    public int getNumStates() {
        return numStates;
    }

    public void setNumStates(int numStates) {
        this.numStates = numStates;
    }

    public boolean isDiagonal() {
        return diagonal;
    }

    public void setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
    }

    public HiddenMarkovModel(Attributes attributes) {
        super(attributes);
    }

    @Override
    protected void buildDAG(Attributes attributes) {

        DynamicVariables vars = new DynamicVariables(attributes);
        Variable discreteHiddenVar = vars.newMultinomialDynamicVariable("discreteHiddenVar", getNumStates());
        dynamicDAG = new DynamicDAG(vars);
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != discreteHiddenVar)
                .forEach(w -> w.addParent(discreteHiddenVar));

        dynamicDAG.getParentSetTimeT(discreteHiddenVar).addParent(discreteHiddenVar.getInterfaceVariable());

        /*
         * Learn full covariance matrix
         */
        if(!isDiagonal()) {
            List<Variable> attrVars = vars.getListOfDynamicVariables()
                    .stream()
                    .filter(v -> !v.equals(discreteHiddenVar))
                    .peek(v-> {
                        if(v.isMultinomial())
                            throw new UnsupportedOperationException("Full covariance matrix cannot be used with" +
                                    " multinomial attributes");
                    })
                    .collect(Collectors.toList());

            for (int i=0; i<attrVars.size()-1; i++){
                for(int j=i+1; j<attrVars.size(); j++) {
                    dynamicDAG.getParentSetTime0(attrVars.get(i)).addParent(attrVars.get(j));
                    dynamicDAG.getParentSetTimeT(attrVars.get(i)).addParent(attrVars.get(j));
                }

            }
        }

    }


    @Override
    public void isValidConfiguration(){
    }

    public static void main(String[] args) {


        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader
                .loadFromFile("datasets/syntheticDataVerdandeScenario3.arff");

        System.out.println("------------------HMM (diagonal matrix) from streaming------------------");
        HiddenMarkovModel HMM = new HiddenMarkovModel(data.getAttributes());
        System.out.println(HMM.getDynamicDAG());
        HMM.learnModel(data);
        System.out.println(HMM.getModel());

        System.out.println("------------------HMM (full cov. matrix) from streaming------------------");
        HMM = new HiddenMarkovModel(data.getAttributes());
        HMM.setDiagonal(false);
        System.out.println(HMM.getDynamicDAG());
        HMM.learnModel(data);
        System.out.println(HMM.getModel());

        System.out.println("------------------HMM (diagonal matrix) from batches------------------");
        HMM = new HiddenMarkovModel(data.getAttributes());
        System.out.println(HMM.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : data.iterableOverBatches(100)) {
            HMM.updateModel(batch);
        }
        System.out.println(HMM.getModel());

    }


}

