package eu.amidst.dynamic.inference;

import eu.amidst.core.distribution.*;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.*;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicToStaticBNConverter;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.dynamic.variables.HashMapDynamicAssignment;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * This class implements the MAP Inference algorithm for {@link DynamicBayesianNetwork} models.
 */
public class DynamicMAPInference {

    /**
     * Represents the search algorithm to be used.
     */
    public enum SearchAlgorithm {
        VMP, IS
    }

    /** Represents the {@link DynamicBayesianNetwork} model. */
    private DynamicBayesianNetwork model;

    /** Represents a {@link BayesianNetwork} object. */
    private BayesianNetwork staticEvenModel, staticOddModel, staticModel;

    /** Represents the number of time steps. */
    private int nTimeSteps = 2;

    /** Represents the MAP {@link Variable}. */
    private Variable MAPvariable;

    /** Represents the name of the MAP {@link Variable}. */
    private String MAPvarName;

    /** Represents a dynamic evidence defined as a list of {@link DynamicAssignment} objects. */
    private List<DynamicAssignment> evidence;

    /** Represents a static evidence defined as an {@link Assignment} object. */
    private Assignment staticEvidence;

    /** Represents the processing mode, either parallel (i.e., true) or not. */
    private boolean parallelMode = true;

    /** Represents the initial seed. */
    private int seed = 125123;

    /** Represents the MAP estimate defined as an {@link Assignment} object. */
    private Assignment MAPestimate;

    /** Represents the Log Probability of the MAP estimate. */
    private double MAPestimateLogProbability;

    String groupedClassName = "==GROUPED_CLASS==";

    /**
     * Sets the evidence for this DynamicMAPInference.
     * @param evidence a list of {@link DynamicAssignment} objects.
     */
    public void setEvidence(List<DynamicAssignment> evidence) {

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

        if (staticEvenModel!=null) {
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
        }
    }

    /**
     * Sets the model for this DynamicMAPInference.
     * @param model a {@link DynamicBayesianNetwork} object.
     */
    public void setModel(DynamicBayesianNetwork model) {
        this.model = model;
    }

    /**
     * Sets the parallel mode for this DynamicMAPInference.
     * @param parallelMode true if the parallel mode is activated, false otherwise.
     */
    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    /**
     * Sets the MAP variable for this DynamicMAPInference.
     * @param MAPvariable a valid {@link Variable} object.
     */
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

    /**
     * Sets the number of time steps.
     * @param ntimeSteps an {@code int} that represents the number of time steps.
     */
    public void setNumberOfTimeSteps(int ntimeSteps) {
        if(ntimeSteps<2) {
            System.out.println("Error: The number of time steps should be at least 2");
            System.exit(-10);
        }
        nTimeSteps = ntimeSteps;
    }

    /**
     *
     * @return
     */
    public Assignment getMAPestimate() {
        return MAPestimate;
    }

    public double getMAPestimateLogProbability() {
        return MAPestimateLogProbability;
    }

    public double getMAPestimateProbability() {
        return Math.exp(MAPestimateLogProbability);
    }

    /**
     * returns the static even model.
     * @return a {@link BayesianNetwork} object.
     */
    public BayesianNetwork getStaticEvenModel() {
        return staticEvenModel;
    }

    /**
     * returns the static odd model.
     * @return a {@link BayesianNetwork} object.
     */
    public BayesianNetwork getStaticOddModel() {
        return staticOddModel;
    }

    /**
     * Returns replicated MAP variables.
     * @return a {@code List} of {@link Variable}.
     */
    public List<Variable> getReplicatedMAPVariables() {
        if (MAPestimate==null) {
            return null;
        }
        return getMAPestimate().getVariables().stream().sorted((var1,var2) -> (var1.getVarID()>var2.getVarID() ? 1 : -1)).collect(Collectors.toList());
    }

    /**
     * Computes Dynamic MAP for the even static model.
     */
    public void computeDynamicMAPEvenModel() {

        Variables variables;
        DynamicDAG dynamicDAG;
        DAG dag;
        BayesianNetwork bn;
        DynamicVariables dynamicVariables = model.getDynamicVariables();

        variables = obtainReplicatedStaticVariables(dynamicVariables, true);

        dynamicDAG = model.getDynamicDAG();
        dag = obtainStaticDAG(dynamicDAG, variables, true);
        bn = this.obtainStaticGroupedClassBayesianNetwork(dag, variables, true);

        this.staticEvenModel = bn;
    }

    /**
     * Computes Dynamic MAP for the odd static model.
     */
    public void computeDynamicMAPOddModel() {

        Variables variables;
        DynamicDAG dynamicDAG;
        DAG dag;
        BayesianNetwork bn;
        DynamicVariables dynamicVariables = model.getDynamicVariables();

        variables = obtainReplicatedStaticVariables(dynamicVariables, false);

        dynamicDAG = model.getDynamicDAG();
        dag = obtainStaticDAG(dynamicDAG, variables, false);

        bn = this.obtainStaticGroupedClassBayesianNetwork(dag, variables, false);

        this.staticOddModel = bn;
    }

    /**
     * Runs the inference algorithm.
     */
    public void runInference() {

        if (MAPvariable==null || MAPvarName==null) {
            System.out.println("Error: The MAP variable has not been set");
            System.exit(-30);
        }

        if (this.staticOddModel == null) {
            this.computeDynamicMAPOddModel();
        }
        if (this.staticEvenModel == null) {
            this.computeDynamicMAPEvenModel();
        }

        if (evidence!=null && staticEvidence==null) {

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
        }
        this.runInference(SearchAlgorithm.VMP);
    }

    /**
     * Runs the inference given an input search algorithm.
     * @param searchAlgorithm a valid {@link SearchAlgorithm} value.
     */
    public void runInference(SearchAlgorithm searchAlgorithm) {

        if (MAPvariable==null || MAPvarName==null) {
            System.out.println("Error: The MAP variable has not been set");
            System.exit(-30);
        }

        if (this.staticOddModel == null) {
            this.computeDynamicMAPOddModel();
        }
        if (this.staticEvenModel == null) {
            this.computeDynamicMAPEvenModel();
        }

        if (evidence!=null && staticEvidence==null) {

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
        }

        InferenceAlgorithm evenModelInference, oddModelInference;
        switch(searchAlgorithm) {
            case VMP:
                //        long timeStart = System.nanoTime();
                evenModelInference = new VMP();
                oddModelInference = new VMP();

                break;

            case IS:
            default:

                evenModelInference = new ImportanceSampling();
                oddModelInference = new ImportanceSampling();

                Random random = new Random((this.seed));
                oddModelInference.setSeed(random.nextInt());
                evenModelInference.setSeed(random.nextInt());

                break;
        }

        IntStream.range(0, 2).parallel().forEach(i -> {
            if (i == 0) {
                evenModelInference.setParallelMode(this.parallelMode);
                evenModelInference.setModel(staticEvenModel);
                if (evidence != null) {
                    evenModelInference.setEvidence(staticEvidence);
                }
                evenModelInference.runInference();
            }
            else {
                oddModelInference.setParallelMode(this.parallelMode);
                oddModelInference.setModel(staticOddModel);
                if (evidence != null) {
                    oddModelInference.setEvidence(staticEvidence);
                }
                oddModelInference.runInference();
            }
        });

        List<UnivariateDistribution> posteriorMAPDistributionsEvenModel = new ArrayList<>();
        List<UnivariateDistribution> posteriorMAPDistributionsOddModel = new ArrayList<>();

        int replicationsMAPVariableEvenModel = nTimeSteps/2 + nTimeSteps%2;
        IntStream.range(0,replicationsMAPVariableEvenModel).forEachOrdered(i -> posteriorMAPDistributionsEvenModel.add(evenModelInference.getPosterior(i)));

        int replicationsMAPVariableOddModel = 1 + (nTimeSteps-1)/2 + (nTimeSteps-1)%2;
        IntStream.range(0,replicationsMAPVariableOddModel).forEachOrdered(i -> posteriorMAPDistributionsOddModel.add(oddModelInference.getPosterior(i)));

        List<double[]> conditionalDistributionsMAPvariable = getCombinedConditionalDistributions(posteriorMAPDistributionsEvenModel, posteriorMAPDistributionsOddModel);
        computeMostProbableSequence(conditionalDistributionsMAPvariable);

    }

    /**
     * Runs the inference for Ungrouped MAP variable given an input search algorithm.
     * @param searchAlgorithm a valid {@link SearchAlgorithm} value.
     */
    public void runInferenceUngroupedMAPVariable(SearchAlgorithm searchAlgorithm) {

        if (MAPvariable==null || MAPvarName==null) {
            System.out.println("Error: The MAP variable has not been set");
            System.exit(-30);
        }

        if (this.staticModel == null) {
            staticModel = DynamicToStaticBNConverter.convertDBNtoBN(model,nTimeSteps);
        }

        if (evidence!=null && staticEvidence==null) {

            staticEvidence = new HashMapAssignment(staticModel.getNumberOfVars());

            evidence.stream().forEach(dynamicAssignment -> {
                int time = (int) dynamicAssignment.getTimeID();
                Set<Variable> dynAssigVariables = dynamicAssignment.getVariables();
                for (Variable dynVariable : dynAssigVariables) {
                    Variable staticVariable = staticModel.getVariables().getVariableByName(dynVariable.getName() + "_t" + Integer.toString(time));
                    double varValue = dynamicAssignment.getValue(dynVariable);
                    staticEvidence.setValue(staticVariable, varValue);
                }

            });
        }

        InferenceAlgorithm staticModelInference;
        switch(searchAlgorithm) {
            case VMP:
                staticModelInference = new VMP();
                break;

            case IS:
            default:
                ImportanceSampling importanceSampling =  new ImportanceSampling();
                importanceSampling.setSampleSize(15*staticModel.getNumberOfVars());
                importanceSampling.setKeepDataOnMemory(true);
                Random random = new Random((this.seed));
                importanceSampling.setSeed(random.nextInt());
                staticModelInference=importanceSampling;
                break;
        }


        staticModelInference.setParallelMode(this.parallelMode);
        staticModelInference.setModel(staticModel);
        if (evidence != null) {
            staticModelInference.setEvidence(staticEvidence);
        }
        staticModelInference.runInference();

        List<UnivariateDistribution> posteriorMAPDistributionsStaticModel = new ArrayList<>();
        IntStream.range(0,nTimeSteps).forEachOrdered(i -> posteriorMAPDistributionsStaticModel.add(staticModelInference.getPosterior(i)));

        double [] probabilities = posteriorMAPDistributionsStaticModel.stream().map(dist -> argMax(dist.getParameters())).mapToDouble(array -> array[0]).toArray();
        double MAPsequenceProbability = Math.exp(Arrays.stream(probabilities).map(prob -> Math.log(prob)).sum());

        int [] MAPsequence = posteriorMAPDistributionsStaticModel.stream().map(dist -> argMax(dist.getParameters())).mapToInt(array -> (int) array[1]).toArray();

        MAPestimate = new HashMapAssignment(nTimeSteps);
        IntStream.range(0,nTimeSteps).forEach(t-> {
            Variables variables = Serialization.deepCopy(this.staticModel.getVariables());
            Variable currentVar;
            if (variables.getVariableByName(MAPvarName + "_t" + Integer.toString(t))!=null) {
                currentVar  = variables.getVariableByName(MAPvarName + "_t" + Integer.toString(t));
            }
            else {
                currentVar  = variables.newMultionomialVariable(MAPvarName + "_t" + Integer.toString(t), MAPvariable.getNumberOfStates());
            }

            MAPestimate.setValue(currentVar,MAPsequence[t]);
        });
        MAPestimateLogProbability = Math.log(MAPsequenceProbability);
    }

    /**
     * Computes the Most Probable Sequence given the conditional distributions of the MAP variable.
     * @param conditionalDistributionsMAPvariable a {@code List} of conditional distribution values.
     */
    private void computeMostProbableSequence(List<double[]> conditionalDistributionsMAPvariable) {
        int[] MAPsequence = new int[nTimeSteps];
        int MAPvarNStates = MAPvariable.getNumberOfStates();

        int[] argMaxValues = new int[nTimeSteps];

        double MAPsequenceProbability=-1;
        double [] current_probs;
        double [] current_max_probs;
        double [] previous_max_probs = new double[MAPvarNStates];;

        for (int t = nTimeSteps-1; t >= 0; t--) {

            current_probs = conditionalDistributionsMAPvariable.get(t);
            double maxProb=-1;

            current_max_probs = new double[MAPvarNStates];

            if (t==(nTimeSteps-1)) { // There are no previous_max_probs
                for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_{t-1}
                    maxProb=-1;
                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_t
                        if (current_probs[j * MAPvarNStates + k] > maxProb) {
                            maxProb = current_probs[j * MAPvarNStates + k];

                        }
                    }
                    current_max_probs[j]=maxProb;
                }
                argMaxValues[t] = (int)argMax(current_max_probs)[1];
                previous_max_probs = current_max_probs;
            }
            else if (t>0 && t<(nTimeSteps-1)) {
                for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_{t-1}
                    maxProb=-1;
                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_t
                        if (current_probs[j * MAPvarNStates + k]*previous_max_probs[j] > maxProb) {
                            maxProb = current_probs[j * MAPvarNStates + k]*previous_max_probs[k];
                        }
                    }
                    current_max_probs[j]=maxProb;
                }
                argMaxValues[t] = (int)argMax(current_max_probs)[1];
                previous_max_probs = current_max_probs;
            }
            else { // Here, t=0
                for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_0
                    maxProb=-1;
                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_1
                        if (current_probs[j]*previous_max_probs[j] > maxProb) {
                            maxProb = current_probs[j]*previous_max_probs[j];
                        }
                    }
                    current_max_probs[j]=maxProb;
                }
                MAPsequenceProbability =  argMax(current_max_probs)[0];
                argMaxValues[t] = (int)argMax(current_max_probs)[1];
                previous_max_probs = current_max_probs;
            }
        }

        int previousVarMAPState = argMaxValues[0];
        MAPsequence[0] = argMaxValues[0];

        int thisVarMAPState = 0;
        for (int t = 1; t < nTimeSteps; t++) {
            current_probs = conditionalDistributionsMAPvariable.get(t);
            previousVarMAPState = argMaxValues[t-1];

            double maxProb = -1;
            for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_t

                if (current_probs[previousVarMAPState * MAPvarNStates + j] >= maxProb) {
                    maxProb = current_probs[previousVarMAPState * MAPvarNStates + j];
                    thisVarMAPState = j;
                }
            }
            MAPsequence[t]=thisVarMAPState;
        }

        MAPestimate = new HashMapAssignment(nTimeSteps);

        if (Arrays.stream(MAPsequence).anyMatch(value -> value<0)) {
            MAPestimateLogProbability=Double.NaN;
        }
        else {
            IntStream.range(0, nTimeSteps).forEach(t -> {
                Variable currentVar = this.staticEvenModel.getVariables().newMultionomialVariable(MAPvarName + "_t" + Integer.toString(t), MAPvariable.getNumberOfStates());
                MAPestimate.setValue(currentVar, MAPsequence[t]);
            });
            MAPestimateLogProbability = Math.log(MAPsequenceProbability);
        }
    }

    /**
     * Returns Combined Conditional Distributions for both even and odd models.
     * @param posteriorMAPDistributionsEvenModel a {@code List} of {@link UnivariateDistribution} of the even model.
     * @param posteriorMAPDistributionsOddModel a {@code List} of {@link UnivariateDistribution} of the odd model.
     * @return a {@code List} of conditional distributions values.
     */
    private List<double[]> getCombinedConditionalDistributions( List<UnivariateDistribution> posteriorMAPDistributionsEvenModel , List<UnivariateDistribution> posteriorMAPDistributionsOddModel) {

        List<double[]> listCondDistributions = new ArrayList<>(nTimeSteps);

        int MAPvarNStates = MAPvariable.getNumberOfStates();

        // Univariate distribution Y_0
        UnivariateDistribution dist0_1 = posteriorMAPDistributionsEvenModel.get(0); // This variable Z_0 groups Y_0 and Y_1
        UnivariateDistribution dist0 = posteriorMAPDistributionsOddModel.get(0); // This variable is just Y_0 (not a group)

        double[] dist0_probs = new double[MAPvariable.getNumberOfStates()];

        dist0_probs = dist0.getParameters();
        for (int i = 0; i < MAPvarNStates; i++) {
            dist0_probs[i] = (double) 1/2 * dist0_probs[i];

            for (int j = 0; j < MAPvarNStates; j++) {
                dist0_probs[i] = dist0_probs[i] + (double) 1/2 * dist0_1.getProbability(i*MAPvarNStates + j);
            }
        }
        listCondDistributions.add(dist0_probs);

        // Conditional distribution Y_1 | Y_0;
        UnivariateDistribution dist_paired1 = posteriorMAPDistributionsEvenModel.get(0); // This variable Z_0 groups Y_0 and Y_1
        UnivariateDistribution dist_unpaired_1 = posteriorMAPDistributionsOddModel.get(1); // This variable groups Y_1 and Y_2 (if nTimeSteps>2)

        double[] dist_probs1 = dist_paired1.getParameters();

        for (int i = 0; i < MAPvarNStates; i++) { // To go over all values of Y_0
            for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_1
                int index = i * MAPvarNStates + j;
                dist_probs1[index] = (double) 1/2 * dist_probs1[index];

                if (nTimeSteps>2) {
                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_2 in the distrib of (Y_1,Y_2)
                        dist_probs1[index] = dist_probs1[index] + (double) 1 / 2 * dist_unpaired_1.getProbability(j * MAPvarNStates + k);
                    }
                }
                else {
                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_2 in the distrib of (Y_1,Y_2)
                        dist_probs1[index] = dist_probs1[index] + (double) 1 / 2 * dist_unpaired_1.getProbability(j);
                    }
                }
            }
        }
        listCondDistributions.add(dist_probs1);

        IntStream.range(2, nTimeSteps - 1).forEachOrdered(t -> {
            if (t % 2 == 0) {
                int idxOdd = 1 + (t - 2) / 2;
                UnivariateDistribution dist_paired = posteriorMAPDistributionsOddModel.get(idxOdd); // This variable groups Y_t and Y_{t-1}

                UnivariateDistribution dist_unpaired_pre = posteriorMAPDistributionsEvenModel.get(idxOdd - 1); // This variable groups Y_{t-2} and Y_{t-1}
                UnivariateDistribution dist_unpaired_post = posteriorMAPDistributionsEvenModel.get(idxOdd); // This variable groups Y_t and Y_{t+1}

                double[] dist_probs = dist_paired.getParameters();

                for (int i = 0; i < MAPvarNStates; i++) {  // To go over all values of Y_{t-1}
                    for (int j = 0; j < MAPvarNStates; j++) {  // To go over all values of Y_t

                        int index = i * MAPvarNStates + j;
                        dist_probs[index] = (double) 1 / 2 * dist_probs[index];

                        for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_{t-2}
                            for (int m = 0; m < MAPvarNStates; m++) {  // To go over all values of Y_{t+1}
                                dist_probs[index] = dist_probs[index] + (double) 1 / 2 * dist_unpaired_pre.getProbability(k * MAPvarNStates + i) * dist_unpaired_post.getProbability(j * MAPvarNStates + m);
                            }
                        }
                    }
                }
                listCondDistributions.add(dist_probs);
            } else {
                int idxEven = (t - 1) / 2;
                UnivariateDistribution dist_paired = posteriorMAPDistributionsEvenModel.get(idxEven); // This variable groups Y_t and Y_{t-1}

                UnivariateDistribution dist_unpaired_pre = posteriorMAPDistributionsOddModel.get(idxEven); // This variable groups Y_{t-2} and Y_{t-1}
                UnivariateDistribution dist_unpaired_post = posteriorMAPDistributionsOddModel.get(idxEven + 1); // This variable groups Y_t and Y_{t+1}

                double[] dist_probs = dist_paired.getParameters();

                for (int i = 0; i < MAPvarNStates; i++) {  // To go over all values of Y_{t-1}
                    for (int j = 0; j < MAPvarNStates; j++) {  // To go over all values of Y_t

                        int index = i * MAPvarNStates + j;
                        dist_probs[index] = (double) 1 / 2 * dist_probs[index];

                        for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_{t-2}
                            for (int m = 0; m < MAPvarNStates; m++) {  // To go over all values of Y_{t+1}
                                dist_probs[index] = dist_probs[index] + (double) 1 / 2 * dist_unpaired_pre.getProbability(k * MAPvarNStates + i) * dist_unpaired_post.getProbability(j * MAPvarNStates + m);
                            }
                        }
                    }
                }
                listCondDistributions.add(dist_probs);
            }
        });

        if (nTimeSteps>2) {
            // Conditional distribution Y_t | Y_{t-1},  with  t = nTimeSteps-1
            int t = (nTimeSteps - 1);
            if (t % 2 == 0) {
                int idxOdd = 1 + (t - 2) / 2;
                UnivariateDistribution dist_paired = posteriorMAPDistributionsOddModel.get(idxOdd); // This variable groups Y_t and Y_{t-1}

                UnivariateDistribution dist_unpaired_pre = posteriorMAPDistributionsEvenModel.get(idxOdd - 1);  // This variable groups Y_{t-2} and Y_{t-1}
                UnivariateDistribution dist_unpaired_post = posteriorMAPDistributionsEvenModel.get(idxOdd); // This variable is just Y_t (not a group)

                double[] dist_probs = dist_paired.getParameters();

                for (int i = 0; i < MAPvarNStates; i++) {  // To go over all values of Y_{t-1}
                    for (int j = 0; j < MAPvarNStates; j++) {  // To go over all values of Y_t

                        int index = i * MAPvarNStates + j;
                        dist_probs[index] = (double) 1 / 2 * dist_probs[index];

                        for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_{t-2}

                            dist_probs[index] = dist_probs[index] + (double) 1 / 2 * dist_unpaired_pre.getProbability(k * MAPvarNStates + i) * dist_unpaired_post.getProbability(j);
                        }
                    }
                }
                listCondDistributions.add(dist_probs);
            }
            else {
                int idxEven = (t - 1) / 2;
                UnivariateDistribution dist_paired = posteriorMAPDistributionsEvenModel.get(idxEven); // This variable groups Y_t and Y_{t-1}

                UnivariateDistribution dist_unpaired_pre = posteriorMAPDistributionsOddModel.get(idxEven);  // This variable groups Y_{t-2} and Y_{t-1}

                double[] dist_probs = dist_paired.getParameters();

                for (int i = 0; i < MAPvarNStates; i++) {  // To go over all values of Y_{t-1}
                    for (int j = 0; j < MAPvarNStates; j++) {  // To go over all values of Y_t

                        int index = i * MAPvarNStates + j;
                        dist_probs[index] = (double) 1 / 2 * dist_probs[index];

                        for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_{t-2}
                            dist_probs[index] = dist_probs[index] + (double) 1 / 2 * dist_unpaired_pre.getProbability(k * MAPvarNStates + i);
                        }
                    }
                }
                listCondDistributions.add(dist_probs);
            }
        }

        return listCondDistributions;
    }

    /**
     * Returns the max value and its corresponding index.
     * @param values an array of {@code double} values.
     * @return the max value and its corresponding index.
     */
    private double[] argMax(double[] values) {

        double maxValue = Arrays.stream(values).max().getAsDouble();
        double indexMaxValue=-1;

        for (int i = 0; i < values.length; i++) {
            if (values[i]==maxValue) {
                indexMaxValue=i;
            }
        }
        return new double[]{maxValue, indexMaxValue};
    }

    /**
     * Returns the replicated static set of variables
     * @param dynamicVariables a {@link DynamicVariables} object.
     * @param even_partition a {@code boolean} that indicates whether the partition is even or not.
     * @return a {@link Variables} object.
     */
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

        // REPLICATIONS OF THE REST OF VARIABLES (EACH ONE REPEATED 'nTimeSteps' TIMES)
        dynamicVariables.getListOfDynamicVariables().stream()
            .filter(var -> !var.equals(MAPvariable))
            .forEach(dynVar ->
                            IntStream.range(0, nTimeSteps).forEach(i -> {
                                VariableBuilder aux = dynVar.getVariableBuilder();
                                aux.setName(dynVar.getName() + "_t" + Integer.toString(i));
                                variables.newVariable(aux);
                            })
            );

        return variables;
    }

    /**
     * Returns the static DAG structure.
     * @param dynamicDAG a {@link DynamicDAG} object.
     * @param variables a {@link Variables} object.
     * @param even_partition a {@code boolean} that indicates whether the partition is even or not.
     * @return a {@link DAG} object.
     */
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
        return dag;
    }

    /**
     * Returns the grouped Distribution of the MAP Variable at Time 0.
     * @param dynVar the dynamic {@link Variable} object.
     * @param staticVar the static {@link Variable} object.
     * @param conDist0 the {@link ConditionalDistribution} at time 0.
     * @param conDistT the {@link ConditionalDistribution} at time T.
     * @return a {@link Multinomial} distribution.
     */
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

    /**
     * Returns the grouped Distribution of the MAP Variable at Time 0.
     * @param dynVar the dynamic {@link Variable} object.
     * @param staticVar the static {@link Variable} object.
     * @param nStatesStaticVarParent the number of static variable parents.
     * @param parents the {@code List} of parent {@link Variable}s.
     * @param conDistT the {@link ConditionalDistribution} at time T.
     * @return a {@link Multinomial_MultinomialParents} distribution.
     */
    private Multinomial_MultinomialParents groupedDistributionMAPVariableTimeT(Variable dynVar, Variable staticVar, int nStatesStaticVarParent, List<Variable> parents, ConditionalDistribution conDistT) {

        Multinomial_MultinomialParents multinomial_multinomialParents = new Multinomial_MultinomialParents(staticVar, parents);
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
            multinomial = new Multinomial(staticVar);
            multinomial.setProbabilities(probs1);
            multinomial_multinomialParents.setMultinomial(m, multinomial);
        }
        return multinomial_multinomialParents;
    }

    /**
     * Returns the {@link BayesianNetwork} related to the static grouped class.
     * @param dag a {@link DAG} object.
     * @param variables a {@link Variables} obejct.
     * @param even_partition a {@code boolean} that indicates whether the partition is even or not.
     * @return a {@link BayesianNetwork} object.
     */
    private BayesianNetwork obtainStaticGroupedClassBayesianNetwork(DAG dag, Variables variables, boolean even_partition) {

        DynamicDAG dynamicDAG = model.getDynamicDAG();
        BayesianNetwork bn = new BayesianNetwork(dag);
        Variable staticVar, dynVar;
        ConditionalDistribution conDist0, conDist1;

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

        if ( even_partition && (replicationsMAPVariable>2 || (replicationsMAPVariable==2 && nTimeSteps>=4))) {

            Variable staticVar_current = variables.getVariableByName(groupedClassName + "_t1");
            Variable staticVar_interface = variables.getVariableByName(groupedClassName + "_t0");
            List<Variable> parents = bn.getDAG().getParentSet(staticVar_current).getParents();
            ConditionalDistribution conDist_dynamic = Serialization.deepCopy(model.getConditionalDistributionsTimeT().get(dynVar.getVarID()));

            generalConditionalDistTimeT = groupedDistributionMAPVariableTimeT(dynVar, staticVar_current, staticVar_interface.getNumberOfStates(), parents, conDist_dynamic);

        }
        else if (!even_partition &&  (replicationsMAPVariable>=3 && nTimeSteps>=5) ) {

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
                    conditionalDistribution.setVar(staticVar1);
                    bn.setConditionalDistribution(staticVar1, conditionalDistribution);

                });
            }
            else {
                IntStream.range(1, replicationsMAPVariable - 1).forEach(i -> {
                    Variable staticVar1 = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(i));
                    ConditionalDistribution conditionalDistribution = Serialization.deepCopy(generalConditionalDistTimeT);
                    conditionalDistribution.setVar(staticVar1);
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
                    multinomial1.setVar(staticVar1);
                    lastConDist.setMultinomial(m, multinomial1);
                }
                bn.setConditionalDistribution(staticVar1, lastConDist);
            }
        }
        else {
            if (nTimeSteps % 2 == 1) {

                // For an odd partition, the first conditional distribution is different
                Variable staticVar0 = variables.getVariableByName(groupedClassName + "_t1");
                Variable staticVar0_interface = variables.getVariableByName(groupedClassName + "_t0");
                List<Variable> parents = bn.getDAG().getParentSet(staticVar0).getParents();
                ConditionalDistribution conDist_dynamic = Serialization.deepCopy(model.getConditionalDistributionsTimeT().get(dynVar.getVarID()));
                ConditionalDistribution conditionalDistTime1 = groupedDistributionMAPVariableTimeT(dynVar, staticVar0, staticVar0_interface.getNumberOfStates(), parents, conDist_dynamic);
                conditionalDistTime1.setVar(staticVar0);
                bn.setConditionalDistribution(staticVar0, conditionalDistTime1);

                // Add the rest of conditional distributions, copies of 'generalConditionalDistTimeT'
                IntStream.range(2, replicationsMAPVariable).forEach(i -> {
                    Variable staticVar1 = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(i));
                    ConditionalDistribution conditionalDistribution = Serialization.deepCopy(generalConditionalDistTimeT);
                    conditionalDistribution.setConditioningVariables(dag.getParentSet(staticVar1).getParents());
                    conditionalDistribution.setVar(staticVar1);
                    bn.setConditionalDistribution(staticVar1, conditionalDistribution);

                });
            }
            else {

                // For an odd partition, the first conditional distribution is different
                Variable staticVar0 = variables.getVariableByName(groupedClassName + "_t1");
                Variable staticVar0_interface = variables.getVariableByName(groupedClassName + "_t0");
                List<Variable> parents = bn.getDAG().getParentSet(staticVar0).getParents();
                ConditionalDistribution conDist_dynamic = Serialization.deepCopy(model.getConditionalDistributionsTimeT().get(dynVar.getVarID()));
                ConditionalDistribution conditionalDistTime1 = groupedDistributionMAPVariableTimeT(dynVar, staticVar0, staticVar0_interface.getNumberOfStates(), parents, conDist_dynamic);
                conditionalDistTime1.setVar(staticVar0);
                bn.setConditionalDistribution(staticVar0, conditionalDistTime1);

                // Add the intermediate conditional distributions, copies of 'generalConditionalDistTimeT'
                IntStream.range(2, replicationsMAPVariable - 1).forEach(i -> {
                    Variable staticVar1 = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(i));
                    ConditionalDistribution conditionalDistribution = Serialization.deepCopy(generalConditionalDistTimeT);
                    conditionalDistribution.setConditioningVariables(dag.getParentSet(staticVar1).getParents());
                    conditionalDistribution.setVar(staticVar1);
                    bn.setConditionalDistribution(staticVar1, conditionalDistribution);

                });

                // For an odd partition with even nTimeSteps, the last distribution is also different
                Variable staticVar1 = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(replicationsMAPVariable - 1));
                Variable staticVar1_interface = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(replicationsMAPVariable - 2));
                Multinomial_MultinomialParents lastConDist = new Multinomial_MultinomialParents(staticVar1, dag.getParentSet(staticVar1).getParents());
                for (int m = 0; m < staticVar1_interface.getNumberOfStates(); m++) {
                    ConditionalDistribution dynConDist = model.getConditionalDistributionTimeT(MAPvariable);
                    Assignment assignment = new HashMapAssignment(1);
                    assignment.setValue(dynVar.getInterfaceVariable(), m % dynVar.getNumberOfStates());
                    Multinomial multinomial1 = (Multinomial) dynConDist.getUnivariateDistribution(assignment);
                    multinomial1.setVar(staticVar1);
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
                List<Variable> parentList0 = bn.getDAG().getParentSet(staticVar1).getParents();
                int indexMAPvariable = thisVarParents.indexOf(MAPvariable);
                //thisVarParents.remove(indexMAPvariable);
                thisVarParents.set(indexMAPvariable, staticMAPVar1);

                BaseDistribution_MultinomialParents staticVar2Distribution = distributionMAPChildrenTimeT(staticVar1, conditionalDistribution, parentList0, even_partition, 0);
                bn.setConditionalDistribution(staticVar1, staticVar2Distribution);
//                BaseDistribution_MultinomialParents baseDist = new BaseDistribution_MultinomialParents(staticVar1,thisVarParents);
//                boolean allParentsMultinomial = thisVarParents.stream().allMatch(parent -> parent.isMultinomial());
//
//                for (int m = 0; m < baseDist.getNumberOfBaseDistributions(); m++) {
//                    Assignment assignment = new HashMapAssignment(1);
//                    assignment.setValue(MAPvariable, m / MAPvariable.getNumberOfStates());
//                    UnivariateDistribution uniDist = conditionalDistribution.getUnivariateDistribution(assignment);
//                    uniDist.setVar(staticVar1);
//                    uniDist.setConditioningVariables(parentList0);
//                    baseDist.setBaseDistribution(m, uniDist);
//                }
//                conditionalDistribution.setConditioningVariables(parentList0);
//                conditionalDistribution.setVar(staticVar1);
//
//                bn.setConditionalDistribution(staticVar1, conditionalDistribution);
//                bn.setConditionalDistribution(staticVar1, baseDist);
            });
        }
        else {
            dynVariablesWithClassParent.stream().forEach(dynVariable -> {
                ConditionalDistribution conditionalDistribution = Serialization.deepCopy(model.getConditionalDistributionTime0(dynVariable));

                Variable staticMAPVar1 = variables.getVariableByName(groupedClassName + "_t0");
                Variable staticVar1 = variables.getVariableByName(dynVariable.getName() + "_t0");
                List<Variable> thisVarParents = conditionalDistribution.getConditioningVariables();
                List<Variable> parentList0 = bn.getDAG().getParentSet(staticVar1).getParents();
                int indexMAPvariable = thisVarParents.indexOf(MAPvariable);
                //thisVarParents.remove(indexMAPvariable);
                thisVarParents.set(indexMAPvariable, staticMAPVar1);

                conditionalDistribution.setConditioningVariables(parentList0);
                conditionalDistribution.setVar(staticVar1);

                bn.setConditionalDistribution(staticVar1, conditionalDistribution);
            });
        }

        /*
         * ADD CONDITIONAL DISTRIBUTIONS FOR VARIABLES HAVING AS A PARENT THE GROUPED MAP/CLASS VARIABLE, AT TIMES T>0
         */
        dynVariablesWithClassParent.stream().forEach(dynVariable -> {
            IntStream.range(1, nTimeSteps).forEachOrdered(i -> {

                ConditionalDistribution dynamicConDist = Serialization.deepCopy(model.getConditionalDistributionTimeT(dynVariable));
                Variable staticVar2 = variables.getVariableByName(dynVariable.getName() + "_t" + Integer.toString(i));
                List<Variable> parentList = bn.getDAG().getParentSet(staticVar2).getParents();

                BaseDistribution_MultinomialParents staticVar2Distribution = distributionMAPChildrenTimeT(staticVar2, dynamicConDist, parentList, even_partition, i);
                bn.setConditionalDistribution(staticVar2, staticVar2Distribution);
            });
        });


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
            conditionalDistribution.setVar(staticVar1);
            bn.setConditionalDistribution(staticVar1, conditionalDistribution);

            // TIMES T>0
            IntStream.range(1, nTimeSteps).forEach(i -> {
                ConditionalDistribution conditionalDistribution1 = Serialization.deepCopy(model.getConditionalDistributionTimeT(dynVariable));
                Variable staticVar2 = variables.getVariableByName(dynVariable.getName() + "_t" + Integer.toString(i));
                List<Variable> thisVarParents1 = conditionalDistribution1.getConditioningVariables();
                thisVarParents1.stream().map(parent -> variables.getVariableByName(parent.getName() + "_t" + Integer.toString(i)));
                conditionalDistribution1.setConditioningVariables(thisVarParents1);
                conditionalDistribution1.setVar(staticVar2);
                bn.setConditionalDistribution(staticVar2, conditionalDistribution1);
            });
        });

        return bn;
    }

    /**
     * Returns the distribution of MAP Children at time T.
     * @param staticVariable the static {@link Variable} object.
     * @param dynamicConditionalDistribution the dynamic {@link ConditionalDistribution} at time T.
     * @param parentList the {@code List} of parent {@link Variable}s.
     * @param even_partition a {@code boolean} that indicates whether the partition is even or not.
     * @param time_step the time step.
     * @return a {@link BaseDistribution_MultinomialParents} distribution.
     */
    private BaseDistribution_MultinomialParents distributionMAPChildrenTimeT(Variable staticVariable, ConditionalDistribution dynamicConditionalDistribution, List<Variable> parentList, boolean even_partition, int time_step) {

        boolean allParentsMultinomial = parentList.stream().allMatch(parent -> parent.isMultinomial());
        List<Variable> multinomialParents = parentList.stream().filter(parent -> parent.isMultinomial()).collect(Collectors.toList());
        List<Variable> continuousParents = parentList.stream().filter(parent -> !parent.isMultinomial()).collect(Collectors.toList());

        BaseDistribution_MultinomialParents staticVarConDist = new BaseDistribution_MultinomialParents(staticVariable, parentList);

        int nStatesMultinomialParents = (int) Math.round(Math.exp(multinomialParents.stream().mapToDouble(parent -> Math.log(parent.getNumberOfStates())).sum()));

        for (int m = 0; m < nStatesMultinomialParents; m++) {
            Assignment staticParentsConfiguration = MultinomialIndex.getVariableAssignmentFromIndex(multinomialParents, m);
            Assignment dynamicParentsConfiguration = new HashMapAssignment(multinomialParents.size());

            IntStream.range(0, multinomialParents.size()).forEach(k -> {
                double parentValue = staticParentsConfiguration.getValue(multinomialParents.get(k));
                String parentName;
                if (multinomialParents.get(k).getName().contains(groupedClassName)) {
                    parentName = multinomialParents.get(k).getName().replace(groupedClassName, MAPvarName).replaceAll("_t\\d+", "");
                    Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);

                    double dynParentValue;

                    if (time_step==0) {
                        dynParentValue = parentValue / MAPvariable.getNumberOfStates();;
                    }
                    else {
                        if ((!even_partition && nTimeSteps % 2 == 0 && (time_step == nTimeSteps - 1)) || (even_partition && nTimeSteps % 2 == 1 && (time_step == nTimeSteps - 1))) {
                            dynParentValue = parentValue;
                        } else {
                            if ((!even_partition && (time_step % 2 == 1)) || (even_partition && (time_step % 2 == 0))) {
                                dynParentValue = parentValue / MAPvariable.getNumberOfStates();
                            } else {
                                dynParentValue = parentValue % MAPvariable.getNumberOfStates();
                            }
                        }
                    }
                    dynamicParentsConfiguration.setValue(dynParent, dynParentValue);
                } else {

                    if (multinomialParents.get(k).getName().endsWith("_t" + Integer.toString(time_step - 1))) {
                        parentName = multinomialParents.get(k).getName().replaceFirst("_t\\d+", "");
                        Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
                        dynamicParentsConfiguration.setValue(dynParent.getInterfaceVariable(), parentValue);
                    } else {
                        parentName = multinomialParents.get(k).getName().replaceFirst("_t\\d+", "");
                        Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
                        dynamicParentsConfiguration.setValue(dynParent, parentValue);
                    }
                }
            });

            if (allParentsMultinomial && staticVariable.isMultinomial()) {
                Multinomial multinomial1 = (Multinomial) dynamicConditionalDistribution.getUnivariateDistribution(dynamicParentsConfiguration);
                multinomial1.setVar(staticVariable);
                multinomial1.setConditioningVariables(multinomialParents);

                staticVarConDist.setBaseDistribution(m, multinomial1);
            }
            else if (allParentsMultinomial && staticVariable.isNormal() ){
                Normal_MultinomialParents normal_multinomialParents = (Normal_MultinomialParents) dynamicConditionalDistribution;
                Normal clg = normal_multinomialParents.getNormal(m/MAPvariable.getNumberOfStates());
                clg.setConditioningVariables(multinomialParents);
                //clg.setConditioningVariables(continuousParents);
                clg.setVar(staticVariable);

                staticVarConDist.setBaseDistribution(m, clg);
            }
            else {
                Normal_MultinomialNormalParents normal_multinomialNormalParents = (Normal_MultinomialNormalParents) dynamicConditionalDistribution;
                ConditionalLinearGaussian clg = normal_multinomialNormalParents.getNormal_NormalParentsDistribution(dynamicParentsConfiguration);
                clg.setConditioningVariables(continuousParents);
                clg.setVar(staticVariable);

                staticVarConDist.setBaseDistribution(m, clg);
            }
        }

        return staticVarConDist;
    }

    public static void main(String[] arguments) throws IOException, ClassNotFoundException {

//        String file = "./networks/CajamarDBN.dbn";
//        DynamicBayesianNetwork cajamarDBN = DynamicBayesianNetworkLoader.loadFromFile(file);
//
//        DynamicMAPInference dynMAP = new DynamicMAPInference();
//        dynMAP.setModel(cajamarDBN);
//        dynMAP.setNumberOfTimeSteps(6);
//
//        cajamarDBN.getDynamicVariables().getListOfDynamicVariables().forEach(var -> System.out.println(var.getName()));
//
//        System.out.println(cajamarDBN.toString());
//        System.out.println("CausalOrder: " + cajamarDBN.getDynamicDAG().toString());
//
//        System.out.println(cajamarDBN.getDynamicDAG().toString());
//
//        Variable mapVariable = cajamarDBN.getDynamicVariables().getVariableByName("DEFAULT");
//        dynMAP.setMAPvariable(mapVariable);
//
//        dynMAP.computeDynamicMAPEvenModel();
//        BayesianNetwork test = dynMAP.getStaticEvenModel();
//
//        System.out.println(test.getDAG().toString());
//        System.out.println(cajamarDBN.toString());
//
//        dynMAP.runInference();

        DynamicMAPInference dynMAP;
        Variable mapVariable;


        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(3);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);
        DynamicBayesianNetworkGenerator.setNumberOfLinks(5);

        DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        System.out.println("ORIGINAL DYNAMIC NETWORK:");

        System.out.println(dynamicBayesianNetwork.toString());
        System.out.println();



        dynMAP = new DynamicMAPInference();
        dynMAP.setModel(dynamicBayesianNetwork);

        // INITIALIZE THE MODEL
        int nTimeSteps = 10;
        dynMAP.setNumberOfTimeSteps(nTimeSteps);
        mapVariable = dynamicBayesianNetwork.getDynamicVariables().getVariableByName("ClassVar");
        dynMAP.setMAPvariable(mapVariable);

        dynMAP.computeDynamicMAPEvenModel();
        BayesianNetwork evenModel = dynMAP.getStaticEvenModel();
//        BayesianNetworkWriter.saveToFile(evenModel,"dynamicMAPhybridEvenModel.bn");

        dynMAP.computeDynamicMAPOddModel();
        BayesianNetwork oddModel = dynMAP.getStaticOddModel();
//        BayesianNetworkWriter.saveToFile(oddModel,"dynamicMAPhybridOddModel.bn");

        System.out.println(evenModel.toString());
        System.out.println();

        System.out.println(oddModel.toString());
        System.out.println();

        /*
         * GENERATE AN EVIDENCE FOR T=0,...,nTimeSteps-1
         */
        List<Variable> varsDynamicModel = dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();

        varsDynamicModel.forEach(var -> System.out.println("Var ID " + var.getVarID() + ": " + var.getName()));
        int indexVarEvidence1 = 2;
        int indexVarEvidence2 = 3;
        int indexVarEvidence3 = 8;
        Variable varEvidence1 = varsDynamicModel.get(indexVarEvidence1);
        Variable varEvidence2 = varsDynamicModel.get(indexVarEvidence2);
        Variable varEvidence3 = varsDynamicModel.get(indexVarEvidence3);

        List<Variable> varsEvidence = new ArrayList<>(3);
        varsEvidence.add(0,varEvidence1);
        varsEvidence.add(1,varEvidence2);
        varsEvidence.add(2,varEvidence3);

        double varEvidenceValue;

        Random random = new Random(931234662);

        List<DynamicAssignment> evidence = new ArrayList<>(nTimeSteps);

        for (int t = 0; t < nTimeSteps; t++) {
            HashMapDynamicAssignment dynAssignment = new HashMapDynamicAssignment(varsEvidence.size());

            for (int i = 0; i < varsEvidence.size(); i++) {

                dynAssignment.setSequenceID(2343253);
                dynAssignment.setTimeID(t);
                Variable varEvidence = varsEvidence.get(i);

                if (varEvidence.isMultinomial()) {
                    varEvidenceValue = random.nextInt(varEvidence1.getNumberOfStates());
                } else {
                    varEvidenceValue = -5 + 10 * random.nextDouble();
                }
                dynAssignment.setValue(varEvidence, varEvidenceValue);
            }
            evidence.add(dynAssignment);
        }

        dynMAP.setEvidence(evidence);
        dynMAP.runInference();

    }
}
