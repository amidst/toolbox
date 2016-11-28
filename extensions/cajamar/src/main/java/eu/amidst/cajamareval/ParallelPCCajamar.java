package eu.amidst.cajamareval;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.huginlink.learning.ParallelPC;

/**
 * Created by dario on 28/11/16.
 */
public class ParallelPCCajamar extends ParallelPC {

    /**
     * Predicts the class membership probabilities for a given instance.
     * @param instance the data instance to be classified.
     * @return an array of doubles containing the estimated membership probabilities of the data instance for each class label.
     */
    public double[] predict(DataInstance instance) {

        BayesianNetwork learnedBN = this.getLearnedBN();
        if (learnedBN==null)
            throw new IllegalArgumentException("The model has not been learned");

        Variable classVar = learnedBN.getVariables().getVariableByName("Default");
        if (!Utils.isMissingValue(instance.getValue(classVar)))
            System.out.println("Class Variable can not be set.");

        return this.predict(instance, classVar);

    }
}
