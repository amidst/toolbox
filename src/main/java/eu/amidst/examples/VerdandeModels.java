package eu.amidst.examples;

import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.variables.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains examples about how we can create Verdande's dynamic models using the AMIDST Toolbox.
 * It show how to create 2T-DBNs over Multinomial, Gassuian and Logistic variables.
 *
 * Created by andresmasegosa on 22/11/14.
 */
public class VerdandeModels {


    /**
     * In this example we show how to create an input-output SKF.
     */
    public static void VerdandeInputOutputSKF(){

        /**
         * 1. Our data is on disk and does not fit in memory. So, we use a DataOnDisk object.
         * 2. Our data is dynamic and is on file, so we create the DataOnDisk using a DynamicDataOnDiskFromFile object.
         * 3. Our data is in Weka format, so we use a WekaDataFileReader.
         */
        DataOnDisk data = new DynamicDataOnDiskFromFile(new WekaDataFileReader(new String("datasets/syntheticDataVerdande.arff")));

        /**
         * 1. Once the data is loaded, we create random dynamic variables for some of the attributes (i.e. data columns)
         * in our data. In this case, we use the method "addObservedDynamicVariable" of the Dynamic Variable class.
         *
         * Here dynamic variable has the same type than static variables. However, there are two main differences
         *      - If we called to the method "isDynamic" of Variable class, it will return true.
         *      - They have a temporal clone (similarly to Hugin). This temporal clone is another variable object. It is
         *      automatically created for each dynamic variable. It can be queried by the method "getTemporalClone" of the
         *      DynamicVariable class.
         *
         * 2. DynamicVariables is the class for doing that. It takes a list of Attributes and internally creates
         * all the variables. We create the variables using DynamicVariables class to guarantee that each variable
         * has a different ID number and make it transparent for the user.
         *
         * 3. We can extract the Variable objects by using the method getVariableByName();
         *
         * 4. The created variables are dynamic. Here in AMIDST toolbox, it implies they have a temporal clone (similarly to Hugin).
         * This temporal clone is automatically created for each dynamic variable.
         */
        Attribute attTRQ = data.getAttributes().getAttributeByName("TRQ");
        Attribute attROP = data.getAttributes().getAttributeByName("ROP");

        List<Attribute> attributeList = new ArrayList();
        attributeList.add(attTRQ);
        attributeList.add(attROP);

        DynamicVariables dynamicVariables = new DynamicVariables();

        Variable observedROP = dynamicVariables.addObservedDynamicVariable(attROP);
        Variable observedTRQ = dynamicVariables.addObservedDynamicVariable(attTRQ);


        /**
         * 1. We now create the hidden variables. For doing that we make use of the class VariableBuilder. When
         * a variable is created from an Attribute object, it contains all the information we need (e.g.
         * the name, the type, etc). But hidden variables does not have an associated attribute
         * and, for this reason, we use now this VariableBuilder to provide this information to
         * DynamicVariables object.
         *
         * 2. Using VariableBuilder, we define the hidden variables and we explicitly indicate if the are Multinomial,
         * Gaussian or Multinomial_Logistic (i.e. a multinomial variable with continuous parents).
         *
         * 3. We finally create the hidden variable using the method "addHiddenDynamicVariable".
         */
        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setName("realTRQ");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpaceType(StateSpaceType.REAL);
        variableBuilder.setDistributionType(DistType.GAUSSIAN);
        Variable realTRQ = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        variableBuilder = new VariableBuilder();
        variableBuilder.setName("HiddenVar");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpaceType(StateSpaceType.REAL);
        variableBuilder.setDistributionType(DistType.GAUSSIAN);
        Variable hidden = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        variableBuilder = new VariableBuilder();
        variableBuilder.setName("Normal_Abnormal");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpaceType(StateSpaceType.FINITE_SET);
        variableBuilder.setDistributionType(DistType.MULTINOMIAL_LOGISTIC);
        variableBuilder.setNumberOfStates(2);
        Variable normal_Abnormal = dynamicVariables.addHiddenDynamicVariable(variableBuilder);


        /**
         * 1. Once we have defined your DynamicVariables object the next step is to create a dynamic DAG
         * structure over this set of variables.
         *
         * 2. To create a dynamic DAG, we just define the transition graph.
         *
         * 4. To add parents to each dynamic variable, we first recover the ParentSet object by the method
         * getParentSetTimeT(Variable var) and then call the method addParent(Variable var).
         *
         * 4. We can define temporal dependencies by referring to the temporal clones of the variables.
         *
         */
        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(observedROP);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(realTRQ);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(hidden);

        dynamicDAG.getParentSetTimeT(realTRQ).addParent(dynamicVariables.getTemporalClone(realTRQ));
        dynamicDAG.getParentSetTimeT(realTRQ).addParent(normal_Abnormal);

        dynamicDAG.getParentSetTimeT(hidden).addParent(normal_Abnormal);
        dynamicDAG.getParentSetTimeT(hidden).addParent(dynamicVariables.getTemporalClone(hidden));


        dynamicDAG.getParentSetTimeT(normal_Abnormal).addParent(dynamicVariables.getTemporalClone(normal_Abnormal));
        dynamicDAG.getParentSetTimeT(normal_Abnormal).addParent(observedROP);


        /**
         * 1. We print the graph to see if it is properly created.
         *
         * 2. The printed graph is structured in two layers. We first display the graph structure for time 0 (no
         * temporal dependencies) and, the, we time t (with temporal dependencies).
         */
        System.out.println(dynamicDAG.toString());

        /**
         * 1. We now create the Bayesian network from the previous DAG.
         *
         * 2. The BN object is created from the DAG. It automatically looks at the distribution type
         * of each variable and their parents to initialize the Distributions objects that are stored
         * inside (i.e. Multinomial, Normal, CLG, etc). The parameters defining these distributions are
         * properly initialized.
         *
         * 3. The network is printed and we can have look at the kind of distributions stored in the BN object. Similarly
         * to dynamic DAG, it is printed in two layers. The model for time 0 and the model from time t.
         */
        DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);
        System.out.println(dynamicBayesianNetwork.toString());

    }



    public static void main(String[] args) {
        VerdandeModels.VerdandeInputOutputSKF();
    }
}
