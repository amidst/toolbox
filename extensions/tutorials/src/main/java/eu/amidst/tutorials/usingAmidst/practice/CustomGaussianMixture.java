package eu.amidst.tutorials.usingAmidst.practice;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.latentvariablemodels.staticmodels.Model;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

/**
 * Created by rcabanas on 23/05/16.
 */

public class CustomGaussianMixture extends Model{

    Attributes attributes;

    public CustomGaussianMixture(Attributes attributes) throws WrongConfigurationException {
        super(attributes);
        this.attributes=attributes;
    }

    @Override
    protected void buildDAG() {

        /** Create a set of variables from the given attributes**/
        Variables variables = new Variables(attributes);

        /** Create a hidden variable with two hidden states*/
        Variable hiddenVar = variables.newMultinomialVariable("HiddenVar",2);

        //We create a standard naive Bayes
        DAG dag = new DAG(vars);

        for (Variable variable: vars){
            if (vars==hiddenVar)
                continue;

            dag.getParentSet(variable).addParent(hiddenVar);
        }



        //This is needed to maintain coherence in the Model class.
        this.dag=dag;
        this.vars = variables;

    }

}
