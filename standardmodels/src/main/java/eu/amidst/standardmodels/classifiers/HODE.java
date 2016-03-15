package eu.amidst.standardmodels.classifiers;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.standardmodels.exceptions.WrongConfigurationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class implements the HODE classification model (extended NB with a multinomial hidden as superparent).
 *
 * For more details:
 *
 * See M. Julia Flores, José A. Gámez, Ana M. Martínez, Jose Miguel Puerta: HODE: Hidden One-Dependence Estimator.
 * ECSQARU 2009: 481-492.
 *
 * Created by ana@cs.aau.dk on 05/03/16.
 */
public class HODE extends Classifier {

    private int numStates = 2;

    public int getNumStates() {
        return numStates;
    }

    public void setNumStates(int numStates) {
        this.numStates = numStates;
    }

    /**
     * Constructor of a classifier which is initialized with the default arguments:
     * the last variable in attributes is the class variable and importance sampling
     * is the inference algorithm for making the predictions.
     *
     * @param attributes list of attributes of the classifier (i.e. its variables)
     * @throws WrongConfigurationException is thrown when the attributes passed are not suitable
     *                                     for such classifier
     */
    public HODE(Attributes attributes) throws WrongConfigurationException {
        super(attributes);

        this.setLearningAlgorithm(new ParallelMaximumLikelihood());
    }

    @Override
    protected void buildDAG() {
        Variable superParentVar = vars.newMultionomialVariable("superParentVar",getNumStates());
        dag = new DAG(vars);
        dag.getParentSets()
                .stream()
                .filter(w -> !w.getMainVar().equals(classVar))
                .filter(w -> !w.getMainVar().equals(superParentVar))
                .forEach(w -> {
                    w.addParent(classVar);
                    w.addParent(superParentVar);
                });
        dag.getParentSet(superParentVar).addParent(classVar);
    }

    @Override
    public boolean isValidConfiguration() {
        return true;
    }

    public static void main(String[] args) throws WrongConfigurationException {

        DataStream<DataInstance> data = DataSetGenerator.generate(1, 1000, 5, 6);

        String classVarName = "DiscreteVar0";

        HODE hode = new HODE(data.getAttributes());
        hode.setClassName(classVarName);

        if(hode.isValidConfiguration()) {
            hode.learnModel(data);
            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {

                hode.updateModel(batch);
            }
            System.out.println(hode.getDAG());
            System.out.println(hode.getModel());
        }

        // predict the class of one instances
        System.out.println("Predicts some instances, i.e. computes the posterior probability of the class");
        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,100);

        double hits = 0;

        for(DataInstance d : dataTest) {

            double realValue = d.getValue(hode.getClassVar());
            double predValue;

            d.setValue(hode.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = hode.predict(d);


            double[] values = posteriorProb.getProbabilities();
            if (values[0]>values[1]) {
                predValue = 0;
            }else {
                predValue = 1;

            }

            if(realValue == predValue) hits++;

            System.out.println("realValue = "+realValue+", predicted ="+predValue);

        }

        System.out.println("hits="+hits);
    }
}
