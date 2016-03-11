package eu.amidst.standardmodels;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.standardmodels.classifiers.NaiveBayesClassifier;
import eu.amidst.standardmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rcabanas on 10/03/16.
 */
public class NaiveBayesClassifierTest extends TestCase {

    protected NaiveBayesClassifier nb;
    DataStream<DataInstance> data;

    protected void setUp() throws WrongConfigurationException {
        data = DataSetGenerator.generate(1234,500, 2, 10);

        System.out.println(data.getAttributes().toString());

        String classVarName = "DiscreteVar0";

        nb = new NaiveBayesClassifier(data.getAttributes());
        nb.setClassName(classVarName);

        if(nb.isValidConfiguration()) {
            nb.learnModel(data);
            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {

                nb.updateModel(batch);
            }
            System.out.println(nb.getModel());
            System.out.println(nb.getDAG());
        }


    }


    //////// test methods

    public void testClassVariable() {
        boolean passedTest = true;

        Variable classVar = nb.getClassVar();

        // class variable is a multinomial
        boolean isMultinomial = classVar.isMultinomial();

        //has not parents
        boolean noParents = nb.getDAG().getParentSet(classVar).getParents().isEmpty();

        //all the attributes are their children
        boolean allAttrChildren = nb.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> nb.getDAG().getParentSet(v).contains(classVar));

        assertTrue(isMultinomial && noParents && allAttrChildren);
    }



    public void testAttributes(){
        Variable classVar = nb.getClassVar();

        // the attributes have a single parent
        boolean numParents = nb.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> nb.getDAG().getParentSet(v).getNumberOfParents()==1);

        assertTrue(numParents);
    }



    public void testPrediction() {

        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,10);


        double hits = 0;

        for(DataInstance d : dataTest) {

            double realValue = d.getValue(nb.getClassVar());
            double predValue;

            d.setValue(nb.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = nb.predict(d);


            double[] values = posteriorProb.getProbabilities();
            if (values[0]>values[1]) {
                predValue = 0;
            }else {
                predValue = 1;

            }

            if(realValue == predValue) hits++;


        }

        assertTrue(hits==10);


    }





/*
    public void testDAG() {
        boolean passedTest = true;



        assertTrue(passedTest);
    }
*/


}
