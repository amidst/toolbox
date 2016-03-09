package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.standardmodels.eu.amidst.standardmodels.exceptions.WrongConfigurationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rcabanas on 09/03/16.
 */
public class HTClassification extends Model {


    private final int numContinuousHidden;
    private final int numStatesHidden;
    private final Variable classVar;


    public HTClassification(Attributes attributes, String classVarName, int numContinuousHidden, int numStatesHidden) throws WrongConfigurationException {
        super(attributes);

        this.numContinuousHidden = numContinuousHidden;
        this.numStatesHidden = numStatesHidden;
        this.classVar = vars.getVariableByName(classVarName);


    }



    @Override
    protected void buildDAG() {



        //Obtain the predictive attributes
        List<Variable> attrVars = vars.getListOfVariables().stream()
                .filter(v -> !v.equals(classVar)).collect(Collectors.toList());


        //Create the hidden variabels
        Variable hiddenMultinomial = vars.newMultionomialVariable("M", numStatesHidden);

        List<Variable> contHiddenList = new ArrayList<Variable>();
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

    public int getNumContinuousHidden() {
        return numContinuousHidden;
    }

    public int getNumStatesHidden() {
        return numStatesHidden;
    }

    public Variable getClassVar() {
        return classVar;
    }


    ///////// main class (example of use) //////

    public static void main(String[] args) throws WrongConfigurationException {

        String file = "datasets/tmp2.arff";
        //file = "datasets/syntheticDataDaimler.arff";
        DataStream<DataInstance> data = DataStreamLoader.openFromFile(file);

        HTClassification htc = new HTClassification(data.getAttributes(), "default", 3,3);

        if(htc.isValidConfiguration()) {
            htc.learnModel(data);
            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
                htc.updateModel(batch);
            }
            System.out.println(htc.getModel());
            System.out.println(htc.getDAG());
        }
    }



}
