package eu.amidst.tutorial.usingAmidst.practice;

import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.latentvariablemodels.dynamicmodels.DynamicModel;
import eu.amidst.latentvariablemodels.dynamicmodels.HiddenMarkovModel;
import eu.amidst.latentvariablemodels.dynamicmodels.KalmanFilter;
import eu.amidst.latentvariablemodels.staticmodels.FactorAnalysis;
import eu.amidst.latentvariablemodels.staticmodels.Model;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rcabanas on 23/05/16.
 */

public class CustomKalmanFilter extends DynamicModel {

    Attributes attributes;

    public CustomKalmanFilter(Attributes attributes) throws WrongConfigurationException {
        super(attributes);
        this.attributes=attributes;
    }


    @Override
    protected void buildDAG() {

        /*number of continuous hidden variable*/
        int numHiddenVars=3;

        /** Create a set of dynamic variables from the given attributes**/
        variables = new DynamicVariables(attributes);

        /* List of continuous hidden vars*/
        List<Variable> gaussianHiddenVars = new ArrayList<>();

        for(int i=0; i<numHiddenVars; i++) {
            Variable Hi = variables.newGaussianDynamicVariable("gaussianHiddenVar" + i);
            gaussianHiddenVars.add(Hi);
        }

        DynamicDAG dynamicDAG = new DynamicDAG(this.variables);


        for (Variable h : gaussianHiddenVars) {
            dynamicDAG.getParentSetTimeT(h).addParent(h.getInterfaceVariable());
        }

        for (Variable variable: variables) {
            if (gaussianHiddenVars.contains(variable))
                continue;

            for (Variable h : gaussianHiddenVars) {
                dynamicDAG.getParentSetTimeT(variable).addParent(h);
            }
        }

        //This is needed to maintain coherence in the DynamicModel class.
        this.variables = variables;
        this.dynamicDAG = dynamicDAG;
    }


    public static void main(String[] args) throws IOException, ExceptionHugin {

        //Load the datastream
        String filename = "datasets/simulated/cajamar.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(filename);


        //Learn the model
        DynamicModel model = new CustomKalmanFilter(data.getAttributes());
        model.updateModel(data);
        DynamicBayesianNetwork dbn = model.getModel();


        System.out.println(dbn);

    }


}
