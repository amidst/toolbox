package eu.amidst.ijcai2016;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.MAPInference;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.inference.DynamicMAPInference;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;
import eu.amidst.dynamic.utils.DynamicToStaticBNConverter;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.dynamic.variables.HashMapDynamicAssignment;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Created by dario on 11/11/15.
 */
public class DynamicMAP_IJCAI_1 {

    public static List<DynamicAssignment> generateRandomEvidence(DynamicBayesianNetwork dynamicBayesianNetwork, Variable mapVariable, int nTimeSteps, int nVarsEvidence, Random random) {

        List<Variable> varsDynamicModel = dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();

        if (nVarsEvidence > varsDynamicModel.size()-1) {
            System.out.println("Too many variables to be observed");
            return null;
        }
        List<Variable> varsEvidence = new ArrayList<>(nVarsEvidence);
        double varEvidenceValue;

        int currentVarsEvidence=0;
        while (currentVarsEvidence < nVarsEvidence) {
            int indexVarEvidence = random.nextInt(dynamicBayesianNetwork.getNumberOfDynamicVars());
            Variable varEvidence = varsDynamicModel.get(indexVarEvidence);

            if (varEvidence.equals(mapVariable) || varsEvidence.contains(varEvidence)) {
                continue;
            }
            varsEvidence.add(varEvidence);
            currentVarsEvidence++;
        }

        List<DynamicAssignment> evidence = new ArrayList<>(nTimeSteps);

        for (int t = 0; t < nTimeSteps; t++) {
            HashMapDynamicAssignment dynAssignment = new HashMapDynamicAssignment(varsEvidence.size());

            for (int i = 0; i < varsEvidence.size(); i++) {

                dynAssignment.setSequenceID(12302253);
                dynAssignment.setTimeID(t);
                Variable varEvidence = varsEvidence.get(i);

                if (varEvidence.isMultinomial()) {
                    varEvidenceValue = random.nextInt(varEvidence.getNumberOfStates());
                }
                else {
                    varEvidenceValue = -5 + 10 * random.nextDouble();
                }
                dynAssignment.setValue(varEvidence, varEvidenceValue);
            }
            evidence.add(dynAssignment);
        }
        return evidence;
    }

    public static List<DynamicAssignment> generateRandomEvidenceWithObservedLeaves(DynamicBayesianNetwork dynamicBayesianNetwork, Variable mapVariable, int nTimeSteps, int nVarsEvidence, Random random) {

        List<Variable> varsDynamicModel = dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();

        if (nVarsEvidence > varsDynamicModel.size()-1) {
            System.out.println("Too many variables to be observe");
            return null;
        }
        List<Variable> varsEvidence = new ArrayList<>(nVarsEvidence);
        double varEvidenceValue;

        int currentVarsEvidence=0;
        while (currentVarsEvidence < nVarsEvidence) {
            int indexVarEvidence = random.nextInt(dynamicBayesianNetwork.getNumberOfDynamicVars());
            Variable varEvidence = varsDynamicModel.get(indexVarEvidence);

            if (varEvidence.equals(mapVariable) || varsEvidence.contains(varEvidence)) {
                continue;
            }
            varsEvidence.add(varEvidence);
            currentVarsEvidence++;
        }

        List<DynamicAssignment> evidence = new ArrayList<>(nTimeSteps);

        // Evidence from t=0 to t=(nTimeSteps-2)
        for (int t = 0; t < nTimeSteps-1; t++) {
            HashMapDynamicAssignment dynAssignment = new HashMapDynamicAssignment(varsEvidence.size());

            for (int i = 0; i < varsEvidence.size(); i++) {

                dynAssignment.setSequenceID(12302253);
                dynAssignment.setTimeID(t);
                Variable varEvidence = varsEvidence.get(i);

                if (varEvidence.isMultinomial()) {
                    varEvidenceValue = random.nextInt(varEvidence.getNumberOfStates());
                }
                else {
                    varEvidenceValue = -5 + 10 * random.nextDouble();
                }
                dynAssignment.setValue(varEvidence, varEvidenceValue);
            }
            evidence.add(dynAssignment);
        }

        // Evidence in t=(nTimeSteps-1) for all variables that are leaves in the DAG.
        int t=nTimeSteps-1;

        for (Variable currentVar : varsDynamicModel) {
            if (!varsDynamicModel.stream().anyMatch(var -> dynamicBayesianNetwork.getDynamicDAG().getParentSetTimeT(var).getParents().contains(currentVar))) {
                varsEvidence.add(currentVar);
            }
        }

        HashMapDynamicAssignment dynAssignment = new HashMapDynamicAssignment(varsEvidence.size());

        for (int i = 0; i < varsEvidence.size(); i++) {

            dynAssignment.setSequenceID(12302253);
            dynAssignment.setTimeID(t);
            Variable varEvidence = varsEvidence.get(i);

            if (varEvidence.isMultinomial()) {
                varEvidenceValue = random.nextInt(varEvidence.getNumberOfStates());
            }
            else {
                varEvidenceValue = -5 + 10 * random.nextDouble();
            }
            dynAssignment.setValue(varEvidence, varEvidenceValue);
        }
        evidence.add(dynAssignment);

        return evidence;
    }

    public static List<DynamicAssignment> generateRandomEvidenceWithObservedLeavesBNSampler(DynamicBayesianNetwork dynamicBayesianNetwork, Variable mapVariable, int nTimeSteps, int nVarsEvidence, Random random) {


        List<Variable> varsDynamicModel = dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();

        if (nVarsEvidence > varsDynamicModel.size()-1) {
            System.out.println("Too many variables to be observe");
            return null;
        }
        List<Variable> varsEvidence = new ArrayList<>(nVarsEvidence);
        double varEvidenceValue;

        int currentVarsEvidence=0;
        while (currentVarsEvidence < nVarsEvidence) {
            int indexVarEvidence = random.nextInt(dynamicBayesianNetwork.getNumberOfDynamicVars());
            Variable varEvidence = varsDynamicModel.get(indexVarEvidence);

            if (varEvidence.equals(mapVariable) || varsEvidence.contains(varEvidence)) {
                continue;
            }
            varsEvidence.add(varEvidence);
            currentVarsEvidence++;
        }

        List<DynamicAssignment> evidence = new ArrayList<>(nTimeSteps);

        // Evidence in t=0
        BayesianNetworkSampler bayesianNetworkSampler0 = new BayesianNetworkSampler(dynamicBayesianNetwork.toBayesianNetworkTime0());
        HashMapDynamicAssignment dynAssignment = new HashMapDynamicAssignment(varsEvidence.size());
        DataInstance sample = bayesianNetworkSampler0.sampleToDataStream(1).stream().findFirst().get();
        varsEvidence.forEach(var -> dynAssignment.setValue(var, sample.getValue(var)));
        evidence.add(dynAssignment);

        // Evidence from t=1 to t=(nTimeSteps-2)
        BayesianNetworkSampler bayesianNetworkSamplerT = new BayesianNetworkSampler(dynamicBayesianNetwork.toBayesianNetworkTimeT());
        for (int t = 1; t < nTimeSteps-1; t++) {
            HashMapDynamicAssignment dynAssignment1 = new HashMapDynamicAssignment(varsEvidence.size());
            DataInstance sample1 = bayesianNetworkSamplerT.sampleToDataStream(1).stream().findFirst().get();
            varsEvidence.forEach(var -> dynAssignment1.setValue(var, sample1.getValue(var)));
            evidence.add(dynAssignment1);
        }

        // Evidence in t=(nTimeSteps-1) for all variables that are leaves in the DAG.
        int t=nTimeSteps-1;

        for (Variable currentVar : varsDynamicModel) {
            if (!varsDynamicModel.stream().anyMatch(var -> dynamicBayesianNetwork.getDynamicDAG().getParentSetTimeT(var).getParents().contains(currentVar))) {
                varsEvidence.add(currentVar);
            }
        }

        HashMapDynamicAssignment dynAssignment2 = new HashMapDynamicAssignment(varsEvidence.size());
        DataInstance sample2 = bayesianNetworkSamplerT.sampleToDataStream(1).stream().findFirst().get();
        varsEvidence.forEach(var -> dynAssignment2.setValue(var, sample2.getValue(var)));
        evidence.add(dynAssignment2);

        return evidence;



    }



    public static List<DynamicAssignment> generateRandomEvidenceWithObservedLeavesDBNSampler(DynamicBayesianNetwork dynamicBayesianNetwork, Variable mapVariable, int nTimeSteps, int nVarsEvidence, Random random) {


        List<Variable> varsDynamicModel = dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();

        if (nVarsEvidence > varsDynamicModel.size()-1) {
            System.out.println("Too many variables to be observe");
            return null;
        }

        List<Variable> varsEvidence = new ArrayList<>(nVarsEvidence);

        int currentVarsEvidence=0;
        while (currentVarsEvidence < nVarsEvidence) {
            int indexVarEvidence = random.nextInt(dynamicBayesianNetwork.getNumberOfDynamicVars());
            Variable varEvidence = varsDynamicModel.get(indexVarEvidence);

            if (varEvidence.equals(mapVariable) || varsEvidence.contains(varEvidence)) {
                continue;
            }
            varsEvidence.add(varEvidence);
            currentVarsEvidence++;
        }

        List<DynamicAssignment> fullEvidence, evidence;
        evidence = new ArrayList<>(nTimeSteps);

        DynamicBayesianNetworkSampler bayesianNetworkSampler0 = new DynamicBayesianNetworkSampler(dynamicBayesianNetwork);
        bayesianNetworkSampler0.setSeed(random.nextInt());
        DataStream<DynamicDataInstance> sample = bayesianNetworkSampler0.sampleToDataBase(1,nTimeSteps);

        //sample.getAttributes().getFullListOfAttributes().forEach(attribute -> System.out.println(attribute.getName()));
        fullEvidence = sample.stream().collect(Collectors.toList());

//        System.out.println("Full Sample:");
//        fullEvidence.stream().forEachOrdered(dynass -> System.out.println(dynass.outputString(dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables())));
//        System.out.println();


        // Evidence in t=0, 1, ..., nTimeSteps-2 for variables in 'varsEvidence'
        IntStream.range(0,nTimeSteps-1).forEachOrdered(t -> {
            HashMapDynamicAssignment dynamicAssignment = new HashMapDynamicAssignment(varsEvidence.size());
            dynamicAssignment.setSequenceID(0);
            dynamicAssignment.setTimeID(t);
            varsDynamicModel.stream().filter(variable -> varsEvidence.contains(variable)).forEach( variable -> dynamicAssignment.setValue(variable,fullEvidence.get(t).getValue(variable)));
            evidence.add(dynamicAssignment);
        });

        // Evidence in t=(nTimeSteps-1) for all variables that are leaves in the DAG.
        for (Variable currentVar : varsDynamicModel) {
            if (!varsDynamicModel.stream().anyMatch(var -> dynamicBayesianNetwork.getDynamicDAG().getParentSetTimeT(var).getParents().contains(currentVar))) {
                varsEvidence.add(currentVar);
            }
        }
        HashMapDynamicAssignment dynamicAssignment1 = new HashMapDynamicAssignment(varsEvidence.size());
        dynamicAssignment1.setSequenceID(0);
        dynamicAssignment1.setTimeID(nTimeSteps-1);
        varsDynamicModel.stream().filter(variable -> varsEvidence.contains(variable)).forEach( variable -> dynamicAssignment1.setValue(variable,fullEvidence.get(nTimeSteps-1).getValue(variable)));
        evidence.add(dynamicAssignment1);

//        System.out.println("Evidence:");
//        evidence.stream().forEachOrdered(dynass -> System.out.println(dynass.outputString(dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables())));
//        System.out.println();

        StringBuilder classVarSequenceBuilder = new StringBuilder();
        classVarSequenceBuilder.append("ClassVar: (");
        fullEvidence.stream().forEachOrdered(dynass -> classVarSequenceBuilder.append( Integer.toString((int)dynass.getValue(mapVariable)) + ","));
        classVarSequenceBuilder.replace(classVarSequenceBuilder.lastIndexOf(","),classVarSequenceBuilder.lastIndexOf(",")+1,"");
        classVarSequenceBuilder.append(")");
        System.out.println("Original sequence:");
        System.out.println(classVarSequenceBuilder.toString());
        System.out.println();

        return evidence;

    }



    public static Assignment dynamicToStaticEvidence(DynamicVariables dynamicVariables, List<DynamicAssignment> dynamicEvidence, Variables staticVariables) {

        Assignment staticEvidence = new HashMapAssignment(staticVariables.getNumberOfVars());

        dynamicEvidence.stream().forEach(dynamicAssignment -> {
            int time = (int) dynamicAssignment.getTimeID();
            List<Variable> dynAssigVariables = dynamicVariables.getListOfDynamicVariables();
            for (Variable dynVariable : dynAssigVariables) {
                Variable staticVariable = staticVariables.getVariableByName(dynVariable.getName() + "_t" + Integer.toString(time));
                double varValue = dynamicAssignment.getValue(dynVariable);
                staticEvidence.setValue(staticVariable, varValue);
            }
        });
        return staticEvidence;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        int nContVars, nDiscreteVars, nTimeSteps, repetitions;
        String DBNtype="";

        int nStatesDiscreteVars = 2;
        int nStatesClassVariable = 2;


        if (args.length==5) {

            String a0 = args[0]; // Number of discrete variables
            String a1 = args[1]; // Number of continuous variables
            String a2 = args[2]; // Number of time steps
            String a3 = args[3]; // Number of repetitions of the experiment
            DBNtype = args[4]; // Type of Dynamic Bayesian Network (NB, TAN or FAN)

            try {

                nDiscreteVars = Integer.parseInt(a0);
                nContVars = Integer.parseInt(a1);
                nTimeSteps = Integer.parseInt(a2);
                repetitions = Integer.parseInt(a3);

            }
            catch (NumberFormatException e) {
                System.out.println(e.toString());
                System.exit(1);
                return;
            }

            if ( !DBNtype.equalsIgnoreCase("TAN") && !DBNtype.equalsIgnoreCase("NB") && !DBNtype.equalsIgnoreCase("FAN") ) {
                DBNtype="NB";
            }

        }
        // NO ARGUMENTS, DEFAULT INITIALIZATION
        else if (args.length==0) {

            nContVars = 2;
            nDiscreteVars = 8;
            nTimeSteps=20;

            DBNtype="TAN";
            repetitions = 10;
        }
        else {

            System.out.println("Invalid number of arguments. See comments in main");
            System.exit(1);
            return;
        }








        /*
         * BUILD A DYNAMIC NETWORK AND PRINTS IT
         */




        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(nContVars);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(nDiscreteVars);
        DynamicBayesianNetworkGenerator.setNumberOfStates(nStatesDiscreteVars);


        DynamicBayesianNetwork dynamicBayesianNetwork;

        switch (DBNtype) {

            case "TAN":
                dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicTAN(new Random(0), nStatesClassVariable, true);
                break;
            case "FAN":
                dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicFAN(new Random(0), nStatesClassVariable, true);
                break;
            case "NB":
            default:
                dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), nStatesClassVariable, true);
                break;

        }

        //System.out.println(dynamicBayesianNetwork.getDynamicDAG().toString());



        /*
         *  INITIALIZE THE DYNAMIC MAP OBJECT
         */


        Variable mapVariable = dynamicBayesianNetwork.getDynamicVariables().getVariableByName("ClassVar");

        DynamicMAPInference dynMAP = new DynamicMAPInference();
        dynMAP.setModel(dynamicBayesianNetwork);
        dynMAP.setNumberOfTimeSteps(nTimeSteps);
        dynMAP.setMAPvariable(mapVariable);



        Random random = new Random(1423);


        System.out.println("Dynamic MAP in a network of " + dynamicBayesianNetwork.getNumberOfDynamicVars() + " variables and " + dynamicBayesianNetwork.getDynamicDAG().toDAGTime0().getNumberOfLinks() + " links");
        System.out.println();

        BayesianNetwork staticBN = DynamicToStaticBNConverter.convertDBNtoBN(dynamicBayesianNetwork,nTimeSteps);
        Variables staticVariables = staticBN.getVariables();
//
//
//        List<Variable> mapVarReplications = staticVariables.getListOfVariables().stream().filter(var -> var.getName().contains(mapVariable.getName())).collect(Collectors.toList());
//
//
//        MAPInference mapInference = new MAPInference();
//
//        mapInference.setModel(staticBN);
//        mapInference.setParallelMode(true);
//        mapInference.setSeed(random.nextInt());
//        mapInference.setSampleSize(60);
//        mapInference.setNumberOfIterations(150);
//        mapInference.setMAPVariables(mapVarReplications);


        /*
         * GENERATE SEVERAL DYNAMIC MODELS AND COMPUTE MAP ESTIMATES FOR EACH
         */

        double[] probDynMapVMP = new double[repetitions];
        double[] probDynMapIS = new double[repetitions];

        double[] probStaticMapVMP = new double[repetitions];
        double[] probStaticMapIS = new double[repetitions];

        double[] probStaticMapHC = new double[repetitions];


        for (int k = 0; k < repetitions; k++) {

            System.out.println("Repetition " + k);


            dynamicBayesianNetwork.randomInitialization(new Random());

            //Force class variable values to be more stable over time
            Multinomial mm0 = dynamicBayesianNetwork.getConditionalDistributionTime0(mapVariable);
            mm0.setProbabilities(new double[]{0.5, 0.5});

            Multinomial_MultinomialParents mmT = dynamicBayesianNetwork.getConditionalDistributionTimeT(mapVariable);

            double P_same_state=0.9;
            mmT.getMultinomial(0).setProbabilities(new double[]{P_same_state, 1-P_same_state});
            mmT.getMultinomial(1).setProbabilities(new double[]{1-P_same_state, P_same_state});


            System.out.println(dynamicBayesianNetwork.toString());

            DynamicBayesianNetworkWriter.saveToFile(dynamicBayesianNetwork,"/Users/dario/Desktop/randomDynTAN.dbn");

            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(dynamicBayesianNetwork);
            dynMAP.setMAPvariable(mapVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);

            DynamicVariables dynamicVariables = dynamicBayesianNetwork.getDynamicVariables();

            /*
             * GENERATE AN EVIDENCE FOR T=0,...,nTimeSteps-1
             */
            int nVarsEvidence = 10;

            //List<DynamicAssignment> evidence = generateRandomEvidenceWithObservedLeaves(dynamicBayesianNetwork, mapVariable, nTimeSteps, nVarsEvidence, random);
            //List<DynamicAssignment> evidence = generateRandomEvidenceWithObservedLeavesBNSampler(dynamicBayesianNetwork, mapVariable, nTimeSteps, nVarsEvidence, random);
            List<DynamicAssignment> evidence = generateRandomEvidenceWithObservedLeavesDBNSampler(dynamicBayesianNetwork, mapVariable, nTimeSteps, nVarsEvidence, random);


//                    System.out.println("EVIDENCE:");
//        evidence.forEach(evid -> {
//            System.out.println("Evidence at time " + evid.getTimeID());
//            evid.getVariables().forEach(variable -> System.out.println(variable.getName() + ": " + Integer.toString((int) evid.getValue(variable))));
//            System.out.println();
//        });


            Assignment staticEvidence = dynamicToStaticEvidence(dynamicVariables, evidence, staticVariables);


            /*
             *  SET THE EVIDENCE AND MAKE INFERENCE
             */
            long timeStart, timeStop;
            double execTime;



            // MAP OVER THE GROUPED VARIABLE NETWORKS WITH VMP

            timeStart = System.nanoTime();

            dynMAP.setEvidence(evidence);
            dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);

            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;

            List<Variable> dynMAPstaticMAPVariables = dynMAP.getReplicatedMAPVariables();

            System.out.println("Dynamic MAP with VMP. Execution time: " + execTime + ", MAP sequence:");
            System.out.println(MAPsequenceToString(mapVariable,dynMAPstaticMAPVariables,dynMAP.getMAPestimate()));
            System.out.println();

            Assignment MAPsequenceDynVMP = dynMAP.getMAPestimate();


            // MAP OVER THE GROUPED VARIABLE NETWORKS WITH IS


            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(dynamicBayesianNetwork);
            dynMAP.setMAPvariable(mapVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);
            dynMAP.setSampleSize(2000);

            timeStart = System.nanoTime();

            dynMAP.setEvidence(evidence);
            dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);

            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;


            System.out.println("Dynamic MAP with IS. Execution time: " + execTime + ", MAP sequence:");
            System.out.println(MAPsequenceToString(mapVariable,dynMAPstaticMAPVariables,dynMAP.getMAPestimate()));
            System.out.println();

            Assignment MAPsequenceDynIS = dynMAP.getMAPestimate();


            // MAP OVER THE STATIC NETWORK WITH VMP

            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(dynamicBayesianNetwork);
            dynMAP.setMAPvariable(mapVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);

            timeStart = System.nanoTime();

            dynMAP.setEvidence(evidence);
            dynMAP.runInferenceUngroupedMAPVariable(DynamicMAPInference.SearchAlgorithm.VMP);

            dynMAPstaticMAPVariables = dynMAP.getReplicatedMAPVariables();

            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;


            System.out.println("Static MAP with VMP. Execution time: " + execTime + ", MAP sequence:");
            System.out.println(MAPsequenceToString(mapVariable,dynMAPstaticMAPVariables,dynMAP.getMAPestimate()));
            System.out.println();

            Assignment MAPsequenceStatVMP = dynMAP.getMAPestimate();


            // MAP OVER THE STATIC NETWORK WITH IS

            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(dynamicBayesianNetwork);
            dynMAP.setMAPvariable(mapVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);
            dynMAP.setSampleSize(2000);

            timeStart = System.nanoTime();

            dynMAP.setEvidence(evidence);
            dynMAP.runInferenceUngroupedMAPVariable(DynamicMAPInference.SearchAlgorithm.IS);

            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;


            System.out.println("Static MAP with IS. Execution time: " + execTime + ", MAP sequence:");
            System.out.println(MAPsequenceToString(mapVariable,dynMAPstaticMAPVariables,dynMAP.getMAPestimate()));
            System.out.println();

            Assignment MAPsequenceStatIS = dynMAP.getMAPestimate();


            // MAP OVER THE STATIC NETWORK WITH HILL CLIMBING


            staticBN = DynamicToStaticBNConverter.convertDBNtoBN(dynamicBayesianNetwork,nTimeSteps);
            Variables staticVariables1 = staticBN.getVariables();

            List<Variable> mapVarReplications = staticVariables1.getListOfVariables().stream().filter(var -> var.getName().contains(mapVariable.getName())).collect(Collectors.toList());


            MAPInference mapInference = new MAPInference();

            mapInference.setModel(staticBN);
            mapInference.setParallelMode(true);
            mapInference.setSeed(random.nextInt());
            mapInference.setSampleSize(20);
            mapInference.setNumberOfIterations(100);
            mapInference.setMAPVariables(mapVarReplications);


            timeStart = System.nanoTime();

            mapInference.setEvidence(staticEvidence);
            mapInference.runInference();

            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            System.out.println("Static MAP with HC. Execution time: " + execTime + ", MAP sequence:");
            System.out.println(MAPsequenceToString(mapVariable,mapVarReplications,mapInference.getEstimate()));
            System.out.println();

            Assignment MAPsequenceStatHC = mapInference.getEstimate();


            /*
             *
             */

            Assignment extendedEvidenceDynVMP = new HashMapAssignment(staticEvidence);
            Assignment extendedEvidenceDynIS = new HashMapAssignment(staticEvidence);
            Assignment extendedEvidenceStatVMP = new HashMapAssignment(staticEvidence);
            Assignment extendedEvidenceStatIS = new HashMapAssignment(staticEvidence);
            Assignment extendedEvidenceStatHC = new HashMapAssignment(staticEvidence);


            MAPsequenceDynVMP.getVariables().stream().forEach(var -> extendedEvidenceDynVMP.setValue(staticVariables1.getVariableByName(var.getName()), MAPsequenceDynVMP.getValue(var)));
            MAPsequenceDynIS.getVariables().stream().forEach(var -> extendedEvidenceDynIS.setValue(staticVariables1.getVariableByName(var.getName()), MAPsequenceDynIS.getValue(var)));

            MAPsequenceStatVMP.getVariables().stream().forEach(var -> extendedEvidenceStatVMP.setValue(staticVariables1.getVariableByName(var.getName()), MAPsequenceStatVMP.getValue(var)));
            MAPsequenceStatIS.getVariables().stream().forEach(var -> extendedEvidenceStatIS.setValue(staticVariables1.getVariableByName(var.getName()), MAPsequenceStatIS.getValue(var)));
            MAPsequenceStatHC.getVariables().stream().forEach(var -> extendedEvidenceStatHC.setValue(staticVariables1.getVariableByName(var.getName()), MAPsequenceStatHC.getValue(var)));


            /*
             *  ESTIMATION OF PROBABILITIES OF MAP SEQUENCES WITH IMPORTANCE SAMPLING AND A LARGE SAMPLE
             */

            ImportanceSampling importanceSampling = new ImportanceSampling();

            importanceSampling.setModel(staticBN);
            importanceSampling.setKeepDataOnMemory(false);
            importanceSampling.setParallelMode(true);
            importanceSampling.setSeed(random.nextInt());
            importanceSampling.setSampleSize(200000);

            System.out.println("Estimating probabilities with Importance Sampling");

            try {
                importanceSampling.setEvidence(extendedEvidenceDynVMP);
                importanceSampling.runInference();

                probDynMapVMP[k] = importanceSampling.getLogProbabilityOfEvidence();
                System.out.println("Prob DynMAP VMP: " + Double.toString(probDynMapVMP[k]));
            }
            catch (Exception e ) {
                System.out.println("Error estimating Prob DynMAP VMP");
                System.out.println(e.getMessage());
            }

            try {
                importanceSampling.setEvidence(extendedEvidenceDynIS);
                importanceSampling.runInference();

                probDynMapIS[k] = importanceSampling.getLogProbabilityOfEvidence();
                System.out.println("Prob DynMAP IS: " + Double.toString(probDynMapIS[k]));
            }
            catch (Exception e ) {
                System.out.println("Error estimating Prob DynMAP IS");
                System.out.println(e.getMessage());
            }

            try {
                importanceSampling.setEvidence(extendedEvidenceStatVMP);
                importanceSampling.runInference();

                probStaticMapVMP[k] = importanceSampling.getLogProbabilityOfEvidence();
                System.out.println("Prob StatMAP VMP: " + Double.toString(probStaticMapVMP[k]));
            }
            catch (Exception e ) {
                System.out.println("Error estimating Prob StatMAP VMP");
                System.out.println(e.getMessage());
            }

            try {
                importanceSampling.setEvidence(extendedEvidenceStatIS);
                importanceSampling.runInference();

                probStaticMapIS[k] = importanceSampling.getLogProbabilityOfEvidence();
                System.out.println("Prob StatMAP IS: " + Double.toString(probStaticMapIS[k]));
            }
            catch (Exception e ) {
                System.out.println("Error estimating Prob StatMAP IS");
                System.out.println(e.getMessage());
            }

            try {
                importanceSampling.setEvidence(extendedEvidenceStatHC);
                importanceSampling.runInference();

                probStaticMapHC[k] = importanceSampling.getLogProbabilityOfEvidence();
                System.out.println("Prob StatMAP HC: " + Double.toString(probStaticMapHC[k]));
            }
            catch (Exception e ) {
                System.out.println("Error estimating Prob StatMAP HC");
                System.out.println(e.getMessage());
            }

            System.out.println();
        }

        System.out.println(Arrays.toString(probDynMapVMP).replace("[","c(").replace("]",")"));
        System.out.println(Arrays.toString(probDynMapIS).replace("[","c(").replace("]",")"));
        System.out.println(Arrays.toString(probStaticMapVMP).replace("[","c(").replace("]",")"));
        System.out.println(Arrays.toString(probStaticMapIS).replace("[","c(").replace("]",")"));
        System.out.println(Arrays.toString(probStaticMapHC).replace("[","c(").replace("]",")"));

//            /*
//             *  SHOW RESULTS
//             */
//                Assignment MAPestimate = dynMAP.getMAPestimate();
//                double MAPestimateProbability = dynMAP.getMAPestimateProbability();
//
//
//                //        System.out.println("MAP sequence over " + mapVariable.getName() + ":");
//                //        List<Variable> MAPvarReplications = MAPestimate.getVariables().stream().sorted((var1,var2) -> (var1.getVarID()>var2.getVarID()? 1 : -1)).collect(Collectors.toList());
//                //
//                //        StringBuilder sequence = new StringBuilder();
//                //        MAPvarReplications.stream().forEachOrdered(var -> sequence.append( Integer.toString((int)MAPestimate.getValue(var)) + ", "));
//                //        //System.out.println(MAPestimate.outputString(MAPvarReplications));
//                //        System.out.println(sequence.toString());
//                //        System.out.println("with probability prop. to: " + MAPestimateProbability);
//
//            }
//
//            double meanTime = Arrays.stream(executionTimes).summaryStatistics().getAverage();
//            System.out.println("With " + nTimeSteps + " replications");
//            System.out.println(Arrays.toString(executionTimes));
//            System.out.println("Mean time: " + meanTime);
//            System.out.println();


    }

    private static String MAPsequenceToString(Variable mapVariable, List<Variable> mapVarReplications, Assignment mapSequenceAssignment) {
        StringBuilder outputStringSequence = new StringBuilder();
        outputStringSequence.append(mapVariable.getName() + ": (");
        mapVarReplications.stream().forEachOrdered(var -> outputStringSequence.append(Integer.toString((int)mapSequenceAssignment.getValue(var)) + ","));
        outputStringSequence.replace(outputStringSequence.lastIndexOf(","),outputStringSequence.lastIndexOf(",")+1,"");
        outputStringSequence.append(")");
        return outputStringSequence.toString();
    }
}
