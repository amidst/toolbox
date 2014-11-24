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
 * This class contains examples about how we can create CajaMar's dynamic models using the AMIDST Toolbox.
 * It show how to create 2T-DBNs over discrete,
 *
 * Created by andresmasegosa on 22/11/14.
 */
public class CajaMarModels {

    /**
     * In this example, we create the first proposed dynamic model for making predictions
     */
    public static void CajaMarDefaulterPredictor(){

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

        for (DataInstance dataInstance: data){
            if (dataInstance.getTimeID()==0) {
                logProb += dynamicBayesianNetwork.getLogProbabiltyOfFullAssignmentTime0(dataInstance);
            }else{
                logProb += dynamicBayesianNetwork.getLogProbabiltyOfFullAssignmentTimeT(dataInstance);
            }
        }

        System.out.println(logProb);
    }
    public static void main(String[] args) {
        CajaMarModels.CajaMarDefaulterPredictor();
    }
}
