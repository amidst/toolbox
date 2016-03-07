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
public class HiddenLayerDynamicModel {

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

    /** Represents the number of states for each Hidden non-observable variables in the {@link DynamicBayesianNetwork} to be generated. */
    private int nStatesHidden = 2;

    /** Represents the number of Gaussian observable variables in the {@link DynamicBayesianNetwork} to be generated. */
    private int nObservableContinuousVars = 5;

    /** Represents the number of Multinomial Hidden (non-observable) variables in the {@link DynamicBayesianNetwork} to be generated. */
    private int nHiddenVars = 5;

    private String classVarName = "ClassVar";

    private String hiddenVarName = "HiddenVar";

    private String discreteVarName = "DiscreteVar";

    private String continuousVarName = "ContinuousVar";


    /**
     * Sets the seed for model generation repeatability.
     * @param seed an {@code int} that represents the seed.
     */
    public void setSeed(int seed) {
        this.seed = seed;
        random = new Random(seed);
    }

    /**
     * Sets the number of Multinomial Hidden (non-observable) variables for this DynamicBayesianNetworkGenerator.
     * @param nHiddenVars an {@code int} that represents the number of Gaussian variables.
     */
    public void setnHiddenContinuousVars(int nHiddenVars) {
        this.nHiddenVars = nHiddenVars;
    }

    /**
     * Sets the number of Gaussian observable variables for this DynamicBayesianNetworkGenerator.
     * @param nObservableContinuousVars an {@code int} that represents the number of Gaussian variables.
     */
    public void setnObservableContinuousVars(int nObservableContinuousVars) {
        this.nObservableContinuousVars = nObservableContinuousVars;
    }

    /**
     * Sets the number of Multinomial observable variables for this DynamicBayesianNetworkGenerator.
     * @param nObservableDiscreteVars an {@code int} that represents the number of Multinomial variables.
     */
    public void setnObservableDiscreteVars(int nObservableDiscreteVars) {
        this.nObservableDiscreteVars = nObservableDiscreteVars;
    }

    /**
     * Sets the number of the number of states of the Multinomial observable variables.
     * @param nStates an {@code int} that represents the number of states.
     */
    public void setnStates(int nStates) {
        this.nStates = nStates;
    }

    /**
     * Sets the number of the number of states of the Multinomial Hidden (non-observable) variables.
     * @param nStatesHidden an {@code int} that represents the number of states.
     */
    public void setnStatesHidden(int nStatesHidden) {
        this.nStatesHidden = nStatesHidden;
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

        // One hidden discrete variable
        Variable hiddenDiscreteVar = dynamicVariables.newMultinomialDynamicVariable("Discrete" + hiddenVarName, nStatesHidden);

        // Hidden continuous variables
        IntStream.range(1,nHiddenVars+1)
                .forEach(i -> dynamicVariables.newGaussianDynamicVariable("Continuous" + hiddenVarName + i));

        // Observable discrete variables
        IntStream.range(1, nObservableDiscreteVars+1)
                .forEach(i -> {
                    Variable discreteObservableVar = dynamicVariables.newMultinomialDynamicVariable(discreteVarName + Integer.toString(i), nStates);
                    observableVars.add(discreteObservableVar);
                });

        // Observable continuous variables
        IntStream.range(1,nObservableContinuousVars+1)
                .forEach(i -> {
                    Variable continuousObservableVar = dynamicVariables.newGaussianDynamicVariable(continuousVarName + Integer.toString(i));
                    observableVars.add(continuousObservableVar);
                });

        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        dag.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());

        dag.getParentSetsTimeT().stream()
            .filter(var -> !var.getMainVar().equals(classVar) && !var.getMainVar().equals(hiddenDiscreteVar))
            .forEach(w -> {
                w.addParent(classVar);
                String thisVarName = w.getMainVar().getName();
                if (thisVarName.contains(hiddenVarName)) {

                    w.addParent(dynamicVariables.getInterfaceVariable(w.getMainVar()));
                    w.addParent(hiddenDiscreteVar);

                    if ( !thisVarName.equals("Continuous" + hiddenVarName + "1")) {
                        String thisVarIndex = thisVarName.substring( ("Continuous" + hiddenVarName).length() );
                        w.addParent(dynamicVariables.getVariableByName( thisVarName.replace(thisVarIndex,Integer.toString(Integer.parseInt(thisVarIndex)-1))) );
                    }
                }
                else {
                    if(thisVarName.contains(discreteVarName)) {
                        w.addParent(hiddenDiscreteVar);
                    }
                    else {
                        dynamicVariables.getListOfDynamicVariables().stream()
                            .filter(variable -> variable.getName().contains("Continuous" + hiddenVarName))
                            .forEach(variable -> w.addParent(variable));
                    }
                }
            });


//
//
//        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(nObservableContinuousVars);
//        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(nObservableDiscreteVars);
//        DynamicBayesianNetworkGenerator.setNumberOfStates(nStates);
//
//        DynamicBayesianNetwork modelObservableVars = DynamicBayesianNetworkGenerator.generateDynamicTAN(random,nStatesClassVar,true);
//
//        System.out.println(modelObservableVars.getDynamicDAG().toString());
//
//        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(0);
//        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(nHiddenVars);
//        DynamicBayesianNetworkGenerator.setNumberOfStates(nStatesHidden);
//        DynamicBayesianNetworkGenerator.setDiscreteVarName("HiddenVar");
//        DynamicBayesianNetwork modelHiddenVars = DynamicBayesianNetworkGenerator.generateDynamicTAN(random,nStatesClassVar,true);
//
//        System.out.println(modelHiddenVars.getDynamicDAG().toString());
//
//
//        DynamicVariables dynamicVariables  = modelHiddenVars.getDynamicVariables();
//
//        modelObservableVars.getDynamicVariables().getListOfDynamicVariables().stream().filter(variable -> !variable.getName().equals("ClassVar"))
//                .forEach(variable -> {
//                    if (variable.isMultinomial()) {
//                        dynamicVariables.newMultinomialDynamicVariable(variable.getName(), variable.getNumberOfStates());
//                    }
//                    else {
//                        dynamicVariables.newGaussianDynamicVariable(variable.getName());
//                    }
//                });
//
//
//        DynamicDAG dag1 = modelHiddenVars.getDynamicDAG();
//        dag1.updateDynamicVariables(dynamicVariables);
//
//        //System.out.println(dag1.toString());
//
//        modelObservableVars.getDynamicVariables().getListOfDynamicVariables().stream().filter(variable -> !variable.getName().equals("ClassVar"))
//                .forEach(variable -> {
//                    System.out.println("Variable:" + variable.getName());
//                    modelObservableVars.getDynamicDAG().getParentSetTimeT(variable).forEach(parent -> {
//                        System.out.println("Parent:" + parent.getName());
//
//                        if(!parent.isInterfaceVariable()) {
//                            if (!parent.getName().equals("ClassVar")) {
//                                dag1.getParentSetTime0(dag1.getDynamicVariables().getVariableByName(variable.getName())).addParent(parent);
//                                dag1.getParentSetTimeT(dag1.getDynamicVariables().getVariableByName(variable.getName())).addParent(parent);
//                            } else {
//                                Variable hiddenVar = dag1.getDynamicVariables().getVariableByName("HiddenVar2");// + (1 + random.nextInt(nHiddenVars)) );
//                                dag1.getParentSetTime0(dag1.getDynamicVariables().getVariableByName(variable.getName())).addParent(hiddenVar);
//                                dag1.getParentSetTimeT(dag1.getDynamicVariables().getVariableByName(variable.getName())).addParent(hiddenVar);
//                            }
//                        }
//                        else {
//                            if (!parent.getName().equals("ClassVar")) {
//                                dag1.getParentSetTimeT(dag1.getDynamicVariables().getVariableByName(variable.getName())).addParent(parent);
//                            } else {
//                                Variable hiddenVar = dag1.getDynamicVariables().getVariableByName("HiddenVar2");// + (1 + random.nextInt(nHiddenVars)) );
//                                //dag1.getParentSetTime0(dag1.getDynamicVariables().getVariableByName(variable.getName())).addParent(hiddenVar.getInterfaceVariable());
//                                dag1.getParentSetTimeT(dag1.getDynamicVariables().getVariableByName(variable.getName())).addParent(hiddenVar.getInterfaceVariable());
//                                //dag1.getParentSetTimeT(dag1.getDynamicVariables().getVariableByName(variable.getName())).addParent(variable.getInterfaceVariable());
//                            }
//                        }
//                    });
//
////                    modelObservableVars.getDynamicDAG().getParentSetTimeT(variable).forEach(parent -> {
////
//////                        System.out.println("Parent:" + parent.getName());
////                        if (parent.isInterfaceVariable()) {
////                            dag1.getParentSetTimeT(dag1.getDynamicVariables().getVariableByName(variable.getName())).addParent(dynamicVariables.getInterfaceVariable( dag1.getDynamicVariables().getVariableByName(parent.getName().replace("_Interface","")) ));
////                        }
////                        else {
////                            if (!parent.getName().equals("ClassVar")) {
////                                dag1.getParentSetTimeT(dag1.getDynamicVariables().getVariableByName(variable.getName())).addParent(dag1.getDynamicVariables().getVariableByName(parent.getName()));
////                            } else {
////                                Variable hiddenVar = dag1.getDynamicVariables().getVariableByName("HiddenVar2");// + (1 + random.nextInt(nHiddenVars)));
////                                dag1.getParentSetTimeT(dag1.getDynamicVariables().getVariableByName(variable.getName())).addParent(hiddenVar);
////                            }
////                        }
////                    });
//                });
//
//        System.out.println(dag1.toString());
//        System.out.println(dag1.containCycles());
//        System.out.println(dag1.toDAGTime0().containCycles() + ", " + dag1.toDAGTimeT().containCycles());

        dynamicVariables.forEach(variable -> System.out.println(variable.getName() + " with " + variable.getNumberOfStates() + " states"));
        System.out.println(dag.toString());

        model = new DynamicBayesianNetwork(dag);
        model.randomInitialization(random);

        List<Variable> allVariables = model.getDynamicVariables().getListOfDynamicVariables();

        allVariables.stream().filter(Variable::isNormal).forEach(variable -> {
            Normal_MultinomialNormalParents distribution = model.getConditionalDistributionTimeT(variable);
//            System.out.println("\nVariable: " + variable.getName());

//            System.out.println(distribution.toString());
            IntStream.range(0,distribution.getNumberOfParentAssignments()).forEach(i -> {
                ConditionalLinearGaussian currentTerm = distribution.getNormal_NormalParentsDistribution(i);

                double[] parameters1 = currentTerm.getCoeffParents();
//                System.out.println(parameters1.length);
//                System.out.println(Arrays.toString(parameters1));

//                for (int j = 0; j < parameters1.length; j++) {
//                    parameters1[j] = 1;//Math.signum(parameters1[j]);
//                }
                //currentTerm.setIntercept(0);
                //currentTerm.setCoeffParents(parameters1);
                currentTerm.setVariance(0.1);
                if (variable.getName().contains(hiddenVarName)) {
                    currentTerm.setCoeffForParent(variable.getInterfaceVariable(), 1);
                }
//                System.out.println(distribution.getNormal_NormalParentsDistribution(i).toString());

            });
        });


        //System.out.println(model);
    }

    public List<DynamicAssignment> generateEvidence(int sequenceLength) {

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(model);
        sampler.setSeed(random.nextInt());

        //sampler.setHiddenVar(model.getDynamicVariables().getVariableByName("Discrete" + hiddenVarName));

        DataStream<DynamicDataInstance> fullSample = sampler.sampleToDataBase(1,sequenceLength);

//        model.getDynamicVariables().getListOfDynamicVariables().forEach(variable -> System.out.println(variable.getName() + fullSample.stream().findFirst().get().getValue(variable)));

        fullSample.stream().forEach(dynamicDataInstance1 -> System.out.println(dynamicDataInstance1.outputString(model.getDynamicVariables().getListOfDynamicVariables())));

        List<DynamicAssignment> sample = new ArrayList<>();

        fullSample.stream().forEachOrdered(dynamicDataInstance -> {
            DynamicAssignment dynamicAssignment = new HashMapDynamicAssignment(observableVars.size());
            observableVars.stream().forEach(var1 -> {
                dynamicAssignment.setValue(var1,dynamicDataInstance.getValue(var1));
//                System.out.println(dynamicDataInstance.getValue(var1));
            });
            sample.add(dynamicAssignment);
        });

        return sample;
    }

//    public List<DynamicAssignment> generateEvidence() {
//
//    }

    public void setProbabilityOfKeepingClass(double probKeeping) {
        if (model==null) {
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
    }


    public static void main(String[] args) {
        HiddenLayerDynamicModel hiddenModel = new HiddenLayerDynamicModel();

        hiddenModel.setnStatesClassVar(2);
        hiddenModel.setnStatesHidden(4);
        hiddenModel.setnStates(2);

        hiddenModel.setnHiddenContinuousVars(2);
        hiddenModel.setnObservableDiscreteVars(3);
        hiddenModel.setnObservableContinuousVars(5);

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
        List<DynamicAssignment> evidence = hiddenModel.generateEvidence(1000);

        evidence.forEach(dynamicAssignment -> System.out.println(dynamicAssignment.outputString(hiddenModel.observableVars)));



    }
}

