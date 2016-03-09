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
 * Created by ana@cs.aau.dk on 08/03/16.
 */
public class FactorialHMM extends DynamicModel {

    private int numHidden = 2;
    private boolean diagonal = true;

    public int getNumHidden() {
        return numHidden;
    }

    public void setNumHidden(int numHidden) {
        this.numHidden = numHidden;
    }

    public boolean isDiagonal() {
        return diagonal;
    }

    public void setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
    }

    public FactorialHMM(Attributes attributes) {
        super(attributes);
    }

    @Override
    protected void buildDAG() {

        List<Variable> binaryHiddenVars = new ArrayList<>();

        IntStream.range(0, getNumHidden()).forEach(i -> {
            Variable binaryHiddenVar = this.variables.newMultinomialDynamicVariable("binaryHiddenVar" + i,2);
            binaryHiddenVars.add(binaryHiddenVar);
        });

        dynamicDAG = new DynamicDAG(this.variables);
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> !binaryHiddenVars.contains(w.getMainVar()))
                .forEach(y -> {
                    binaryHiddenVars.stream()
                            .forEach(h -> y.addParent(h));
                });

        for (Variable gaussianHiddenVar : binaryHiddenVars) {
            dynamicDAG.getParentSetTimeT(gaussianHiddenVar).addParent(gaussianHiddenVar.getInterfaceVariable());
        }

        /*
         * Learn full covariance matrix
         */
        if(!isDiagonal()) {
            List<Variable> observedVars = this.variables.getListOfDynamicVariables()
                    .stream()
                    .filter(w -> !binaryHiddenVars.contains(w))
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

        DataStream<DynamicDataInstance> dataHybrid= DataSetGenerator.generate(1,1000,3,10);
        DataStream<DynamicDataInstance> dataGaussians = DataSetGenerator.generate(1,1000,0,10);
        //DataStream<DynamicDataInstance> data = DynamicDataStreamLoader
        //        .loadFromFile("datasets/syntheticDataVerdandeScenario3.arff");

        System.out.println("------------------Factorial HMM (diagonal matrix) from streaming------------------");
        FactorialHMM factorialHMM = new FactorialHMM(dataHybrid.getAttributes());
        System.out.println(factorialHMM.getDynamicDAG());
        factorialHMM.learnModel(dataHybrid);
        System.out.println(factorialHMM.getModel());

        System.out.println("------------------Factorial HMM (full cov. matrix) from streaming------------------");
        factorialHMM = new FactorialHMM(dataGaussians.getAttributes());
        factorialHMM.setDiagonal(false);
        System.out.println(factorialHMM.getDynamicDAG());
        factorialHMM.learnModel(dataGaussians);
        System.out.println(factorialHMM.getModel());

        System.out.println("------------------Factorial HMM (diagonal matrix) from batches------------------");
        factorialHMM = new FactorialHMM(dataHybrid.getAttributes());
        System.out.println(factorialHMM.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataHybrid.iterableOverBatches(100)) {
            factorialHMM.updateModel(batch);
        }
        System.out.println(factorialHMM.getModel());

    }

}
