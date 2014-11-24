package eu.amidst.core.huginlink;

import COM.hugin.HAPI.*;
import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by afa on 14/11/14.
 */
public class ConverterToAmidst {


    private BayesianNetwork amidstBN;
    private Domain huginBN;

    public ConverterToAmidst(Domain huginNetwork){
        this.huginBN = huginNetwork;
    }

    public BayesianNetwork getAmidstNetwork() {
        return amidstBN;
    }

    public void setNodesAndParents() throws ExceptionHugin {

        List<Attribute> atts = new ArrayList<>();

        NodeList huginNodes = this.huginBN.getNodes();
        int numNodes = huginNodes.size();

        for(int i=0;i<numNodes;i++){
            Node n = (Node)huginNodes.get(i);
            if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                int numStates = (int)((DiscreteChanceNode)n).getNumberOfStates();
                atts.add(new Attribute(i, n.getName(), "", StateSpaceType.MULTINOMIAL, numStates));
                System.out.println("  Discrete: " + n.getName());
            }
            else if (n.getKind().compareTo(NetworkModel.H_KIND_CONTINUOUS) == 0) {
                atts.add(new Attribute(i, n.getName(), "", StateSpaceType.REAL, 0));
                System.out.println("Continuous: " + n.getName());
            }
        }

        StaticVariables staticVariables = new StaticVariables(new Attributes(atts));
        DAG dag = new DAG(staticVariables);

        StaticVariables amidstVariables = staticVariables;

        for(int i=0;i<huginNodes.size();i++){

            Node huginChild = (Node)huginNodes.get(i);
            NodeList huginParents = huginChild.getParents();
            Variable amidstChild = amidstVariables.getVariableByName(huginChild.getName());

            //Only multinomial parents are indexed in Hugin in a reverse order!!
            List<Integer> positionsMultinomialParents = new ArrayList<>();
            for (int j=0;j<huginParents.size();j++) {
                Node huginParent = (Node) huginParents.get(j);
                if (huginParent.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                    int indexParent = huginNodes.indexOf(huginParent);
                    positionsMultinomialParents.add(indexParent);
                }
            }
            Collections.reverse(positionsMultinomialParents);

            for(int j=0;j<huginParents.size();j++) {
                Node huginParent = (Node) huginParents.get(j);
                int indexParent;
                if (huginParent.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                     indexParent = positionsMultinomialParents.get(j);
                }
                else {
                     indexParent = huginNodes.indexOf(huginParent);
                }
                Variable amidstParent = amidstVariables.getVariableByName(huginChild.getName());
                dag.getParentSet(amidstChild).addParent(amidstParent);
            }
            System.out.print(amidstChild.getName() + " - Parents: ");
            for(int j=0;j<dag.getParentSet(amidstChild).getParents().size();j++)
                System.out.print(dag.getParentSet(amidstChild).getParents().get(j).getName()+ " ");
            System.out.println();
        }
        this.amidstBN = BayesianNetwork.newBayesianNetwork(dag);
    }

    public void setMultinomial_MultinomialParents(Node huginVar) throws ExceptionHugin {

        int indexNode = this.huginBN.getNodes().indexOf(huginVar);
        Variable amidstVar = this.amidstBN.getStaticVariables().getVariableById(indexNode);
        int numStates = amidstVar.getNumberOfStates();

        double[] huginProbabilities = huginVar.getTable().getData();

        List<Variable> parents = this.amidstBN.getDAG().getParentSet(amidstVar).getParents();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(parents);

        //Borrar
        System.out.println("Hugin probabilities:");
        for(int i=0;i<huginProbabilities.length;i++){
            System.out.println(huginProbabilities[i]+ " ");
        }





        int pos=0;
        for(int i=0;i<numParentAssignments;i++){




            double[] amidstProbabilities_i = new double[numStates];
            for(int k=0;k<numStates;k++){
                amidstProbabilities_i[k] = huginProbabilities[i*numStates+k];
            }

            //Borrar
            System.out.println("Amidst probabilities:");

           for(int k=0;k<amidstProbabilities_i.length;k++){
                System.out.println(amidstProbabilities_i[k]);
            }


            Multinomial_MultinomialParents dist = this.amidstBN.getDistribution(amidstVar);
            dist.getMultinomial(i).setProbabilities(amidstProbabilities_i);
            pos = pos+numStates;
        }
    }

    public void setNormal_NormalParents(Node huginVar) throws ExceptionHugin {

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

    public void setNormal(Node huginVar, Normal normal, int i) throws ExceptionHugin {

        double huginMean_i  = ((ContinuousChanceNode)huginVar).getAlpha(i);
        double huginVariance_i  = ((ContinuousChanceNode)huginVar).getGamma(i);

        System.out.println("HUGIN VARIANCE:" + huginVariance_i);

        normal.setMean(huginMean_i);
        normal.setSd(Math.sqrt(huginVariance_i));
    }

    public void setNormal_MultinomialParents(Node huginVar) throws ExceptionHugin {

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

    public void setNormal_MultinomialNormalParents(Node huginVar) throws ExceptionHugin {

        int indexNode = this.huginBN.getNodes().indexOf(huginVar);
        Variable amidstVar = this.amidstBN.getStaticVariables().getVariableById(indexNode);
        Normal_MultinomialNormalParents dist = this.amidstBN.getDistribution(amidstVar);

        List<Variable> multinomialParents = dist.getMultinomialParents();

        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);


        for(int i=0;i<numParentAssignments;i++) {
            Normal_NormalParents normal_normal = dist.getNormal_NormalParentsDistribution(i);

            double huginIntercept = ((ContinuousChanceNode)huginVar).getAlpha(i);
            normal_normal.setIntercept(huginIntercept);

            List<Variable> normalParents = dist.getNormalParents();
            int numParents = normalParents.size();
            double[] coeffs = new double[numParents];

            for(int j=0;j<numParents;j++){
                String nameAmidstNormalParent = normalParents.get(j).getName();
                System.out.println("NORMAL PARENT:"+ nameAmidstNormalParent);
                ContinuousChanceNode huginParent =  (ContinuousChanceNode)this.huginBN.getNodeByName(nameAmidstNormalParent);
                coeffs[j]= ((ContinuousChanceNode)huginVar).getBeta(huginParent,j);
            }
            normal_normal.setCoeffParents(coeffs);

            double huginVariance = ((ContinuousChanceNode)huginVar).getGamma(i);
            normal_normal.setSd(huginVariance);
        }
    }

     public void setDistributions() throws ExceptionHugin {

        NodeList huginNodes = this.huginBN.getNodes();
        StaticVariables amidstVariables = this.amidstBN.getStaticVariables();

        for (int i = 0; i < huginNodes.size(); i++) {

            Variable amidstVar = amidstVariables.getVariableById(i);
            Node huginVar = (Node)huginNodes.get(i);

            System.out.println("-----> " + amidstVar.getName());
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

    public void convertToAmidstBN() throws ExceptionHugin {

        this.setNodesAndParents();
        this.setDistributions();
    }

}










