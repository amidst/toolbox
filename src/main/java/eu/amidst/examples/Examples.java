package eu.amidst.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DynamicDataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.variables.*;

import java.util.Arrays;

/**
 * Created by Hanen on 05/03/15.
 */
public class Examples {

    public static void BNExample() throws Exception{

        DataStream<DataInstance> data = DataStreamLoader.loadFromFile("datasets/staticData.arff");

        StaticVariables variables = new StaticVariables(data.getAttributes());

        Variable A = variables.getVariableByName("A");
        Variable B = variables.getVariableByName("B");
        Variable C = variables.getVariableByName("C");
        Variable D = variables.getVariableByName("D");

        Variable H = variables.newMultionomialVariable("H", Arrays.asList("TRUE", "FALSE"));

        DAG dag = new DAG(variables);

        dag.getParentSet(A).addParent(H);
        dag.getParentSet(B).addParent(H);
        dag.getParentSet(C).addParent(H);
        dag.getParentSet(D).addParent(H);

        System.out.println(dag.toString());

        BayesianNetwork bnet = BayesianNetwork.newBayesianNetwork(dag);
        System.out.println("-- CPDs are automatically created --");
        System.out.println(bnet.toString());

        Multinomial_MultinomialParents distA = bnet.getDistribution(A);
        Assignment parentConf = new HashMapAssignment(H.getNumberOfStates());
        parentConf.setValue(H, 0);
        distA.getMultinomial(parentConf).setProbabilities(new double[]{0.7, 0.3});
        parentConf.setValue(H, 1);
        distA.getMultinomial(parentConf).setProbabilities(new double[]{0.2, 0.8});

        Normal_MultinomialParents distC = bnet.getDistribution(C);
        parentConf.setValue(H, 0);
        distC.getNormal(0).setMean(0.15);
        distC.getNormal(0).setVariance(0.25);
        parentConf.setValue(H, 1);
        distC.getNormal(1).setMean(0.24);
        distC.getNormal(1).setMean(1);

        System.out.println("-- After modifying CPDs manually --");
        System.out.println(bnet.toString());

    }


    public static void DBNExample() throws Exception {

        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile("datasets/dynamicData.arff");

        DynamicVariables dynamicVariables = new DynamicVariables(data.getAttributes());

        Variable A = dynamicVariables.getVariable("A");
        Variable B = dynamicVariables.getVariable("B");
        Variable C = dynamicVariables.getVariable("C");
        Variable D = dynamicVariables.getVariable("D");

        Variable H = dynamicVariables.newMultinomialDynamicVariable("H",Arrays.asList("TRUE", "FALSE"));

        // Time 0: Parents at time 0 are automatically created when adding parents at time t, the structure can be
        //         modified in any case.
        // Time t

        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(B).addParent(H);
        dynamicDAG.getParentSetTimeT(C).addParent(H);
        dynamicDAG.getParentSetTimeT(D).addParent(H);
        dynamicDAG.getParentSetTimeT(H).addParent(A);
        dynamicDAG.getParentSetTimeT(A).addParent(A.getInterfaceVariable());
        dynamicDAG.getParentSetTimeT(H).addParent(H.getInterfaceVariable());

        dynamicDAG.getParentSetTime0(B).addParent(A);
        dynamicDAG.getParentSetTime0(C).addParent(A);
        dynamicDAG.getParentSetTime0(B).removeParent(H);

        System.out.println(dynamicDAG.toString());

        DynamicBayesianNetwork dynamicbnet = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);

        System.out.println("-- CPDs are automatically created --");
        System.out.println(dynamicbnet.toString());


        Multinomial distA = dynamicbnet.getDistributionTime0(A);
        distA.setProbabilities(new double[]{0.1, 0.9});

        Normal_MultinomialParents distC = dynamicbnet.getDistributionTime0(C);

        Assignment parentConf = new HashMapAssignment(H.getNumberOfStates()*A.getNumberOfStates());


        parentConf.setValue(H, 0);
        parentConf.setValue(A, 0);
        distC.getNormal(parentConf).setMean(0.7);
        distC.getNormal(parentConf).setVariance(0.04);


        parentConf.setValue(H, 1);
        parentConf.setValue(A, 0);
        distC.getNormal(parentConf).setMean(0.4);
        distC.getNormal(parentConf).setVariance(1);


        parentConf.setValue(H, 0);
        parentConf.setValue(A, 1);
        distC.getNormal(parentConf).setMean(0.75);
        distC.getNormal(parentConf).setVariance(0.0025);


        parentConf.setValue(H, 1);
        parentConf.setValue(A, 1);
        distC.getNormal(parentConf).setMean(0.66);
        distC.getNormal(parentConf).setVariance(0.0016);

        System.out.println("-- After modifying CPDs manually --");
        System.out.println(dynamicbnet.toString());

    }


    public static void main(String[] args) throws Exception {

        System.out.println("-------------------------------");
        System.out.println("-----Example for STATIC BN-----");
        System.out.println("-------------------------------");
        Examples.BNExample();

        System.out.println("--------------------------------");
        System.out.println("-----Example for DYNAMIC BN-----");
        System.out.println("--------------------------------");
        Examples.DBNExample();
    }
}
