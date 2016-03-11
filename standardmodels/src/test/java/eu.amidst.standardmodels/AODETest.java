package eu.amidst.standardmodels;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.standardmodels.classifiers.AODE;
import eu.amidst.standardmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ana@cs.aau.dk on 11/03/16.
 */
public class AODETest extends TestCase {
    protected AODE aode;
    DataStream<DataInstance> data;

    protected void setUp() throws WrongConfigurationException {
        data = DataSetGenerator.generate(1234,500, 5, 0);

        System.out.println(data.getAttributes().toString());

        String classVarName = "DiscreteVar0";

        aode = new AODE(data.getAttributes());
        aode.setClassName(classVarName);

        if(aode.isValidConfiguration()) {
            aode.learnModel(data);
            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {

                aode.updateModel(batch);
            }
            System.out.println(aode.getModel());
            System.out.println(aode.getDAG());
        }


    }


    //////// test methods

    public void testClassVariable() {
        boolean passedTest = true;

        Variable classVar = aode.getClassVar();

        // class variable is a multinomial
        boolean isMultinomial = classVar.isMultinomial();

        //has not parents
        boolean noParents = aode.getDAG().getParentSet(classVar).getParents().isEmpty();

        assertTrue(isMultinomial && noParents);
    }





    public void testPrediction() {

        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,10);


        double hits = 0;

        for(DataInstance d : dataTest) {

            double realValue = d.getValue(aode.getClassVar());
            double predValue;

            d.setValue(aode.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = aode.predict(d);


            double[] values = posteriorProb.getProbabilities();
            if (values[0]>values[1]) {
                predValue = 0;
            }else {
                predValue = 1;

            }

            if(realValue == predValue) hits++;


        }
        assertTrue(hits==9);


    }

}
