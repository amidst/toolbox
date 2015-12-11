package eu.amidst.dynamic.utils;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.VariableBuilder;
import eu.amidst.core.variables.Variables;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by dario on 16/11/15.
 */
public class DynamicToStaticBNConverter {

    DynamicBayesianNetwork dbn;
    BayesianNetwork bn;
    int nTimeSteps = 2;

//    public void setDynamicBayesianNetwork(DynamicBayesianNetwork dbn) {
//        this.dbn = dbn;
//    }
//
//    public void setNumberOfTimeSteps(int nTimeSteps) {
//        this.nTimeSteps = nTimeSteps;
//    }
//
//    public BayesianNetwork getBayesianNetwork() {
//        return bn;
//    }

    public static BayesianNetwork convertDBNtoBN(DynamicBayesianNetwork dbn, int nTimeSteps) {


        if (dbn==null)
            return null;

        DynamicVariables dynamicVariables = dbn.getDynamicVariables();
        Variables variables = new Variables();
        DynamicDAG dynamicDAG = dbn.getDynamicDAG();

        /*
         * CREATE STATIC DAG FROM THE DYNAMIC DAG
         * 1st STEP: ADD REPLICATED VARIABLES
         */
        // REPLICATIONS OF THE REST OF VARIABLES (EACH ONE REPEATED 'nTimeSteps' TIMES)
        Variables staticVariablesTimeT = dynamicVariables.toVariablesTimeT();

        dynamicVariables.getListOfDynamicVariables().stream()
                .forEach(dynVar ->
                                IntStream.range(0, nTimeSteps).forEach(i -> {

                                    Variable newVar = staticVariablesTimeT.getVariableByName(dynVar.getName());
                                    VariableBuilder aux = newVar.getVariableBuilder();
                                    aux.setName(dynVar.getName() + "_t" + Integer.toString(i));
                                    variables.newVariable(aux);
                                })
                );
        DAG dag = new DAG(variables);

        /*
         * CREATE STATIC DAG FROM THE DYNAMIC DAG
         * 2nd STEP: ADD EDGES
         */
        for (int i = 0; i < nTimeSteps; i++) {
            for (int j = 0; j < dynamicVariables.getNumberOfVars(); j++) {

                Variable dynVar = dynamicVariables.getVariableById(j);
                Variable staticVar = variables.getVariableByName(dynVar.getName() + "_t" + Integer.toString(i));

                if (i==0) { // t=0
                    dynamicDAG.getParentSetTime0(dynVar).getParents().stream().forEach(parentaux2 -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(parentaux2.getName() + "_t0")));
                } else {
                    final int final_i=i;
                    dynamicDAG.getParentSetTimeT(dynVar).getParents().stream().filter(parentVar -> parentVar.isInterfaceVariable()).forEach(parentVar -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(parentVar.getName().replace("_Interface", "_t" + Integer.toString(final_i - 1)))));
                    dynamicDAG.getParentSetTimeT(dynVar).getParents().stream().filter(parentVar -> !parentVar.isInterfaceVariable()).forEach(parentVar -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(parentVar.getName() + "_t" + Integer.toString(final_i))));
                }
            }
        }
        BayesianNetwork bn = new BayesianNetwork(dag);

        /*
         * CREATE STATIC BN FROM THE DYNAMIC BN
         * 3nd STEP: ADD CONDITIONAL DISTRIBUTIONS
         */
        for (int i = 0; i < nTimeSteps; i++) {
            for (int j = 0; j < dynamicVariables.getNumberOfVars(); j++) {

                List<Variable> parentList = new ArrayList<>();
                ConditionalDistribution cdist;
                Variable staticVar;

                if(i==0) {
                    staticVar = variables.getVariableByName(dynamicVariables.getVariableById(j).getName() + "_t0");

                    cdist = Serialization.deepCopy(dbn.getConditionalDistributionsTime0().get(j));
                    cdist.getConditioningVariables().stream().forEachOrdered(cdvar -> parentList.add(variables.getVariableByName(cdvar.getName() + "_t0")));
                }
                else {

                    final int final_i = i;
                    staticVar = variables.getVariableByName(dynamicVariables.getVariableById(j).getName() + "_t" + Integer.toString(final_i));

                    cdist = Serialization.deepCopy(dbn.getConditionalDistributionsTimeT().get(j));
                    cdist.getConditioningVariables().stream().forEachOrdered(cdvar -> {
                        parentList.add(((cdvar.isInterfaceVariable() || (cdvar.getName().contains("_t"))) ?
                                variables.getVariableByName(cdvar.getName().replace("_Interface", "_t" + Integer.toString(final_i - 1))) :
                                variables.getVariableByName(cdvar.getName() + "_t" + Integer.toString(final_i))));
                    });
                }
                cdist.setConditioningVariables(parentList);
                cdist.setVar(staticVar);
                bn.setConditionalDistribution(staticVar, cdist);
            }
        }
        return bn;
    }

    public static void main(String[] args) {

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);
        DynamicBayesianNetworkGenerator.setNumberOfLinks(5);

        DynamicBayesianNetwork dynamicNaiveBayes = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        System.out.println("ORIGINAL DYNAMIC DAG:");

        System.out.println(dynamicNaiveBayes.getDynamicDAG().toString());
        //System.out.println(dynamicNaiveBayes.toString());
        System.out.println();
        //dynamicNaiveBayes.getDynamicVariables().getListOfDynamicVariables().forEach(var -> System.out.println(var.getName()));
        //dynamicNaiveBayes.getDynamicVariables().getListOfDynamicVariables().forEach(var -> System.out.println(var.getName()));

        BayesianNetwork bn = DynamicToStaticBNConverter.convertDBNtoBN(dynamicNaiveBayes,4);
        System.out.println("NEW STATIC DAG:");
        System.out.println();
        System.out.println(bn.getDAG().toString());

        System.out.println();
        System.out.println("ORIGINAL DYNAMIC BN:");
        System.out.println(dynamicNaiveBayes.toString());

        System.out.println("STATIC BN:");
        System.out.println(bn.toString());
    }
}
