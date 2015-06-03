package eu.amidst.examples.huginlink.converters;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.examples.core.distribution.Multinomial;
import eu.amidst.examples.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.examples.core.models.DynamicBayesianNetwork;
import eu.amidst.examples.core.models.DynamicDAG;
import eu.amidst.examples.core.utils.MultinomialIndex;
import eu.amidst.examples.core.variables.DynamicVariables;
import eu.amidst.examples.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.examples.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * This class converts a Dynamic Bayesian network model from AMIDST to Hugin. It works only for multinomial
 * distributions.
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 7/1/15
 */
public class DBNConverterToHugin {

    /**
     * The Dynamic Bayesian network model in Hugin format.
     */
    private Class huginDBN;

    /**
     * Class constructor.
     *
     * @throws ExceptionHugin
     */
    public DBNConverterToHugin() throws ExceptionHugin {
        huginDBN = new Class(new ClassCollection());
    }

    /**
     * Sets the nodes and temporal clones (if exist) in the Hugin network from the AMIDST model.
     *
     * @param amidstDBN the Dynamic Bayesian network model in AMIDST format.
     * @throws ExceptionHugin
     */
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
                    String stateName = ((FiniteStateSpace)amidstVar.getStateSpaceType()).getStatesName(j);
                    n.setStateLabel(j, stateName);
            }
            Node huginVar = this.huginDBN.getNodeByName(amidstVar.getName());
            Node clone = huginVar.createTemporalClone();
            clone.setName("T_"+huginVar.getName());
            clone.setLabel("T_"+huginVar.getLabel());
        }
     }

    /**
     * Sets the Hugin model structure from the AMIDST Dynamic DAG.
     *
     * @param amidstDBN the Dynamic Bayesian network model in AMIDST format.
     * @throws ExceptionHugin
     */
    private void setStructure (DynamicBayesianNetwork amidstDBN) throws ExceptionHugin {

        DynamicDAG dynamicDAG = amidstDBN.getDynamicDAG();

        DynamicVariables dynamicVariables = amidstDBN.getDynamicVariables();
        List<Variable> amidstVars = dynamicVariables.getListOfDynamicVariables();

        for (Variable amidstChild: amidstVars){
            List<Variable> amidstParents = dynamicDAG.getParentSetTimeT(amidstChild).getParents();
            Node huginChild = this.huginDBN.getNodeByName(amidstChild.getName());
            for(Variable amidstParent: amidstParents) {
                if(amidstParent.isInterfaceVariable()) {
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

    /**
     * Sets the distributions for all the variables in the Hugin network from the distributions in the AMIDST model.
     * Note that only multinomial distributions are allowed in this conversion.
     *
     * @param amidstDBN the Dynamic Bayesian network model in AMIDST format.
     * @throws ExceptionHugin
     */
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
                if (amidstDBN.getConditionalDistributionTimeT(amidstVar) instanceof Multinomial){
                    dist = new Multinomial_MultinomialParents(amidstVar,new ArrayList());
                    dist.setMultinomial(0,amidstDBN.getConditionalDistributionTimeT(amidstVar));
                }else {
                    dist = amidstDBN.getConditionalDistributionTimeT(amidstVar);
                }
                nStates = amidstVar.getNumberOfStates();
            }

            //Temporal clones. TIME 0 from AMIDST
            if(huginNode.getTemporalClone()==null){
                Variable amidstVar = amidstDBN.getDynamicVariables().getVariableByName(huginNode.getTemporalMaster().getName());
                if (amidstDBN.getConditionalDistributionTime0(amidstVar) instanceof Multinomial){
                    dist = new Multinomial_MultinomialParents(amidstVar,new ArrayList());
                    dist.setMultinomial(0,amidstDBN.getConditionalDistributionTime0(amidstVar));
                }else {
                    dist = amidstDBN.getConditionalDistributionTime0(amidstVar);
                }
                nStates = amidstVar.getNumberOfStates();
            }

            List<Multinomial> probabilities = dist.getMultinomialDistributions();
            List<Variable> conditioningVariables = dist.getConditioningVariables();
            int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);

            int sizeArray = numParentAssignments * nStates;
            double[] finalArray = new double[sizeArray];

            for (int j = 0; j < numParentAssignments; j++) {
                double[] sourceArray = probabilities.get(j).getProbabilities();
                System.arraycopy(sourceArray, 0, finalArray, j * nStates, nStates);
            }
            huginNode.getTable().setData(finalArray);
        }
    }

    /**
     * Converts a Dynamic Bayesian network from AMIDST to Hugin format.
     * @param amidstDBN the AMIDST Dynamic Bayesian network to be converted.
     * @return the converted Hugin Dynamic Bayesian network.
     * @throws ExceptionHugin
     */
    public static Class convertToHugin(DynamicBayesianNetwork amidstDBN) throws ExceptionHugin {

        DBNConverterToHugin DBNconverterToHugin  = new DBNConverterToHugin();
        DBNconverterToHugin.setNodesAndTemporalClones(amidstDBN);
        DBNconverterToHugin.setStructure(amidstDBN);
        DBNconverterToHugin.setDistributions(amidstDBN);

        return DBNconverterToHugin.huginDBN;
    }
}
