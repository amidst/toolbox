package eu.amidst.examples;

import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.variables.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andresmasegosa on 22/11/14.
 */
public class DynamicBayesianNetworks {

    public static void CajaMarLikeModels(){

        DataOnDisk data = new DynamicDataOnDiskFromFile(new WekaDataFileReader(new String("datasets/syntheticDataCajaMar.arff")));

        //***************************************** Network structure **************************************************
        //Create the structure by hand
        DynamicVariables dynamicVariables = new DynamicVariables(data.getAttributes());

        Variable defaulter = dynamicVariables.getVariable("DEFAULTER");
        Variable sex = dynamicVariables.getVariable("SEX");
        Variable creditCard = dynamicVariables.getVariable("CREDITCARD");
        Variable balance = dynamicVariables.getVariable("BALANCE");
        Variable withDraw = dynamicVariables.getVariable("WITHDRAW");
        Variable salary = dynamicVariables.getVariable("SALARY");
        Variable monthlyBalance = dynamicVariables.getVariable("MONTHLYBALANCE");
        Variable movements = dynamicVariables.getVariable("MOVEMENTS");

        Variable indicatorSalary = dynamicVariables.addIndicatorDynamicVariable(salary);

        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(defaulter).addParent(sex);
        dynamicDAG.getParentSetTimeT(defaulter).addParent(dynamicVariables.getTemporalClone(defaulter));


        dynamicDAG.getParentSetTimeT(creditCard).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(creditCard).addParent(dynamicVariables.getTemporalClone(creditCard));


        dynamicDAG.getParentSetTimeT(balance).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(balance).addParent(dynamicVariables.getTemporalClone(balance));

        dynamicDAG.getParentSetTimeT(withDraw).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(withDraw).addParent(dynamicVariables.getTemporalClone(withDraw));

        dynamicDAG.getParentSetTimeT(salary).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(salary).addParent(dynamicVariables.getTemporalClone(salary));
        dynamicDAG.getParentSetTimeT(salary).addParent(indicatorSalary);



        dynamicDAG.getParentSetTimeT(monthlyBalance).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(monthlyBalance).addParent(dynamicVariables.getTemporalClone(balance));
        dynamicDAG.getParentSetTimeT(monthlyBalance).addParent(dynamicVariables.getTemporalClone(monthlyBalance));


        dynamicDAG.getParentSetTimeT(movements).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(movements).addParent(creditCard);

        System.out.println(dynamicDAG.toString());

        DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);

        System.out.println(dynamicBayesianNetwork.toString());



        double logProb = 0;

        while (data.hasMoreDataInstances()){
            DataInstance dataInstance = data.nextDataInstance();
            if (dataInstance.getTimeID()==1) {
                logProb += dynamicBayesianNetwork.getLogProbabiltyOfFullAssignmentTime0(dataInstance);
            }else{
                logProb += dynamicBayesianNetwork.getLogProbabiltyOfFullAssignmentTimeT(dataInstance);
            }
        }

        System.out.println(logProb);
    }

    public static void VerdandeInputOutputSKF(){

        DataOnDisk data = new DynamicDataOnDiskFromFile(new WekaDataFileReader(new String("datasets/syntheticDataVerdande.arff")));

        //***************************************** Network structure **************************************************

        Attribute attTRQ = data.getAttributes().getAttributeByName("TRQ");
        Attribute attROP = data.getAttributes().getAttributeByName("ROP");

        List<Attribute> attributeList = new ArrayList();
        attributeList.add(attTRQ);
        attributeList.add(attROP);

        DynamicVariables dynamicVariables = new DynamicVariables();

        Variable observedROP = dynamicVariables.addObservedDynamicVariable(attROP);
        Variable observedTRQ = dynamicVariables.addObservedDynamicVariable(attTRQ);


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
        variableBuilder.setStateSpaceType(StateSpaceType.MULTINOMIAL);
        variableBuilder.setDistributionType(DistType.MULTINOMIAL_LOGISTIC);
        variableBuilder.setNumberOfStates(2);
        Variable normal_Abnormal = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

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

        System.out.println(dynamicDAG.toString());

        DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);

        System.out.println(dynamicBayesianNetwork.toString());

    }

    public static void main(String[] args) {
        DynamicBayesianNetworks.CajaMarLikeModels();
        DynamicBayesianNetworks.VerdandeInputOutputSKF();

    }
}
