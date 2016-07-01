package eu.amidst.tutorials.huginsa2016.practice;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.Model;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

/**
 * Created by rcabanas on 23/05/16.
 */

public class CustomGaussianMixture extends Model{
    /*number of states of the hidden variable*/
    private int numStatesHiddenVar;
    /** hidden variable */
    private Variable hiddenVar;


    public CustomGaussianMixture(Attributes attributes) throws WrongConfigurationException {
        super(attributes);

        //TODO: Write the contructor code here
        numStatesHiddenVar = 2;

    }


    @Override
    protected void buildDAG() {
        //TODO: Write the code building a DAG for your custom model

        hiddenVar = vars.newMultinomialVariable("HiddenVar",numStatesHiddenVar);

        //We create a standard naive Bayes
        dag = new DAG(vars);
        dag.getParentSets()
                .stream()
                .filter(w -> !w.getMainVar().equals(hiddenVar))
                .forEach(w -> w.addParent(hiddenVar));


    }


    public void setNumStatesHiddenVar(int numStatesHiddenVar) {
        this.numStatesHiddenVar = numStatesHiddenVar;
    }

    public int getNumStatesHiddenVar() {
        return numStatesHiddenVar;
    }

    public static void main(String[] args) {

        //Load the datastream
        String filename = "datasets/simulated/exampleDS_d0_c5.arff";
        DataStream<DataInstance> data = DataStreamLoader.open(filename);

        //Learn the model
        CustomGaussianMixture model = new CustomGaussianMixture(data.getAttributes());
        model.setNumStatesHiddenVar(3);
        model.setWindowSize(200);
        model.updateModel(data);
        BayesianNetwork bn = model.getModel();

        System.out.println(bn);
    }


}
