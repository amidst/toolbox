package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import org.apache.commons.lang.NotImplementedException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rcabanas on 07/03/16.
 */


public class GaussianMixture extends Model {


    public GaussianMixture(Attributes attributes) {
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
        throw new NotImplementedException("The method isValidConfiguration() has not been implemented for the class "+this.getClass().getName());
    }
}
