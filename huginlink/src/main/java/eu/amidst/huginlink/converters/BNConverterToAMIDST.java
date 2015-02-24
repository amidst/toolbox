package eu.amidst.huginlink.converters;

import COM.hugin.HAPI.*;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.distribution.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by afa on 14/11/14.
 */
public class BNConverterToAMIDST {

    private BayesianNetwork amidstBN;
    private Domain huginBN;

    public BNConverterToAMIDST(Domain huginBN_){
        this.huginBN = huginBN_;
    }

    private void setNodesAndParents() throws ExceptionHugin {

        List<Attribute> atts = new ArrayList<>();

        NodeList huginNodes = this.huginBN.getNodes();
        int numNodes = huginNodes.size();

        for(int i=0;i<numNodes;i++){
            Node n = (Node)huginNodes.get(i);
            if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                int numStates = (int)((DiscreteChanceNode)n).getNumberOfStates();
                atts.add(new Attribute(i, n.getName(), "", StateSpaceType.FINITE_SET, numStates));
            }
            else if (n.getKind().compareTo(NetworkModel.H_KIND_CONTINUOUS) == 0) {
                atts.add(new Attribute(i, n.getName(), "", StateSpaceType.REAL, 0));
            }
        }
        StaticVariables staticVariables = new StaticVariables(new Attributes(atts));
        DAG dag = new DAG(staticVariables);

        StaticVariables amidstVariables = staticVariables;

        for(int i=0;i<numNodes;i++){
            Node huginChild = huginNodes.get(i);
            NodeList huginParents = huginChild.getParents();
            Variable amidstChild = amidstVariables.getVariableByName(huginChild.getName());


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
                Node huginParent = huginParents.get(parentsIndexes.get(j));
                Variable amidstParent = amidstVariables.getVariableByName(huginParent.getName());
                dag.getParentSet(amidstChild).addParent(amidstParent);
            }
        }
        this.amidstBN = BayesianNetwork.newBayesianNetwork(dag);
    }

    private void setMultinomial_MultinomialParents(Node huginVar) throws ExceptionHugin {

        int indexNode = this.huginBN.getNodes().indexOf(huginVar);
        Variable amidstVar = this.amidstBN.getStaticVariables().getVariableById(indexNode);
        int numStates = amidstVar.getNumberOfStates();

        double[] huginProbabilities = huginVar.getTable().getData();

        List<Variable> parents = this.amidstBN.getDAG().getParentSet(amidstVar).getParents();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(parents);

       // int pos=0;
        for(int i=0;i<numParentAssignments;i++){

            double[] amidstProbabilities = new double[numStates];
            for(int k=0;k<numStates;k++){
                amidstProbabilities[k] = huginProbabilities[i*numStates+k];
            }
            Multinomial_MultinomialParents dist = this.amidstBN.getDistribution(amidstVar);
            dist.getMultinomial(i).setProbabilities(amidstProbabilities);
          //  pos = pos+numStates;
        }
    }

    private void setNormal_NormalParents(Node huginVar) throws ExceptionHugin {

        int indexNode = this.huginBN.getNodes().indexOf(huginVar);
        Variable amidstVar = this.amidstBN.getStaticVariables().getVariableById(indexNode);
        Normal_NormalParents dist = this.amidstBN.getDistribution(amidstVar);

        double huginIntercept = ((ContinuousChanceNode)huginVar).getAlpha(0);
        dist.setIntercept(huginIntercept);

        NodeList huginParents = huginVar.getParents();
        int numParents = huginParents.size();
        double[] coeffs = new double[numParents];

        for(int i=0;i<numParents;i++){
            ContinuousChanceNode huginParent = (ContinuousChanceNode)huginParents.get(i);
            coeffs[i]= ((ContinuousChanceNode)huginVar).getBeta(huginParent,0);
        }
        dist.setCoeffParents(coeffs);

        double huginVariance = ((ContinuousChanceNode)huginVar).getGamma(0);
        dist.setSd(Math.sqrt(huginVariance));
    }

    private void setNormal(Node huginVar, Normal normal, int i) throws ExceptionHugin {

        double huginMean  = ((ContinuousChanceNode)huginVar).getAlpha(i);
        double huginVariance  = ((ContinuousChanceNode)huginVar).getGamma(i);
        normal.setMean(huginMean);
        normal.setSd(Math.sqrt(huginVariance));
    }

    private void setNormal_MultinomialParents(Node huginVar) throws ExceptionHugin {

        int indexNode = this.huginBN.getNodes().indexOf(huginVar);
        Variable amidstVar = this.amidstBN.getStaticVariables().getVariableById(indexNode);
        Normal_MultinomialParents dist = this.amidstBN.getDistribution(amidstVar);

        List<Variable> conditioningVariables = dist.getConditioningVariables();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);

        for(int i=0;i<numParentAssignments;i++) {
            Normal normal = dist.getNormal(i);
            this.setNormal(huginVar,normal, i);
        }
    }

    private void setNormal_MultinomialNormalParents(Node huginVar) throws ExceptionHugin {

        int indexNode = this.huginBN.getNodes().indexOf(huginVar);
        Variable amidstVar = this.amidstBN.getStaticVariables().getVariableById(indexNode);
        Normal_MultinomialNormalParents dist = this.amidstBN.getDistribution(amidstVar);

        List<Variable> multinomialParents = dist.getMultinomialParents();

        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);

        for(int i=0;i<numParentAssignments;i++) {

            Normal_NormalParents normalNormal = dist.getNormal_NormalParentsDistribution(i);

            double huginIntercept = ((ContinuousChanceNode)huginVar).getAlpha(i);
            normalNormal.setIntercept(huginIntercept);

            List<Variable> normalParents = dist.getNormalParents();
            int numParents = normalParents.size();
            double[] coeffs = new double[numParents];

            for(int j=0;j<numParents;j++){
                String nameAmidstNormalParent = normalParents.get(j).getName();
                ContinuousChanceNode huginParent =  (ContinuousChanceNode)this.huginBN.getNodeByName(nameAmidstNormalParent);
                coeffs[j]= ((ContinuousChanceNode)huginVar).getBeta(huginParent,i);
            }
            normalNormal.setCoeffParents(coeffs);

            double huginVariance = ((ContinuousChanceNode)huginVar).getGamma(i);
            normalNormal.setSd(Math.sqrt(huginVariance));
        }
    }

    private void setDistributions() throws ExceptionHugin {

        NodeList huginNodes = this.huginBN.getNodes();
        StaticVariables amidstVariables = this.amidstBN.getStaticVariables();

        for (int i = 0; i < huginNodes.size(); i++) {

            Variable amidstVar = amidstVariables.getVariableById(i);
            Node huginVar = (Node)huginNodes.get(i);

            int type = Utils.getConditionalDistributionType(amidstVar, amidstBN);

            switch (type) {
                case 0:
                    this.setMultinomial_MultinomialParents(huginVar);
                    break;
                case 1:
                    this.setNormal_NormalParents(huginVar);
                    break;
                case 2:
                    this.setNormal_MultinomialParents(huginVar);
                    break;
                case 3:
                    this.setNormal_MultinomialNormalParents(huginVar);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized DistributionType. ");
            }
        }
    }

    public static BayesianNetwork convertToAmidst(Domain huginBN) throws ExceptionHugin {

        BNConverterToAMIDST converterToAMIDST = new BNConverterToAMIDST(huginBN);
        converterToAMIDST.setNodesAndParents();
        converterToAMIDST.setDistributions();

        return converterToAMIDST.amidstBN;
    }

}









