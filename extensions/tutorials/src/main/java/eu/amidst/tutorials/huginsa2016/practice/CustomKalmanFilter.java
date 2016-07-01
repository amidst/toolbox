package eu.amidst.tutorials.huginsa2016.practice;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.latentvariablemodels.dynamicmodels.DynamicModel;
import eu.amidst.latentvariablemodels.staticmodels.Model;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rcabanas on 23/05/16.
 */

public class CustomKalmanFilter extends DynamicModel {

    /*number of continuous hidden variable*/
    private int numHiddenVars;


    /* List of continuous hidden vars*/
    List<Variable> gaussianHiddenVars = null;



    public CustomKalmanFilter(Attributes attributes) throws WrongConfigurationException {
        super(attributes);

        //TODO: Write the contructor code here
        numHiddenVars = 2;

    }


    @Override
    protected void buildDAG() {

        gaussianHiddenVars = new ArrayList<>();

        for(int i=0; i<numHiddenVars; i++) {
            Variable Hi = this.variables.newGaussianDynamicVariable("gaussianHiddenVar" + i);
            gaussianHiddenVars.add(Hi);
        }

        dynamicDAG = new DynamicDAG(this.variables);


        //Parents of each hidden variables (to previous instant)
        for (Variable h : gaussianHiddenVars) {
            dynamicDAG.getParentSetTimeT(h).addParent(h.getInterfaceVariable());
        }


        // Parents of observ. variables
        dynamicDAG.getDynamicVariables().getListOfDynamicVariables()
                .stream()
                .filter(v -> !gaussianHiddenVars.contains(v))
                .forEach(x -> gaussianHiddenVars
                        .stream()
                        .forEach(h -> {
                            dynamicDAG.getParentSetTimeT(x).addParent(h);
                        }
                        ));


    }


    public int getNumHiddenVars() {
        return numHiddenVars;
    }

    public void setNumHiddenVars(int numHiddenVars) {
        this.numHiddenVars = numHiddenVars;
    }

    public static void main(String[] args) {

        //Load the datastream
        String filename = "datasets/simulated/exampleDS_d0_c5.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(filename);

        //Learn the model
        CustomKalmanFilter model = new CustomKalmanFilter(data.getAttributes());
        model.setNumHiddenVars(3);
        model.setWindowSize(200);
        model.updateModel(data);
        DynamicBayesianNetwork bn = model.getModel();

        System.out.println(bn);
    }


}
