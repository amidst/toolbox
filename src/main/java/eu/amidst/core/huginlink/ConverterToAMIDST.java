package eu.amidst.core.huginlink;

import COM.hugin.HAPI.*;
import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.variables.*;
import eu.amidst.core.modelstructure.BayesianNetwork;
import eu.amidst.core.modelstructure.DAG;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by afa on 14/11/14.
 */
public class ConverterToAMIDST {


    private BayesianNetwork amidstNetwork;
    private Domain huginNetwork;

    public ConverterToAMIDST (Domain huginNetwork){
        this.huginNetwork = huginNetwork;
    }

    public BayesianNetwork getAmidstNetwork() {
        return amidstNetwork;
    }


    public void setNodes(){

        Attribute att = new Attribute(0,"A","A",StateSpaceType.MULTINOMIAL,2);
        VariableBuilder builder = new VariableBuilder(att);

        List<Attribute> atts = new ArrayList<Attribute>();

        try {

            NodeList huginNodes = this.huginNetwork.getNodes();

            int numNodes = huginNodes.size();

            for(int i=0;i<numNodes;i++){
                Node n = (Node)huginNodes.get(i);

                try {
                    if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                        int numStates = (int)((DiscreteChanceNode)n).getNumberOfStates();
                        atts.add(new Attribute(i, n.getName(), " ", StateSpaceType.MULTINOMIAL, numStates));
                    }
                    else if (n.getKind().compareTo(NetworkModel.H_KIND_CONTINUOUS) == 0) {
                        atts.add(new Attribute(i, n.getName(), n.getName(), StateSpaceType.REAL, 0));
                    }
                }
                catch (ExceptionHugin e) {
                    System.out.println("Exception caught: " + e.getMessage());
                }
            }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }

        StaticVariables modelHeader = new StaticVariables(new Attributes(atts));
        DAG dag = new DAG(modelHeader);
        this.amidstNetwork = BayesianNetwork.newBayesianNetwork(dag);
    }

    public void setStructure(){


        List<Variable> amidstVariables = this.amidstNetwork.getVariables();

        try {
            NodeList huginNodes = this.huginNetwork.getNodes();

            for(int i=0;i<huginNodes.size();i++){

                Node huginChild = (Node)huginNodes.get(i);
                NodeList huginParents = huginChild.getParents();
                Variable amidstChild = amidstVariables.get(i);

                for (int j=0;j<huginParents.size();j++){
                    Node huginParent = (Node)huginParents.get(j);
                    int parentIndex = huginNodes.indexOf(huginParent);
                    Variable amidstParent = amidstVariables.get(parentIndex);
                    this.amidstNetwork.getDAG().getParentSet(amidstChild).addParent(amidstParent);
                }
            }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

    public void setMultinomial_MultinomialParents(Node huginVar) {

        try {
            int indexNode = this.huginNetwork.getNodes().indexOf(huginVar);
            Variable amidstVar = this.amidstNetwork.getVariables().get(indexNode);
            int numStates = amidstVar.getNumberOfStates();

            double[] huginProbabilities = huginVar.getTable().getData();

            List<Variable> parents = this.amidstNetwork.getDAG().getParentSet(amidstVar).getParents();
            int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(parents);

            int pos=0;
            for(int i=0;i<numParentAssignments;i++){
                double[] amidstProbabilities_i = Arrays.copyOfRange(huginProbabilities, pos, numStates*(i+1)-1);
                Multinomial_MultinomialParents dist = this.amidstNetwork.getDistribution(amidstVar);
                dist.getMultinomial(i).setProbabilities(amidstProbabilities_i);
                pos = numStates*(i+1);
            }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

    public void setDistributions(NodeList huginNodes){

        List<Variable> amidstVariables = this.amidstNetwork.getVariables();

        for (int i = 0; i < huginNodes.size(); i++) {

            Variable amidstVar = amidstVariables.get(i);
            List<Variable> amidstParents = this.amidstNetwork.getDAG().getParentSet(amidstVar).getParents();
            Node huginVar = (Node)huginNodes.get(i);

            if (amidstVar.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {

                this.setMultinomial_MultinomialParents(huginVar);
            } else if (amidstVar.getDistributionType().compareTo(DistType.GAUSSIAN) == 0) {

                boolean multinomialParents = false;
                boolean normalParents = false;

                for (Variable amidstParent : amidstParents) {
                    if (amidstParent.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
                        multinomialParents = true;
                    } else if (amidstParent.getDistributionType().compareTo(DistType.GAUSSIAN) == 0) {
                        normalParents = true;
                    } else {
                        throw new IllegalArgumentException("Unrecognized DistributionType. ");
                    }
                }
                if (normalParents && !multinomialParents) {
                    System.out.println("Normal_NormalParents");
                    //this.setNormal_NormalParents(dist,0);
                } else if ((!normalParents & multinomialParents) || (amidstParents.size() == 0)) {
                    System.out.println("Normal_MultinomialParents");
                    //this.setNormal_MultinomialParents(dist);
                } else if (normalParents & multinomialParents) {
                    System.out.println("Normal_MultinomialNormalParents");
                    //this.setNormal_MultinomialNormalParents(dist);
                } else {
                    throw new IllegalArgumentException("Unrecognized DistributionType. ");
                }
            } else {
                throw new IllegalArgumentException("Unrecognized DistributionType. ");
            }
        }
    }

}










