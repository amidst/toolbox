package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DataSetGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 05/03/16.
 */
public class KalmanFilter extends DynamicModel {
    private boolean diagonal = true;
    private int numHidden = 2;

    public boolean isDiagonal() {
        return diagonal;
    }

    public void setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
    }

    public int getNumHidden() {
        return numHidden;
    }

    public void setNumHidden(int numHidden) {
        this.numHidden = numHidden;
    }

    public KalmanFilter(Attributes attributes) {
        super(attributes);
    }

    @Override
    protected void buildDAG() {

        List<Variable> gaussianHiddenVars = new ArrayList<>();

        IntStream.range(0,getNumHidden()).forEach(i -> {
            Variable gaussianHiddenVar = this.variables.newGaussianDynamicVariable("gaussianHiddenVar" + i);
            gaussianHiddenVars.add(gaussianHiddenVar);
        });

        dynamicDAG = new DynamicDAG(this.variables);

        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> !gaussianHiddenVars.contains(w.getMainVar()))
                .forEach(y -> {
                    gaussianHiddenVars.stream()
                            .forEach(h -> y.addParent(h));
                });

        for (Variable gaussianHiddenVar : gaussianHiddenVars) {
            dynamicDAG.getParentSetTimeT(gaussianHiddenVar).addParent(gaussianHiddenVar.getInterfaceVariable());
        }


        for (int i=0; i<gaussianHiddenVars.size()-1; i++){
            for(int j=i+1; j<gaussianHiddenVars.size(); j++) {
                dynamicDAG.getParentSetTime0(gaussianHiddenVars.get(i)).addParent(gaussianHiddenVars.get(j));
                dynamicDAG.getParentSetTimeT(gaussianHiddenVars.get(i)).addParent(gaussianHiddenVars.get(j));
            }

        }



        /*
         * Learn full covariance matrix
         */
        if(!isDiagonal()) {
            List<Variable> observedVars = this.variables.getListOfDynamicVariables()
                    .stream()
                    .filter(w -> !gaussianHiddenVars.contains(w))
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
    public void isValidConfiguration() {
        this.variables.getListOfDynamicVariables()
                .stream()
                .forEach(var -> {
                    if (!var.isNormal())
                        throw new UnsupportedOperationException("Invalid configuration: all the variables must be real");
                });
    }

    public static void main(String[] args) {

        DataStream<DynamicDataInstance> dataGaussians = DataSetGenerator.generate(1,1000,0,10);

        //DataStream<DynamicDataInstance> data = DynamicDataStreamLoader
        //        .loadFromFile("datasets/syntheticDataVerdandeScenario3.arff");

        System.out.println("------------------KF (diagonal matrix) from streaming------------------");
        KalmanFilter KF = new KalmanFilter(dataGaussians.getAttributes());
        KF.setNumHidden(2);
        System.out.println(KF.getDynamicDAG());
        KF.learnModel(dataGaussians);
        System.out.println(KF.getModel());

        System.out.println("------------------KF (full cov. matrix) from streaming------------------");
        KF = new KalmanFilter(dataGaussians.getAttributes());
        KF.setDiagonal(false);
        System.out.println(KF.getDynamicDAG());
        KF.learnModel(dataGaussians);
        System.out.println(KF.getModel());

        System.out.println("------------------KF (diagonal matrix) from batches------------------");
        KF = new KalmanFilter(dataGaussians.getAttributes());
        System.out.println(KF.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataGaussians.iterableOverBatches(100)) {
            KF.updateModel(batch);
        }
        System.out.println(KF.getModel());

    }
}
