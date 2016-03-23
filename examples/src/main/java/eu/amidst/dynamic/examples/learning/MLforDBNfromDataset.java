package eu.amidst.dynamic.examples.learning;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.learning.dynamic.DynamicMaximumLikelihood;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.IOException;

/**
 * This example shows how to learn the parameters of a dynamic Bayesian network using maximum likelihood
 * from a given data.
 *
 * Created by ana@cs.aau.dk on 01/12/15.
 */
public class MLforDBNfromDataset {

    /**
     * This method returns a DynamicDAG object with naive Bayes structure for the given attributes.
     * @param attributes
     * @param classIndex
     * @return
     */
    public static DynamicDAG getNaiveBayesStructure(Attributes attributes, int classIndex){

        //We create a Variables object from the attributes of the data stream
        DynamicVariables dynamicVariables = new DynamicVariables(attributes);

        //We define the predicitive class variable
        Variable classVar = dynamicVariables.getVariableById(classIndex);

        //Then, we create a DAG object with the defined model header
        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        //We set the links of the DAG.
        dag.getParentSetsTimeT().stream()
                .filter(var -> var.getMainVar().getVarID()!=classVar.getVarID())
                .forEach(w -> {
                            w.addParent(classVar);
                            //Connect children in consecutive time steps
                            w.addParent(dynamicVariables.getInterfaceVariable(w.getMainVar()));
                        }
                );

        //Connect the class variable in consecutive time steps
        dag.getParentSetTimeT(classVar).addParent(dynamicVariables.getInterfaceVariable(classVar));

        return dag;
    }


    public static void main(String[] args) throws IOException {
        //We can open the data stream using the static class DataStreamLoader
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(
                "datasetsTests/WasteIncineratorSample.arff");

        //Parameter Learning
        //We set the batch size which will be employed to learn the model in parallel
        DynamicMaximumLikelihood.setBatchSize(1000);
        DynamicMaximumLikelihood.setParallelMode(true);


        //We fix the DAG structure, the data and learn the DBN
        DynamicBayesianNetwork dbn = DynamicMaximumLikelihood.learnDynamic(
                MLforDBNfromDataset.getNaiveBayesStructure(data.getAttributes(),2), data);

        //We print the model
        System.out.println(dbn.toString());
    }

}
