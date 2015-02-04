package eu.amidst.huginlink;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.FiniteStateSpace;
import eu.amidst.core.variables.Variable;

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



    private void setNodesAndTemporalClones(DynamicBayesianNetwork amidstDBN) throws ExceptionHugin {

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
            Node huginVar = this.huginDBN.getNodeByName(amidstVar.getName());
            Node clone = huginVar.createTemporalClone();
            clone.setName("T_"+huginVar.getName());
            clone.setLabel("T_"+huginVar.getLabel());
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
                    Node huginParentClone = huginParent.getTemporalClone();
                    Node huginChildClone = huginChild.getTemporalClone();
                    huginChildClone.addParent(huginParentClone);
                }
            }
        }
    }

    private void setDistributions(DynamicBayesianNetwork amidstDBN) throws ExceptionHugin {

        NodeList huginNodes = this.huginDBN.getNodes();
        int numNodes = huginNodes.size();
        Multinomial_MultinomialParents dist = null;
        int nStates=0;
        for (int i = 0; i < numNodes; i++) {
            Node huginNode = huginNodes.get(i);

            //Master nodes. TIME T from AMIDST
            if (huginNode.getTemporalMaster() == null) {
                Variable amidstVar = amidstDBN.getDynamicVariables().getVariableByName(huginNode.getName());
                dist = amidstDBN.getDistributionTimeT(amidstVar);
                nStates = amidstVar.getNumberOfStates();
            }

            //Temporal clones. TIME 0 from AMIDST
            if(huginNode.getTemporalClone()==null){
                Variable amidstVar = amidstDBN.getDynamicVariables().getVariableByName(huginNode.getTemporalMaster().getName());
                dist = amidstDBN.getDistributionTime0(amidstVar);
                nStates = amidstVar.getNumberOfStates();
            }

            Multinomial[] probabilities = dist.getProbabilities();
            List<Variable> conditioningVariables = dist.getConditioningVariables();
            int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);

            int sizeArray = numParentAssignments * nStates;
            double[] finalArray = new double[sizeArray];

            for (int j = 0; j < numParentAssignments; j++) {
                double[] sourceArray = probabilities[j].getProbabilities();
                System.arraycopy(sourceArray, 0, finalArray, j * nStates, nStates);
            }
            huginNode.getTable().setData(finalArray);
        }
    }

    public static Class convertToHugin(DynamicBayesianNetwork amidstDBN) throws ExceptionHugin {

        DBNConverterToHugin DBNconverterToHugin  = new DBNConverterToHugin();
        DBNconverterToHugin.setNodesAndTemporalClones(amidstDBN);
        DBNconverterToHugin.setStructure(amidstDBN);
        DBNconverterToHugin.setDistributions(amidstDBN);

        return DBNconverterToHugin.huginDBN;
    }
}
