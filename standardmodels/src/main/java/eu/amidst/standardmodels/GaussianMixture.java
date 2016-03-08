package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.standardmodels.eu.amidst.standardmodels.exceptions.WrongConfigurationException;

import java.util.List;

/**
 * Created by rcabanas on 07/03/16.
 */


public class GaussianMixture extends Model {


    /**
     * Constructor for the gaussian mixture model from a list of attributes (e.g. from a datastream).
     * @param attributes
     */
    public GaussianMixture(Attributes attributes) throws WrongConfigurationException {
        super(attributes);
    }

    @Override
    protected void buildDAG(Attributes attributes) {

        Variables vars = new Variables(attributes);
        dag = new DAG(vars);

        List<Variable> variableList = vars.getListOfVariables();

        for (int i=0; i<variableList.size()-1; i++){
            for(int j=i+1; j<variableList.size(); j++) {
                // Add the links
                dag.getParentSet(variableList.get(i)).addParent(variableList.get(j));

            }

        }

    }


    @Override
    public boolean isValidConfiguration(){
        Variables vars = new Variables(attributes);
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

        GaussianMixture GMM = new GaussianMixture(data.getAttributes());

        GMM.learnModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
            System.out.println("update model");
            GMM.updateModel(batch);
        }
        System.out.println(GMM.getModel());
        System.out.println(GMM.getDAG());

    }
}

