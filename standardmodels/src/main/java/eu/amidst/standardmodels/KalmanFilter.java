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
 * Created by ana@cs.aau.dk on 05/03/16.
 */
public class KalmanFilter extends DynamicModel {
    private boolean diagonal = true;

    public boolean isDiagonal() {
        return diagonal;
    }

    public void setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
    }

    public KalmanFilter(Attributes attributes) {
        super(attributes);
    }

    @Override
    protected void buildDAG(Attributes attributes) {

        DynamicVariables vars = new DynamicVariables(attributes);
        Variable discreteHiddenVar = vars.newGaussianDynamicVariable("HiddenVar");
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

    public static void main(String[] args) {


        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader
                .loadFromFile("datasets/syntheticDataVerdandeScenario3.arff");

        System.out.println("------------------KF (diagonal matrix) from streaming------------------");
        KalmanFilter dynamicModel = new KalmanFilter(data.getAttributes());
        System.out.println(dynamicModel.getDynamicDAG());
        dynamicModel.learnModel(data);
        System.out.println(dynamicModel.getModel());

        System.out.println("------------------KF (full cov. matrix) from streaming------------------");
        dynamicModel = new KalmanFilter(data.getAttributes());
        dynamicModel.setDiagonal(false);
        System.out.println(dynamicModel.getDynamicDAG());
        dynamicModel.learnModel(data);
        System.out.println(dynamicModel.getModel());

        System.out.println("------------------KF (diagonal matrix) from batches------------------");
        dynamicModel = new KalmanFilter(data.getAttributes());
        System.out.println(dynamicModel.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : data.iterableOverBatches(100)) {
            dynamicModel.updateModel(batch);
        }
        System.out.println(dynamicModel.getModel());

    }
}
