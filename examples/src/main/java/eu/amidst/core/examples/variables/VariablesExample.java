package eu.amidst.core.examples.variables;


import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;

import java.util.Arrays;

/**
 *
 * This example show the basic functionality of the classes Variables and Variable.
 *
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class VariablesExample {

    public static void main(String[] args) throws Exception {

        //We first create an empty Variables object
        Variables variables = new Variables();

        //We invoke the "new" methods of the object Variables to create new variables.
        //Now we create a Gaussian variables
        Variable gaussianVar = variables.newGaussianVariable("Gaussian");

        //Now we create a Multinomial variable with two states
        Variable multinomialVar = variables.newMultinomialVariable("Multinomial", 2);

        //Now we create a Multinomial variable with two states: TRUE and FALSE
        Variable multinomialVar2 = variables.newMultinomialVariable("Multinomial2", Arrays.asList("TRUE, FALSE"));

        //For Multinomial variables we can iterate over their different states
        FiniteStateSpace states = multinomialVar2.getStateSpaceType();
        states.getStatesNames().forEach(System.out::println);

        //Variable objects can also be used, for example, to know if one variable can be set as parent of some other variable
        System.out.println("Can a Gaussian variable be parent of Multinomial variable? " +
                (multinomialVar.getDistributionType().isParentCompatible(gaussianVar)));

        System.out.println("Can a Multinomial variable be parent of Gaussian variable? " +
                (gaussianVar.getDistributionType().isParentCompatible(multinomialVar)));

    }
}