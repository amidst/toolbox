package eu.amidst.dynamic.examples.variables;

import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.DynamicModelFactory;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.util.Arrays;

/**
 * This example show the basic functionality of the classes DynamicVariables and Variable.
 *
 * Created by ana@cs.aau.dk on 02/12/15.
 */
public class DynamicVariablesExample {

    public static void main(String[] args) throws Exception {

        //We first create an empty Variables object
        DynamicVariables variables = DynamicModelFactory.newDynamicVariables();

        //We invoke the "new" methods of the object Variables to create new variables.
        //Now we create a Gaussian variables
        Variable gaussianVar = variables.newGaussianDynamicVariable("GaussianVar");

        //Now we create a Multinomial variable with two states
        Variable multinomialVar = variables.newMultinomialDynamicVariable("MultinomialVar", 2);

        //Now we create a Multinomial variable with two states: TRUE and FALSE as for (static) variables
        Variable multinomialVar2 = variables.newMultinomialDynamicVariable("MultinomialVar2", Arrays.asList("TRUE, FALSE"));

        //All "dynamic Variables" have an interface variable
        Variable gaussianVarInt = gaussianVar.getInterfaceVariable();
        Variable multinomialVarInt = multinomialVar.getInterfaceVariable();

        //We can get the "main" Variable associated with each interface variable through the DynamicVariable object
        Variable mainMultinomialVar = variables.getVariableFromInterface(multinomialVarInt);

        //We can check whether a variable is an interface variable
        System.out.println("Is Variable "+gaussianVar.getName()+" an interface variable? "
                +gaussianVar.isInterfaceVariable());
        System.out.println("Is Variable "+gaussianVarInt.getName()+" an interface variable? "
                +gaussianVarInt.isInterfaceVariable());

        //We can check whether a variable is a dynamic variable
        System.out.println("Is Variable "+multinomialVar.getName()+" a dynamic variable? "
                +gaussianVar.isDynamicVariable());
    }
}
