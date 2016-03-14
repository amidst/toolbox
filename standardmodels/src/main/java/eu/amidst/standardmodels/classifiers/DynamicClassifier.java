package eu.amidst.standardmodels.classifiers;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.inference.FactoredFrontierForDBN;
import eu.amidst.dynamic.inference.InferenceAlgorithmForDBN;
import eu.amidst.dynamic.inference.InferenceEngineForDBN;
import eu.amidst.standardmodels.DynamicModel;
import eu.amidst.standardmodels.exceptions.WrongConfigurationException;

/**
 * The DynamicClassifier abstract class is defined for dynamic Bayesian classification models.
 *
 * Created by ana@cs.aau.dk on 11/03/16.
 */
public abstract class DynamicClassifier extends DynamicModel{

    /** Represents the static inference algorithm. */
    private InferenceAlgorithm inferenceAlgoPredict = new ImportanceSampling();

    /** Factored Frontier algorithm for DBN*/
    private InferenceAlgorithmForDBN dynamicInferenceAlgoPredict;

    /** class variable */
    protected Variable classVar;

    public InferenceAlgorithm getInferenceAlgoPredict() {
        return inferenceAlgoPredict;
    }

    public void setInferenceAlgoPredict(InferenceAlgorithm inferenceAlgoPredict) {
        this.inferenceAlgoPredict = inferenceAlgoPredict;
    }

    /**
     * Method to obtain the class variable
     * @return object of the type {@link Variable} indicating which is the class variable
     */
    public Variable getClassVar() {
        return classVar;
    }


    /**
     * Method to set the class variable. Note that it should be multinomial
     * @param classVar object of the type {@link Variable} indicating which is the class variable
     * @throws WrongConfigurationException is thrown when the variable is not a multinomial.
     */
    public void setClassVar(Variable classVar){

        if(!classVar.isMultinomial()) {
            throw new UnsupportedOperationException("class variable is not a multinomial");
        }

        this.classVar = classVar;
        dynamicDAG = null;

    }

    /**
     * Method to set the class variable. Note that it should be multinomial
     * @param className String with the name of the class variable
     * @throws WrongConfigurationException is thrown when the variable is not a multinomial.
     */
    public void setClassName(String className) throws WrongConfigurationException {
        setClassVar(variables.getVariableByName(className));
    }

    public DynamicClassifier(Attributes attributes) {
        super(attributes);
        classVar = variables.getListOfDynamicVariables().get(variables.getNumberOfVars()-1);
        dynamicInferenceAlgoPredict = new FactoredFrontierForDBN(getInferenceAlgoPredict());
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(dynamicInferenceAlgoPredict);
    }


    /**
     * Predicts the class membership probabilities for a given instance.
     * @param instance the data instance to be classified. The value associated to the class variable must be
     *                 a missing value (i.e. a NaN)
     * @return the posterior probability of the class variable
     */
    public Multinomial predict(DynamicDataInstance instance) {
        if (!Utils.isMissingValue(instance.getValue(classVar)))
            System.out.println("Class Variable can not be set.");

        InferenceEngineForDBN.setModel(this.getModel());
        if (instance.getTimeID()==0) {
            InferenceEngineForDBN.reset();
        }
        InferenceEngineForDBN.addDynamicEvidence(instance);

        System.out.println(instance);

        InferenceEngineForDBN.runInference();

        return InferenceEngineForDBN.getFilteredPosterior(classVar);



    }
}
