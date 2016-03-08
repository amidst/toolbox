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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rcabanas on 08/03/16.
 */
public class BayesianLinearRegression extends Model {


    /* diagonal flag */
    final private boolean diagonal;

    /** class variable */
    final private Variable classVar;

    /**
     * Constructor of classifier from a list of attributes (e.g. from a datastream).
     * @param attributes
     * @param classVarName
     * @param diagonal
     * @throws WrongConfigurationException
     */
    public BayesianLinearRegression(Attributes attributes, String classVarName, boolean diagonal) throws WrongConfigurationException {
        super(attributes);
        classVar = vars.getVariableByName(classVarName);
        this.diagonal = diagonal;

    }




    @Override
    protected void buildDAG() {
        dag = new DAG(vars);


        //arcs from the features to the class
        vars.getListOfVariables().stream()
                .filter(v -> !v.equals(classVar))
                .forEach(v -> dag.getParentSet(classVar).addParent(v));

        // if it is not diagonal add the links between the attributes (features)
        if(!isDiagonal()) {

            List<Variable> attrVars = vars.getListOfVariables().stream().filter(v -> !v.equals(classVar)).collect(Collectors.toList());

            for (int i=0; i<attrVars.size()-1; i++){
                for(int j=i+1; j<attrVars.size(); j++) {
                    // Add the links
                    dag.getParentSet(attrVars.get(i)).addParent(attrVars.get(j));



                }

            }


        }



    }

    @Override
    public boolean isValidConfiguration() {


        boolean isValid = vars.getListOfVariables().stream()
                .allMatch(v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.REAL));

        if(!isValid) {
            setErrorMessage("All the variables must be REAL");
        }


        return isValid;
    }


    //////Getters and setters ////////

    /**
     * Method to obtain the value of the diagonal flag.
     * @return boolean value
     */
    public boolean isDiagonal() {
        return diagonal;
    }



    /**
     * Method to obtain the class variable
     * @return object of the type {@link Variable} indicating which is the class variable
     */
    public Variable getClassVar() {
        return classVar;
    }




    //////// Example of use ///////

    public static void main(String[] args) throws WrongConfigurationException {

        String file = "datasets/syntheticDataDaimler.arff";
       // file = "datasets/tmp2.arff"; //example of inappropriate dataset
        DataStream<DataInstance> data = DataStreamLoader.openFromFile(file);


        String className = "O_LAT_MEAS";
       // className = "default";

        BayesianLinearRegression BLR = new BayesianLinearRegression(data.getAttributes(), className, false);

        if(BLR.isValidConfiguration()) {
            BLR.learnModel(data);
            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
                BLR.updateModel(batch);
            }
            System.out.println(BLR.getModel());
            System.out.println(BLR.getDAG());
        }
    }







}
