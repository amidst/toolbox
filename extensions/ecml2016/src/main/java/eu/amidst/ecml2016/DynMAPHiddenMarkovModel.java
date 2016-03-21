package eu.amidst.ecml2016;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.dynamic.variables.HashMapDynamicAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by dario on 24/02/16.
 */
public class DynMAPHiddenMarkovModel {

    private DynamicBayesianNetwork model;

    private List<Variable> observableVars;

    private int seed=23664;

    private Random random;

    /** Represents the number of Multinomial observable variables in the {@link DynamicBayesianNetwork} to be generated. */
    private int nObservableDiscreteVars = 10;

    /** Represents the number of states for each Multinomial observable variables in the {@link DynamicBayesianNetwork} to be generated. */
    private int nStates = 2;

    /** Represents the number of states for each Multinomial observable variables in the {@link DynamicBayesianNetwork} to be generated. */
    private int nStatesClassVar = 2;

    /** Represents the number of Gaussian observable variables in the {@link DynamicBayesianNetwork} to be generated. */
    private int nObservableContinuousVars = 5;

    private String classVarName = "ClassVar";

    private String continuousVarName = "ContinuousVar";

    private List<DynamicAssignment> lastEvidence;

    private double probabilityKeepClassState;

    /**
     * Sets the seed for model generation repeatability.
     * @param seed an {@code int} that represents the seed.
     */
    public void setSeed(int seed) {
        this.seed = seed;
        random = new Random(seed);
    }


    /**
     * Sets the number of Gaussian observable variables for this DynamicBayesianNetworkGenerator.
     * @param nObservableContinuousVars an {@code int} that represents the number of Gaussian variables.
     */
    public void setnObservableContinuousVars(int nObservableContinuousVars) {
        this.nObservableContinuousVars = nObservableContinuousVars;
    }


    /**
     * Sets the number of the number of states of the Multinomial observable variables.
     * @param nStates an {@code int} that represents the number of states.
     */
    public void setnStates(int nStates) {
        this.nStates = nStates;
    }

    /**
     * Sets the number of the number of states of the Multinomial class variable.
     * @param nStatesClassVar an {@code int} that represents the number of states.
     */
    public void setnStatesClassVar(int nStatesClassVar) {
        this.nStatesClassVar = nStatesClassVar;
    }

    public void generateModel() {

        random = new Random(seed);

        DynamicVariables dynamicVariables  = new DynamicVariables();
        observableVars = new ArrayList<>();

        // Class variable which is always discrete
        Variable classVar = dynamicVariables.newMultinomialDynamicVariable(classVarName, nStatesClassVar);
        observableVars.add(classVar);


        // Observable continuous variables
        IntStream.range(1,nObservableContinuousVars+1)
            .forEach(i -> {
                Variable continuousObservableVar = dynamicVariables.newGaussianDynamicVariable(continuousVarName + Integer.toString(i));
                observableVars.add(continuousObservableVar);
            });

        DynamicDAG dag = new DynamicDAG(dynamicVariables);
        dag.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());

        // Observable continuous variables
        IntStream.range(1,nObservableContinuousVars+1)
                .forEach(i -> {
                    Variable continuousObservableVar = dynamicVariables.getVariableByName(continuousVarName + Integer.toString(i));

                    //dag.getParentSetTime0(continuousObservableVar).addParent(classVar);
                    dag.getParentSetTimeT(continuousObservableVar).addParent(classVar);
                    if(i>=2) {
                        IntStream.range(1, i).forEach(j -> {
                            Variable parent = dynamicVariables.getVariableByName(continuousVarName + Integer.toString(j));
                            System.out.println("Parent" + parent.getName());
                            //dag.getParentSetTime0(continuousObservableVar).addParent(parent);
                            dag.getParentSetTimeT(continuousObservableVar).addParent(parent);
                        });
                    }
                });


        model = new DynamicBayesianNetwork(dag);
        model.randomInitialization(random);

        if (Double.isFinite(probabilityKeepClassState)) {
            this.setProbabilityOfKeepingClass(probabilityKeepClassState);
        }
    }

    public List<DynamicAssignment> generateEvidence(int sequenceLength) {

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(model);
        sampler.setSeed(random.nextInt());

        //sampler.setHiddenVar(model.getDynamicVariables().getVariableByName("Discrete" + hiddenVarName));

        DataStream<DynamicDataInstance> fullSample = sampler.sampleToDataBase(1,sequenceLength);

//        model.getDynamicVariables().getListOfDynamicVariables().forEach(variable -> System.out.println(variable.getName() + fullSample.stream().findFirst().get().getValue(variable)));

        //fullSample.stream().forEach(dynamicDataInstance1 -> System.out.println(dynamicDataInstance1.outputString(model.getDynamicVariables().getListOfDynamicVariables())));

        List<DynamicAssignment> sample = new ArrayList<>();

        fullSample.stream().forEachOrdered(dynamicDataInstance -> {
            DynamicAssignment dynamicAssignment = new HashMapDynamicAssignment(observableVars.size());
            ((HashMapDynamicAssignment)dynamicAssignment).setTimeID((int)dynamicDataInstance.getTimeID());
            observableVars.stream().forEach(var1 -> {
                dynamicAssignment.setValue(var1,dynamicDataInstance.getValue(var1));
//                System.out.println(dynamicDataInstance.getValue(var1));
            });
            sample.add(dynamicAssignment);
        });

        this.lastEvidence=sample;

        return sample;
    }


    public DynamicBayesianNetwork getModel() {
        return model;
    }

    public List<DynamicAssignment> getEvidence() {
        return lastEvidence;
    }

    public List<DynamicAssignment> getEvidenceNoClass() {

        List<DynamicAssignment> evidenceNoClass = new ArrayList<>();

        this.lastEvidence.forEach(dynamicAssignment -> {
            DynamicAssignment dynamicAssignmentNoClass = new HashMapDynamicAssignment(dynamicAssignment.getVariables().size()-1);
            ((HashMapDynamicAssignment)dynamicAssignmentNoClass).setTimeID((int)dynamicAssignment.getTimeID());
            dynamicAssignment.getVariables().stream()
                .filter(variable -> !variable.equals(this.getClassVariable()))
                .forEach(variable -> dynamicAssignmentNoClass.setValue(variable,dynamicAssignment.getValue(variable)));
            evidenceNoClass.add(dynamicAssignmentNoClass);
        });

        return evidenceNoClass;
    }

    public int[] getClassSequence() {

        int[] classSequence = new int[lastEvidence.size()];

        this.lastEvidence.forEach(dynamicAssignment -> {

            classSequence[(int) dynamicAssignment.getTimeID()] = (int) dynamicAssignment.getValue(this.getClassVariable());

        });
        return classSequence;
    }

    public Variable getClassVariable() {
        return model.getDynamicVariables().getVariableByName(classVarName);
    }

    public void setProbabilityOfKeepingClass(double probKeeping) {
        if (model==null) {
            this.probabilityKeepClassState=probKeeping;
            return;
        }
        Variable classVar = model.getDynamicVariables().getVariableByName(classVarName);
        Multinomial_MultinomialParents classVarCondDistribution = model.getConditionalDistributionTimeT(classVar);

        double probOtherStates = (1-probKeeping)/(classVar.getNumberOfStates()-1);

        IntStream.range(0,classVar.getNumberOfStates()).forEach(k -> {
            double[] probabilities = new double[classVar.getNumberOfStates()];
            for (int i = 0; i <classVar.getNumberOfStates(); i++) {
                if(i==k) {
                    probabilities[i] = probKeeping;
                }
                else {
                    probabilities[i] = probOtherStates;
                }

            }
            classVarCondDistribution.getMultinomial(k).setProbabilities(probabilities);
        });
        this.probabilityKeepClassState=probKeeping;
    }

    public void randomInitialization(Random random) {
        if(model!=null) {
            model.randomInitialization(random);


            List<Variable> allVariables = model.getDynamicVariables().getListOfDynamicVariables();

            model.randomInitialization(random);

            if(Double.isFinite(probabilityKeepClassState)) {
                this.setProbabilityOfKeepingClass(probabilityKeepClassState);
            }
        }

    }

    public void printDAG() {
        System.out.println(this.model.getDynamicDAG().toString());
    }

    public void printHiddenLayerModel() {
        System.out.println(this.model.toString());
    }

    public static void main(String[] args) {
        DynMAPHiddenMarkovModel hiddenModel = new DynMAPHiddenMarkovModel();

        hiddenModel.setnStatesClassVar(2);
        hiddenModel.setnStates(2);

        hiddenModel.setnObservableContinuousVars(3);

        hiddenModel.generateModel();

        System.out.println(hiddenModel.model.getDynamicDAG().toString());


        hiddenModel.setSeed((new Random()).nextInt());

//        System.out.println(hiddenModel.model.toString());

//        System.out.println("\nDYNAMIC VARIABLES");
//        hiddenModel.model.getDynamicVariables().forEach(var -> System.out.println(var.getName()));
//
//        System.out.println("\nOBSERVABLE VARIABLES");
//        hiddenModel.observableVars.forEach(var -> System.out.println(var.getName()));


        hiddenModel.setProbabilityOfKeepingClass(0.98);
        System.out.println(hiddenModel.model.toString());


        System.out.println("\nEVIDENCE");
        List<DynamicAssignment> evidence = hiddenModel.generateEvidence(10);

        evidence.forEach(dynamicAssignment -> System.out.println(dynamicAssignment.outputString(hiddenModel.observableVars)));



    }
}

