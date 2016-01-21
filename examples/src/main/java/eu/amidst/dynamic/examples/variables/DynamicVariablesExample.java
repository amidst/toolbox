package eu.amidst.dynamic.examples.variables;

import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.util.Arrays;

/**
 * This example show the basic functionalities related to dynamic variables.
 */
public class DynamicVariablesExample {

    public static void main(String[] args) throws Exception {

        //Create an empty DynamicVariables object
        DynamicVariables variables = new DynamicVariables();

        //Invoke the "new" methods of the object DynamicVariables to create new dynamic variables.

        //Create a Gaussian dynamic variables
        Variable gaussianVar = variables.newGaussianDynamicVariable("GaussianVar");

        //Create a Multinomial dynamic variable with two states
        Variable multinomialVar = variables.newMultinomialDynamicVariable("MultinomialVar", 2);

        //Create a Multinomial dynamic variable with two states: TRUE and FALSE
        Variable multinomialVar2 = variables.newMultinomialDynamicVariable("MultinomialVar2", Arrays.asList("TRUE, FALSE"));

        //All dynamic Variables have an interface variable
        Variable gaussianVarInt = gaussianVar.getInterfaceVariable();
        Variable multinomialVarInt = multinomialVar.getInterfaceVariable();

        //Get the "main" Variable associated with each interface variable through the DynamicVariable object
        Variable mainMultinomialVar = variables.getVariableFromInterface(multinomialVarInt);

        //Check whether a variable is an interface variable
        System.out.println("Is Variable "+gaussianVar.getName()+" an interface variable? "
                +gaussianVar.isInterfaceVariable());
        System.out.println("Is Variable "+gaussianVarInt.getName()+" an interface variable? "
                +gaussianVarInt.isInterfaceVariable());

        //Check whether a variable is a dynamic variable
        System.out.println("Is Variable "+multinomialVar.getName()+" a dynamic variable? "
                +gaussianVar.isDynamicVariable());
    }
}
