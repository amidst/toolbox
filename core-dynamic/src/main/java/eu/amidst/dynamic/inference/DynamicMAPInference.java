/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.dynamic.inference;

import eu.amidst.core.distribution.*;
import eu.amidst.core.inference.ImportanceSamplingRobust;
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
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class implements the MAP Inference algorithm for {@link DynamicBayesianNetwork} models.
 */
public class DynamicMAPInference implements InferenceAlgorithmForDBN {

    /**
     * Represents the search algorithm to be used.
     */
    public enum SearchAlgorithm {
        VMP, IS
    }

    /** Represents the {@link DynamicBayesianNetwork} model. */
    private DynamicBayesianNetwork model;

    /** Represents a {@link BayesianNetwork} object. */
    private BayesianNetwork unfoldedStaticModel;

    /** Represents a {@link BayesianNetwork} object. */
    private List<BayesianNetwork> mergedClassVarModels;

    /** Represents the number of time steps. */
    private int nTimeSteps = 2;

    /** Represents the number of consecutive class variables to merge */
    private int nMergedClassVars = 2;

    /** Represents the sample size when using IS. */
    private int sampleSize = 1000;

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

    /** Represents the MAP sequence defined as an {@link Assignment} object. */
    private Assignment MAPestimate;

    /** Represents the MAP sequence defined as an array of integers. */
    private int[] MAPsequence;

    /** Represents the Log Probability of the MAP estimate. */
    private double MAPestimateLogProbability;

    String groupedClassName = "__GROUPED_CLASS__";

    private List<int[]> bestSequenceEachModel;

    List<List<UnivariateDistribution>> allGroupedPosteriorDistributions;
    List<UnivariateDistribution> allUngroupedPosteriorDistributions;

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDynamicEvidence(DynamicAssignment assignment) {
        throw new UnsupportedOperationException("Operation not supported in Dynamic MAP Inference");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {

        seed=125123;
        parallelMode = true;
        sampleSize=1000;
        nMergedClassVars = 2;
        nTimeSteps = 2;

        model=null;
        unfoldedStaticModel =null;
        mergedClassVarModels=null;

        evidence=null;
        MAPvarName=null;
        MAPvariable=null;
        MAPestimate=null;
        MAPsequence=null;
        MAPestimateLogProbability=Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends UnivariateDistribution> E getFilteredPosterior(Variable var) {
        throw new UnsupportedOperationException("Operation not supported in Dynamic MAP Inference");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead) {
        throw new UnsupportedOperationException("Operation not supported in Dynamic MAP Inference");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeIDOfLastEvidence() {
        return (long)this.evidence.stream().mapToDouble(DynamicAssignment::getTimeID).max().getAsDouble();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeIDOfPosterior() {
        throw new UnsupportedOperationException("Operation not supported in Dynamic MAP Inference");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DynamicBayesianNetwork getOriginalModel() {
        return this.model;
    }

    /**
     * Sets the evidence for this DynamicMAPInference.
     * @param evidence a list of {@link DynamicAssignment} objects.
     */
    public void setEvidence(List<DynamicAssignment> evidence) {

        if(evidence==null) {
            this.evidence = null;
            this.staticEvidence = null;
            return;
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

        if (unfoldedStaticModel!=null) {
            staticEvidence = new HashMapAssignment(unfoldedStaticModel.getNumberOfVars());

            evidence.stream().forEach(dynamicAssignment -> {
                int time = (int) dynamicAssignment.getTimeID();
                Set<Variable> dynAssigVariables = dynamicAssignment.getVariables();
                for (Variable dynVariable : dynAssigVariables) {
                    Variable staticVariable = unfoldedStaticModel.getVariables().getVariableByName(dynVariable.getName() + "_t" + Integer.toString(time));
                    double varValue = dynamicAssignment.getValue(dynVariable);
                    staticEvidence.setValue(staticVariable, varValue);
                }

            });
        }
    }

//    public void setEvidence(DataStream<DynamicDataInstance> evidence1) {
//
//        DynamicDataInstance currentDataInstance = evidence1.stream().findFirst().get();
//        long sequenceID = currentDataInstance.getSequenceID();
//
//        List<DynamicAssignment> evidence2 = new ArrayList<>(1);
//        for(DynamicDataInstance instance : evidence1) {
//            if(instance.getSequenceID()!=sequenceID) {
//                continue;
//            }
//            instance.setValue(this.MAPvariable, Utils.missingValue());
//
//            DynamicAssignment dynamicAssignment = new HashMapDynamicAssignment(this.model.getNumberOfVars());
//            this.model.getDynamicVariables().getListOfDynamicVariables().stream().filter(var -> !(var.getName()==this.MAPvarName)).forEach(var -> {
//                dynamicAssignment.setValue(var,instance.getValue(var));
//            });
//            evidence2.add(dynamicAssignment);
//        }
//
//        //evidence2.sort( (l1,l2) -> (l1.getTimeID()<l2.getTimeID() ? 1 : -1) ) ;
//
////        evidence2.forEach(evid -> {
////            System.out.println(evid.outputString(this.model.getDynamicVariables().getListOfDynamicVariables()));
//////            System.out.println(evid.getSequenceID());
//////            System.out.println(evid.getTimeID());
////        });
//
//
//        this.evidence = evidence2;
//
////        if (staticEvenModel!=null) {
////            staticEvidence = new HashMapAssignment(staticEvenModel.getNumberOfVars());
////
////            evidence.stream().forEach(dynamicAssignment -> {
////                int time = (int) dynamicAssignment.getTimeID();
////                Set<Variable> dynAssigVariables = dynamicAssignment.getVariables();
////                for (Variable dynVariable : dynAssigVariables) {
////                    Variable staticVariable = staticEvenModel.getVariables().getVariableByName(dynVariable.getName() + "_t" + Integer.toString(time));
////                    double varValue = dynamicAssignment.getValue(dynVariable);
////                    staticEvidence.setValue(staticVariable, varValue);
////                }
////
////            });
////        }
//    }

    public List<List<UnivariateDistribution>> getGroupedPosteriorDistributions() {
        return allGroupedPosteriorDistributions;
    }

    public List<UnivariateDistribution> getUngroupedPosteriorDistributions() {
        return allUngroupedPosteriorDistributions;
    }

    /**
     * Sets the model for this DynamicMAPInference.
     * @param model a {@link DynamicBayesianNetwork} object.
     */
    public void setModel(DynamicBayesianNetwork model) {
        this.model = model;
        this.unfoldedStaticModel = DynamicToStaticBNConverter.convertDBNtoBN(model,nTimeSteps);
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
            System.out.println("Error: The dynamic MAP variable must not have parents");
            System.exit(-5);
        }

        if (!MAPvariable.isMultinomial()) {
            System.out.println("Error: The dynamic MAP variable must be multinomial");
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
            System.exit(-12);
        }
        nTimeSteps = ntimeSteps;

        this.setModel(this.model);

    }

    /**
     * Sets the number of consecutive copies of the class variable to merge.
     * Should be less than or equal to the number of time steps.
     * @param nMergedClassVars an {@code int} that represents the number of class variable copies to merge.
     */
    public void setNumberOfMergedClassVars(int nMergedClassVars) {
        if (nMergedClassVars > nTimeSteps) {
            System.out.println("Error: The number of merged class variables should be less or equal than the number of time steps");
            System.exit(-13);
        }
        if (nMergedClassVars<2 || nMergedClassVars > 10) {
            System.out.println("Error: The number of merged class variables should be between 2 and 10");
            System.exit(-14);
        }
        this.nMergedClassVars = nMergedClassVars;
    }

    /**
     * Sets the size of the sample when using Importance Sampling for inference.
     * @param sampleSize an {@code int} that represents the size of the sample.
     */
    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    /**
     * Sets the seed for repeatability in simulations.
     * @param seed an {@code int} that represents the seed.
     */
    public void setSeed(int seed) {
        this.seed = seed;
    }

    /**
     * Returns the MAP sequence found as an {@link Assignment} of variables
     * @return an {@link Assignment} object with the MAP sequence
     */
    public Assignment getMAPestimate() {
        return MAPestimate;
    }

    /**
     * Returns the MAP sequence found as array of integers
     * @return an array of integers representing the MAP sequence
     */
    public int[] getMAPsequence() {
        return MAPsequence;
    }

    public double getMAPestimateLogProbability() {
        return MAPestimateLogProbability;
    }

    public double getMAPestimateProbability() {
        return Math.exp(MAPestimateLogProbability);
    }

    /**
     * Returns the list of models with merged class variable.
     * @return a List of {@link BayesianNetwork} objects.
     */
    public List<BayesianNetwork> getMergedClassVarModels() {
        return mergedClassVarModels;
    }

    /**
     * Returns a single {@link BayesianNetwork} object, corresponding to the unfolded dynamic network
     * over the specified number of time steps.
     * @return a {@link BayesianNetwork} object.
     */
    public BayesianNetwork getUnfoldedStaticModel() {
        return unfoldedStaticModel;
    }

    public Assignment getUnfoldedEvidence() {
        return staticEvidence;
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

    public List<int[]> getBestSequencesForEachSubmodel() {
        return bestSequenceEachModel;
    }

    /**
     * Computes Dynamic MAP for the even static model.
     */
    public void computeMergedClassVarModels() {

        DynamicDAG dynamicDAG = model.getDynamicDAG();
        DynamicVariables dynamicVariables = model.getDynamicVariables();

        mergedClassVarModels = new ArrayList<>(nMergedClassVars);

        IntStream.range(0,nMergedClassVars).forEachOrdered(modelNumber -> {

//            System.out.println("Model number " + modelNumber);
            Variables variables = obtainReplicatedStaticVariables(dynamicVariables, modelNumber);

            DAG dag = obtainStaticDAG(dynamicDAG,variables,modelNumber);
//            System.out.println(dag.toString());

            BayesianNetwork bn = obtainStaticMergedClassVarNetwork(dag,variables,modelNumber);
//            System.out.println(bn.toString());


//            int replicationsMAPVariable = (modelNumber==0 ? 0 : 1) + (nTimeSteps-modelNumber)/nMergedClassVars + ((nTimeSteps-modelNumber)%nMergedClassVars==0 ? 0 : 1);

//            IntStream.range(0,replicationsMAPVariable).forEach(i-> {
//                System.out.println("Variable " + groupedClassName + "_t" + i);
//                System.out.println(bn.getConditionalDistribution(bn.getVariables().getVariableByName(groupedClassName + "_t" + i)).toString());
//                System.out.println();
//            });

//            System.out.println(bn.getConditionalDistribution(bn.getVariables().getVariableByName(groupedClassName + "_t1")).toString());
//            System.out.println();
//            System.out.println(bn.getConditionalDistribution(bn.getVariables().getVariableByName(groupedClassName + "_t2")).toString());
//            System.out.println();
//            System.out.println(bn.getConditionalDistribution(bn.getVariables().getVariableByName(groupedClassName + "_t3")).toString());
//            System.out.println();
            mergedClassVarModels.add(bn);
//            System.out.println("MODEL " + modelNumber);
//            System.out.println(bn);
        });
    }


    /**
     * Runs the inference algorithm.
     */
    public void runInference() {

        if (MAPvariable == null || MAPvarName == null) {
            System.out.println("Error: The MAP variable has not been set");
            System.exit(-30);
        }


        if (this.mergedClassVarModels == null) {
            this.computeMergedClassVarModels();
        }
//        if (this.staticEvenModel == null) {
//            this.computeDynamicMAPEvenModel();
//        }
//
//        if (evidence!=null && staticEvidence==null) {
//
//            staticEvidence = new HashMapAssignment(staticEvenModel.getNumberOfVars());
//
//            evidence.stream().forEach(dynamicAssignment -> {
//                int time = (int) dynamicAssignment.getTimeID();
//                Set<Variable> dynAssigVariables = dynamicAssignment.getVariables();
//                for (Variable dynVariable : dynAssigVariables) {
//                    Variable staticVariable = staticEvenModel.getVariables().getVariableByName(dynVariable.getName() + "_t" + Integer.toString(time));
//                    double varValue = dynamicAssignment.getValue(dynVariable);
//                    staticEvidence.setValue(staticVariable, varValue);
//                }
//
//            });
//        }
        this.runInference(SearchAlgorithm.VMP);
    }

    /**
     * Runs the inference given an input search algorithm.
     * @param searchAlgorithm a valid {@link SearchAlgorithm} value.
     */
    public void runInference(SearchAlgorithm searchAlgorithm) {

        if (MAPvariable == null || MAPvarName == null) {
            System.out.println("Error: The MAP variable has not been set");
            System.exit(-30);
        }

        if (this.mergedClassVarModels == null) {
            this.computeMergedClassVarModels();
        }

        if (this.unfoldedStaticModel == null) {
            unfoldedStaticModel = DynamicToStaticBNConverter.convertDBNtoBN(model,nTimeSteps);
        }

//
//        if (evidence!=null && staticEvidence==null) {
//
//            staticEvidence = new HashMapAssignment(staticEvenModel.getNumberOfVars());
//
//            evidence.stream().forEach(dynamicAssignment -> {
//                int time = (int) dynamicAssignment.getTimeID();
//                Set<Variable> dynAssigVariables = dynamicAssignment.getVariables();
//                for (Variable dynVariable : dynAssigVariables) {
//                    Variable staticVariable = staticEvenModel.getVariables().getVariableByName(dynVariable.getName() + "_t" + Integer.toString(time));
//                    double varValue = dynamicAssignment.getValue(dynVariable);
//                    staticEvidence.setValue(staticVariable, varValue);
//                }
//
//            });
//        }
//

        if (evidence!=null && staticEvidence==null) {

            staticEvidence = new HashMapAssignment(unfoldedStaticModel.getNumberOfVars());

            evidence.stream().forEach(dynamicAssignment -> {
                int time = (int) dynamicAssignment.getTimeID();
                Set<Variable> dynAssigVariables = dynamicAssignment.getVariables();
                for (Variable dynVariable : dynAssigVariables) {
                    Variable staticVariable = unfoldedStaticModel.getVariables().getVariableByName(dynVariable.getName() + "_t" + Integer.toString(time));
                    double varValue = dynamicAssignment.getValue(dynVariable);
                    staticEvidence.setValue(staticVariable, varValue);
                }
            });
        }


        List<InferenceAlgorithm> staticModelsInference = new ArrayList<>(nMergedClassVars);
        IntStream.range(0,nMergedClassVars).forEachOrdered(i -> {
            InferenceAlgorithm currentModelInference;
            switch (searchAlgorithm) {
                case VMP:
                    currentModelInference = new VMP();
                    //((VMP)currentModelInference).setTestELBO(true);
                    ((VMP)currentModelInference).setThreshold(0.0001);
                    ((VMP) currentModelInference).setMaxIter(3000);
                    break;

                case IS:
                default:

                    currentModelInference = new ImportanceSamplingRobust();

                    Random random = new Random((this.seed));
                    currentModelInference.setSeed(random.nextInt());

                    this.seed = random.nextInt();

                    ((ImportanceSamplingRobust) currentModelInference).setSampleSize(sampleSize);

                    break;
            }
            currentModelInference.setParallelMode(this.parallelMode);
            currentModelInference.setModel(mergedClassVarModels.get(i));

            if(searchAlgorithm==SearchAlgorithm.IS) {
                ((ImportanceSamplingRobust) currentModelInference).setVariablesAPosteriori(mergedClassVarModels.get(i).getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains(groupedClassName)).collect(Collectors.toList()));
            }

            BayesianNetwork thisModel = mergedClassVarModels.get(i);

            if(staticEvidence!=null) {
                Assignment thisEvidence = new HashMapAssignment();
                for (Variable varEvidence : staticEvidence.getVariables()) {
                    thisEvidence.setValue(thisModel.getVariables().getVariableByName(varEvidence.getName()), staticEvidence.getValue(varEvidence));
                }
                currentModelInference.setEvidence(thisEvidence);
            }
            currentModelInference.runInference();

            //System.out.println(currentModelInference.getLogProbabilityOfEvidence());

            staticModelsInference.add(currentModelInference);
        });
//
//        IntStream.range(0, 2).parallel().forEach(i -> {
//            if (i == 0) {
//                evenModelInference.setParallelMode(this.parallelMode);
//                evenModelInference.setModel(staticEvenModel);
//                if (evidence != null) {
//                    evenModelInference.setEvidence(staticEvidence);
//                }
//                evenModelInference.runInference();
//            }
//            else {
//                oddModelInference.setParallelMode(this.parallelMode);
//                oddModelInference.setModel(staticOddModel);
//                if (evidence != null) {
//                    oddModelInference.setEvidence(staticEvidence);
//                }
//                oddModelInference.runInference();
//            }
//        });

        List<List<UnivariateDistribution>> posteriorMAPDistributions = new ArrayList<>();
        IntStream.range(0,nMergedClassVars).forEachOrdered(modelNumber -> {

            List<UnivariateDistribution> currentModelPosteriorMAPDistributions = new ArrayList<>();
            int nReplicationsMAPVariable = (modelNumber==0 ? 0 : 1) + (nTimeSteps-modelNumber)/nMergedClassVars + ((nTimeSteps-modelNumber)%nMergedClassVars==0 ? 0 : 1);

            IntStream.range(0,nReplicationsMAPVariable).forEachOrdered(i -> {
                currentModelPosteriorMAPDistributions.add(staticModelsInference.get(modelNumber).getPosterior(i));
                //System.out.println(staticModelsInference.get(modelNumber).getPosterior(i).toString());
            });

            posteriorMAPDistributions.add(currentModelPosteriorMAPDistributions);
        });

//        int replicationsMAPVariableEvenModel = nTimeSteps/2 + nTimeSteps%2;
//        IntStream.range(0,replicationsMAPVariableEvenModel).forEachOrdered(i -> posteriorMAPDistributionsEvenModel.add(evenModelInference.getPosterior(i)));
//
//        int replicationsMAPVariableOddModel = 1 + (nTimeSteps-1)/2 + (nTimeSteps-1)%2;
//        IntStream.range(0,replicationsMAPVariableOddModel).forEachOrdered(i -> posteriorMAPDistributionsOddModel.add(oddModelInference.getPosterior(i)));



        posteriorMAPDistributions.forEach(list -> {

            StringBuilder stringBuilder = new StringBuilder();

            list.forEach(uniDist -> {
                stringBuilder.append(Arrays.toString(uniDist.getParameters()));
                stringBuilder.append(" ,  ");
            });
            //System.out.println("Model number " + posteriorMAPDistributions.indexOf(list) + ": " + stringBuilder.toString());
        });

        allGroupedPosteriorDistributions = posteriorMAPDistributions;

        bestSequenceEachModel = new ArrayList<>();
        IntStream.range(0,nMergedClassVars).forEachOrdered(modelNumber -> {
            int [] thisModelBestSequence = new int[nTimeSteps];
            int indexSequence = 0;

            List<UnivariateDistribution> thisModelPosteriors = posteriorMAPDistributions.get(modelNumber);

            for (int k = 0; k < thisModelPosteriors.size(); k++) {
                UnivariateDistribution thisDistribution = thisModelPosteriors.get(k);

                int indexMaxProbability = (int)argMax(thisDistribution.getParameters())[1];
                int thisDistribNumberOfMergedVars = (int) Math.round(Math.log(thisDistribution.getVariable().getNumberOfStates()) / Math.log(MAPvariable.getNumberOfStates()));

                String m_base_nStates = Integer.toString(Integer.parseInt(Integer.toString(indexMaxProbability), 10), MAPvariable.getNumberOfStates());
                m_base_nStates = StringUtils.leftPad(m_base_nStates, thisDistribNumberOfMergedVars, '0');

                for (int j = 0; j < m_base_nStates.length(); j++) {
                    thisModelBestSequence[indexSequence] = Integer.parseInt(m_base_nStates.substring(j,j+1));
                    indexSequence++;
                }
            }

            //System.out.println("Best sequence model " + modelNumber + ": " + Arrays.toString(thisModelBestSequence));
            bestSequenceEachModel.add(thisModelBestSequence);
        });

        List<double[]> conditionalDistributionsMAPvariable = obtainMAPVariableConditionalDistributions(posteriorMAPDistributions);
        //System.out.println("Cond Distributions: " + Arrays.toString(conditionalDistributionsMAPvariable));

        StringBuilder stringBuilder = new StringBuilder();
        conditionalDistributionsMAPvariable.forEach(conDistr -> {

            stringBuilder.append(Arrays.toString(conDistr));
            stringBuilder.append(" ,  ");
        });
        //System.out.println("Combined Conditional Distributions: \n" + stringBuilder.toString());


        computeMostProbableSequence(conditionalDistributionsMAPvariable);

    }

    /**
     * Runs the inference for Ungrouped MAP variable given an input search algorithm.
     * @param searchAlgorithm a valid {@link SearchAlgorithm} value.
     */
    public void runInferenceUngroupedMAPVariable(SearchAlgorithm searchAlgorithm) {

        if (MAPvariable == null || MAPvarName == null) {
            System.out.println("Error: The MAP variable has not been set");
            System.exit(-30);
        }


        if (this.unfoldedStaticModel == null) {
            unfoldedStaticModel = DynamicToStaticBNConverter.convertDBNtoBN(model,nTimeSteps);
        }

        if (evidence!=null && staticEvidence==null) {

            staticEvidence = new HashMapAssignment(unfoldedStaticModel.getNumberOfVars());

            evidence.stream().forEach(dynamicAssignment -> {
                int time = (int) dynamicAssignment.getTimeID();
                Set<Variable> dynAssigVariables = dynamicAssignment.getVariables();
                for (Variable dynVariable : dynAssigVariables) {
                    Variable staticVariable = unfoldedStaticModel.getVariables().getVariableByName(dynVariable.getName() + "_t" + Integer.toString(time));
                    double varValue = dynamicAssignment.getValue(dynVariable);
                    staticEvidence.setValue(staticVariable, varValue);
                }

            });
        }

        InferenceAlgorithm staticModelInference;
        switch(searchAlgorithm) {
            case VMP:
                staticModelInference = new VMP();
                //((VMP)staticModelInference).setTestELBO(true);
                ((VMP)staticModelInference).setThreshold(0.0001);
                ((VMP)staticModelInference).setMaxIter(3000);
                break;

            case IS:
            default:
                ImportanceSamplingRobust importanceSampling =  new ImportanceSamplingRobust();
                importanceSampling.setSampleSize(this.sampleSize);
                Random random = new Random((this.seed));
                importanceSampling.setSeed(random.nextInt());
                staticModelInference=importanceSampling;
                break;
        }


        staticModelInference.setParallelMode(this.parallelMode);
        staticModelInference.setModel(unfoldedStaticModel);

        if (searchAlgorithm==SearchAlgorithm.IS) {
            ((ImportanceSamplingRobust)staticModelInference).setVariablesAPosteriori(unfoldedStaticModel.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains(MAPvarName)).collect(Collectors.toList()));
        }
        if (evidence != null) {
            staticModelInference.setEvidence(staticEvidence);
        }
        staticModelInference.runInference();

        List<UnivariateDistribution> posteriorMAPDistributionsStaticModel = new ArrayList<>();
        IntStream.range(0,nTimeSteps).forEachOrdered(i -> {
            posteriorMAPDistributionsStaticModel.add(staticModelInference.getPosterior(i));
            //System.out.println("Ungrouped Posterior " + i + staticModelInference.getPosterior(i).toString());
        });

        allUngroupedPosteriorDistributions = posteriorMAPDistributionsStaticModel;

        double [] probabilities = posteriorMAPDistributionsStaticModel.stream().map(dist -> argMax(dist.getParameters())).mapToDouble(array -> array[0]).toArray();
        double MAPsequenceProbability = Math.exp(Arrays.stream(probabilities).map(prob -> Math.log(prob)).sum());

        int [] MAPsequence = posteriorMAPDistributionsStaticModel.stream().map(dist -> argMax(dist.getParameters())).mapToInt(array -> (int) array[1]).toArray();

        MAPestimate = new HashMapAssignment(nTimeSteps);
        IntStream.range(0,nTimeSteps).forEach(t-> {
            Variables variables = Serialization.deepCopy(this.unfoldedStaticModel.getVariables());
            Variable currentVar;
            if (variables.getVariableByName(MAPvarName + "_t" + Integer.toString(t))!=null) {
                currentVar  = variables.getVariableByName(MAPvarName + "_t" + Integer.toString(t));
            }
            else {
                currentVar  = variables.newMultinomialVariable(MAPvarName + "_t" + Integer.toString(t), MAPvariable.getNumberOfStates());
            }

            MAPestimate.setValue(currentVar,MAPsequence[t]);
        });
        MAPestimateLogProbability = Math.log(MAPsequenceProbability);
        this.MAPsequence = MAPsequence;
    }



    private List<double[]> obtainMAPVariableConditionalDistributions(List<List<UnivariateDistribution>> posteriorMAPVariableDistributions ) {

        List<double[]> listCondDistributions = new ArrayList<>(nTimeSteps);

        int nStates = MAPvariable.getNumberOfStates();

        // Univariate distribution Y_0
        // UnivariateDistribution dist0_1 = posteriorMAPDistributionsEvenModel.get(0); // This variable Z_0 groups Y_0 and Y_1
        // UnivariateDistribution dist0 = posteriorMAPDistributionsOddModel.get(0); // This variable is just Y_0 (not a group)
//        double[] dist0_probs = new double[nStates];

//        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n");
        IntStream.range(0, nTimeSteps).forEachOrdered(timeStep -> {

//            System.out.println("\n\nTime step " + timeStep);
            double[] combinedConditionalDistributionProbabilities, baseDistributionProbabilities;

            int baseModelIndex = (timeStep+1)%nMergedClassVars;
            int baseDistributionIndex = (timeStep >= baseModelIndex) ? (baseModelIndex == 0 ? 0 : 1) + (timeStep - baseModelIndex) / nMergedClassVars : (timeStep - baseModelIndex) / nMergedClassVars;
            baseDistributionProbabilities = posteriorMAPVariableDistributions.get(baseModelIndex).get(baseDistributionIndex).getParameters();
            int nStatesBaseDistribution = baseDistributionProbabilities.length;
            int baseDistrib_nMergedVars = (int) Math.round(Math.log(nStatesBaseDistribution) / Math.log(nStates));

            combinedConditionalDistributionProbabilities =

                    IntStream.range(0, nMergedClassVars).mapToObj(modelNumber -> {

                        if (modelNumber==baseModelIndex) {
//                            System.out.println("\nModel number " + modelNumber);
                            //System.out.println(Arrays.toString(baseDistributionProbabilities));
                            return baseDistributionProbabilities;
                        }

//                        System.out.println("\nModel number " + modelNumber);

                        int distributionIndex = (timeStep >= modelNumber) ? (modelNumber == 0 ? 0 : 1) + (timeStep - modelNumber) / nMergedClassVars : (timeStep - modelNumber) / nMergedClassVars;
                        int currentVarIndex = (timeStep >= modelNumber) ? (timeStep - modelNumber) % nMergedClassVars : timeStep;
//                        System.out.println("CurrentVarIndex " + currentVarIndex);

                        UnivariateDistribution currentDistrib = posteriorMAPVariableDistributions.get(modelNumber).get(distributionIndex);
                        //System.out.println(currentDistrib.toString());


                        double[] probabilities = new double[nStatesBaseDistribution];

                        int currentDistrib_nMergedVars = (int) Math.round(Math.log(currentDistrib.getVariable().getNumberOfStates()) / Math.log(nStates));
                        int current_nMergedVarsBaseDist = (int) Math.round(Math.log(baseDistributionProbabilities.length) / Math.log(nStates));


                        if (distributionIndex==0) {

//                            System.out.println("Current nMergedVars " + currentDistrib_nMergedVars + ", current nMergedVarsBaseDist " + current_nMergedVarsBaseDist);
                            for (int m = 0; m < Math.pow(nStates, currentDistrib_nMergedVars); m++) {

                                String m_base_nStates = Integer.toString(Integer.parseInt(Integer.toString(m), 10), nStates);
                                m_base_nStates = StringUtils.leftPad(m_base_nStates, currentDistrib_nMergedVars, '0');

//                                int index_init = currentVarIndex - ((timeStep >= nMergedClassVars) ? nMergedClassVars : timeStep);
                                int index_init = currentVarIndex + 1 - baseDistrib_nMergedVars;
                                int index_end = currentVarIndex + 1;

//                                String statesSequence = m_base_nStates.substring(currentVarIndex, currentVarIndex + current_nMergedVarsBaseDist);
                                String statesSequence = m_base_nStates.substring(index_init, index_end);
                                int currentState = Integer.parseInt(statesSequence, nStates);

//                                System.out.println("Current state " + currentState);

                                probabilities[currentState] += currentDistrib.getParameters()[m];
                            }
                        }
                        else {
                            UnivariateDistribution previousDistrib = posteriorMAPVariableDistributions.get(modelNumber).get(distributionIndex - 1);
                            int previousDistrib_nMergedVars = (int) Math.round(Math.log(previousDistrib.getVariable().getNumberOfStates()) / Math.log(nStates));

//                            System.out.println("Current nMergedVars " + currentDistrib_nMergedVars + ", previous nMergedVars " + previousDistrib_nMergedVars + ", current nMergedVarsBaseDist " + current_nMergedVarsBaseDist);

                            for (int n = 0; n < Math.pow(nStates, previousDistrib_nMergedVars); n++) {

                                String n_base_nStates = Integer.toString(Integer.parseInt(Integer.toString(n), 10), nStates);
                                n_base_nStates = StringUtils.leftPad(n_base_nStates, previousDistrib_nMergedVars, '0');

                                for (int m = 0; m < Math.pow(nStates, currentDistrib_nMergedVars); m++) {

                                    String m_base_nStates = Integer.toString(Integer.parseInt(Integer.toString(m), 10), nStates);
                                    m_base_nStates = StringUtils.leftPad(m_base_nStates, currentDistrib_nMergedVars, '0');

//                                    String statesSequence = m_base_nStates.substring(currentVarIndex, currentVarIndex + current_nMergedVarsBaseDist);
//                                    int currentState = Integer.parseInt(statesSequence, nStates);


                                    String n_concat_m_base_nStates = n_base_nStates.concat(m_base_nStates);
                                    int index_init = previousDistrib_nMergedVars + currentVarIndex + 1 - baseDistrib_nMergedVars;
                                    int index_end = previousDistrib_nMergedVars + currentVarIndex + 1;
                                    String statesSequence = n_concat_m_base_nStates.substring( index_init, index_end);
//                                    System.out.println("Complete sequence: " + n_concat_m_base_nStates + ", statesSequence:" + statesSequence);

//                                    int subIndices_m = currentVarIndex;
//                                    int subIndices_n = 1 + ((timeStep >= nMergedClassVars) ? (previousDistrib_nMergedVars - nMergedClassVars + currentVarIndex) : (previousDistrib_nMergedVars - (nMergedClassVars-timeStep) + currentVarIndex));
//
//                                    System.out.println("n_base_nStates: " + n_base_nStates + "m_base_nStates: " + m_base_nStates );
//
//                                    System.out.println("subIndices_m: " + Integer.toString(subIndices_m));
//                                    System.out.println("subIndices_n: " + Integer.toString(subIndices_n));
//
//                                    String statesSequence_m = m_base_nStates.substring(0, subIndices_m);
//                                    String statesSequence_n = n_base_nStates.substring(subIndices_n, previousDistrib_nMergedVars);
//
//
//                                    System.out.println("statesSequence n: " + statesSequence_n + ", statesSequence m: " + statesSequence_m );
//
//                                    String statesSequence = statesSequence_n.concat(statesSequence_m);
//                                    System.out.println("States sequence length: " + statesSequence.length() + ", sequence: " + statesSequence);

                                    int currentState = Integer.parseInt(statesSequence, nStates);
//                                    System.out.println("Current state " + currentState);

                                    probabilities[currentState] += previousDistrib.getParameters()[n] * currentDistrib.getParameters()[m];
                                }
                            }
                        }
//                        System.out.println("Model distribution: " + Arrays.toString(probabilities));
                        return probabilities;
                    })
                            .reduce(new double[baseDistributionProbabilities.length], (doubleArray1, doubleArray2) -> {
                                if (doubleArray1.length != doubleArray2.length) {
//                            System.out.println("Problem with lengths");
                                    System.exit(-40);
                                }
                                for (int i = 0; i < doubleArray1.length; i++)
                                    doubleArray1[i] += ((double) 1 / nMergedClassVars) * doubleArray2[i];
                                return doubleArray1;
                            });

            //System.out.println("Combined distribution " + Arrays.toString(combinedConditionalDistributionProbabilities));
            listCondDistributions.add(combinedConditionalDistributionProbabilities);
        });

//        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n");
        return listCondDistributions;
    }

    /**
     * Computes the Most Probable Sequence given the posterior distributions of the MAP variable.
     * @param posteriorDistributionsMAPvariable a {@code List} of conditional distribution values.
     */
    private void computeMostProbableSequence(List<double[]> posteriorDistributionsMAPvariable) {

        int[] MAPsequence = new int[nTimeSteps];
        int nStates = MAPvariable.getNumberOfStates();

        int[] argMaxValues = new int[nTimeSteps-1];

        double MAPsequenceProbability=-1;
        double [] current_probs;
        double [] current_max_probs = new double[(int)Math.pow(nStates,nMergedClassVars-1)];
        double [] previous_max_probs = new double[(int)Math.pow(nStates,nMergedClassVars-1)];


        for (int t = nTimeSteps-1; t >= 1; t--) {

//            System.out.println("Time:" + t);
            double [] currentDistProbabilities = posteriorDistributionsMAPvariable.get(t);

            if (Arrays.stream(currentDistProbabilities).anyMatch(Double::isNaN)) {
                MAPsequence = new int[nTimeSteps];
                for (int i = 0; i < MAPsequence.length; i++) {
                    MAPsequence[i]=-1;
                    return;
                }
            }
            int currentDistrib_nMergedVars = (int) Math.round(Math.log(currentDistProbabilities.length)/Math.log(nStates));

//            System.out.println("Current Probabilities:" + Arrays.toString(currentDistProbabilities));
            current_max_probs = new double[(int)Math.pow(nStates, currentDistrib_nMergedVars-1)];

            if (t==nTimeSteps-1) {
                previous_max_probs = Arrays.stream(previous_max_probs).map(d -> 1).toArray();
            }
//            System.out.println("Current Max Probs Length: " + current_max_probs.length);
//            System.out.println("Previous Max Probs: " + Arrays.toString(previous_max_probs));

            for (int m = 0; m < Math.pow(nStates, currentDistrib_nMergedVars); m++) {

//                System.out.println("State: " + m );
                String m_base_nStates = Integer.toString(Integer.parseInt(Integer.toString(m), 10), nStates);
                m_base_nStates = StringUtils.leftPad(m_base_nStates, currentDistrib_nMergedVars, '0');


                int currentStateFirstVars;
                int currentStateLastVars;
                if (t>0) {
                    String stateFirstVars = m_base_nStates.substring(0, currentDistrib_nMergedVars-1);
                    currentStateFirstVars = Integer.parseInt(stateFirstVars, nStates);

                    String stateLastVars = m_base_nStates.substring(1,currentDistrib_nMergedVars);
                    currentStateLastVars = Integer.parseInt(stateLastVars, nStates);
                }
                else {
                    currentStateFirstVars=0;
                    currentStateLastVars= Integer.parseInt(m_base_nStates, nStates);
                }

//                System.out.println("FirstVars:" + currentStateFirstVars + ", LastVars:" + currentStateLastVars);

                double currentProb = currentDistProbabilities[m] * previous_max_probs[currentStateLastVars];
                double maxProb = current_max_probs[currentStateFirstVars];

                if (currentProb > maxProb) {
                    current_max_probs[currentStateFirstVars] = currentProb;
                }
            }

//            System.out.println("Current Max Probabilities:" + Arrays.toString(current_max_probs));

            argMaxValues[t-1] = (int)argMax(current_max_probs)[1];
            previous_max_probs = current_max_probs;

            if (t==1) {
                MAPsequenceProbability =  argMax(current_max_probs)[0];
            }
//            System.out.println("MAP Sequence Prob:" + MAPsequenceProbability);
//            System.out.println("Arg Max Value: " + argMaxValues[t-1]+ "\n\n\n");

        }

//        for (int t = nTimeSteps-1; t >= 0; t--) {
//
//            current_probs = posteriorDistributionsMAPvariable.get(t);
//            double maxProb=-1;
//
//            current_max_probs = new double[MAPvarNStates];
//
//            if (t==(nTimeSteps-1)) { // There are no previous_max_probs
//                for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_{t-1}
//                    maxProb=-1;
//                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_t
//                        if (current_probs[j * MAPvarNStates + k] > maxProb) {
//                            maxProb = current_probs[j * MAPvarNStates + k];
//
//                        }
//                    }
//                    current_max_probs[j]=maxProb;
//                }
//                argMaxValues[t] = (int)argMax(current_max_probs)[1];
//                previous_max_probs = current_max_probs;
//            }
//            else if (t>0 && t<(nTimeSteps-1)) {
//                for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_{t-1}
//                    maxProb=-1;
//                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_t
//                        if (current_probs[j * MAPvarNStates + k]*previous_max_probs[j] > maxProb) {
//                            maxProb = current_probs[j * MAPvarNStates + k]*previous_max_probs[k];
//                        }
//                    }
//                    current_max_probs[j]=maxProb;
//                }
//                argMaxValues[t] = (int)argMax(current_max_probs)[1];
//                previous_max_probs = current_max_probs;
//            }
//            else { // Here, t=0
//                for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_0
//                    maxProb=-1;
//                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_1
//                        if (current_probs[j]*previous_max_probs[j] > maxProb) {
//                            maxProb = current_probs[j]*previous_max_probs[j];
//                        }
//                    }
//                    current_max_probs[j]=maxProb;
//                }
//                MAPsequenceProbability =  argMax(current_max_probs)[0];
//                argMaxValues[t] = (int)argMax(current_max_probs)[1];
//                previous_max_probs = current_max_probs;
//            }
//        }

        //System.out.println(Arrays.toString(argMaxValues));

//        System.out.println("\n\n TRACEBACK \n\n");

        //int previousVarMAPState = argMaxValues[0];
        MAPsequence[0] = argMaxValues[0];

        int thisVarMAPState = 0;
        for (int t = 1; t < nTimeSteps; t++) {

//            System.out.println("Time Step: " + t);
            current_probs = posteriorDistributionsMAPvariable.get(t);

            StringBuilder prevVarsStateBuilder = new StringBuilder();

            int j_max = Math.min(nMergedClassVars-1, t);
            for (int j = 0; j < Math.min(nMergedClassVars-1, t); j++) {
//                System.out.println("Append: " + Integer.toString(MAPsequence[t-j_max+j]) );
                prevVarsStateBuilder.append( Integer.toString(MAPsequence[t-j_max+j]) );
            }
            //previousVarMAPState = argMaxValues[t-1];

//            System.out.println("PrevVarsState: " + prevVarsStateBuilder.toString());
//            String prevVarsState = Integer.toString(Integer.parseInt(prevVarsStateBuilder.toString()), nStates);
            String prevVarsState = prevVarsStateBuilder.toString();

//            System.out.println("Prev Vars State:" + prevVarsState);


//            String m_base_nStates = Integer.toString(Integer.parseInt(Integer.toString(previousVarMAPState), 10), nStates);
//            m_base_nStates = StringUtils.leftPad(m_base_nStates, currentDistrib_nMergedVars, '0');
//
//            String stateConditioningVars = m_base_nStates.substring(0, currentDistrib_nMergedVars-1);
//            int currentStateConditioningVars = Integer.parseInt(stateConditioningVars, nStates);
//
//            String stateLastVar = m_base_nStates.substring(currentDistrib_nMergedVars-1, currentDistrib_nMergedVars);
//            int currentStateLastVar = Integer.parseInt(stateLastVar, nStates);
//
//            double currentProb=currentDistProbabilities[m] * previous_max_probs[currentStateLastVar];
//            double maxProb=current_max_probs[currentStateConditioningVars];
//
//            if (currentProb>maxProb) {
//                current_max_probs[currentStateConditioningVars] = currentProb ;
//            }

            double maxProb = -1;
            for (int j = 0; j < nStates; j++) { // To go over all values of Y_t

                int currentState = Integer.parseInt(prevVarsState.concat(Integer.toString(j)),nStates);
//                System.out.println("Current state:" + currentState);

                if (current_probs[currentState] > maxProb) {
                    maxProb = current_probs[currentState];
                    thisVarMAPState = j;
                }
            }
            MAPsequence[t]=thisVarMAPState;
//            System.out.println("Currente Sequence Value: " + MAPsequence[t] + "\n\n");
        }

//        int previousVarMAPState = argMaxValues[0];
//        MAPsequence[0] = argMaxValues[0];
//
//        int thisVarMAPState = 0;
//        for (int t = 1; t < nTimeSteps; t++) {
//            current_probs = posteriorDistributionsMAPvariable.get(t);
//            previousVarMAPState = argMaxValues[t-1];
//
//            double maxProb = -1;
//            for (int j = 0; j < nStates; j++) { // To go over all values of Y_t
//
//                if (current_probs[previousVarMAPState * nStates + j] >= maxProb) {
//                    maxProb = current_probs[previousVarMAPState * nStates + j];
//                    thisVarMAPState = j;
//                }
//            }
//            MAPsequence[t]=thisVarMAPState;
//        }







        if (Arrays.stream(MAPsequence).anyMatch(value -> value<0)) {
            MAPestimateLogProbability=Double.NaN;
        }
        else {
            MAPestimateLogProbability = Math.log(MAPsequenceProbability);
        }
        this.MAPsequence = MAPsequence;

//        System.out.println("FINAL SEQUENCE: " + Arrays.toString(MAPsequence));
    }

//    /**
//     * Computes the Most Probable Sequence given the posterior distributions of the MAP variable.
//     * @param posteriorDistributionsMAPvariable a {@code List} of conditional distribution values.
//     */
//    private void computeMostProbableSequence(List<double[]> posteriorDistributionsMAPvariable) {
//
//        int[] MAPsequence = new int[nTimeSteps];
//        int MAPvarNStates = MAPvariable.getNumberOfStates();
//
//        int[] argMaxValues = new int[nTimeSteps];
//
//        double MAPsequenceProbability=-1;
//        double [] current_probs;
//        double [] current_max_probs;
//        double [] previous_max_probs = new double[MAPvarNStates];;
//
//        for (int t = nTimeSteps-1; t >= 0; t--) {
//
//            current_probs = posteriorDistributionsMAPvariable.get(t);
//            double maxProb=-1;
//
//            current_max_probs = new double[MAPvarNStates];
//
//            if (t==(nTimeSteps-1)) { // There are no previous_max_probs
//                for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_{t-1}
//                    maxProb=-1;
//                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_t
//                        if (current_probs[j * MAPvarNStates + k] > maxProb) {
//                            maxProb = current_probs[j * MAPvarNStates + k];
//
//                        }
//                    }
//                    current_max_probs[j]=maxProb;
//                }
//                argMaxValues[t] = (int)argMax(current_max_probs)[1];
//                previous_max_probs = current_max_probs;
//            }
//            else if (t>0 && t<(nTimeSteps-1)) {
//                for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_{t-1}
//                    maxProb=-1;
//                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_t
//                        if (current_probs[j * MAPvarNStates + k]*previous_max_probs[j] > maxProb) {
//                            maxProb = current_probs[j * MAPvarNStates + k]*previous_max_probs[k];
//                        }
//                    }
//                    current_max_probs[j]=maxProb;
//                }
//                argMaxValues[t] = (int)argMax(current_max_probs)[1];
//                previous_max_probs = current_max_probs;
//            }
//            else { // Here, t=0
//                for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_0
//                    maxProb=-1;
//                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_1
//                        if (current_probs[j]*previous_max_probs[j] > maxProb) {
//                            maxProb = current_probs[j]*previous_max_probs[j];
//                        }
//                    }
//                    current_max_probs[j]=maxProb;
//                }
//                MAPsequenceProbability =  argMax(current_max_probs)[0];
//                argMaxValues[t] = (int)argMax(current_max_probs)[1];
//                previous_max_probs = current_max_probs;
//            }
//        }
//
//        int previousVarMAPState = argMaxValues[0];
//        MAPsequence[0] = argMaxValues[0];
//
//        int thisVarMAPState = 0;
//        for (int t = 1; t < nTimeSteps; t++) {
//            current_probs = posteriorDistributionsMAPvariable.get(t);
//            previousVarMAPState = argMaxValues[t-1];
//
//            double maxProb = -1;
//            for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_t
//
//                if (current_probs[previousVarMAPState * MAPvarNStates + j] >= maxProb) {
//                    maxProb = current_probs[previousVarMAPState * MAPvarNStates + j];
//                    thisVarMAPState = j;
//                }
//            }
//            MAPsequence[t]=thisVarMAPState;
//        }
//
//        MAPestimate = new HashMapAssignment(nTimeSteps);
//
//        if (Arrays.stream(MAPsequence).anyMatch(value -> value<0)) {
//            MAPestimateLogProbability=Double.NaN;
//        }
//        else {
//            IntStream.range(0, nTimeSteps).forEach(t -> {
////                Variables varsAux = Serialization.deepCopy(this.staticEvenModel.getVariables());
////                Variable currentVar = varsAux.newMultinomialVariable(MAPvarName + "_t" + Integer.toString(t), MAPvariable.getNumberOfStates());
//                Variable currentVar;
//                try {
//                    currentVar = this.staticEvenModel.getVariables().getVariableByName(MAPvarName + "_t" + Integer.toString(t));
//                }
//                catch (Exception e) {
//                    Variables copy = Serialization.deepCopy(this.staticEvenModel.getVariables());
//                    currentVar = copy.newMultinomialVariable(MAPvarName + "_t" + Integer.toString(t), MAPvariable.getNumberOfStates());
//                }
//                MAPestimate.setValue(currentVar, MAPsequence[t]);
//            });
//            MAPestimateLogProbability = Math.log(MAPsequenceProbability);
//        }
//
//        this.MAPsequence = MAPsequence;
//    }



//    /**
//     * Returns Combined Conditional Distributions for both even and odd models.
//     * @param posteriorMAPVariableDistributions a {@code List} of {@code List} of {@link UnivariateDistribution} for each model.
//     * @return a {@code List} of conditional distributions values.
//     */
//    private List<double[]> combinePosteriorConditionalDistributions(List<List<UnivariateDistribution>> posteriorMAPVariableDistributions) {
//
//        List<double[]> listCondDistributions = new ArrayList<>(nTimeSteps);
//
//        int nStates = MAPvariable.getNumberOfStates();
//
//        // Univariate distribution Y_0
//        // UnivariateDistribution dist0_1 = posteriorMAPDistributionsEvenModel.get(0); // This variable Z_0 groups Y_0 and Y_1
//        // UnivariateDistribution dist0 = posteriorMAPDistributionsOddModel.get(0); // This variable is just Y_0 (not a group)
////        double[] dist0_probs = new double[nStates];
//
//        IntStream.range(0, nTimeSteps).forEachOrdered(timeStep -> {
//
////            System.out.println("\n\nTime step " + timeStep);
//            double [] combinedConditionalDistributionProbabilities;
//
//            combinedConditionalDistributionProbabilities =
//
//            IntStream.range(0, nMergedClassVars).mapToObj(modelNumber -> {
//
////                System.out.println("\nModel number " + modelNumber);
//
//                int distributionIndex = (timeStep>=modelNumber) ? (modelNumber==0 ? 0 : 1) + (timeStep-modelNumber)/nMergedClassVars : (timeStep-modelNumber)/nMergedClassVars;
//
//                UnivariateDistribution dist0 = posteriorMAPVariableDistributions.get(modelNumber).get(distributionIndex);
////                System.out.println(dist0.toString());
//
//                double[] probabilities  = new double[nStates];
//
//                int current_nMergedVars = (int)Math.round(Math.log(dist0.getVariable().getNumberOfStates())/Math.log(nStates));
//
//                for (int m = 0; m < Math.pow(nStates,current_nMergedVars); m++) {
//                    String m_base_nStates = Integer.toString(Integer.parseInt(Integer.toString(m), 10), nStates);
//                    m_base_nStates = StringUtils.leftPad(m_base_nStates, current_nMergedVars, '0');
//                    int currentVarIndex = (timeStep>=modelNumber) ? (timeStep-modelNumber)%nMergedClassVars : timeStep;
//                    int currentState = Integer.parseInt(m_base_nStates.substring(currentVarIndex,currentVarIndex+1));
//
//                    probabilities[currentState] += dist0.getParameters()[m];
//                }
////                System.out.println(Arrays.toString(probabilities));
//                return probabilities;
//
//            })
//            .reduce(new double[nStates], (doubleArray1, doubleArray2)-> {
//                for (int i=0; i<doubleArray1.length; i++)
//                    doubleArray1[i] += ((double)1/nMergedClassVars) * doubleArray2[i];
//                return doubleArray1;
//            });
//
////            System.out.println("Combined distribution " + Arrays.toString(combinedConditionalDistributionProbabilities));
//            listCondDistributions.add(combinedConditionalDistributionProbabilities);
//        });
//
//
//
//
////
////        double[] probs1  = new double[nMergedStates];
////        int probs_index1 = 0;
////
////        for (int m = 0; m < Math.pow(nStates,repetitionsConDistT); m++) {
////            String m_base_nStates = Integer.toString(Integer.parseInt(Integer.toString(m), 10), nStates);
////            m_base_nStates = StringUtils.leftPad(m_base_nStates, repetitionsConDistT, '0');
////
////            double probT=1;
////            for ( int n=0; n < m_base_nStates.length(); n++) {
////                int currentState = Integer.parseInt(m_base_nStates.substring(n,n+1));
////
////                int previousState;
////                if(n>=1)
////                    previousState = Integer.parseInt(m_base_nStates.substring(n-1,n));
////                else
////                    previousState = parentState;
////
////                assignment1 = new HashMapAssignment(2);
////                assignment1.setValue(dynVar.getInterfaceVariable(), previousState);
////                assignment1.setValue(dynVar,currentState);
////
////
////                probT = probT * conDistT.getConditionalProbability(assignment1);
////            }
////            probs1[probs_index1] =  probT;
////            probs_index1++;
////        }
////
////
////
////
////
////        dist0_probs = dist0.getParameters();
////        for (int i = 0; i < MAPvarNStates; i++) {
////            dist0_probs[i] = (double) 1/2 * dist0_probs[i];
////
////            for (int j = 0; j < MAPvarNStates; j++) {
////                dist0_probs[i] = dist0_probs[i] + (double) 1/2 * dist0_1.getProbability(i*MAPvarNStates + j);
////            }
////        }
////        listCondDistributions.add(dist0_probs);
////
////
//
//
//
//
//
////
////        // Conditional distribution Y_1 | Y_0;
////        UnivariateDistribution dist_paired1 = posteriorMAPDistributionsEvenModel.get(0); // This variable Z_0 groups Y_0 and Y_1
////        UnivariateDistribution dist_unpaired_1 = posteriorMAPDistributionsOddModel.get(1); // This variable groups Y_1 and Y_2 (if nTimeSteps>2)
////
////        double[] dist_probs1 = dist_paired1.getParameters();
////
////        for (int i = 0; i < MAPvarNStates; i++) { // To go over all values of Y_0
////            for (int j = 0; j < MAPvarNStates; j++) { // To go over all values of Y_1
////                int index = i * MAPvarNStates + j;
////                dist_probs1[index] = (double) 1/2 * dist_probs1[index];
////
////                if (nTimeSteps>2) {
////                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_2 in the distrib of (Y_1,Y_2)
////                        dist_probs1[index] = dist_probs1[index] + (double) 1 / 2 * dist_unpaired_1.getProbability(j * MAPvarNStates + k);
////                    }
////                }
////                else {
////                    for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_2 in the distrib of (Y_1,Y_2)
////                        dist_probs1[index] = dist_probs1[index] + (double) 1 / 2 * dist_unpaired_1.getProbability(j);
////                    }
////                }
////            }
////        }
////        listCondDistributions.add(dist_probs1);
////
////        IntStream.range(2, nTimeSteps - 1).forEachOrdered(t -> {
////            if (t % 2 == 0) {
////                int idxOdd = 1 + (t - 2) / 2;
////                UnivariateDistribution dist_paired = posteriorMAPDistributionsOddModel.get(idxOdd); // This variable groups Y_t and Y_{t-1}
////
////                UnivariateDistribution dist_unpaired_pre = posteriorMAPDistributionsEvenModel.get(idxOdd - 1); // This variable groups Y_{t-2} and Y_{t-1}
////                UnivariateDistribution dist_unpaired_post = posteriorMAPDistributionsEvenModel.get(idxOdd); // This variable groups Y_t and Y_{t+1}
////
////                double[] dist_probs = dist_paired.getParameters();
////
////                for (int i = 0; i < MAPvarNStates; i++) {  // To go over all values of Y_{t-1}
////                    for (int j = 0; j < MAPvarNStates; j++) {  // To go over all values of Y_t
////
////                        int index = i * MAPvarNStates + j;
////                        dist_probs[index] = (double) 1 / 2 * dist_probs[index];
////
////                        for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_{t-2}
////                            for (int m = 0; m < MAPvarNStates; m++) {  // To go over all values of Y_{t+1}
////                                dist_probs[index] = dist_probs[index] + (double) 1 / 2 * dist_unpaired_pre.getProbability(k * MAPvarNStates + i) * dist_unpaired_post.getProbability(j * MAPvarNStates + m);
////                            }
////                        }
////                    }
////                }
////                listCondDistributions.add(dist_probs);
////            } else {
////                int idxEven = (t - 1) / 2;
////                UnivariateDistribution dist_paired = posteriorMAPDistributionsEvenModel.get(idxEven); // This variable groups Y_t and Y_{t-1}
////
////                UnivariateDistribution dist_unpaired_pre = posteriorMAPDistributionsOddModel.get(idxEven); // This variable groups Y_{t-2} and Y_{t-1}
////                UnivariateDistribution dist_unpaired_post = posteriorMAPDistributionsOddModel.get(idxEven + 1); // This variable groups Y_t and Y_{t+1}
////
////                double[] dist_probs = dist_paired.getParameters();
////
////                for (int i = 0; i < MAPvarNStates; i++) {  // To go over all values of Y_{t-1}
////                    for (int j = 0; j < MAPvarNStates; j++) {  // To go over all values of Y_t
////
////                        int index = i * MAPvarNStates + j;
////                        dist_probs[index] = (double) 1 / 2 * dist_probs[index];
////
////                        for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_{t-2}
////                            for (int m = 0; m < MAPvarNStates; m++) {  // To go over all values of Y_{t+1}
////                                dist_probs[index] = dist_probs[index] + (double) 1 / 2 * dist_unpaired_pre.getProbability(k * MAPvarNStates + i) * dist_unpaired_post.getProbability(j * MAPvarNStates + m);
////                            }
////                        }
////                    }
////                }
////                listCondDistributions.add(dist_probs);
////            }
////        });
////
////        if (nTimeSteps>2) {
////            // Conditional distribution Y_t | Y_{t-1},  with  t = nTimeSteps-1
////            int t = (nTimeSteps - 1);
////            if (t % 2 == 0) {
////                int idxOdd = 1 + (t - 2) / 2;
////                UnivariateDistribution dist_paired = posteriorMAPDistributionsOddModel.get(idxOdd); // This variable groups Y_t and Y_{t-1}
////
////                UnivariateDistribution dist_unpaired_pre = posteriorMAPDistributionsEvenModel.get(idxOdd - 1);  // This variable groups Y_{t-2} and Y_{t-1}
////                UnivariateDistribution dist_unpaired_post = posteriorMAPDistributionsEvenModel.get(idxOdd); // This variable is just Y_t (not a group)
////
////                double[] dist_probs = dist_paired.getParameters();
////
////                for (int i = 0; i < MAPvarNStates; i++) {  // To go over all values of Y_{t-1}
////                    for (int j = 0; j < MAPvarNStates; j++) {  // To go over all values of Y_t
////
////                        int index = i * MAPvarNStates + j;
////                        dist_probs[index] = (double) 1 / 2 * dist_probs[index];
////
////                        for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_{t-2}
////
////                            dist_probs[index] = dist_probs[index] + (double) 1 / 2 * dist_unpaired_pre.getProbability(k * MAPvarNStates + i) * dist_unpaired_post.getProbability(j);
////                        }
////                    }
////                }
////                listCondDistributions.add(dist_probs);
////            }
////            else {
////                int idxEven = (t - 1) / 2;
////                UnivariateDistribution dist_paired = posteriorMAPDistributionsEvenModel.get(idxEven); // This variable groups Y_t and Y_{t-1}
////
////                UnivariateDistribution dist_unpaired_pre = posteriorMAPDistributionsOddModel.get(idxEven);  // This variable groups Y_{t-2} and Y_{t-1}
////
////                double[] dist_probs = dist_paired.getParameters();
////
////                for (int i = 0; i < MAPvarNStates; i++) {  // To go over all values of Y_{t-1}
////                    for (int j = 0; j < MAPvarNStates; j++) {  // To go over all values of Y_t
////
////                        int index = i * MAPvarNStates + j;
////                        dist_probs[index] = (double) 1 / 2 * dist_probs[index];
////
////                        for (int k = 0; k < MAPvarNStates; k++) { // To go over all values of Y_{t-2}
////                            dist_probs[index] = dist_probs[index] + (double) 1 / 2 * dist_unpaired_pre.getProbability(k * MAPvarNStates + i);
////                        }
////                    }
////                }
////                listCondDistributions.add(dist_probs);
////            }
////        }
//
//        return listCondDistributions;
//    }

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
     * @param modelNumber a {@code int} that indicates the number of the model being constructed.
     * @return a {@link Variables} object.
     */
    private Variables obtainReplicatedStaticVariables(DynamicVariables dynamicVariables, int modelNumber) {

        Variables variables = new Variables();

        // REPLICATIONS OF THE MAP VARIABLE (EACH 'nMergedClassVars' CONSECUTIVE ARE GROUPED)
        int replicationsMAPVariable = (modelNumber==0 ? 0 : 1) + (nTimeSteps-modelNumber)/nMergedClassVars + ((nTimeSteps-modelNumber)%nMergedClassVars==0 ? 0 : 1);

        IntStream.range(0, replicationsMAPVariable).forEach(mergedClassVarIndex -> {

            int nStatesMAPVariable = (int) Math.pow(MAPvariable.getNumberOfStates(),nMergedClassVars);

            // If it is the first merged variable and not the first model (not 'complete')
            if ( (modelNumber!=0) && (mergedClassVarIndex==0) ) {
                nStatesMAPVariable = (int) Math.pow(MAPvariable.getNumberOfStates(),modelNumber);
            }
            // If it is the last merged variable and not 'complete'
            if ( ((nTimeSteps-modelNumber)%nMergedClassVars!=0) && (mergedClassVarIndex==replicationsMAPVariable-1) ) {
                nStatesMAPVariable = (int) Math.pow(MAPvariable.getNumberOfStates(),(nTimeSteps-modelNumber)%nMergedClassVars);
            }

            variables.newMultinomialVariable(groupedClassName + "_t" + Integer.toString(mergedClassVarIndex), nStatesMAPVariable);
        });

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
     * @param modelNumber an integer
     * @return a {@link DAG} object.
     */
    private DAG obtainStaticDAG(DynamicDAG dynamicDAG, Variables variables, int modelNumber) {

        DAG dag = new DAG(variables);
        DynamicVariables dynamicVariables = dynamicDAG.getDynamicVariables();

        /*
         * PARENTS OF THE MAP VARIABLE (ONLY THE PREVIOUS TEMPORAL COPY OF ITSELF)
         */
        int replicationsMAPVariable = (modelNumber==0 ? 0 : 1) + (nTimeSteps-modelNumber)/nMergedClassVars + ((nTimeSteps-modelNumber)%nMergedClassVars==0 ? 0 : 1);

        IntStream.range(1, replicationsMAPVariable).forEach(mergedClassVarIndex -> {
            Variable staticVar = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(mergedClassVarIndex));
            dag.getParentSet(staticVar).addParent(variables.getVariableByName(groupedClassName + "_t" + Integer.toString(mergedClassVarIndex - 1)));
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
                    IntStream.range(1, nTimeSteps).forEach(timeStep -> {

                        Variable staticVar = variables.getVariableByName(dynVar.getName() + "_t" + Integer.toString(timeStep));

                        List<Variable> parents = dynamicDAG.getParentSetTimeT(dynVar).getParents();

                        int indexMAPReplication = (timeStep>=modelNumber) ? (modelNumber==0 ? 0 : 1) + (timeStep-modelNumber)/nMergedClassVars : (timeStep-modelNumber)/nMergedClassVars;

                        if (indexMAPReplication>=replicationsMAPVariable) {
                            System.out.println("Error in obtainStaticDAG: Bad MAP variable index");
                            System.exit(-50);
                        }

                        // PARENTS WHICH ARE INTERFACE VARIABLES
                        List<Variable> parentsInterface = parents.stream().filter(parentVar -> parentVar.isInterfaceVariable()).collect(Collectors.toList());

                        parentsInterface.stream().filter(parent -> parent.equals(MAPvariable)).forEach(parentVar -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(groupedClassName + "_t" + Integer.toString(indexMAPReplication - 1))));
                        parentsInterface.stream().filter(parent -> !parent.equals(MAPvariable)).forEach(parentVar -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(parentVar.getName().replace("_Interface", "_t" + Integer.toString(timeStep - 1)))));

                        // PARENTS WHICH ARE NOT INTERFACE VARIABLES
                        List<Variable> parentsNotInterface = parents.stream().filter(parentVar -> !parentVar.isInterfaceVariable()).collect(Collectors.toList());

                        parentsNotInterface.stream().filter(parent -> parent.equals(MAPvariable)).forEach(parentVar -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(groupedClassName + "_t" + Integer.toString(indexMAPReplication))));
                        parentsNotInterface.stream().filter(parent -> !parent.equals(MAPvariable)).forEach(parentVar -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(parentVar.getName() + "_t" + Integer.toString(timeStep))));

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
    private Multinomial groupedDistributionMAPVariableTime0(Variable dynVar, Variable staticVar, ConditionalDistribution conDist0, ConditionalDistribution conDistT, int modelNumber) {

        if(modelNumber == 1) {
            return (Multinomial) conDist0;
        }

        Assignment assignment0;
        assignment0 = new HashMapAssignment(1);

        int nStates = dynVar.getNumberOfStates();
        int nMergedStates = staticVar.getNumberOfStates();

        Multinomial multinomial = new Multinomial(staticVar);

        double[] probs = new double[nMergedStates];
        int probs_index=0;

        int repetitionsConDistT = (int)Math.round(Math.log(nMergedStates)/Math.log(nStates)-1);

        Assignment assignment1;
        for (int k = 0; k < nStates; k++) {

            // Probabilities at t=0
            assignment0.setValue(dynVar, k);
            double prob0 = conDist0.getConditionalProbability(assignment0);

            for (int m = 0; m < Math.pow(nStates,repetitionsConDistT); m++) {
                String m_base_nStates = Integer.toString(Integer.parseInt(Integer.toString(m), 10), nStates);
                m_base_nStates = StringUtils.leftPad(m_base_nStates, repetitionsConDistT, '0');
                //System.out.println(m_base_nStates);

                double probT=1;
                for ( int n=0; n < m_base_nStates.length(); n++) {
                    int currentState = Integer.parseInt(m_base_nStates.substring(n,n+1));

                    int previousState;
                    if(n>=1)
                        previousState = Integer.parseInt(m_base_nStates.substring(n-1,n));
                    else
                        previousState = k;

                    assignment1 = new HashMapAssignment(2);
                    assignment1.setValue(dynVar.getInterfaceVariable(), previousState);
                    assignment1.setValue(dynVar,currentState);
                    probT = probT * conDistT.getConditionalProbability(assignment1);
                }
                probs[probs_index] =  prob0 * probT;
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
    private Multinomial_MultinomialParents groupedDistributionMAPVariableTimeT(Variable dynVar, Variable staticVar, int nStatesStaticVarParent, List<Variable> parents, ConditionalDistribution conDistT, int modelNumber) {

        Multinomial_MultinomialParents multinomial_multinomialParents = new Multinomial_MultinomialParents(staticVar, parents);
        Assignment assignment1;
        Multinomial multinomial;

        int nStates = dynVar.getNumberOfStates();
        int nMergedStates = staticVar.getNumberOfStates();

        int repetitionsConDistT = (int)Math.round(Math.log(nMergedStates)/Math.log(nStates));

        for (int s = 0; s < nStatesStaticVarParent; s++) {
            int parentState = s % nStates;

            double[] probs1  = new double[nMergedStates];
            int probs_index1 = 0;

            for (int m = 0; m < Math.pow(nStates,repetitionsConDistT); m++) {
                String m_base_nStates = Integer.toString(Integer.parseInt(Integer.toString(m), 10), nStates);
                m_base_nStates = StringUtils.leftPad(m_base_nStates, repetitionsConDistT, '0');

                double probT=1;
                for ( int n=0; n < m_base_nStates.length(); n++) {
                    int currentState = Integer.parseInt(m_base_nStates.substring(n,n+1));

                    int previousState;
                    if(n>=1)
                        previousState = Integer.parseInt(m_base_nStates.substring(n-1,n));
                    else
                        previousState = parentState;

                    assignment1 = new HashMapAssignment(2);
                    assignment1.setValue(dynVar.getInterfaceVariable(), previousState);
                    assignment1.setValue(dynVar,currentState);


                    probT = probT * conDistT.getConditionalProbability(assignment1);
                }
                probs1[probs_index1] =  probT;
                probs_index1++;
            }

            multinomial = new Multinomial(staticVar);
            multinomial.setProbabilities(probs1);
            multinomial_multinomialParents.setMultinomial(s, multinomial);
        }
        return multinomial_multinomialParents;
    }

    /**
     * Returns the {@link BayesianNetwork} related to the static grouped class.
     * @param dag a {@link DAG} object.
     * @param variables a {@link Variables} obejct.
     * @param modelNumber an integer
     * @return a {@link BayesianNetwork} object.
     */
    private BayesianNetwork obtainStaticMergedClassVarNetwork(DAG dag, Variables variables, int modelNumber) {

        DynamicDAG dynamicDAG = model.getDynamicDAG();
        BayesianNetwork bn = new BayesianNetwork(dag);
        Variable staticVar, dynVar;
        ConditionalDistribution conDist0, conDistT;

        int replicationsMAPVariable = (modelNumber==0 ? 0 : 1) + (nTimeSteps-modelNumber)/nMergedClassVars + ((nTimeSteps-modelNumber)%nMergedClassVars==0 ? 0 : 1);


        /*
         * OBTAIN AND SET THE CONDITIONAL (UNIVARIATE) DISTRIBUTION FOR THE GROUPED MAP/CLASS VARIABLE AT TIME T=0
         */
        staticVar = variables.getVariableByName(groupedClassName + "_t0");
        dynVar = model.getDynamicVariables().getVariableByName(MAPvarName);

        conDist0 = Serialization.deepCopy(model.getConditionalDistributionsTime0().get(dynVar.getVarID()));
        conDistT = Serialization.deepCopy(model.getConditionalDistributionsTimeT().get(dynVar.getVarID()));

        Multinomial multinomial = groupedDistributionMAPVariableTime0(dynVar, staticVar, conDist0, conDistT, modelNumber);

        multinomial.setVar(staticVar);
        bn.setConditionalDistribution(staticVar, multinomial);


        /*
         * CREATE THE GENERAL (TIME T) CONDITIONAL DISTRIBUTION OF THE GROUPED MAP/CLASS VARIABLE, IF NEEDED
         */
        Multinomial_MultinomialParents generalConditionalDistTimeT;

        // ToDo: Review Condition: nTimeSteps>= 4,5
        if (modelNumber==0 && (replicationsMAPVariable>2 || (replicationsMAPVariable==2 && nTimeSteps>=4))) {
            Variable staticVar_current = variables.getVariableByName(groupedClassName + "_t1");
            Variable staticVar_interface = variables.getVariableByName(groupedClassName + "_t0");
            List<Variable> parents = bn.getDAG().getParentSet(staticVar_current).getParents();
            ConditionalDistribution conDist_dynamic = Serialization.deepCopy(conDistT);

            generalConditionalDistTimeT = groupedDistributionMAPVariableTimeT(dynVar, staticVar_current, staticVar_interface.getNumberOfStates(), parents, conDist_dynamic, modelNumber);

        }
        else if (modelNumber>0 && (replicationsMAPVariable>3 || replicationsMAPVariable==3 && nTimeSteps>=5)) {
            Variable staticVar_current = variables.getVariableByName(groupedClassName + "_t2");
            Variable staticVar_interface = variables.getVariableByName(groupedClassName + "_t1");
            List<Variable> parents = bn.getDAG().getParentSet(staticVar_current).getParents();
            ConditionalDistribution conDist_dynamic = Serialization.deepCopy(conDistT);

            generalConditionalDistTimeT = groupedDistributionMAPVariableTimeT(dynVar, staticVar_current, staticVar_interface.getNumberOfStates(), parents, conDist_dynamic, modelNumber);

        }
        else { // In this case, 'generalConditionalDistTimeT' will never be used.
            generalConditionalDistTimeT = new Multinomial_MultinomialParents(staticVar, bn.getDAG().getParentSet(staticVar).getParents());
        }


        /*
         * ADD CONDITIONAL DISTRIBUTIONS FOR THE REPLICATIONS OF THE GROUPED MAP/CLASS VARIABLE
         */
        // FIRST CONDITIONAL DISTRIBUTION, t_1 | t_0, IF IT'S NOT THE GENERAL ONE
        if (modelNumber!=0) {
            Variable staticVar0 = variables.getVariableByName(groupedClassName + "_t1");
            Variable staticVar0_interface = variables.getVariableByName(groupedClassName + "_t0");
            List<Variable> parents = bn.getDAG().getParentSet(staticVar0).getParents();
            ConditionalDistribution conDist_dynamic = Serialization.deepCopy(model.getConditionalDistributionsTimeT().get(dynVar.getVarID()));
            ConditionalDistribution conditionalDistTime1 = groupedDistributionMAPVariableTimeT(dynVar, staticVar0, staticVar0_interface.getNumberOfStates(), parents, conDist_dynamic, modelNumber);
            conditionalDistTime1.setVar(staticVar0);
            bn.setConditionalDistribution(staticVar0, conditionalDistTime1);
        }

        // INTERMEDIATE COMPLETE CONDITIONAL DISTRIBUTIONS, t_i | t_{i-1}, FOLLOWING THE GENERAL CONDITIONAL DISTRIBUTION
        int initialTimeStep=2;
        int finalTimeStep=replicationsMAPVariable-1;

        if (modelNumber==0)
            initialTimeStep = 1;
        if ((nTimeSteps-modelNumber)%nMergedClassVars==0)
            finalTimeStep = replicationsMAPVariable;

        IntStream.range(initialTimeStep, finalTimeStep).forEach(timeStep -> {
            Variable staticVar1 = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(timeStep));
            ConditionalDistribution conditionalDistribution = Serialization.deepCopy(generalConditionalDistTimeT);
            conditionalDistribution.setConditioningVariables(dag.getParentSet(staticVar1).getParents());
            conditionalDistribution.setVar(staticVar1);
            bn.setConditionalDistribution(staticVar1, conditionalDistribution);

        });


        // LAST CONDITIONAL DISTRIBUTION, t_{nTimeSteps} | t_{nTimeSteps-1}, IF IT'S NOT THE GENERAL ONE
        if ((nTimeSteps-modelNumber)%nMergedClassVars!=0 ) {

            Variable staticVar1 = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(replicationsMAPVariable - 1));
            Variable staticVar1_interface = variables.getVariableByName(groupedClassName + "_t" + Integer.toString(replicationsMAPVariable - 2));
            List<Variable> parents1 = bn.getDAG().getParentSet(staticVar1).getParents();

            Multinomial_MultinomialParents lastConDist = groupedDistributionMAPVariableTimeT(dynVar, staticVar1, staticVar1_interface.getNumberOfStates(), parents1, conDistT, modelNumber);

            bn.setConditionalDistribution(staticVar1, lastConDist);
        }

        /*
         * ADD CONDITIONAL DISTRIBUTIONS FOR VARIABLES HAVING AS A PARENT THE GROUPED MAP/CLASS VARIABLE, AT TIME T=0
         */
        List<Variable> dynVariables = model.getDynamicVariables().getListOfDynamicVariables();
        List<Variable> dynVariablesWithClassParent = dynVariables.stream().filter(var -> !var.equals(MAPvariable)).filter(var -> dynamicDAG.getParentSetTime0(var).contains(MAPvariable)).collect(Collectors.toList());
        List<Variable> dynVariablesNoClassParent = dynVariables.stream().filter(var -> !var.equals(MAPvariable)).filter(var -> !dynamicDAG.getParentSetTime0(var).contains(MAPvariable)).collect(Collectors.toList());


//        dynVariablesWithClassParent.stream().forEach(dynVariable -> {
//            ConditionalDistribution conditionalDistribution = Serialization.deepCopy(model.getConditionalDistributionTime0(dynVariable));
//
//            Variable staticMAPVar1 = variables.getVariableByName(groupedClassName + "_t0");
//            Variable staticVar1 = variables.getVariableByName(dynVariable.getName() + "_t0");
//            List<Variable> thisVarParents = conditionalDistribution.getConditioningVariables();
//            List<Variable> parentList0 = bn.getDAG().getParentSet(staticVar1).getParents();
//            int indexMAPvariable = thisVarParents.indexOf(MAPvariable);
//
//            thisVarParents.set(indexMAPvariable, staticMAPVar1);
//
//
//            conditionalDistribution.setConditioningVariables(parentList0);
//            conditionalDistribution.setVar(staticVar1);
//
////            if(modelNumber==1)
////                bn.setConditionalDistribution(staticVar1, conditionalDistribution);
////            else {
//            BaseDistribution_MultinomialParents staticVar2Distribution = obtainDistributionOfMAPChildren(staticVar1, conditionalDistribution, parentList0, modelNumber, 0);
//            bn.setConditionalDistribution(staticVar1, staticVar2Distribution);
////            }
//
//        });


        /*
         * ADD CONDITIONAL DISTRIBUTIONS FOR VARIABLES HAVING AS A PARENT THE GROUPED MAP/CLASS VARIABLE, AT ANY TIME T
         */
        dynVariablesWithClassParent.stream().forEach(dynVariable -> {
            IntStream.range(0, nTimeSteps).forEachOrdered(timeStep -> {

                ConditionalDistribution dynamicConDist;
                dynamicConDist = Serialization.deepCopy( timeStep==0 ? model.getConditionalDistributionTime0(dynVariable) : model.getConditionalDistributionTimeT(dynVariable) );
//                )if(timeStep==0) {
//                     = Serialization.deepCopy(model.getConditionalDistributionTime0(dynVariable));
//                }
//                else {
//                    dynamicConDist = Serialization.deepCopy(model.getConditionalDistributionTimeT(dynVariable));
//                }
                Variable staticVar2 = variables.getVariableByName(dynVariable.getName() + "_t" + Integer.toString(timeStep));
                List<Variable> parentList = bn.getDAG().getParentSet(staticVar2).getParents();

                ConditionalDistribution staticVar2Distribution = obtainDistributionOfMAPChildren(staticVar2, dynamicConDist, parentList, modelNumber, timeStep);
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
            thisVarParents = thisVarParents.stream().map(parent -> variables.getVariableByName(parent.getName() + "_t0")).collect(Collectors.toList());

            conditionalDistribution.setConditioningVariables(thisVarParents);
            conditionalDistribution.setVar(staticVar1);
            bn.setConditionalDistribution(staticVar1, conditionalDistribution);

            // TIMES T>0
            IntStream.range(1, nTimeSteps).forEach(i -> {
                ConditionalDistribution conditionalDistribution1 = Serialization.deepCopy(model.getConditionalDistributionTimeT(dynVariable));
                Variable staticVar2 = variables.getVariableByName(dynVariable.getName() + "_t" + Integer.toString(i));
                List<Variable> thisVarParents1 = conditionalDistribution1.getConditioningVariables();
                thisVarParents1 = thisVarParents1.stream().map(parent -> {
                    if(parent.getName().contains("_Interface")) {
                        return variables.getVariableByName(parent.getName().replace("_Interface","_t" + Integer.toString(i-1)));
                    }
                    else {
                        return variables.getVariableByName(parent.getName() + "_t" + Integer.toString(i));
                    }
                }).collect(Collectors.toList());
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
     * @param modelNumber an integer
     * @param time_step an integer with the time step.
     * @return a {@link BaseDistribution_MultinomialParents} distribution.
     */
    private ConditionalDistribution obtainDistributionOfMAPChildren(Variable staticVariable, ConditionalDistribution dynamicConditionalDistribution, List<Variable> parentList, int modelNumber, int time_step) {

        boolean allParentsMultinomial = parentList.stream().allMatch(parent -> parent.isMultinomial());
        List<Variable> multinomialParents = parentList.stream().filter(parent -> parent.isMultinomial()).collect(Collectors.toList());
        List<Variable> continuousParents = parentList.stream().filter(parent -> !parent.isMultinomial()).collect(Collectors.toList());

        //BaseDistribution_MultinomialParents staticVarConDist = new BaseDistribution_MultinomialParents(staticVariable, parentList);

        ConditionalDistribution staticVarConDist;

        // In this method, all variables have at least one parent (either discrete or continuous)
        int distributionType = -1;
        if (staticVariable.isMultinomial()) {
            distributionType = 0;
            staticVarConDist = new Multinomial_MultinomialParents(staticVariable, parentList);
        }
        else if (staticVariable.isNormal()) {

            int nMultinomialParents = multinomialParents.size();
            int nNormalParents = continuousParents.size();

            if (nNormalParents>0 && nMultinomialParents==0) {
                distributionType = 1;
                staticVarConDist = new ConditionalLinearGaussian(staticVariable, parentList);
            }
            else if (nNormalParents==0 && nMultinomialParents>0) {
                distributionType = 2;
                staticVarConDist = new Normal_MultinomialParents(staticVariable, parentList);
            }
            else if (nNormalParents>0 && nMultinomialParents>0) {
                distributionType = 3;
                staticVarConDist = new Normal_MultinomialNormalParents(staticVariable, parentList);
            }
            else {
                throw new IllegalArgumentException("Unrecognized DistributionType. ");
            }
        }
        else {
            throw new IllegalArgumentException("Unrecognized DistributionType. ");
        }

        int nStatesMultinomialParents = (int) Math.round(Math.exp(multinomialParents.stream().mapToDouble(parent -> Math.log(parent.getNumberOfStates())).sum()));
        int nStatesMAPVariable = MAPvariable.getNumberOfStates();

        for (int m = 0; m < nStatesMultinomialParents; m++) {
            Assignment staticParentsConfiguration = MultinomialIndex.getVariableAssignmentFromIndex(multinomialParents, m);
            Assignment dynamicParentsConfiguration = new HashMapAssignment(multinomialParents.size());

            IntStream.range(0, multinomialParents.size()).forEach(k -> {
                Variable currentParent = multinomialParents.get(k);
                int parentValue = (int)staticParentsConfiguration.getValue(currentParent);
                String parentName;

                if (currentParent.getName().contains(groupedClassName)) {
                    parentName = currentParent.getName().replace(groupedClassName, MAPvarName).replaceAll("_t\\d+", "");
                    Variable dynCurrentParent = model.getDynamicVariables().getVariableByName(parentName);

                    int dynParentValue;


                    int nMergedStates = currentParent.getNumberOfStates();
                    int repetitionsConDistT = (int)Math.round(Math.log(nMergedStates)/Math.log(nStatesMAPVariable));
                    int indexCurrentParentState;
                    if(time_step>=modelNumber)
                        indexCurrentParentState = (time_step - modelNumber)%nMergedClassVars;
                    else
                        indexCurrentParentState = time_step;

                    String m_base_nStates = Integer.toString(Integer.parseInt(Integer.toString(parentValue), 10), nStatesMAPVariable);
                    m_base_nStates = StringUtils.leftPad(m_base_nStates, repetitionsConDistT, '0');

                    int dynamicParentState = Integer.parseInt(m_base_nStates.substring(indexCurrentParentState,indexCurrentParentState+1));


                    dynParentValue = dynamicParentState;
                    dynamicParentsConfiguration.setValue(dynCurrentParent, dynParentValue);

//                    System.out.println("Variable: " + staticVariable.getName() + " with " + staticVariable.getNumberOfStates() + " states and " + parentList.size() + " parents");
//                    System.out.println("Parent " + parentName + " with " + nMergedStates + " states");
//                    System.out.println("Time step " + time_step + " and model number " + modelNumber);
//                    System.out.println("Parent state number " + parentValue + " which is " + m_base_nStates);
//                    System.out.println("Index parent state " + indexCurrentParentState);
//                    System.out.println("Dynamic parent state number " + dynamicParentState);
//                    System.out.println();
//                    if (time_step==0) { // Variable at time t=0
//                        if(modelNumber!=1) {
//                            //dynParentValue = parentValue / (int) Math.pow(nStatesMAPVariable, nMergedClassVars - 1);
////                            System.out.println(currentParent.getNumberOfStates());
////                            System.out.println(nStatesMAPVariable);
////                            System.out.println(parentValue);
////                            System.out.println(parentValue / (currentParent.getNumberOfStates()/nStatesMAPVariable));
//                            dynParentValue = parentValue / (int) Math.pow(nStatesMAPVariable, nMergedClassVars - 1);
//                        }
//                        else {
//                            dynParentValue = parentValue;
//                        }
//                    } // Variable at time t=nTimeSteps-1 (last copy) and not complete
//                    else {
//
//
//                        if ((time_step - modelNumber) % nMergedClassVars != 0 && (time_step == nTimeSteps - 1)) {
//                            dynParentValue = parentValue % nStatesMAPVariable;
//                        } else {
//                            if ((time_step - modelNumber) % nMergedClassVars == 0) {
//                                dynParentValue = parentValue / (currentParent.getNumberOfStates() / nStatesMAPVariable);
//                                ;
//                            } else {
//                                dynParentValue = parentValue % nStatesMAPVariable;
//                            }
//                        }
//                    }
//                        if ((!even_partition && nTimeSteps % 2 == 0 && (time_step == nTimeSteps - 1)) || (even_partition && nTimeSteps % 2 == 1 && (time_step == nTimeSteps - 1))) {
//                            dynParentValue = parentValue;
//                        } else {
//                            if ((!even_partition && (time_step % 2 == 1)) || (even_partition && (time_step % 2 == 0))) {
//                                dynParentValue = parentValue / MAPvariable.getNumberOfStates();
//                            } else {
//                                dynParentValue = parentValue % MAPvariable.getNumberOfStates();
//                            }
//                        }
                }
                else {
                    if (multinomialParents.get(k).getName().endsWith("_t" + Integer.toString(time_step - 1))) {
                        parentName = multinomialParents.get(k).getName().replaceFirst("_t\\d+", "");
                        Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
                        dynamicParentsConfiguration.setValue(dynParent.getInterfaceVariable(), parentValue);
                    }
                    else {
                        parentName = multinomialParents.get(k).getName().replaceFirst("_t\\d+", "");
                        Variable dynParent = model.getDynamicVariables().getVariableByName(parentName);
                        dynamicParentsConfiguration.setValue(dynParent, parentValue);
                    }
                }
            });

//            System.out.println(dynamicParentsConfiguration.outputString());

//            if (allParentsMultinomial && staticVariable.isMultinomial()) {
////                try {
//
//                Multinomial_MultinomialParents multinomial_multinomialParents = (Multinomial_MultinomialParents) dynamicConditionalDistribution;
//                Multinomial multinomial1 = (Multinomial) multinomial_multinomialParents.getMultinomial(dynamicParentsConfiguration);
//
//                multinomial1.setVar(staticVariable);
//                multinomial1.setConditioningVariables(multinomialParents);
//
////                System.out.println(multinomial1.toString()+"\n\n");
//                staticVarConDist.setBaseDistribution(m, multinomial1);
//                staticVarConDist.set
////                }
////                catch(Exception e) {
////                    System.out.println("Exception");
////                    System.out.println(e.getMessage());
////                    System.out.println(staticVariable.getName());
////                    System.out.println(dynamicParentsConfiguration.outputString());
////                }
//            }
//            else if (allParentsMultinomial && staticVariable.isNormal() ){
//                Normal_MultinomialParents normal_multinomialParents = (Normal_MultinomialParents) dynamicConditionalDistribution;
//                Normal clg = normal_multinomialParents.getNormal(dynamicParentsConfiguration);
//                clg.setConditioningVariables(multinomialParents);
//                //clg.setConditioningVariables(continuousParents);
//                clg.setVar(staticVariable);
//
//                staticVarConDist.setBaseDistribution(m, clg);
//            }
//            else {
//                Normal_MultinomialNormalParents normal_multinomialNormalParents = (Normal_MultinomialNormalParents) dynamicConditionalDistribution;
//                ConditionalLinearGaussian clg = normal_multinomialNormalParents.getNormal_NormalParentsDistribution(dynamicParentsConfiguration);
//                clg.setConditioningVariables(continuousParents);
//                clg.setVar(staticVariable);
//
//                staticVarConDist.setBaseDistribution(m, clg);
//            }


            if (distributionType==0) { // Multinomial_Multinomial

                Multinomial_MultinomialParents multinomial_multinomialParents = (Multinomial_MultinomialParents) dynamicConditionalDistribution;
                Multinomial multinomial1 = (Multinomial) multinomial_multinomialParents.getMultinomial(dynamicParentsConfiguration);

                multinomial1.setVar(staticVariable);
                multinomial1.setConditioningVariables(multinomialParents);

                ((Multinomial_MultinomialParents)staticVarConDist).setMultinomial(m, multinomial1);

            }
            else if (distributionType==2 ){ // Normal_Multinomial

                Normal_MultinomialParents normal_multinomialParents = (Normal_MultinomialParents) dynamicConditionalDistribution;

                Normal normal1 = normal_multinomialParents.getNormal(dynamicParentsConfiguration);
                normal1.setConditioningVariables(multinomialParents);
                //clg.setConditioningVariables(continuousParents);
                normal1.setVar(staticVariable);

                ((Normal_MultinomialParents)staticVarConDist).setNormal(m, normal1);

            }
            else if (distributionType==3) { // Normal_MultinomialNormal

                Normal_MultinomialNormalParents normal_multinomialNormalParents = (Normal_MultinomialNormalParents) dynamicConditionalDistribution;
                ConditionalLinearGaussian clg = normal_multinomialNormalParents.getNormal_NormalParentsDistribution(dynamicParentsConfiguration);
                clg.setConditioningVariables(continuousParents);
                clg.setVar(staticVariable);

                ((Normal_MultinomialNormalParents)staticVarConDist).setNormal_NormalParentsDistribution(m, clg);

            }
            else { // ConditionalLinearGaussian, distributionType==1
                ConditionalLinearGaussian clg = (ConditionalLinearGaussian) dynamicConditionalDistribution;
                //((ConditionalLinearGaussian)staticVarConDist)
                staticVarConDist = clg;
            }

        }

//        if (allParentsMultinomial && staticVariable.isNormal())
//            return (Normal_MultinomialParents)staticVarConDist;
//        else {
//            return staticVarConDist;
//        }
        return staticVarConDist;
    }



//    public static Iterator<List<DynamicAssignment>> generateEvidence(DynamicBayesianNetwork dynamicBayesianNetwork, String mapVariableName, int numberOfSequences, int sequenceLength, double percentageOfEvidence, int seed) {
//
//        Random random = new Random(seed);
//        Variable mapVariable = dynamicBayesianNetwork.getDynamicVariables().getVariableByName(mapVariableName);
//        List<List<DynamicAssignment>> evidences = new ArrayList<>(numberOfSequences);
//
//        int nVarsEvidence = (int)(percentageOfEvidence * dynamicBayesianNetwork.getNumberOfVars());
//
//
//        //DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicBayesianNetwork);
//        //DataStream<DynamicDataInstance> sample = sampler.sampleToDataBase(numberOfSequences,sequenceLength);
//
//        for (int i = 0; i < numberOfSequences; i++) {
//
//
//            List<Variable> varsDynamicModel = dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();
//
//            if (nVarsEvidence > varsDynamicModel.size()-1) {
//                System.out.println("Too many variables to be observe");
//                return null;
//            }
//
//            List<Variable> varsEvidence = new ArrayList<>(nVarsEvidence);
//
//            int currentVarsEvidence=0;
//            while (currentVarsEvidence < nVarsEvidence) {
//                int indexVarEvidence = random.nextInt(dynamicBayesianNetwork.getNumberOfDynamicVars());
//                Variable varEvidence = varsDynamicModel.get(indexVarEvidence);
//
//                if (varEvidence.equals(mapVariable) || varsEvidence.contains(varEvidence)) {
//                    continue;
//                }
//                varsEvidence.add(varEvidence);
//                currentVarsEvidence++;
//            }
//
//            List<DynamicAssignment> fullEvidence, evidence;
//            evidence = new ArrayList<>(sequenceLength);
//
//            DynamicBayesianNetworkSampler bayesianNetworkSampler0 = new DynamicBayesianNetworkSampler(dynamicBayesianNetwork);
//            bayesianNetworkSampler0.setSeed(random.nextInt());
//            DataStream<DynamicDataInstance> sample = bayesianNetworkSampler0.sampleToDataBase(1,sequenceLength);
//
//            //sample.getAttributes().getFullListOfAttributes().forEach(attribute -> System.out.println(attribute.getName()));
//            fullEvidence = sample.stream().collect(Collectors.toList());
//
////        System.out.println("Full Sample:");
////        fullEvidence.stream().forEachOrdered(dynass -> System.out.println(dynass.outputString(dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables())));
////        System.out.println();
//
//
//            // Evidence in t=0, 1, ..., nTimeSteps-2 for variables in 'varsEvidence'
//            IntStream.range(0,sequenceLength-1).forEachOrdered(t -> {
//                HashMapDynamicAssignment dynamicAssignment = new HashMapDynamicAssignment(varsEvidence.size());
//                dynamicAssignment.setSequenceID(0);
//                dynamicAssignment.setTimeID(t);
//                varsDynamicModel.stream().filter(variable -> varsEvidence.contains(variable)).forEach( variable -> dynamicAssignment.setValue(variable,fullEvidence.get(t).getValue(variable)));
//                evidence.add(dynamicAssignment);
//            });
//
//            // Evidence in t=(nTimeSteps-1) for all variables that are leaves in the DAG.
//            for (Variable currentVar : varsDynamicModel) {
//                if (!varsDynamicModel.stream().anyMatch(var -> dynamicBayesianNetwork.getDynamicDAG().getParentSetTimeT(var).getParents().contains(currentVar))) {
//                    varsEvidence.add(currentVar);
//                }
//            }
//            HashMapDynamicAssignment dynamicAssignment1 = new HashMapDynamicAssignment(varsEvidence.size());
//            dynamicAssignment1.setSequenceID(0);
//            dynamicAssignment1.setTimeID(sequenceLength-1);
//            varsDynamicModel.stream().filter(variable -> varsEvidence.contains(variable)).forEach( variable -> dynamicAssignment1.setValue(variable,fullEvidence.get(sequenceLength-1).getValue(variable)));
//            evidence.add(dynamicAssignment1);
//
////        System.out.println("Evidence:");
////        evidence.stream().forEachOrdered(dynass -> System.out.println(dynass.outputString(dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables())));
////        System.out.println();
//
//            StringBuilder classVarSequenceBuilder = new StringBuilder();
//            classVarSequenceBuilder.append("ClassVar: (");
//            fullEvidence.stream().forEachOrdered(dynass -> classVarSequenceBuilder.append( Integer.toString((int)dynass.getValue(mapVariable)) + ","));
//            classVarSequenceBuilder.replace(classVarSequenceBuilder.lastIndexOf(","),classVarSequenceBuilder.lastIndexOf(",")+1,"");
//            classVarSequenceBuilder.append(")");
//            System.out.println("Original sequence:");
//            System.out.println(classVarSequenceBuilder.toString());
//            System.out.println();
//
//
//            evidences.add(evidence);
//        }
//
//        return evidences.iterator();
//    }



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


        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(10);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);
        DynamicBayesianNetworkGenerator.setNumberOfLinks(5);
        int nStatesClassVar=2;

        DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(50), nStatesClassVar, true);
//
//        System.out.println("ORIGINAL DYNAMIC NETWORK:");
//
//        System.out.println(dynamicBayesianNetwork.toString());
//        System.out.println();


        dynMAP = new DynamicMAPInference();
        dynMAP.setModel(dynamicBayesianNetwork);

        // INITIALIZE THE MODEL
        int nTimeSteps = 5;
        dynMAP.setNumberOfTimeSteps(nTimeSteps);
        int nMergedClassVars = 3;
        dynMAP.setNumberOfMergedClassVars(nMergedClassVars);

        mapVariable = dynamicBayesianNetwork.getDynamicVariables().getVariableByName("ClassVar");
        dynMAP.setMAPvariable(mapVariable);

        System.out.println(dynamicBayesianNetwork.toString());
//        System.out.println("ORIGINAL COND DISTRIBUTIONS MAP VARIABLE:");
//        System.out.println(dynamicBayesianNetwork.getConditionalDistributionTime0(mapVariable).toString());
//        System.out.println(dynamicBayesianNetwork.getConditionalDistributionTimeT(mapVariable).toString());
//        System.out.println();

        dynMAP.computeMergedClassVarModels();

//
//        /*
//         * GENERATE AN EVIDENCE FOR T=0,...,nTimeSteps-1
//         */
//        List<Variable> varsDynamicModel = dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();
//
//        varsDynamicModel.forEach(var -> System.out.println("Var ID " + var.getVarID() + ": " + var.getName()));
//        int indexVarEvidence1 = 2;
//        int indexVarEvidence2 = 3;
//        int indexVarEvidence3 = 8;
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
//        Random random = new Random(931234662);
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
//                dynAssignment.setValue(varEvidence, varEvidenceValue);
//            }
//            evidence.add(dynAssignment);
//        }
//
//        dynMAP.setEvidence(evidence);

        dynMAP.runInference();

//
//        System.out.println("\nMAP sequence: " + Arrays.toString(dynMAP.getMAPsequence()) + " with probability " + dynMAP.getMAPestimateProbability());
//
////        Iterator<List<DynamicAssignment>> iterator = DynamicMAPInference.generateEvidence(dynamicBayesianNetwork,mapVariable.getName(),5,nTimeSteps,0.50,3634);
////        while(iterator.hasNext()) {
////            List<DynamicAssignment> evidence1 = iterator.next();
////            evidence1.forEach(assign -> System.out.println(assign.outputString()));
////        }


//        int nStates = 3;
//        int nRepets = 5;
//
//        for (int m = 0; m < Math.pow(nStates,nRepets); m++) {
//            String m_base_nStates = Integer.toString(Integer.parseInt(Integer.toString(m), 10), nStates);
//            m_base_nStates = StringUtils.leftPad(m_base_nStates, nRepets, '0');
//            System.out.println(m_base_nStates);
//            //System.out.println(String.format("%0" + nRepets + "d",m));
//        }

    }


}