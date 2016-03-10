package eu.amidst.standardmodels.classifiers;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.MessagePassingAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.standardmodels.eu.amidst.standardmodels.exceptions.WrongConfigurationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rcabanas on 09/03/16.
 */
public class LatentClassificationModel extends Classifier {


    /** number of continuous hidden variables */
    private int numContinuousHidden;

    /** states of the multinomial hidden variable */
    private int numStatesHidden;

    /** multinomial hidden variable */
    private Variable hiddenMultinomial;

    /** set of continuous hidden variables*/
    private List<Variable> contHiddenList;


    /**
     * Constructor of classifier from a list of attributes.
     * The default parameters are used: the class variable is the last one and the
     * diagonal flag is set to false (predictive variables are NOT independent).
     * @param attributes list of attributes of the classifier (i.e. its variables)
     * @throws WrongConfigurationException
     */
    public LatentClassificationModel(Attributes attributes) throws WrongConfigurationException {
        super(attributes);

        //default values
        this.numContinuousHidden = 2;
        this.numStatesHidden = 2;


    }


    /**
     * Builds the DAG over the set of variables given with the naive Bayes structure
     */
    @Override
    protected void buildDAG() {



        //Obtain the predictive attributes
        List<Variable> attrVars = vars.getListOfVariables().stream()
                .filter(v -> !v.equals(classVar)).collect(Collectors.toList());


        //Create the hidden variabels
        hiddenMultinomial = vars.newMultionomialVariable("M", numStatesHidden);

        contHiddenList = new ArrayList<Variable>();
        for(int i=0; i<numContinuousHidden; i++) {
            contHiddenList.add(vars.newGaussianVariable("Z"+Integer.toString(i)));
        }


        dag = new DAG(vars);

        //arcs from the class to the hidden variables
        dag.getParentSet(hiddenMultinomial).addParent(classVar);
        contHiddenList.stream().forEach(z -> dag.getParentSet(z).addParent(classVar));


        //arcs from the hidden vars to each attribute
        attrVars.stream().forEach(x->dag.getParentSet(x).addParent(hiddenMultinomial));

        for (Variable z : contHiddenList) {
            attrVars.stream().forEach(x->dag.getParentSet(x).addParent(z));
        }


    }
    /*
    * tests if the attributes passed as an argument in the constructor are suitable for this classifier
    * @return boolean value with the result of the test.
            */
    public boolean isValidConfiguration(){
        boolean isValid = true;


        long numReal = vars.getListOfVariables().stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.REAL))
                .count();

        long numFinite = vars.getListOfVariables().stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.FINITE_SET))
                .count();


        if(numFinite != 1 || numReal != vars.getNumberOfVars()-1) {
            isValid = false;
            String errorMsg = "Invalid configuration: wrong number types of variables domains. It should contain 1 discrete variable and the rest shoud be real";
            this.setErrorMessage(errorMsg);

        }

        return  isValid;

    }



    //////// Getters and setters /////////

    /**
     * Method to obtain the number of continuous hidden variables
     * @return integer value
     */
    public int getNumContinuousHidden() {
        return numContinuousHidden;
    }

    /**
     * method for getting number of states of the hidden multinomial variable
     * @return integer value
     */
    public int getNumStatesHidden() {
        return numStatesHidden;
    }

    /**
     * sets the number of continuous hidden variables
     * @param numContinuousHidden integer value
     */
    public void setNumContinuousHidden(int numContinuousHidden) {
        this.numContinuousHidden = numContinuousHidden;
        dag = null;
    }

    /**
     * sets the number of states of the hidden multinomial variable
     * @param numStatesHidden integer value
     */
    public void setNumStatesHidden(int numStatesHidden) {
        this.numStatesHidden = numStatesHidden;
        dag = null;
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

    ///////// main class (example of use) //////

    public static void main(String[] args) throws WrongConfigurationException {

        DataStream<DataInstance> data = DataSetGenerator.generate(1234,1000, 1, 10);
        System.out.println(data.getAttributes().toString());

        //Parameters of the classifier
        String classVarName = "DiscreteVar0";
        int numContinuousHidden = 4;
        int numStatesHidden = 3;

        //Initializes the classifier
        LatentClassificationModel lcm = new LatentClassificationModel(data.getAttributes());
        lcm.setClassName(classVarName);
        lcm.setNumContinuousHidden(numContinuousHidden);
        lcm.setNumStatesHidden(numStatesHidden);


        //Learning

        lcm.learnModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
            lcm.updateModel(batch);
        }

        //Shows the resulting model
        //System.out.println(lcm.getModel());
        //System.out.println(lcm.getDAG());


        // Uncomment the following 2 lines to get the bug
        //InferenceAlgorithm algo = new VMP();
        //lcm.setInferenceAlgoPredict(algo);


        // predict the class of one instances
        System.out.println("Predicts some instances, i.e. computes the posterior probability of the class");
        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,100);


        int i = 1;
        for(DataInstance d : dataTest) {

                d.setValue(lcm.getClassVar(), Utils.missingValue());
                Multinomial posteriorProb = lcm.predict(d);
                System.out.println(posteriorProb.toString());

        }



    }



}
