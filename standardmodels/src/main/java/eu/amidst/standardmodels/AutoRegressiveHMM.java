package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicDAG;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ana@cs.aau.dk on 08/03/16.
 */
public class AutoRegressiveHMM extends DynamicModel  {

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

    public AutoRegressiveHMM(Attributes attributes) {
        super(attributes);
    }

    @Override
    protected void buildDAG() {

        Variable discreteHiddenVar = this.variables.newMultinomialDynamicVariable("discreteHiddenVar", getNumStates());
        dynamicDAG = new DynamicDAG(this.variables);
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != discreteHiddenVar)
                .forEach(w -> {
                    w.addParent(discreteHiddenVar);
                    w.addParent(w.getMainVar().getInterfaceVariable());
                });

        dynamicDAG.getParentSetTimeT(discreteHiddenVar).addParent(discreteHiddenVar.getInterfaceVariable());

        /*
         * Learn full covariance matrix
         */
        if(!isDiagonal()) {
            List<Variable> observedVars = this.variables.getListOfDynamicVariables()
                    .stream()
                    .filter(v -> !v.equals(discreteHiddenVar))
                    .peek(v-> {
                        if(v.isMultinomial())
                            throw new UnsupportedOperationException("Full covariance matrix cannot be used with" +
                                    " multinomial attributes");
                    })
                    .collect(Collectors.toList());

            for (int i=0; i<observedVars.size()-1; i++){
                for(int j=i+1; j<observedVars.size(); j++) {
                    dynamicDAG.getParentSetTime0(observedVars.get(i)).addParent(observedVars.get(j));
                    dynamicDAG.getParentSetTimeT(observedVars.get(i)).addParent(observedVars.get(j));
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

        System.out.println("------------------Auto-Regressive HMM (diagonal matrix) from streaming------------------");
        AutoRegressiveHMM autoRegressiveHMM = new AutoRegressiveHMM(data.getAttributes());
        System.out.println(autoRegressiveHMM.getDynamicDAG());
        autoRegressiveHMM.learnModel(data);
        System.out.println(autoRegressiveHMM.getModel());

        System.out.println("------------------Auto-Regressive HMM (full cov. matrix) from streaming------------------");
        autoRegressiveHMM = new AutoRegressiveHMM(data.getAttributes());
        autoRegressiveHMM.setDiagonal(false);
        System.out.println(autoRegressiveHMM.getDynamicDAG());
        autoRegressiveHMM.learnModel(data);
        System.out.println(autoRegressiveHMM.getModel());

        System.out.println("------------------Auto-Regressive HMM (diagonal matrix) from batches------------------");
        autoRegressiveHMM = new AutoRegressiveHMM(data.getAttributes());
        System.out.println(autoRegressiveHMM.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : data.iterableOverBatches(100)) {
            autoRegressiveHMM.updateModel(batch);
        }
        System.out.println(autoRegressiveHMM.getModel());

    }
}
