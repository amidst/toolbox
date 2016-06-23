package eu.amidst.tutorials.usingAmidst.practice;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.latentvariablemodels.dynamicmodels.DynamicModel;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

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
        DynamicVariables variables = new DynamicVariables(attributes);

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


}
