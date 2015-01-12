package eu.amidst.core.huginlink;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.distribution.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.*;
import org.omg.CORBA.PUBLIC_MEMBER;

import java.util.List;

/**
 * Created by afa on 7/1/15.
 */
public class DBNConverterToHugin {

    // The dynamic BN in Hugin
    private Class huginDBN;

    public DBNConverterToHugin() throws ExceptionHugin {
        huginDBN = new Class(new ClassCollection()); // I'm not sure about creating a new ClassCollection but it works!
    }

    private void setVariables(DynamicBayesianNetwork amidstDBN) throws ExceptionHugin {

        DynamicVariables dynamicVars = amidstDBN.getDynamicVariables();
        int size = dynamicVars.getNumberOfVars();

        //Hugin always inserts variables in position 0, i.e, for an order A,B,C, it stores C,B,A !!!
        //A reverse order of the variables is used instead.
        for(int i=1;i<=size;i++){
            Variable amidstVar = dynamicVars.getVariableById(size-i);
            LabelledDCNode n = new LabelledDCNode(huginDBN);

            n.setName(amidstVar.getName());
            n.setNumberOfStates(amidstVar.getNumberOfStates());
            n.setLabel(amidstVar.getName());

            for (int j=0;j<n.getNumberOfStates();j++){
                    String stateName = ((FiniteStateSpace)amidstVar.getStateSpace()).getStatesName(j);
                    n.setStateLabel(j, stateName);
            }
        }
     }

    private void setTemporalClones(DynamicBayesianNetwork amidstDBN) throws ExceptionHugin {

        DynamicDAG dynamicDAG = amidstDBN.getDynamicDAG();
        List<Variable> amidstVars = amidstDBN.getDynamicVariables().getListOfDynamicVariables();

        for (Variable amidstChild: amidstVars){
            List<Variable> amidstParents = dynamicDAG.getParentSetTimeT(amidstChild).getParents();
            Node huginChild = this.huginDBN.getNodeByName(amidstChild.getName());
            for(Variable amidstParent: amidstParents) {
                if(amidstParent.isTemporalClone()) {
                    Node clone = huginChild.createTemporalClone();
                    clone.setName("T_"+huginChild.getName());
                    clone.setLabel("T_"+huginChild.getLabel());
                }
            }
        }
    }

    private void setStructure (DynamicBayesianNetwork amidstDBN) throws ExceptionHugin {

        DynamicDAG dynamicDAG = amidstDBN.getDynamicDAG();

        DynamicVariables dynamicVariables = amidstDBN.getDynamicVariables();
        List<Variable> amidstVars = dynamicVariables.getListOfDynamicVariables();

        for (Variable amidstChild: amidstVars){
            List<Variable> amidstParents = dynamicDAG.getParentSetTimeT(amidstChild).getParents();
            Node huginChild = this.huginDBN.getNodeByName(amidstChild.getName());
            for(Variable amidstParent: amidstParents) {
                if(amidstParent.isTemporalClone()) {
                   huginChild.addParent(huginChild.getTemporalClone());
                }
                else { //Variable
                    Node huginParent = this.huginDBN.getNodeByName(amidstParent.getName());
                    huginChild.addParent(huginParent);
                }
            }
      }
    }

    private void setDistributions(DynamicBayesianNetwork amidstDBN) throws ExceptionHugin {

        List<Variable> amidstVars = amidstDBN.getDynamicVariables().getListOfDynamicVariables();

        for (Variable amidstVar : amidstVars) {

            if (amidstVar.getDistributionType().compareTo(DistType.MULTINOMIAL) != 0)
                throw new IllegalArgumentException("Only multinomial distributions are allowed");

            Node huginVar = this.huginDBN.getNodeByName(amidstVar.getName());
            Multinomial_MultinomialParents dist = amidstDBN.getDistributionTimeT(amidstVar);

            Multinomial[] probabilities = dist.getProbabilities();
            List<Variable> conditioningVariables = dist.getConditioningVariables();
            int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);
            int nStates = amidstVar.getNumberOfStates();
            int sizeArray = numParentAssignments * nStates;
            double[] finalArray = new double[sizeArray];

            for (int i = 0; i < numParentAssignments; i++) {
                double[] sourceArray = probabilities[i].getProbabilities();
                System.arraycopy(sourceArray, 0, finalArray, i * nStates, nStates);
            }
            huginVar.getTable().setData(finalArray);

            // Set the distributions for those variables with temporal clones
            if (huginVar.getTemporalClone() != null) {
                Multinomial_MultinomialParents distClone = amidstDBN.getDistributionTime0(amidstVar);
                finalArray = distClone.getProbabilities()[0].getProbabilities();
                huginVar.getTemporalClone().getTable().setData(finalArray);
            }
        }
    }

    public static Class convertToHugin(DynamicBayesianNetwork amidstDBN) throws ExceptionHugin {

        DBNConverterToHugin DBNconverterToHugin  = new DBNConverterToHugin();
        DBNconverterToHugin.setVariables(amidstDBN);
        DBNconverterToHugin.setTemporalClones(amidstDBN);
        DBNconverterToHugin.setStructure(amidstDBN);
        DBNconverterToHugin.setDistributions(amidstDBN);

        return DBNconverterToHugin.huginDBN;
    }
}
