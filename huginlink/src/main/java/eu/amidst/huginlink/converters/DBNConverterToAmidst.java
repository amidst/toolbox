package eu.amidst.huginlink.converters;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by afa on 7/1/15.
 */
public class DBNConverterToAmidst {

    private DynamicBayesianNetwork amidstDBN;
    private Class huginDBN;

    public DBNConverterToAmidst(Class huginDBN_){
        this.huginDBN = huginDBN_;
    }

    private void setNodesAndParents() throws ExceptionHugin {

        List<Attribute> atts = new ArrayList<>();

        NodeList huginNodes = this.huginDBN.getNodes();
        int numNodes = huginNodes.size();

        for(int i=0;i<numNodes;i++){

            Node n = (Node)huginNodes.get(i);
            if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) != 0)
                throw new IllegalArgumentException("Only multinomial distributions are allowed.");

            //Only temporal master nodes
            if (n.getTemporalMaster()==null) {
                int numStates = (int) ((DiscreteChanceNode) n).getNumberOfStates();
                atts.add(new Attribute(i, n.getName(), "", StateSpaceType.FINITE_SET, numStates));
            }
        }

        Attributes attributes = new Attributes(atts);
        DynamicVariables dynamicVariables = new DynamicVariables(attributes);
        DynamicDAG dynamicDAG  = new DynamicDAG(dynamicVariables);

        // Set the ParentSet at time T. ParentSet at time 0 are automatically created at the same time.
        for(int i=0;i<numNodes;i++){
            Node huginChild = (Node)huginNodes.get(i);
            if(huginChild.getTemporalMaster()==null){ //Only master nodes
                Variable amidstChild = dynamicVariables.getVariableByName(huginChild.getName());
                NodeList huginParents = huginChild.getParents();

                // Only multinomial parents are indexed in reverse order in Hugin
                //-----------------------------------------------------------------------------
                ArrayList<Integer> multinomialParentsIndexes = new ArrayList();
                for (int j=0;j<huginParents.size();j++) {
                    Node huginParent = huginParents.get(j);
                    if (huginParent.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                        multinomialParentsIndexes.add(j);
                    }
                }
                Collections.reverse(multinomialParentsIndexes);
                ArrayList<Integer> parentsIndexes = new ArrayList();
                for (int j=0;j<huginParents.size();j++) {
                    Node huginParent = huginParents.get(j);
                    if (huginParent.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                        parentsIndexes.add(multinomialParentsIndexes.get(0));
                        multinomialParentsIndexes.remove(0);
                    }
                    else {
                        parentsIndexes.add(j);
                    }
                }
                //-----------------------------------------------------------------------------

                for(int j=0;j<huginParents.size();j++) {
                    Node huginParent = (Node) huginParents.get(parentsIndexes.get(j));
                    if(huginParent.getTemporalMaster()==null){
                        Variable amidstParent = dynamicVariables.getVariableByName(huginParent.getName());
                        dynamicDAG.getParentSetTimeT(amidstChild).addParent(amidstParent);
                    }
                    else {
                        Variable amidstClone = dynamicVariables.getTemporalClone(amidstChild);
                        dynamicDAG.getParentSetTimeT(amidstChild).addParent(amidstClone);
                    }
                }
            }
        }
        this.amidstDBN = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);
    }

    private void setDistributions() throws ExceptionHugin {

        List<Variable> amidstVars = amidstDBN.getDynamicVariables().getListOfDynamicVariables();

        for (Variable amidstVar : amidstVars) {

            Node huginVar = this.huginDBN.getNodeByName(amidstVar.getName());
            Node huginTemporalClone = huginVar.getTemporalClone();

            //************************************ TIME T *****************************************************
            double[] huginProbabilitiesTimeT = huginVar.getTable().getData();
            Multinomial_MultinomialParents dist_TimeT = amidstDBN.getDistributionTimeT(amidstVar);
            List<Variable> parentsTimeT = amidstDBN.getDynamicDAG().getParentSetTimeT(amidstVar).getParents();
            int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(parentsTimeT);
            int numStates = amidstVar.getNumberOfStates();
            for (int i = 0; i < numParentAssignments; i++) {
                double[] amidstProbabilities = new double[numStates];
                for (int k = 0; k < numStates; k++) {
                    amidstProbabilities[k] = huginProbabilitiesTimeT[i * numStates + k];
                }
                dist_TimeT.getMultinomial(i).setProbabilities(amidstProbabilities);
            }
            //************************************ TIME 0 *****************************************************
            double[] huginProbabilitiesTime0 = huginTemporalClone.getTable().getData();
            Multinomial_MultinomialParents dist_Time0 = amidstDBN.getDistributionTime0(amidstVar);
            List<Variable> parentsTime0 = amidstDBN.getDynamicDAG().getParentSetTime0(amidstVar).getParents();
            int numParentAssignmentsTime0 = MultinomialIndex.getNumberOfPossibleAssignments(parentsTime0);
            for (int i = 0; i < numParentAssignmentsTime0; i++) {
                double[] amidstProbabilities = new double[numStates];
                for (int k = 0; k < numStates; k++) {
                    amidstProbabilities[k] = huginProbabilitiesTime0[i * numStates + k];
                }
                dist_Time0.getMultinomial(i).setProbabilities(amidstProbabilities);
            }
        }
    }

    public static DynamicBayesianNetwork convertToAmidst(Class huginDBN) throws ExceptionHugin {

        DBNConverterToAmidst DBNconverterToAMIDST = new DBNConverterToAmidst(huginDBN);
        DBNconverterToAMIDST.setNodesAndParents();
        DBNconverterToAMIDST.setDistributions();
        return DBNconverterToAMIDST.amidstDBN;
    }
}
