package eu.amidst.standardmodels.classifiers;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DataSetGenerator;
import eu.amidst.standardmodels.eu.amidst.standardmodels.exceptions.WrongConfigurationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ana@cs.aau.dk on 11/03/16.
 */
public class DynamicLatentClassificationModel extends DynamicClassifier{

    /** number of continuous hidden variables */
    private int numContinuousHidden;

    /** states of the multinomial hidden variable */
    private int numStatesHidden;

    /** multinomial hidden variable */
    private Variable hiddenMultinomial;

    /** set of continuous hidden variables*/
    private List<Variable> contHiddenList;

    public int getNumContinuousHidden() {
        return numContinuousHidden;
    }

    public void setNumContinuousHidden(int numContinuousHidden) {
        this.numContinuousHidden = numContinuousHidden;
    }

    public int getNumStatesHidden() {
        return numStatesHidden;
    }

    public void setNumStatesHidden(int numStatesHidden) {
        this.numStatesHidden = numStatesHidden;
    }

    /**
     * method for getting the hidden multinomial variable
     * @return object of type Variable
     */
    public Variable getHiddenMultinomial() {
        return hiddenMultinomial;
    }

    /**
     * method for getting the list of continuous hidden variables
     * @return
     */
    public List<Variable> getContHiddenList() {
        return contHiddenList;
    }

    public DynamicLatentClassificationModel(Attributes attributes) {
        super(attributes);
        this.numContinuousHidden = 2;
        this.numStatesHidden = 2;
    }


    @Override
    protected void buildDAG() {

        //Obtain the predictive attributes
        List<Variable> attrVars = variables.getListOfDynamicVariables().stream()
                .filter(v -> !v.equals(classVar)).collect(Collectors.toList());


        //Create the hidden variabels
        hiddenMultinomial = variables.newMultinomialDynamicVariable("M", numStatesHidden);

        contHiddenList = new ArrayList<Variable>();
        for(int i=0; i<numContinuousHidden; i++) {
            contHiddenList.add(variables.newGaussianDynamicVariable("Z"+Integer.toString(i)));
        }


        dynamicDAG = new DynamicDAG(variables);

        //arcs from the class to the hidden variables
        dynamicDAG.getParentSetTimeT(hiddenMultinomial).addParent(classVar);
        contHiddenList.stream().forEach(z -> dynamicDAG.getParentSetTimeT(z).addParent(classVar));


        //arcs from the hidden vars to each attribute
        attrVars.stream().forEach(x->dynamicDAG.getParentSetTimeT(x).addParent(hiddenMultinomial));

        for (Variable z : contHiddenList) {
            attrVars.stream().forEach(x->dynamicDAG.getParentSetTimeT(x).addParent(z));
        }

        //Add dynamic links on class and continuous layer
        dynamicDAG.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());

        for (Variable variable : contHiddenList) {
            dynamicDAG.getParentSetTimeT(variable).addParent(variable.getInterfaceVariable());
        }

    }

    @Override
    public void isValidConfiguration() {

        long numReal = variables.getListOfDynamicVariables()
                .stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.REAL))
                .count();

        long numFinite = variables.getListOfDynamicVariables()
                .stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.FINITE_SET))
                .count();


        if(numFinite != 1 || numReal != variables.getNumberOfVars()-1) {
            throw new UnsupportedOperationException("Invalid configuration: wrong number types of variables domains. " +
                    "It should contain 1 discrete variable and the rest shoud be real");
        }
    }

    public static void main(String[] args) throws WrongConfigurationException {

        DataStream<DynamicDataInstance> data = DataSetGenerator.generate(1,1000, 1, 10);

        //Parameters of the classifier
        String classVarName = "DiscreteVar0";
        int numContinuousHidden = 4;
        int numStatesHidden = 3;

        //Initializes the classifier
        DynamicLatentClassificationModel dLCM = new DynamicLatentClassificationModel(data.getAttributes());
        dLCM.setClassName(classVarName);
        dLCM.setNumContinuousHidden(numContinuousHidden);
        dLCM.setNumStatesHidden(numStatesHidden);


        //Learning

        dLCM.learnModel(data);
        for (DataOnMemory<DynamicDataInstance> batch : data.iterableOverBatches(100)) {
            dLCM.updateModel(batch);
        }

        //Shows the resulting model
        System.out.println(dLCM.getDynamicDAG());
        System.out.println(dLCM.getModel());


        //Change the inference algorithm to use as factored frontier
        dLCM.setInferenceAlgoPredict(new VMP());


        // predict the class of one instances
        System.out.println("Predicts some instances, i.e. computes the posterior probability of the class");
        List<DynamicDataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,100);


        int i = 1;
        for(DynamicDataInstance d : dataTest) {

            d.setValue(dLCM.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = dLCM.predict(d);
            System.out.println(posteriorProb.toString());

        }



    }

}
