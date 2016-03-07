package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import org.apache.commons.lang.NotImplementedException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ana@cs.aau.dk on 07/03/16.
 */
public class SwitchingKalmanFilter  extends DynamicModel {

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

    public SwitchingKalmanFilter(Attributes attributes) {
        super(attributes);
    }

    @Override
    protected void buildDAG(Attributes attributes) {

        DynamicVariables vars = new DynamicVariables(attributes);
        Variable discreteHiddenVar = vars.newMultinomialDynamicVariable("discreteHiddenVar", getNumStates());
        Variable gaussianHiddenVar = vars.newGaussianDynamicVariable("gaussianHiddenVar");

        dynamicDAG = new DynamicDAG(vars);
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != discreteHiddenVar)
                .filter(w -> w.getMainVar() != gaussianHiddenVar)
                .forEach(w -> {
                    w.addParent(discreteHiddenVar);
                    w.addParent(gaussianHiddenVar);
                });

        dynamicDAG.getParentSetTimeT(gaussianHiddenVar).addParent(discreteHiddenVar);
        dynamicDAG.getParentSetTimeT(gaussianHiddenVar).addParent(gaussianHiddenVar.getInterfaceVariable());
        dynamicDAG.getParentSetTimeT(discreteHiddenVar).addParent(discreteHiddenVar.getInterfaceVariable());

        /*
         * Learn full covariance matrix
         */
        if(!isDiagonal()) {
            List<Variable> attrVars = vars.getListOfDynamicVariables()
                    .stream()
                    .filter(v -> !v.equals(discreteHiddenVar))
                    .filter(v -> !v.equals(gaussianHiddenVar))
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
    public boolean isValidConfiguration(){
        throw new NotImplementedException("The method isValidConfiguration() has not been implemented for the class "+this.getClass().getName());
    }


    public static void main(String[] args) {


        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader
                .loadFromFile("datasets/syntheticDataVerdandeScenario3.arff");

        System.out.println("------------------SKF (diagonal matrix) from streaming------------------");
        SwitchingKalmanFilter SKF = new SwitchingKalmanFilter(data.getAttributes());
        System.out.println(SKF.getDynamicDAG());
        SKF.learnModel(data);
        System.out.println(SKF.getModel());

        System.out.println("------------------SKF (full cov. matrix) from streaming------------------");
        SKF = new SwitchingKalmanFilter(data.getAttributes());
        SKF.setDiagonal(false);
        System.out.println(SKF.getDynamicDAG());
        SKF.learnModel(data);
        System.out.println(SKF.getModel());

        System.out.println("------------------SKF (diagonal matrix) from batches------------------");
        SKF = new SwitchingKalmanFilter(data.getAttributes());
        System.out.println(SKF.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : data.iterableOverBatches(100)) {
            SKF.updateModel(batch);
        }
        System.out.println(SKF.getModel());

    }
}
