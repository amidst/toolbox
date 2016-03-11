package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.standardmodels.exceptions.WrongConfigurationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rcabanas on 07/03/16.
 */
public class GaussianMixture extends Model {



    /* diagonal flag*/
    private final boolean diagonal;

    /*number of states of the hidden variable*/
    private final int numStatesHiddenVar;



    /**
     * Constructor of classifier from a list of attributes (e.g. from a datastream).
     *
     * @param attributes
     * @param numStatesHiddenVar
     * @param diagonal
     */
    public GaussianMixture(Attributes attributes, int numStatesHiddenVar, boolean diagonal) throws WrongConfigurationException {
        super(attributes);
        this.numStatesHiddenVar = numStatesHiddenVar;
        this.diagonal = diagonal;
    }





    @Override
    protected void buildDAG() {

        Variable hiddenVar = vars.newMultionomialVariable("HiddenVar",numStatesHiddenVar);

        //We create a standard naive Bayes
        dag = new DAG(vars);

        dag.getParentSets().stream().filter(w -> !w.getMainVar().equals(hiddenVar)).forEach(w -> w.addParent(hiddenVar));

        // if it is not diagonal add the links between the attributes (features)
        if(!isDiagonal()) {
            List<Variable> attrVars = vars.getListOfVariables().stream().filter(v -> !v.equals(hiddenVar)).collect(Collectors.toList());

            for (int i=0; i<attrVars.size()-1; i++){
                for(int j=i+1; j<attrVars.size(); j++) {
                    // Add the links
                    dag.getParentSet(attrVars.get(i)).addParent(attrVars.get(j));



                }

            }


        }


    }




    /////// Getters and setters

    /**
     * Method to obtain the value of the diagonal flag.
     * @return boolean value
     */
    public boolean isDiagonal() {
        return diagonal;
    }

    /**
     * Method to obtain the number of states of the hidden (latent) variable
     * @return integer value
     */

    public int getNumStatesHiddenVar() {
        return numStatesHiddenVar;
    }

    @Override
    public boolean isValidConfiguration(){

        boolean isValid = true;
        if(!vars.getListOfVariables().stream()
                .map( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.REAL))
                .reduce((n1,n2) -> n1 && n2).get().booleanValue()){
            isValid = false;
            System.err.println("Invalid configuration: all the variables must be real");
        }
        return  isValid;

    }

    //////////// example of use

    public static void main(String[] args) throws WrongConfigurationException {

        String file = "datasets/syntheticDataDaimler.arff";
        //file = "datasets/tmp2.arff"; //example of inappropriate dataset
        DataStream<DataInstance> data = DataStreamLoader.openFromFile(file);

        GaussianMixture GMM = new GaussianMixture(data.getAttributes(), 2, true);

        GMM.learnModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
            GMM.updateModel(batch);
        }
        System.out.println(GMM.getModel());
        System.out.println(GMM.getDAG());

    }
}

