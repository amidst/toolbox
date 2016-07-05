package eu.amidst.core.variables.distributionTypes;

import eu.amidst.core.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import org.junit.Test;

/**
 * Created by ana@cs.aau.dk on 26/04/16.
 */
public class IndicatorTypeTest {

    @Test
    public void testingIndicatorVariables() {

        /**
         *  ORIGINAL BN - Bayesian network WITHOUT indicator variables.
         */

        if (Main.VERBOSE) System.out.println("----------------------------------------------------------");
        if (Main.VERBOSE) System.out.println("ORIGINAL BN - Bayesian network WITHOUT indicator variables");
        if (Main.VERBOSE) System.out.println("----------------------------------------------------------");

        DataStream<DataInstance> data = DataStreamLoader.open("../datasets/dataWeka/labor.arff");
        Variables variables = new Variables();
        Variable durationVar =  variables.newVariable(data.getAttributes().getAttributeByName("duration"));
        Variable wageIncreaseFirstYearVar = variables.newVariable(data.getAttributes().getAttributeByName("wage-increase-first-year"));
        Variable pensionVar = variables.newVariable(data.getAttributes().getAttributeByName("pension"));

        DAG dag = new DAG(variables);

        SVB svb = new SVB();
        svb.setWindowsSize(100);
        svb.setSeed(0);

        svb.setDAG(dag);
        svb.setDataStream(data);
        svb.runLearning();

        BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(learnBN.toString());


        /**
         *  Bayesian network WITH indicator variables.
         */

        if (Main.VERBOSE) System.out.println("------------------------------------------");
        if (Main.VERBOSE) System.out.println("Bayesian network WITH indicator variables.");
        if (Main.VERBOSE) System.out.println("------------------------------------------");

        Variable duration_IndVar = variables.newIndicatorVariable(durationVar,1.0);
        Variable wageIncreaseFirstYear_IndVar = variables.newIndicatorVariable(wageIncreaseFirstYearVar,Double.NaN);
        Variable pension_IndVar = variables.newIndicatorVariable(pensionVar,"'none'");

        dag = new DAG(variables);

        dag.getParentSet(durationVar).addParent(duration_IndVar);
        dag.getParentSet(wageIncreaseFirstYearVar).addParent(wageIncreaseFirstYear_IndVar);
        dag.getParentSet(pensionVar).addParent(pension_IndVar);

        svb = new SVB();
        svb.setWindowsSize(100);
        svb.setSeed(0);

        svb.setDAG(dag);
        svb.setDataStream(data);
        svb.runLearning();

        learnBN = svb.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(learnBN.toString());



    }
}
