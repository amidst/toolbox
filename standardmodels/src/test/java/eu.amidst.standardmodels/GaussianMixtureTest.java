package eu.amidst.standardmodels;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.standardmodels.classifiers.GaussianDiscriminantAnalysis;
import eu.amidst.standardmodels.eu.amidst.standardmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rcabanas on 10/03/16.
 */
public class GaussianMixtureTest extends TestCase {

    protected GaussianMixture gmm;
    DataStream<DataInstance> data;

    protected void setUp() throws WrongConfigurationException {


        DataStream<DataInstance> data = DataSetGenerator.generate(1234,500, 0, 4);



        gmm = new GaussianMixture(data.getAttributes());
        gmm.setDiagonal(false);
        gmm.setNumStatesHiddenVar(3);

        gmm.learnModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
            gmm.updateModel(batch);
        }


    }


    //////// test methods

    public void testHiddenVar() {
        boolean passedTest = true;

        Variable classVar = gmm.getHiddenVar();

        // class variable is a multinomial
        boolean isMultinomial = classVar.isMultinomial();

        //has not parents
        boolean noParents = gmm.getDAG().getParentSet(classVar).getParents().isEmpty();

        //all the attributes are their children
        boolean allAttrChildren = gmm.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> gmm.getDAG().getParentSet(v).contains(classVar));

        assertTrue(isMultinomial && noParents && allAttrChildren);
    }



    public void testAttributes(){
        Variable classVar = gmm.getHiddenVar();

        // the attributes have a single parent
        boolean numParents = gmm.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> gmm.getDAG().getParentSet(v).getNumberOfParents()==1);

        assertTrue(!gmm.isDiagonal() || numParents);
    }





}
