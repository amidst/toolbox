package eu.amidst.dynamic.inference;

import eu.amidst.core.distribution.*;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.*;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by dario on 11/11/15.
 */
        public class DynamicMAPInference {

            private DynamicBayesianNetwork model;
            private BayesianNetwork staticEvenModel, staticOddModel;

            private int nTimeSteps = 2;

            private Variable MAPvariable;
            private String MAPvarName;

            private List<DynamicAssignment> evidence;
            private Assignment staticEvidence;

            private boolean parallelMode = true;

            private DynamicAssignment MAPestimate;
            private double MAPestimateLogProbability;

            String groupedClassName = "==GROUPED_CLASS==";

            public void setEvidence(List<DynamicAssignment> evidence) {

                if (staticEvenModel==null || staticOddModel==null) {
                    System.out.println("Error: Even and/or odd model have not been initialized");
                    System.exit(-25);
                }
                long sequenceID = evidence.get(0).getSequenceID();
                for(DynamicAssignment dynAssig : evidence) {
                    if (dynAssig.getSequenceID()!=sequenceID) {
                        System.out.println("Error: Different sequence IDs in the evidence");
                        System.exit(-15);
                    }
                    if (dynAssig.getTimeID()>=nTimeSteps || dynAssig.getTimeID()<0) {
                        System.out.println("Error: Evidence time ID out of the range");
                        System.exit(-20);
                    }
                    if (!Double.isNaN(dynAssig.getValue(MAPvariable))) {
                        System.out.println("Error: MAP variable should not be in the evidence");
                        System.exit(-30);
                    }
                }
                this.evidence = evidence;

                staticEvidence = new HashMapAssignment(staticEvenModel.getNumberOfVars());

                evidence.stream().forEach(dynamicAssignment -> {
                    int time = (int) dynamicAssignment.getTimeID();
                    Set<Variable> dynAssigVariables = dynamicAssignment.getVariables();
                    for (Variable dynVariable : dynAssigVariables) {
                        Variable staticVariable = staticEvenModel.getVariables().getVariableByName(dynVariable.getName() + "_t" + Integer.toString(time));
                        double varValue = dynamicAssignment.getValue(dynVariable);
                        staticEvidence.setValue(staticVariable, varValue);
                    }

                });
                System.out.println(staticEvidence.outputString(staticEvenModel.getVariables().getListOfVariables()));
    }

    public void setModel(DynamicBayesianNetwork model) {
        this.model = model;
    }

    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    public void setMAPvariable(Variable MAPvariable) {

        boolean parents0 = model.getDynamicDAG().getParentSetTime0(MAPvariable).getNumberOfParents()>0;
        boolean parentsT = model.getDynamicDAG().getParentSetTimeT(MAPvariable).getParents().stream().anyMatch(parent -> !parent.isInterfaceVariable());

        if (parents0 || parentsT) {
            System.out.println("Error: The dynamic MAP Variable must not have parents");
            System.exit(-5);
        }

        if (!MAPvariable.isMultinomial()) {
            System.out.println("Error: The dynamic MAP Variable must be multinomial");
            System.exit(-10);
        }
        this.MAPvariable = MAPvariable;
        this.MAPvarName = MAPvariable.getName();
    }

    public void setNumberOfTimeSteps(int ntimeSteps) {
        if(ntimeSteps<2) {
            System.out.println("Error: The number of time steps should be at least 2");
            System.exit(-10);
        }
        nTimeSteps = ntimeSteps;
    }

    public void runInference() {

        if (this.staticOddModel==null || this.staticEvenModel == null) {
            return;
        }
//        DynamicVMP dynamicVMP = new DynamicVMP();
//        dynamicVMP.setModel(this.model);
//        dynamicVMP.runInference();
//        UnivariateDistribution posterior = dynamicVMP.getPredictivePosterior(this.MAPvariable, this.nTimesSteps);

//        VMP vmpEvenModel = new VMP();
//        vmpEvenModel.setModel(staticEvenModel);
//        vmpEvenModel.setEvidence(staticEvidence);
//        vmpEvenModel.runInference();


        VMP vmpOddModel = new VMP();
        vmpOddModel.setModel(staticOddModel);
        //vmpOddModel.setEvidence(staticEvidence);
        vmpOddModel.runInference();

    }

    private Variables obtainReplicatedStaticVariables(DynamicVariables dynamicVariables, boolean even_partition) {

        Variables variables = new Variables();

        // REPLICATIONS OF THE MAP VARIABLE (EACH CONSECUTIVE 2 ARE GROUPED)
        int replicationsMAPVariable;

        if (even_partition) {
            replicationsMAPVariable = nTimeSteps/2 + nTimeSteps%2;
        }
        else {
            replicationsMAPVariable = 1 + (nTimeSteps-1)/2 + (nTimeSteps-1)%2;
        }

        int nStatesMAPVariable = (int) Math.pow(MAPvariable.getNumberOfStates(),2);

        if (even_partition) {
            if (nTimeSteps%2 == 0) {
                IntStream.range(0, replicationsMAPVariable).forEach(i -> variables.newMultionomialVariable(groupedClassName + "_t" + Integer.toString(i), nStatesMAPVariable));
            }
            else {
                IntStream.range(0, replicationsMAPVariable-1).forEach(i -> variables.newMultionomialVariable(groupedClassName + "_t" + Integer.toString(i), nStatesMAPVariable));
                variables.newMultionomialVariable(groupedClassName + "_t" + Integer.toString(replicationsMAPVariable-1), MAPvariable.getNumberOfStates());
            }
        }
        else {
            variables.newMultionomialVariable(groupedClassName + "_t0", MAPvariable.getNumberOfStates());
            if (nTimeSteps%2 == 1) {
                IntStream.range(1, replicationsMAPVariable).forEach(i -> variables.newMultionomialVariable(groupedClassName + "_t" + Integer.toString(i), nStatesMAPVariable));
            }
            else {
                IntStream.range(1, replicationsMAPVariable-1).forEach(i -> variables.newMultionomialVariable(groupedClassName + "_t" + Integer.toString(i), nStatesMAPVariable));
                variables.newMultionomialVariable(groupedClassName + "_t" + Integer.toString(replicationsMAPVariable-1), MAPvariable.getNumberOfStates());
            }
        }
        //variables.getListOfVariables().forEach(var -> System.out.println(var.getName()));


        // REPLICATIONS OF THE REST OF VARIABLES (EACH ONE REPEATED 'nTimeSteps' TIMES)
        Variables staticVariables = dynamicVariables.toVariablesTimeT();
        //staticVariables.forEach(var -> System.out.println(var.getName() + var.getClass().toString()));
        //System.out.println("\n\n");

        dynamicVariables.getListOfDynamicVariables().stream()
                .filter(var -> !var.equals(MAPvariable))
                .forEach(dynVar ->
                                IntStream.range(0, nTimeSteps).forEach(i -> {
                                    //VariableBuilder aux = dynVar.getVariableBuilder();
                                    //aux.setName(dynVar.getName() + "_t" + Integer.toString(i));
                                    Variable newVar = staticVariables.getVariableByName(dynVar.getName());
                                    VariableBuilder aux = newVar.getVariableBuilder();
                                    aux.setName(dynVar.getName() + "_t" + Integer.toString(i));
                                    variables.newVariable(aux);
                                })
                );
        variables.forEach(var -> System.out.println(var.getName() + ", " + var.getClass().toString()));

        return variables;
    }

    private DAG obtainStaticDAG(DynamicDAG dynamicDAG, Variables variables, boolean even_partition) {

        DAG dag = new DAG(variables);
        DynamicVariables dynamicVariables = dynamicDAG.getDynamicVariables();

        /*
         * PARENTS OF THE MAP VARIABLE (ONLY THE PREVIOUS TEMPORAL COPY OF ITSELF)
         */
        int replicationsMAPVariable;

        if (even_partition) {
            replicationsMAPVariable = nTimeSteps/2 + nTimeSteps%2;
        }
        else {
            replicationsMAPVariable = 1 + (nTimeSteps-1)/2 + (nTimeSteps-1)%2;
        }

        IntStream.range(1, replicationsMAPVariable).forEach(i -> {
            Variable staticVar = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(i));
            dag.getParentSet(staticVar).addParent(variables.getVariableByName(groupedClassName + "_t" + Integer.toString(i - 1)));
        });

        /*
         * PARENTS OF THE REST OF VARIABLES
         */
        dynamicVariables.getListOfDynamicVariables().stream()
                .filter(var -> !var.equals(MAPvariable))
                .forEach(dynVar -> {

                    // ADD PARENTS AT TIME T=0
                    Variable staticVar0 = variables.getVariableByName(dynVar.getName() + "_t0");

                    List<Variable> parents0 = dynamicDAG.getParentSetTime0(dynVar).getParents();

                    parents0.stream().filter(parent -> parent.equals(MAPvariable)).forEach(parentaux2 -> dag.getParentSet(staticVar0).addParent(variables.getVariableByName(groupedClassName + "_t0")));
                    parents0.stream().filter(parent -> !parent.equals(MAPvariable)).forEach(parentaux2 -> dag.getParentSet(staticVar0).addParent(variables.getVariableByName(parentaux2.getName() + "_t0")));

                    // ADD PARENTS AT TIMES T>0
                    IntStream.range(1, nTimeSteps).forEach(i -> {

                        Variable staticVar = variables.getVariableByName(dynVar.getName() + "_t" + Integer.toString(i));

                        List<Variable> parents = dynamicDAG.getParentSetTimeT(dynVar).getParents();

                        int indexMAPReplication;
                        if (even_partition) {
                            indexMAPReplication = i / 2;
                        } else {
                            indexMAPReplication = 1 + (i - 1) / 2;
                        }

                        // PARENTS WHICH ARE INTERFACE VARIABLES
                        List<Variable> parentsInterface = parents.stream().filter(parentVar -> parentVar.isInterfaceVariable()).collect(Collectors.toList());

                        parentsInterface.stream().filter(parent -> parent.equals(MAPvariable)).forEach(parentVar -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(groupedClassName + "_t" + Integer.toString(indexMAPReplication - 1))));
                        parentsInterface.stream().filter(parent -> !parent.equals(MAPvariable)).forEach(parentVar -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(parentVar.getName().replace("_Interface", "_t" + Integer.toString(i - 1)))));

                        // PARENTS WHICH ARE NOT INTERFACE VARIABLES
                        List<Variable> parentsNotInterface = parents.stream().filter(parentVar -> !parentVar.isInterfaceVariable()).collect(Collectors.toList());

                        parentsNotInterface.stream().filter(parent -> parent.equals(MAPvariable)).forEach(parentVar -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(groupedClassName + "_t" + Integer.toString(indexMAPReplication))));
                        parentsNotInterface.stream().filter(parent -> !parent.equals(MAPvariable)).forEach(parentVar -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(parentVar.getName() + "_t" + Integer.toString(i))));

                    });
                });
//        System.out.println("\n" +
//                "\n\n" +
//                "\n\n" +
//                "\n\n" +
//                "\n\n" +
//                "\n\n" +
//                "\n\n" +
//                "\n\n" +
//                "\n\n" +
//                even_partition +
//                "\n" + dag.toString() + "\n\n\n\n\n");
        return dag;
    }

    private Multinomial groupedDistributionMAPVariableTime0(Variable dynVar, Variable staticVar, ConditionalDistribution conDist0, ConditionalDistribution conDistT) {

        Assignment assignment0, assignment1;
        assignment0 = new HashMapAssignment(1);
        assignment1 = new HashMapAssignment(2);

        Multinomial multinomial = new Multinomial(staticVar);

        double[] probs = new double[staticVar.getNumberOfStates()];
        int probs_index=0;

        for (int k = 0; k < dynVar.getNumberOfStates(); k++) {
            assignment0.setValue(dynVar, k);
            for (int l = 0; l < dynVar.getNumberOfStates(); l++) {
                assignment1.setValue(dynVar.getInterfaceVariable(), k);
                assignment1.setValue(dynVar, l);
                probs[probs_index] = conDist0.getConditionalProbability(assignment0) * conDistT.getConditionalProbability(assignment1);
                probs_index++;
            }
        }
        multinomial.setProbabilities(probs);
        return multinomial;
    }

    private Multinomial_MultinomialParents groupedDistributionMAPVariableTimeT(Variable dynVar, Variable staticVar, int nStatesStaticVarParent, List<Variable> parents, ConditionalDistribution conDistT) {

        Multinomial_MultinomialParents multinomial_multinomialParents = new Multinomial_MultinomialParents(staticVar, parents);
        //System.out.println(multinomial_multinomialParents.toString());
        Assignment assignment0, assignment1;
        Multinomial multinomial;

        for (int m = 0; m < nStatesStaticVarParent; m++) {
            double y_jminus2 = m % dynVar.getNumberOfStates();

            double[] probs1  = new double[staticVar.getNumberOfStates()];

            int probs_index1 = 0;

            assignment0 = new HashMapAssignment(1);
            assignment1 = new HashMapAssignment(1);
            UnivariateDistribution uniDist_y_jminus1, uniDist_uniDist_y_j;

            assignment0.setValue(dynVar.getInterfaceVariable(), y_jminus2);
            uniDist_y_jminus1 = conDistT.getUnivariateDistribution(assignment0);

            for (int k = 0; k < dynVar.getNumberOfStates(); k++) {

                double y_jminus1 = k;
                double prob1 = uniDist_y_jminus1.getProbability(y_jminus1);

                assignment1.setValue(dynVar.getInterfaceVariable(), y_jminus1);
                uniDist_uniDist_y_j = conDistT.getUnivariateDistribution(assignment1);

                if (staticVar.getNumberOfStates() >= nStatesStaticVarParent && nTimeSteps>2) {
                    for (int l = 0; l < dynVar.getNumberOfStates(); l++) {
                        double y_j = l;

                        double prob2 = uniDist_uniDist_y_j.getProbability(y_j);
                        probs1[probs_index1] = prob1 * prob2;
                        probs_index1++;
                    }
                }
                else {
                    probs1[probs_index1] = prob1;
                    probs_index1++;
                }
            }
            //System.out.println(Arrays.toString(probs1));
            multinomial = new Multinomial(staticVar);
            multinomial.setProbabilities(probs1);
            multinomial_multinomialParents.setMultinomial(m, multinomial);
        }
        return multinomial_multinomialParents;
    }

    private BayesianNetwork obtainStaticGroupedClassBayesianNetwork(DAG dag, Variables variables, boolean even_partition) {

        DynamicDAG dynamicDAG = model.getDynamicDAG();
        BayesianNetwork bn = new BayesianNetwork(dag);
        Variable staticVar, dynVar;
        ConditionalDistribution conDist0, conDist1;
        //Assignment assignment0, assignment1;

        int replicationsMAPVariable;

        if (even_partition) {
            replicationsMAPVariable = nTimeSteps/2 + nTimeSteps%2;
        }
        else {
            replicationsMAPVariable = 1 + (nTimeSteps-1)/2 + (nTimeSteps-1)%2;
        }

        /*
         * ADD CONDITIONAL (UNIVARIATE) DISTRIBUTION FOR THE GROUPED MAP/CLASS VARIABLE AT TIME T=0
         */
        staticVar = variables.getVariableByName(groupedClassName + "_t0");
        dynVar = model.getDynamicVariables().getVariableByName(MAPvarName);

        conDist0 = Serialization.deepCopy(model.getConditionalDistributionsTime0().get(dynVar.getVarID()));
        conDist1 = Serialization.deepCopy(model.getConditionalDistributionsTimeT().get(dynVar.getVarID()));

        Multinomial multinomial;
        if (even_partition) {
            multinomial = groupedDistributionMAPVariableTime0(dynVar, staticVar, conDist0, conDist1);
        }
        else {
            multinomial = (Multinomial) conDist0;
            multinomial.setVar(staticVar);
        }
        bn.setConditionalDistribution(staticVar, multinomial);


        /*
         * CREATE THE GENERAL (TIME T) CONDITIONAL DISTRIBUTION OF THE GROUPED MAP/CLASS VARIABLE, IF NEEDED
         */
        Multinomial_MultinomialParents generalConditionalDistTimeT;

        if ( even_partition && (replicationsMAPVariable>2 || (replicationsMAPVariable==2 && nTimeSteps>3))) {

            Variable staticVar_current = variables.getVariableByName(groupedClassName + "_t1");
            Variable staticVar_interface = variables.getVariableByName(groupedClassName + "_t0");
            List<Variable> parents = bn.getDAG().getParentSet(staticVar_current).getParents();
            ConditionalDistribution conDist_dynamic = Serialization.deepCopy(model.getConditionalDistributionsTimeT().get(dynVar.getVarID()));

            generalConditionalDistTimeT = groupedDistributionMAPVariableTimeT(dynVar, staticVar_current, staticVar_interface.getNumberOfStates(), parents, conDist_dynamic);

        }
        else if (!even_partition &&  (replicationsMAPVariable>3) ) {

            Variable staticVar_current = variables.getVariableByName(groupedClassName + "_t2");
            Variable staticVar_interface = variables.getVariableByName(groupedClassName + "_t1");
            List<Variable> parents = bn.getDAG().getParentSet(staticVar_current).getParents();
            ConditionalDistribution conDist_dynamic = Serialization.deepCopy(model.getConditionalDistributionsTimeT().get(dynVar.getVarID()));

            generalConditionalDistTimeT = groupedDistributionMAPVariableTimeT(dynVar, staticVar_current, staticVar_interface.getNumberOfStates(), parents, conDist_dynamic);

        }
        else { // In this case, 'generalConditionalDistTimeT' will never be used.
            generalConditionalDistTimeT = new Multinomial_MultinomialParents(staticVar, bn.getDAG().getParentSet(staticVar).getParents());
        }

        /*
         * ADD CONDITIONAL DISTRIBUTIONS FOR THE REPLICATIONS OF THE GROUPED MAP/CLASS VARIABLE
         */
        if (even_partition) {
            if (nTimeSteps % 2 == 0) {
                IntStream.range(1, replicationsMAPVariable).forEach(i -> {
                    Variable staticVar1 = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(i));
                    ConditionalDistribution conditionalDistribution = Serialization.deepCopy(generalConditionalDistTimeT);
                    conditionalDistribution.setConditioningVariables(dag.getParentSet(staticVar1).getParents());
                    bn.setConditionalDistribution(staticVar1, conditionalDistribution);

                });
            }
            else {
                IntStream.range(1, replicationsMAPVariable - 1).forEach(i -> {
                    Variable staticVar1 = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(i));
                    ConditionalDistribution conditionalDistribution = Serialization.deepCopy(generalConditionalDistTimeT);
                    conditionalDistribution.setConditioningVariables(dag.getParentSet(staticVar1).getParents());
                    bn.setConditionalDistribution(staticVar1, conditionalDistribution);

                });

                // For an even partition with odd nTimeSteps, the last distribution is different
                Variable staticVar1 = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(replicationsMAPVariable - 1));
                Variable staticVar1_interface = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(replicationsMAPVariable - 2));
                Multinomial_MultinomialParents lastConDist = new Multinomial_MultinomialParents(staticVar1, dag.getParentSet(staticVar1).getParents());
                for (int m = 0; m < staticVar1_interface.getNumberOfStates(); m++) {
                    ConditionalDistribution dynConDist = model.getConditionalDistributionTimeT(MAPvariable);
                    Assignment assignment = new HashMapAssignment(1);
                    assignment.setValue(dynVar.getInterfaceVariable(), m % dynVar.getNumberOfStates());
                    Multinomial multinomial1 = (Multinomial) dynConDist.getUnivariateDistribution(assignment);
                    lastConDist.setMultinomial(m, multinomial1);
                }
                bn.setConditionalDistribution(staticVar1, lastConDist);
            }
        }
        else {
            if (nTimeSteps % 2 == 1) {

                // For an even partition, the first conditional distribution is different
                Variable staticVar0 = variables.getVariableByName(groupedClassName + "_t1");
                Variable staticVar0_interface = variables.getVariableByName(groupedClassName + "_t0");
                List<Variable> parents = bn.getDAG().getParentSet(staticVar0).getParents();
                ConditionalDistribution conDist_dynamic = Serialization.deepCopy(model.getConditionalDistributionsTimeT().get(dynVar.getVarID()));
                ConditionalDistribution conditionalDistTime1 = groupedDistributionMAPVariableTimeT(dynVar, staticVar0, staticVar0_interface.getNumberOfStates(), parents, conDist_dynamic);
                bn.setConditionalDistribution(staticVar0, conditionalDistTime1);

                // Add the rest of conditional distributions, copies of 'generalConditionalDistTimeT'
                IntStream.range(2, replicationsMAPVariable).forEach(i -> {
                    Variable staticVar1 = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(i));
                    ConditionalDistribution conditionalDistribution = Serialization.deepCopy(generalConditionalDistTimeT);
                    conditionalDistribution.setConditioningVariables(dag.getParentSet(staticVar1).getParents());
                    bn.setConditionalDistribution(staticVar1, conditionalDistribution);

                });
            }
            else {

                // For an even partition, the first conditional distribution is different
                Variable staticVar0 = variables.getVariableByName(groupedClassName + "_t1");
                Variable staticVar0_interface = variables.getVariableByName(groupedClassName + "_t0");
                //System.out.println(staticVar0.getNumberOfStates());
                //System.out.println(staticVar0_interface.getNumberOfStates());
                List<Variable> parents = bn.getDAG().getParentSet(staticVar0).getParents();
                ConditionalDistribution conDist_dynamic = Serialization.deepCopy(model.getConditionalDistributionsTimeT().get(dynVar.getVarID()));
                ConditionalDistribution conditionalDistTime1 = groupedDistributionMAPVariableTimeT(dynVar, staticVar0, staticVar0_interface.getNumberOfStates(), parents, conDist_dynamic);
                bn.setConditionalDistribution(staticVar0, conditionalDistTime1);

                // Add the intermediate conditional distributions, copies of 'generalConditionalDistTimeT'
                IntStream.range(2, replicationsMAPVariable - 1).forEach(i -> {
                    Variable staticVar1 = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(i));
                    ConditionalDistribution conditionalDistribution = Serialization.deepCopy(generalConditionalDistTimeT);
                    conditionalDistribution.setConditioningVariables(dag.getParentSet(staticVar1).getParents());
                    bn.setConditionalDistribution(staticVar1, conditionalDistribution);

                });

                // For an even partition with even nTimeSteps, the last distribution is also different
                Variable staticVar1 = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(replicationsMAPVariable - 1));
                Variable staticVar1_interface = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(replicationsMAPVariable - 2));
                Multinomial_MultinomialParents lastConDist = new Multinomial_MultinomialParents(staticVar1, dag.getParentSet(staticVar1).getParents());
                for (int m = 0; m < staticVar1_interface.getNumberOfStates(); m++) {
                    ConditionalDistribution dynConDist = model.getConditionalDistributionTimeT(MAPvariable);
                    Assignment assignment = new HashMapAssignment(1);
                    assignment.setValue(dynVar.getInterfaceVariable(), m % dynVar.getNumberOfStates());
                    Multinomial multinomial1 = (Multinomial) dynConDist.getUnivariateDistribution(assignment);
                    lastConDist.setMultinomial(m, multinomial1);
                }
                bn.setConditionalDistribution(staticVar1, lastConDist);
            }
        }

        /*
         * ADD CONDITIONAL DISTRIBUTIONS FOR VARIABLES HAVING AS A PARENT THE GROUPED MAP/CLASS VARIABLE, AT TIME T=0
         */
        List<Variable> dynVariables = model.getDynamicVariables().getListOfDynamicVariables();
        List<Variable> dynVariablesWithClassParent = dynVariables.stream().filter(var -> !var.equals(MAPvariable)).filter(var -> dynamicDAG.getParentSetTime0(var).contains(MAPvariable)).collect(Collectors.toList());
        List<Variable> dynVariablesNoClassParent = dynVariables.stream().filter(var -> !var.equals(MAPvariable)).filter(var -> !dynamicDAG.getParentSetTime0(var).contains(MAPvariable)).collect(Collectors.toList());

        if (even_partition) {
            dynVariablesWithClassParent.stream().forEach(dynVariable -> {
                ConditionalDistribution conditionalDistribution = Serialization.deepCopy(model.getConditionalDistributionTime0(dynVariable));

                Variable staticMAPVar1 = variables.getVariableByName(groupedClassName + "_t0");
                Variable staticVar1 = variables.getVariableByName(dynVariable.getName() + "_t0");
                List<Variable> thisVarParents = conditionalDistribution.getConditioningVariables();
                int indexMAPvariable = thisVarParents.indexOf(MAPvariable);
                thisVarParents.remove(indexMAPvariable);
                thisVarParents.add(indexMAPvariable, staticMAPVar1);

                BaseDistribution_MultinomialParents baseDist = new BaseDistribution_MultinomialParents(staticVar1,thisVarParents);
                for (int m = 0; m < baseDist.getNumberOfBaseDistributions(); m++) {
                    Assignment assignment = new HashMapAssignment(1);
                    assignment.setValue(MAPvariable, m / MAPvariable.getNumberOfStates());
                    baseDist.setBaseDistribution(m, conditionalDistribution.getUnivariateDistribution(assignment));
                }

                bn.setConditionalDistribution(staticVar1, baseDist);
            });
        }
        else {
            dynVariablesWithClassParent.stream().forEach(dynVariable -> {
                ConditionalDistribution conditionalDistribution = Serialization.deepCopy(model.getConditionalDistributionTime0(dynVariable));

                Variable staticMAPVar1 = variables.getVariableByName(groupedClassName + "_t0");
                Variable staticVar1 = variables.getVariableByName(dynVariable.getName() + "_t0");
                List<Variable> thisVarParents = conditionalDistribution.getConditioningVariables();
                int indexMAPvariable = thisVarParents.indexOf(MAPvariable);
                thisVarParents.remove(indexMAPvariable);
                thisVarParents.add(indexMAPvariable, staticMAPVar1);

                conditionalDistribution.setConditioningVariables(thisVarParents);
                conditionalDistribution.setVar(staticVar1);
                //System.out.println(conditionalDistribution.toString());

                bn.setConditionalDistribution(staticVar1, conditionalDistribution);
            });
        }

        /*
         * ADD CONDITIONAL DISTRIBUTIONS FOR VARIABLES HAVING AS A PARENT THE GROUPED MAP/CLASS VARIABLE, AT TIMES T>0
         */
        if (even_partition) {
            dynVariablesWithClassParent.stream().forEach(dynVariable -> {
                IntStream.range(1, nTimeSteps).forEachOrdered(i -> {

                    ConditionalDistribution dynamicConDist = Serialization.deepCopy(model.getConditionalDistributionTimeT(dynVariable));
                    Variable staticVar2 = variables.getVariableByName(dynVariable.getName() + "_t" + Integer.toString(i));
                    List<Variable> parentList = bn.getDAG().getParentSet(staticVar2).getParents();
                    if (parentList.stream().allMatch(parent -> parent.isMultinomial())) {
                        BaseDistribution_MultinomialParents staticConDist = new BaseDistribution_MultinomialParents(staticVar2, parentList); //= Serialization.deepCopy(bn.getConditionalDistribution(staticVar2));

                        int nStatesParents = (int) Math.round(Math.exp(parentList.stream().mapToDouble(parent -> Math.log(parent.getNumberOfStates())).sum()));
                        for (int m = 0; m < nStatesParents; m++) {
                            Assignment staticParentsConfigurations = MultinomialIndex.getVariableAssignmentFromIndex(parentList, m);
                            Assignment dynamicParentsConfiguration = new HashMapAssignment(parentList.size());
                            IntStream.range(0, parentList.size()).forEach(k -> {
                                double parentValue = staticParentsConfigurations.getValue(parentList.get(k));
                                String parentName;
                                if (parentList.get(k).getName().contains(groupedClassName)) {
                                    parentName = parentList.get(k).getName().replace(groupedClassName, MAPvarName).replaceFirst("_t.", "");
                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);

                                    double dynParentValue;

                                    if (i % 2 == 0) {
                                        dynParentValue = parentValue / MAPvariable.getNumberOfStates();
                                    } else {
                                        dynParentValue = parentValue % MAPvariable.getNumberOfStates();
                                    }
                                    dynamicParentsConfiguration.setValue(dynParent, dynParentValue);
                                } else {
                                    parentName = parentList.get(k).getName().replaceFirst("_t.", "");
                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
                                    dynamicParentsConfiguration.setValue(dynParent.getInterfaceVariable(), parentValue);
                                }
                            });
                            staticConDist.setBaseDistribution(staticParentsConfigurations, dynamicConDist.getUnivariateDistribution(dynamicParentsConfiguration));
                        }
                        bn.setConditionalDistribution(staticVar2, staticConDist);
                    }
                    else {
                        List<Variable> multinomialParents = parentList.stream().filter(parent -> parent.isMultinomial()).collect(Collectors.toList());
                        int nStatesParents = (int) Math.round(Math.exp(multinomialParents.stream().mapToDouble(parent -> Math.log(parent.getNumberOfStates())).sum()));
                        //System.out.println(nStatesParents);

                        BaseDistribution_MultinomialParents staticConDist = new BaseDistribution_MultinomialParents(staticVar2, multinomialParents);

                        for (int m = 0; m < nStatesParents; m++) {

                            Assignment staticParentsConfigurations = MultinomialIndex.getVariableAssignmentFromIndex(multinomialParents, m);
//                            System.out.println(staticParentsConfigurations.outputString());
                            Assignment dynamicParentsConfiguration = new HashMapAssignment(multinomialParents.size());

                            IntStream.range(0, multinomialParents.size()).forEach(k -> {
                                double parentValue = staticParentsConfigurations.getValue(multinomialParents.get(k));
                                String parentName;
                                if (multinomialParents.get(k).getName().contains(groupedClassName)) {
                                    parentName = multinomialParents.get(k).getName().replace(groupedClassName, MAPvarName).replaceFirst("_t.", "");
                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);

                                    double dynParentValue;

                                    if (i % 2 == 0) {
                                        dynParentValue = parentValue / MAPvariable.getNumberOfStates();
                                    } else {
                                        dynParentValue = parentValue % MAPvariable.getNumberOfStates();
                                    }
                                    dynamicParentsConfiguration.setValue(dynParent, dynParentValue);
                                } else {
                                    parentName = multinomialParents.get(k).getName().replaceFirst("_t.", "");
                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
                                    dynamicParentsConfiguration.setValue(dynParent.getInterfaceVariable(), parentValue);
                                }
//                                System.out.println(parentName + ", " + parentValue);
                            });
//                            System.out.println(dynamicConDist.toString());
//                            System.out.println(dynamicParentsConfiguration.outputString());
                            //UnivariateDistribution uniDist = dynamicConDist.getUnivariateDistribution(dynamicParentsConfiguration);
                            Normal_MultinomialNormalParents clgDist = dynamicConDist.toEFConditionalDistribution().toConditionalDistribution();
                            //ConditionalLinearGaussian clgaux = clgDist.getNormal_NormalParentsDistribution(dynamicParentsConfiguration);
                            //clgaux.setConditioningVariables(parentList.stream().filter(parent -> !parent.isMultinomial()).collect(Collectors.toList()));
                            //ConditionalLinearGaussian clgDist = dynamicConDist.toEFConditionalDistribution().toConditionalDistribution().;

//                            System.out.println(clgDist.toString());
//                            System.out.println(clgDist.getNormal_NormalParentsDistribution(dynamicParentsConfiguration).toString());
                            //System.out.println(dynamicConDist.getUnivariateDistribution(dynamicParentsConfiguration).toString());

                            staticConDist.setBaseDistribution(staticParentsConfigurations, clgDist.getNormal_NormalParentsDistribution(dynamicParentsConfiguration));
                        }
                        bn.setConditionalDistribution(staticVar2, staticConDist);
                    }
//                    if (parentList.stream().allMatch(parent -> parent.isMultinomial())) {
//                        BaseDistribution_MultinomialParents staticConDist = new BaseDistribution_MultinomialParents(staticVar2, parentList); //= Serialization.deepCopy(bn.getConditionalDistribution(staticVar2));
//                        staticConDist = new BaseDistribution_MultinomialParents(staticVar2, parentList);
//                        int nStatesParents = (int) Math.round(Math.exp(parentList.stream().mapToDouble(parent -> Math.log(parent.getNumberOfStates())).sumNonStateless()));
//                        //System.out.println(nStatesParents);
//
//                        for (int m = 0; m < nStatesParents; m++) {
//                            Assignment staticParentsConfigurations = MultinomialIndex.getVariableAssignmentFromIndex(parentList, m);
//                            Assignment dynamicParentsConfiguration = new HashMapAssignment(parentList.size());
//                            IntStream.range(0, parentList.size()).forEach(k -> {
//                                double parentValue = staticParentsConfigurations.getValue(parentList.get(k));
//                                String parentName;
//                                if (parentList.get(k).getName().contains(groupedClassName)) {
//                                    parentName = parentList.get(k).getName().replace(groupedClassName, MAPvarName).replaceFirst("_t.", "");
//                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
//
//                                    double dynParentValue;
//
//                                    if (i % 2 == 0) {
//                                        dynParentValue = parentValue / MAPvariable.getNumberOfStates();
//                                    } else {
//                                        dynParentValue = parentValue % MAPvariable.getNumberOfStates();
//                                    }
//                                    dynamicParentsConfiguration.setValue(dynParent, dynParentValue);
//                                } else {
//                                    parentName = parentList.get(k).getName().replaceFirst("_t.", "");
//                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
//                                    dynamicParentsConfiguration.setValue(dynParent.getInterfaceVariable(), parentValue);
//                                }
//                            });
//                            staticConDist.setBaseDistribution(staticParentsConfigurations, dynamicConDist.getUnivariateDistribution(dynamicParentsConfiguration));
//                        }
//                        bn.setConditionalDistribution(staticVar2, staticConDist);
//                    }
                });
            });
        }
        else {
            dynVariablesWithClassParent.stream().forEach(dynVariable -> {
                IntStream.range(1, nTimeSteps).forEachOrdered(i -> {

                    ConditionalDistribution dynamicConDist = Serialization.deepCopy(model.getConditionalDistributionTimeT(dynVariable));
                    Variable staticVar2 = variables.getVariableByName(dynVariable.getName() + "_t" + Integer.toString(i));

                    List<Variable> parentList = bn.getDAG().getParentSet(staticVar2).getParents();
                    List<Variable> multinomialParents = parentList.stream().filter(parent -> parent.isMultinomial()).collect(Collectors.toList());

                    System.out.println("Variable " + staticVar2.getName() + " with " + parentList.size() + " parents");

                    int nStatesMultinomialParents = (int) Math.round(Math.exp(multinomialParents.stream().mapToDouble(parent -> Math.log(parent.getNumberOfStates())).sum()));

                    if (staticVar2.isMultinomial()) {
                        Multinomial_MultinomialParents staticVar2conDist = new Multinomial_MultinomialParents(staticVar2, multinomialParents);

                        for (int m = 0; m < nStatesMultinomialParents; m++) {
                            Assignment staticParentsConfigurations = MultinomialIndex.getVariableAssignmentFromIndex(multinomialParents, m);
                            Assignment dynamicParentsConfiguration = new HashMapAssignment(multinomialParents.size());

                            IntStream.range(0, multinomialParents.size()).forEach(k -> {
                                double parentValue = staticParentsConfigurations.getValue(multinomialParents.get(k));
                                String parentName;
                                if (multinomialParents.get(k).getName().contains(groupedClassName)) {
                                    parentName = multinomialParents.get(k).getName().replace(groupedClassName, MAPvarName).replaceFirst("_t.", "");
                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);

                                    double dynParentValue;

                                    if (i % 2 == 1) {
                                        dynParentValue = parentValue / MAPvariable.getNumberOfStates();
                                    } else {
                                        dynParentValue = parentValue % MAPvariable.getNumberOfStates();
                                    }
                                    dynamicParentsConfiguration.setValue(dynParent, dynParentValue);
                                } else {
                                    parentName = multinomialParents.get(k).getName().replaceFirst("_t.", "");
                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
                                    dynamicParentsConfiguration.setValue(dynParent.getInterfaceVariable(), parentValue);
                                }
                            });
                            Multinomial multinomial1 = (Multinomial)dynamicConDist.getUnivariateDistribution(dynamicParentsConfiguration);
                            multinomial1.setVar(staticVar2);
                            multinomial1.setConditioningVariables(multinomialParents);

                            System.out.println(multinomial1.toString());
                            multinomial1.getConditioningVariables().forEach(condVar -> condVar.getName());
                            staticVar2conDist.setMultinomial(m, multinomial1);
                        }
                        bn.setConditionalDistribution(staticVar2, staticVar2conDist);
                    }
                    else {
                        Normal_MultinomialNormalParents staticVar2conDist = new Normal_MultinomialNormalParents(staticVar2,parentList);
                        //staticVar2conDist = Serialization.deepCopy(bn.getConditionalDistribution(staticVar2));

                        for (int m = 0; m < nStatesMultinomialParents; m++) {
                            Assignment staticParentsConfigurations = MultinomialIndex.getVariableAssignmentFromIndex(multinomialParents, m);
                            Assignment dynamicParentsConfiguration = new HashMapAssignment(multinomialParents.size());

                            IntStream.range(0, multinomialParents.size()).forEach(k -> {
                                double parentValue = staticParentsConfigurations.getValue(multinomialParents.get(k));
                                String parentName;
                                if (multinomialParents.get(k).getName().contains(groupedClassName)) {
                                    parentName = multinomialParents.get(k).getName().replace(groupedClassName, MAPvarName).replaceFirst("_t.", "");
                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);

                                    double dynParentValue;

                                    if (i % 2 == 1) {
                                        dynParentValue = parentValue / MAPvariable.getNumberOfStates();
                                    } else {
                                        dynParentValue = parentValue % MAPvariable.getNumberOfStates();
                                    }
                                    dynamicParentsConfiguration.setValue(dynParent, dynParentValue);
                                } else {
                                    parentName = multinomialParents.get(k).getName().replaceFirst("_t.", "");
                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
                                    dynamicParentsConfiguration.setValue(dynParent.getInterfaceVariable(), parentValue);
                                }
                            });

                            Normal_MultinomialNormalParents N_MNP_aux = (Normal_MultinomialNormalParents) dynamicConDist;
                            System.out.println(N_MNP_aux.getVariable().getName());
                            //N_MNP_aux.setConditioningVariables(parentList);
                            N_MNP_aux.getConditioningVariables().forEach(var -> System.out.println(var.getName() + ", " + var.getClass().getCanonicalName()));
                            ConditionalLinearGaussian clg = N_MNP_aux.getNormal_NormalParentsDistribution(staticParentsConfigurations);
                            clg.setConditioningVariables(parentList);
                            clg.setVar(staticVar2);

                            N_MNP_aux.getConditioningVariables().forEach(var -> var.getName());
                            System.out.println(clg.toString());
                            //clg.setConditioningVariables(parentList);
//                            ConditionalLinearGaussian clgAux = clgDist.getNormal_NormalParentsDistribution(dynamicParentsConfiguration);
                            //clgAux.setConditioningVariables(parentList.stream().filter(parent -> !parent.isMultinomial()).collect(Collectors.toList()));

                            staticVar2conDist.setNormal_NormalParentsDistribution(m, clg);
                        }
                        bn.setConditionalDistribution(staticVar2, staticVar2conDist);
                    }

//                    if (parentList.stream().allMatch(parent -> parent.isMultinomial())) {
//                        BaseDistribution_MultinomialParents staticConDist = new BaseDistribution_MultinomialParents(staticVar2, parentList); //= Serialization.deepCopy(bn.getConditionalDistribution(staticVar2));
//
//                        int nStatesParents = (int) Math.round(Math.exp(parentList.stream().mapToDouble(parent -> Math.log(parent.getNumberOfStates())).sumNonStateless()));
//                        for (int m = 0; m < nStatesParents; m++) {
//                            Assignment staticParentsConfigurations = MultinomialIndex.getVariableAssignmentFromIndex(parentList, m);
//                            Assignment dynamicParentsConfiguration = new HashMapAssignment(parentList.size());
//                            IntStream.range(0, parentList.size()).forEach(k -> {
//                                double parentValue = staticParentsConfigurations.getValue(parentList.get(k));
//                                String parentName;
//                                if (parentList.get(k).getName().contains(groupedClassName)) {
//                                    parentName = parentList.get(k).getName().replace(groupedClassName, MAPvarName).replaceFirst("_t.", "");
//                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
//
//                                    double dynParentValue;
//
//                                    if (i % 2 == 1) {
//                                        dynParentValue = parentValue / MAPvariable.getNumberOfStates();
//                                    } else {
//                                        dynParentValue = parentValue % MAPvariable.getNumberOfStates();
//                                    }
//                                    dynamicParentsConfiguration.setValue(dynParent, dynParentValue);
//                                }
//                                else {
//                                    parentName = parentList.get(k).getName().replaceFirst("_t.", "");
//                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
//                                    dynamicParentsConfiguration.setValue(dynParent.getInterfaceVariable(), parentValue);
//                                }
//                            });
//                            staticConDist.setBaseDistribution(staticParentsConfigurations, dynamicConDist.getUnivariateDistribution(dynamicParentsConfiguration));
//                        }
//                        bn.setConditionalDistribution(staticVar2, staticConDist);
//                    }
//                    else {
//                        List<Variable> multinomialParents = parentList.stream().filter(parent -> parent.isMultinomial()).collect(Collectors.toList());
//                        int nStatesParents = (int) Math.round(Math.exp(multinomialParents.stream().mapToDouble(parent -> Math.log(parent.getNumberOfStates())).sumNonStateless()));
//                        System.out.println(nStatesParents);
//
//                        parentList.forEach(parent -> System.out.println("Variable " + staticVar2.getName() + ", with parent " + parent.getName()));
//                        BaseDistribution_MultinomialParents staticConDist = new BaseDistribution_MultinomialParents(staticVar2, parentList);
//                        staticConDist.randomInitialization(new Random());
//                        System.out.println();
//                        System.out.println("Time " + i);
//                        System.out.println(staticConDist.toString());
//                        System.out.println();
//                        for (int m = 0; m < nStatesParents; m++) {
//
//                            Assignment staticParentsConfigurations = MultinomialIndex.getVariableAssignmentFromIndex(multinomialParents, m);
////                            System.out.println(staticParentsConfigurations.outputString());
//                            Assignment dynamicParentsConfiguration = new HashMapAssignment(multinomialParents.size());
//
//                            IntStream.range(0, multinomialParents.size()).forEach(k -> {
//                                double parentValue = staticParentsConfigurations.getValue(multinomialParents.get(k));
//                                String parentName;
//                                if (multinomialParents.get(k).getName().contains(groupedClassName)) {
//                                    parentName = multinomialParents.get(k).getName().replace(groupedClassName, MAPvarName).replaceFirst("_t.", "");
//                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
//
//                                    double dynParentValue;
//
//                                    if (i % 2 == 1) {
//                                        dynParentValue = parentValue / MAPvariable.getNumberOfStates();
//                                    } else {
//                                        dynParentValue = parentValue % MAPvariable.getNumberOfStates();
//                                    }
//                                    dynamicParentsConfiguration.setValue(dynParent, dynParentValue);
//                                } else {
//                                    parentName = multinomialParents.get(k).getName().replaceFirst("_t.", "");
//                                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
//                                    dynamicParentsConfiguration.setValue(dynParent.getInterfaceVariable(), parentValue);
//                                }
////                                System.out.println(parentName + ", " + parentValue);
//                            });
////                            System.out.println(dynamicConDist.toString());
//                            System.out.println(staticParentsConfigurations.outputString());
//                            System.out.println(dynamicParentsConfiguration.outputString());
//                            System.out.println(staticConDist.getNumberOfBaseDistributions() + ", " + Arrays.toString(staticConDist.getParameters()));
//                            System.out.println();
//                            staticConDist.setConditioningVariables(parentList);
//
//                            System.out.println(staticConDist.toString());
//                            System.out.println();
////                            UnivariateDistribution uniDist = dynamicConDist.getUnivariateDistribution(dynamicParentsConfiguration);
//
//                            Normal_MultinomialNormalParents clgDist = dynamicConDist.toEFConditionalDistribution().toConditionalDistribution();
//                            ConditionalLinearGaussian clgAux = clgDist.getNormal_NormalParentsDistribution(dynamicParentsConfiguration);
//                            clgAux.setConditioningVariables(parentList.stream().filter(parent -> !parent.isMultinomial()).collect(Collectors.toList()));
//                            //clgAux.toEFConditionalDistribution().to
////                            System.out.println(clgDist.toString());
////                            System.out.println(clgDist.getNormal_NormalParentsDistribution(dynamicParentsConfiguration).toString());
//                            //System.out.println(dynamicConDist.getUnivariateDistribution(dynamicParentsConfiguration).toString());
//
//                            staticConDist.setBaseDistribution(staticParentsConfigurations, clgAux.getNormal(dynamicParentsConfiguration));
//                        }
//                        bn.setConditionalDistribution(staticVar2, staticConDist);
//                    }

                });
            });
        }

        /*
         * ADD CONDITIONAL DISTRIBUTIONS FOR VARIABLES NOT HAVING AS A PARENT THE GROUPED MAP/CLASS VARIABLE, AT ANY TIME T
         */
        dynVariablesNoClassParent.stream().forEach(dynVariable -> {

            // TIME T=0
            ConditionalDistribution conditionalDistribution = Serialization.deepCopy(model.getConditionalDistributionTime0(dynVariable));

            Variable staticVar1 = variables.getVariableByName(dynVariable.getName() + "_t0");

            List<Variable> thisVarParents = conditionalDistribution.getConditioningVariables();
            thisVarParents.stream().map(parent -> variables.getVariableByName(parent.getName() + "_t0"));
            conditionalDistribution.setConditioningVariables(thisVarParents);
            //System.out.println(conditionalDistribution.toString());

            bn.setConditionalDistribution(staticVar1, conditionalDistribution);

            // TIMES T>0
            IntStream.range(1, nTimeSteps).forEach(i -> {
                ConditionalDistribution conditionalDistribution1 = Serialization.deepCopy(model.getConditionalDistributionTimeT(dynVariable));

                Variable staticVar2 = variables.getVariableByName(dynVariable.getName() + "_t" + Integer.toString(i));

                List<Variable> thisVarParents1 = conditionalDistribution1.getConditioningVariables();
                thisVarParents1.stream().map(parent -> variables.getVariableByName(parent.getName() + "_t" + Integer.toString(i)));
                conditionalDistribution1.setConditioningVariables(thisVarParents1);

                bn.setConditionalDistribution(staticVar2, conditionalDistribution1);
            });

        });

        return bn;
    }

    public BayesianNetwork getDynamicMAPEvenModel() {

        Variables variables;
        DynamicDAG dynamicDAG = model.getDynamicDAG();
        DAG dag;
        BayesianNetwork bn;
        DynamicVariables dynamicVariables = model.getDynamicVariables();

        //System.out.println("EVEN PARTITION");
        variables = obtainReplicatedStaticVariables(dynamicVariables, true);
        //variables.forEach(var -> System.out.println(var.getName()));

        dynamicDAG = model.getDynamicDAG();
        dag = obtainStaticDAG(dynamicDAG, variables, true);
        //System.out.println(dag.toString());

        bn = this.obtainStaticGroupedClassBayesianNetwork(dag, variables, true);

        //System.out.println(bn.toString());
        this.staticEvenModel = bn;
        return bn;
    }



    public BayesianNetwork getDynamicMAPOddModel() {

        Variables variables;
        DynamicDAG dynamicDAG = model.getDynamicDAG();
        DAG dag;
        BayesianNetwork bn;
        DynamicVariables dynamicVariables = model.getDynamicVariables();

        //System.out.println("ODD PARTITION");
        variables = obtainReplicatedStaticVariables(dynamicVariables, false);
        //variables.forEach(var -> System.out.println(var.getName()));

        dag = obtainStaticDAG(dynamicDAG, variables, false);
        //System.out.println(dag.toString());

        bn = this.obtainStaticGroupedClassBayesianNetwork(dag, variables, false);

        //System.out.println(bn.toString());
        this.staticOddModel = bn;
        return bn;
    }



    public static void main(String[] arguments) throws IOException, ClassNotFoundException {

//        String file = "./networks/CajamarDBN.dbn";
//        DynamicBayesianNetwork cajamarDBN = DynamicBayesianNetworkLoader.loadFromFile(file);
//
//        DynamicMAPInference dynMAP = new DynamicMAPInference();
//        dynMAP.setModel(cajamarDBN);
//        dynMAP.setNumberOfTimeSteps(6);
////        //dynMAP.runInference();
////

////
////
////
////
////        cajamarDBN.getDynamicVariables().getListOfDynamicVariables().forEach(var -> System.out.println(var.getName()));
//
////
//
////
//         System.out.println(cajamarDBN.toString());
////        System.out.println("CausalOrder: " + cajamarDBN.getDynamicDAG().toString());
////
////
//
//        System.out.println(cajamarDBN.getDynamicDAG().toString());
//
//        Variable mapVariable = cajamarDBN.getDynamicVariables().getVariableByName("DEFAULT");
//        dynMAP.setMAPvariable(mapVariable);
//        BayesianNetwork test = dynMAP.getDynamicMAPEvenModel();
//        //System.out.println(test.getDAG().toString());


        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);
        DynamicBayesianNetworkGenerator.setNumberOfLinks(5);

        DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        System.out.println("ORIGINAL DYNAMIC DAG:");

        //System.out.println(dynamicNaiveBayes.getDynamicDAG().toString());
        System.out.println(dynamicBayesianNetwork.toString());
        System.out.println();

//        dynamicBayesianNetwork.getConditionalDistributionsTimeT().forEach(cDist -> {
//            System.out.println(cDist.getVariable().getName());
//            cDist.getConditioningVariables().forEach(cDistCondVar -> System.out.println(cDistCondVar.getName() + ", " + cDistCondVar.getClass().getName()));
//            System.out.println(cDist.getClass().getName());
//            System.out.println(cDist.toString());
//        });
//        System.out.println();

        //dynamicNaiveBayes.getDynamicVariables().getListOfDynamicVariables().forEach(var -> System.out.println(var.getName()));
        //dynamicNaiveBayes.getDynamicVariables().getListOfDynamicVariables().forEach(var -> System.out.println(var.getName()));


        int nTimeSteps = 2;


        DynamicMAPInference dynMAP = new DynamicMAPInference();
        dynMAP.setModel(dynamicBayesianNetwork);
        dynMAP.setNumberOfTimeSteps(nTimeSteps);

        dynMAP.setMAPvariable(dynamicBayesianNetwork.getDynamicVariables().getVariableByName("ClassVar"));

        BayesianNetwork evenModel = dynMAP.getDynamicMAPEvenModel();
//        BayesianNetworkWriter.saveToFile(evenModel,"dynamicMAPhybridEvenModel.bn");

        BayesianNetwork oddModel = dynMAP.getDynamicMAPOddModel();
//        BayesianNetworkWriter.saveToFile(oddModel,"dynamicMAPhybridOddModel.bn");

//        System.out.println(evenModel.toString());
//        System.out.println();

        System.out.println(oddModel.toString());
        System.out.println();

        System.out.println(oddModel.getDAG().toString());

        //EF_BayesianNetwork efBN = new EF_BayesianNetwork(oddModel);
        //efBN.getDistributionList().forEach(dist -> System.out.println(dist.toConditionalDistribution().toString()));

        //System.out.println(oddModel.getDAG().toString());

        /*
         * GENERATE AN EVIDENCE FOR T=0,...,nTimeSteps-1
         */
//        List<Variable> varsDynamicModel = dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();
//
//        varsDynamicModel.forEach(var -> System.out.println("Var ID " + var.getVarID() + ": " + var.getName()));
//        int indexVarEvidence1 = 1;
//        int indexVarEvidence2 = 2;
//        int indexVarEvidence3 = 3;
//        Variable varEvidence1 = varsDynamicModel.get(indexVarEvidence1);
//        Variable varEvidence2 = varsDynamicModel.get(indexVarEvidence2);
//        Variable varEvidence3 = varsDynamicModel.get(indexVarEvidence3);
//
//        List<Variable> varsEvidence = new ArrayList<>(3);
//        varsEvidence.add(0,varEvidence1);
//        varsEvidence.add(1,varEvidence2);
//        varsEvidence.add(2,varEvidence3);
//
//        double varEvidenceValue;
//
//        Random random = new Random();
//
//        List<DynamicAssignment> evidence = new ArrayList<>(nTimeSteps);
//
//        for (int t = 0; t < nTimeSteps; t++) {
//            HashMapDynamicAssignment dynAssignment = new HashMapDynamicAssignment(varsEvidence.size());
//
//            for (int i = 0; i < varsEvidence.size(); i++) {
//
//                dynAssignment.setSequenceID(2343253);
//                dynAssignment.setTimeID(t);
//                Variable varEvidence = varsEvidence.get(i);
//
//                if (varEvidence.isMultinomial()) {
//                    varEvidenceValue = random.nextInt(varEvidence1.getNumberOfStates());
//                } else {
//                    varEvidenceValue = -5 + 10 * random.nextDouble();
//                }
//
//                dynAssignment.setValue(varEvidence, varEvidenceValue);
//            }
//            evidence.add(dynAssignment);
//        }
//
//        dynMAP.setEvidence(evidence);


        evenModel.getVariables().forEach(var -> System.out.println(var.getName() + ", " + var.getClass().getName() + ", " + var.getNumberOfStates() + " states, multinomial? " + var.isMultinomial() + ", " + var.getDistributionType().getClass().getName()));
        oddModel.getVariables().forEach(var -> System.out.println(var.getName() + ", " + var.getClass().getName() + ", " + var.getNumberOfStates() + " states, multinomial? " + var.isMultinomial() + ", " + var.getDistributionType().getClass().getName()));

        evenModel.getDAG().getVariables().forEach(var -> System.out.println(var.getName() + ", " + var.getClass().getName() + ", " + var.getNumberOfStates() + " states, multinomial? " + var.isMultinomial() + ", " + var.getDistributionType().getClass().getName()));
        oddModel.getDAG().getVariables().forEach(var -> System.out.println(var.getName() + ", " + var.getClass().getName() + ", " + var.getNumberOfStates() + " states, multinomial? " + var.isMultinomial() + ", " + var.getDistributionType().getClass().getName()));

        evenModel.getDAG().getParentSets().forEach(parentSet -> {System.out.println("Parents of " + parentSet.getMainVar().getName() + ": " + parentSet.toString()); parentSet.getParents().stream().forEach(parent -> System.out.println((parent.getName() + ", " + parent.isMultinomial() + ", " + parent.getClass().getName()))); });
        oddModel.getDAG().getParentSets().forEach(parentSet -> System.out.println("Parents of " + parentSet.getMainVar().getName() + ": " + parentSet.toString()));

        System.out.println(oddModel.toString());

        oddModel.getConditionalDistributions().forEach(cDist -> {
            System.out.println(cDist.toString());
            System.out.println(cDist.getVariable().getName() + ", " + cDist.getVariable().getClass().getName());
            cDist.getConditioningVariables().forEach(cDistVar -> System.out.println(cDistVar.getName() + ", " + cDistVar.getClass().getName()));
        });

        dynMAP.runInference();
    }
}
