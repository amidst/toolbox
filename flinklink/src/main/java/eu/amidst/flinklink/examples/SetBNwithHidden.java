package eu.amidst.flinklink.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by Hanen on 09/12/15.
 */
public class SetBNwithHidden {

    public static DAG getHiddenNaiveBayesStructure(DataFlink<DataInstance> dataStream) {

        // Create a Variables object from the attributes of the input data stream.
        Variables modelHeader = new Variables(dataStream.getAttributes());

        // Define the global latent binary variable.
        Variable globalHiddenDiscrete = modelHeader.newMultionomialVariable("globalHiddenDiscrete", 2);

        // Define the global Gaussian latent binary variable.
        Variable globalHiddenGaussian = modelHeader.newGaussianVariable("globalHiddenGaussian");

        // Define the class variable.
        Variable classVariable = modelHeader.getVariableByName("ClassVar"); //getVariableById(0);


        // Create a DAG object with the defined model header.
        DAG dag = new DAG(modelHeader);

        // Define the structure of the DAG, i.e., set the links between the variables.
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVariable)
                .filter(w -> w.getMainVar() != globalHiddenDiscrete)
                .filter(w -> w.getMainVar() != globalHiddenGaussian)
                .filter(w -> w.getMainVar().isMultinomial())
                .forEach(w -> w.addParent(globalHiddenDiscrete));

        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVariable)
                .filter(w -> w.getMainVar() != globalHiddenDiscrete)
                .filter(w -> w.getMainVar() != globalHiddenGaussian)
                .filter(w -> w.getMainVar().isNormal())
                .forEach(w -> w.addParent(globalHiddenGaussian));

        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVariable)
                .forEach(w -> w.addParent(classVariable));

        // Return the DAG.
        return dag;
    }

    public static void main(String[] args) throws Exception {

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadData(env, "./data.arff");

        DAG dag = SetBNwithHidden.getHiddenNaiveBayesStructure(dataFlink);

        BayesianNetwork bnet = new BayesianNetwork(dag);

        System.out.println("\n Number of variables \n " + bnet.getDAG().getVariables().getNumberOfVars());

        System.out.println(dag.toString());

        BayesianNetworkWriter.saveToFile(bnet, "./BNHiddenExample.bn");

    }

}
