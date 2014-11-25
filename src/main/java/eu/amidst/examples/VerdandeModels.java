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
 * Created by andresmasegosa on 22/11/14.
 */
public class VerdandeModels {

    public static void VerdandeInputOutputSKF(){

        DataOnDisk data = new DynamicDataOnDiskFromFile(new WekaDataFileReader(new String("datasets/syntheticDataVerdandeScenario1.arff")));

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
        variableBuilder.setStateSpaceType(StateSpaceType.FINITE_SET);
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

    public static void VerdandeInputOutputKFwithMG(){

        DataOnDisk data = new DynamicDataOnDiskFromFile(new WekaDataFileReader(new String("datasets/syntheticDataVerdandeScenario2.arff")));

        //***************************************** Network structure **************************************************
        /* Figure 4.29 in D2.1 */

        /* Control variables */
        Attribute attWOB = data.getAttributes().getAttributeByName("WOB");
        Attribute attRPM = data.getAttributes().getAttributeByName("RPMB");
        Attribute attMFI = data.getAttributes().getAttributeByName("MFI");

        /* Response variables */
        Attribute attTRQ = data.getAttributes().getAttributeByName("TRQ");
        Attribute attROP = data.getAttributes().getAttributeByName("ROP");
        Attribute attPRESSURE = data.getAttributes().getAttributeByName("PRESSURE");

        List<Attribute> attributeList = new ArrayList();
        attributeList.add(attWOB);
        attributeList.add(attRPM);
        attributeList.add(attMFI);
        attributeList.add(attTRQ);
        attributeList.add(attROP);
        attributeList.add(attPRESSURE);

        DynamicVariables dynamicVariables = new DynamicVariables();

        Variable observedWOB = dynamicVariables.addObservedDynamicVariable(attWOB);
        Variable observedRPMB = dynamicVariables.addObservedDynamicVariable(attRPM);
        Variable observedMFI = dynamicVariables.addObservedDynamicVariable(attMFI);
        Variable observedROP = dynamicVariables.addObservedDynamicVariable(attTRQ);
        Variable observedTRQ = dynamicVariables.addObservedDynamicVariable(attROP);
        Variable observedPRESSURE = dynamicVariables.addObservedDynamicVariable(attPRESSURE);


        /* In Figure 4.29, these 3 variables are part of the subnetwork Continuous */
        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setName("realWOB");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpaceType(StateSpaceType.REAL);
        variableBuilder.setDistributionType(DistType.GAUSSIAN);
        Variable realTRQ = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        variableBuilder = new VariableBuilder();
        variableBuilder.setName("realRPM");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpaceType(StateSpaceType.REAL);
        variableBuilder.setDistributionType(DistType.GAUSSIAN);
        Variable realROP = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        variableBuilder = new VariableBuilder();
        variableBuilder.setName("realMFI");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpaceType(StateSpaceType.REAL);
        variableBuilder.setDistributionType(DistType.GAUSSIAN);
        Variable realPRESSURE = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        variableBuilder = new VariableBuilder();
        variableBuilder.setName("HiddenVar");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpaceType(StateSpaceType.REAL);
        variableBuilder.setDistributionType(DistType.GAUSSIAN);
        Variable hidden = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        variableBuilder = new VariableBuilder();
        variableBuilder.setName("Mixture");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpaceType(StateSpaceType.FINITE_SET);
        variableBuilder.setDistributionType(DistType.MULTINOMIAL_LOGISTIC);
        variableBuilder.setNumberOfStates(2);
        Variable mixture = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(observedMFI);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(realTRQ);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(hidden);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(mixture);

        dynamicDAG.getParentSetTimeT(observedROP).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(observedROP).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(observedROP).addParent(observedMFI);
        dynamicDAG.getParentSetTimeT(observedROP).addParent(realROP);
        dynamicDAG.getParentSetTimeT(observedROP).addParent(hidden);
        dynamicDAG.getParentSetTimeT(observedROP).addParent(mixture);

        dynamicDAG.getParentSetTimeT(observedPRESSURE).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(observedPRESSURE).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(observedPRESSURE).addParent(observedMFI);
        dynamicDAG.getParentSetTimeT(observedPRESSURE).addParent(realPRESSURE);
        dynamicDAG.getParentSetTimeT(observedPRESSURE).addParent(hidden);
        dynamicDAG.getParentSetTimeT(observedPRESSURE).addParent(mixture);


        dynamicDAG.getParentSetTimeT(realTRQ).addParent(dynamicVariables.getTemporalClone(realTRQ));
        dynamicDAG.getParentSetTimeT(realTRQ).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(realTRQ).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(realTRQ).addParent(observedMFI);

        dynamicDAG.getParentSetTimeT(realROP).addParent(dynamicVariables.getTemporalClone(realROP));
        dynamicDAG.getParentSetTimeT(realROP).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(realROP).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(realROP).addParent(observedMFI);

        dynamicDAG.getParentSetTimeT(realPRESSURE).addParent(dynamicVariables.getTemporalClone(realTRQ));
        dynamicDAG.getParentSetTimeT(realPRESSURE).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(realPRESSURE).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(realPRESSURE).addParent(observedMFI);

        dynamicDAG.getParentSetTimeT(hidden).addParent(dynamicVariables.getTemporalClone(hidden));
        dynamicDAG.getParentSetTimeT(hidden).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(hidden).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(hidden).addParent(observedMFI);

        dynamicDAG.getParentSetTimeT(mixture).addParent(dynamicVariables.getTemporalClone(mixture));
        dynamicDAG.getParentSetTimeT(mixture).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(mixture).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(mixture).addParent(observedMFI);


        System.out.println(dynamicDAG.toString());

        DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);

        System.out.println(dynamicBayesianNetwork.toString());

    }




    public static void main(String[] args) {
        VerdandeModels.VerdandeInputOutputSKF();
        VerdandeModels.VerdandeInputOutputKFwithMG();
    }
}
