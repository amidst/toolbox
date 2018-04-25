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

package eu.amidst.flinklink.core.learning.dynamic;

import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.utils.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.data.DataFlinkConverter;
import eu.amidst.flinklink.core.learning.parametric.utils.IdenitifableModelling;
import eu.amidst.flinklink.core.learning.parametric.utils.ParameterIdentifiableModel;
import eu.amidst.flinklink.core.utils.Batch;
import eu.amidst.flinklink.core.utils.ConversionToBatches;
import org.apache.flink.api.common.aggregators.DoubleSumAggregator;
import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.functions.RichMapPartitionFunction;
import org.apache.flink.api.common.operators.base.JoinOperatorBase;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.operators.IterativeDataSet;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 21/09/15.
 */
public class DynamicParallelVB implements ParameterLearningAlgorithm, Serializable{

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    static String LATENT_VARIABLE_NAMES = "LATENT_VARIABLE_NAMES";
    static String LATENT_INTERFACE_VARIABLE_NAMES = "LATENT_INTERFACE_VARIABLE_NAMES";



    DynamicDAG dynamicDAG;
    DAG dagTimeT;
    DAG dagTime0;
    int seed = 5;
    int batchSize;
    boolean output;
    protected int maximumGlobalIterations = 10;
    protected int maximumLocalIterations = 100;

    protected double globalThreshold = 0.0001;
    protected double localThreshold = 0.0001;

    protected eu.amidst.flinklink.core.learning.parametric.ParallelVB parallelVBTime0;
    protected SVB svbTimeT;

    transient DataSet<DataPosteriorAssignment> dataPosteriorDataSet;

    PlateuStructure plateuStructure = new PlateuIIDReplication();
    TransitionMethod transitionMethod;

    protected List<String> latentVariablesNames;
    protected List<String> latentInterfaceVariablesNames;
    protected List<String> noLatentVariablesNames;

    IdenitifableModelling idenitifableModelling = new ParameterIdentifiableModel();

    boolean randomStart = true;


    public void setIdenitifableModelling(IdenitifableModelling idenitifableModelling) {
        this.idenitifableModelling = idenitifableModelling;
    }

    public int getMaximumLocalIterations() {
        return maximumLocalIterations;
    }

    public void setMaximumLocalIterations(int maximumLocalIterations) {
        this.maximumLocalIterations = maximumLocalIterations;
    }

    public double getLocalThreshold() {
        return localThreshold;
    }

    public void setLocalThreshold(double localThreshold) {
        this.localThreshold = localThreshold;
    }

    public void setPlateuStructure(PlateuStructure plateuStructure){
        this.plateuStructure = plateuStructure;
    }

    public PlateuStructure getPlateuStructure() {
        return plateuStructure;
    }

    public void setTransitionMethod(TransitionMethod transitionMethod) {
        this.transitionMethod = transitionMethod;
    }

    public TransitionMethod getTransitionMethod() {
        return transitionMethod;
    }

    @Override
    public void updateModelWithNewTimeSlice(int timeSlice, DataFlink<DynamicDataInstance> data) {
        if (timeSlice==0)
            this.updateTime0(data);
        else
            this.updateTimeT(data);
    }

    private void updateTime0(DataFlink<DynamicDataInstance> data){
        DataFlink<DataInstance> newdata = DataFlinkConverter.convertToStatic(data);
        this.parallelVBTime0.updateModel(newdata);
        List<Variable> vars = this.latentVariablesNames
                                        .stream()
                                        .map(name -> this.dagTime0.getVariables().getVariableByName(name))
                                        .collect(Collectors.toList());

        this.dataPosteriorDataSet = this.parallelVBTime0.computePosteriorAssignment(newdata, vars);

    }

    public DataSet<DataPosteriorAssignment> getDataPosteriorDataSet() {
        return dataPosteriorDataSet;
    }

    private void updateTimeT(DataFlink<DynamicDataInstance> data){
        try{

            /********************************  JOIN DATA ************************************/
            DataSet<DataPosteriorAssignment> dataPosteriorInstanceDataSet = this.joinData2(data.getDataSet());
            /**************************************************************************/

            /********************************  ITERATIVE VMP ************************************/
            CompoundVector parameterPrior = this.svbTimeT.getNaturalParameterPrior();

            DataSet<CompoundVector> paramSet = data.getDataSet().getExecutionEnvironment().fromElements(parameterPrior);


            // set number of bulk iterations for KMeans algorithm
            IterativeDataSet<CompoundVector> loop = paramSet.iterate(maximumGlobalIterations)
                    .registerAggregationConvergenceCriterion("ELBO_" + this.dagTimeT.getName(), new DoubleSumAggregator(),
                            new eu.amidst.flinklink.core.learning.parametric.ParallelVB.ConvergenceELBO(
                                    this.globalThreshold, System.nanoTime()));

            Configuration config = new Configuration();
            config.setString(eu.amidst.flinklink.core.learning.parametric.ParameterLearningAlgorithm.BN_NAME, this.dagTimeT.getName());
            config.setBytes(eu.amidst.flinklink.core.learning.parametric.ParallelVB.SVB, Serialization.serializeObject(svbTimeT));
            config.setBytes(LATENT_INTERFACE_VARIABLE_NAMES, Serialization.serializeObject(this.latentInterfaceVariablesNames));

            Batch<DataPosteriorAssignment> emtpyBatch = new Batch(Double.NaN, new ArrayList<DataPosteriorAssignment>());
            DataSet<Batch<DataPosteriorAssignment>> unionData =
                    ConversionToBatches.toBatches(dataPosteriorInstanceDataSet, this.batchSize)
                            .union(data.getDataSet().getExecutionEnvironment().fromCollection(Arrays.asList(emtpyBatch),
                                    TypeExtractor.getForClass((Class<Batch<DataPosteriorAssignment>>) Class.forName("eu.amidst.flinklink.core.utils.Batch"))));


            DataSet<CompoundVector> newparamSet = unionData
                    .map(new DynamicParallelVB.ParallelVBMap(data.getAttributes(), this.dagTimeT.getVariables().getListOfVariables(),randomStart, idenitifableModelling))
                    .withParameters(config)
                    .withBroadcastSet(loop, "VB_PARAMS_" + this.dagTimeT.getName())
                    .reduce(new eu.amidst.flinklink.core.learning.parametric.ParallelVB.ParallelVBReduce());

            // feed new centroids back into next iteration
            DataSet<CompoundVector> finlparamSet = loop.closeWith(newparamSet);

            parameterPrior = finlparamSet.collect().get(0);

            this.svbTimeT.updateNaturalParameterPosteriors(parameterPrior);

            this.svbTimeT.updateNaturalParameterPrior(parameterPrior);

            this.svbTimeT.applyTransition();
            /**************************************************************************/

            /******************************* UPDATE DATA POSTERIORS********************/
            config = new Configuration();
            config.setString(eu.amidst.flinklink.core.learning.parametric.ParameterLearningAlgorithm.BN_NAME, this.dagTimeT.getName());
            config.setBytes(eu.amidst.flinklink.core.learning.parametric.ParallelVB.SVB, Serialization.serializeObject(svbTimeT));
            config.setBytes(LATENT_VARIABLE_NAMES, Serialization.serializeObject(this.latentVariablesNames));
            config.setBytes(LATENT_INTERFACE_VARIABLE_NAMES, Serialization.serializeObject(this.latentInterfaceVariablesNames));


            this.dataPosteriorDataSet = ConversionToBatches.toBatches(dataPosteriorInstanceDataSet, this.batchSize)
                                        .flatMap(new CajaMarLearnMapInferenceAssignment(data.getAttributes(), this.dagTimeT.getVariables().getListOfVariables()))
                                        .withParameters(config);
            /**************************************************************************/

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

    }
    //First we translate evidence for non-interface variables to interface variables
    protected DataSet<DataPosteriorAssignment> translate(DataSet<DataPosteriorInstance> data) {
        return data.mapPartition(new ParallelVBTranslate(this.dagTimeT, this.latentVariablesNames, this.latentInterfaceVariablesNames,this.noLatentVariablesNames));
    }

    protected DataSet<DataPosteriorAssignment> joinData(DataSet<DynamicDataInstance> data){
        //TODO: Define which is the best join strategy!!!!
        DataSet<DataPosteriorInstance>  dataJoined = data.join(dataPosteriorDataSet, JoinOperatorBase.JoinHint.REPARTITION_HASH_FIRST)
                .where(new KeySelector<DynamicDataInstance, Long>() {
                    @Override
                    public Long getKey(DynamicDataInstance value) throws Exception {
                        return value.getSequenceID();
                    }
                }).equalTo(new KeySelector<DataPosteriorAssignment, Long>() {
                    @Override
                    public Long getKey(DataPosteriorAssignment value) throws Exception {
                        return value.getPosterior().getId();
                    }
                }).with(new JoinFunction<DynamicDataInstance, DataPosteriorAssignment, DataPosteriorInstance>() {
                    @Override
                    public DataPosteriorInstance join(DynamicDataInstance dynamicDataInstance, DataPosteriorAssignment dataPosterior) throws Exception {
                        return new DataPosteriorInstance(dataPosterior, dynamicDataInstance);
                    }
                });

        return this.translate(dataJoined);
    }

    protected DataSet<DataPosteriorAssignment> joinData2(DataSet<DynamicDataInstance> data){
        //TODO: Define which is the best join strategy!!!!
        DataSet<DataPosteriorInstance>  dataJoined = dataPosteriorDataSet.join(data, JoinOperatorBase.JoinHint.REPARTITION_SORT_MERGE)
                .where(new KeySelector<DataPosteriorAssignment, Long>() {
                    @Override
                    public Long getKey(DataPosteriorAssignment value) throws Exception {
                        return value.getPosterior().getId();
                    }
                }).equalTo(new KeySelector<DynamicDataInstance, Long>() {
                    @Override
                    public Long getKey(DynamicDataInstance value) throws Exception {
                        return value.getSequenceID();
                    }
                }).with(new JoinFunction<DataPosteriorAssignment,DynamicDataInstance, DataPosteriorInstance>() {
                    @Override
                    public DataPosteriorInstance join(DataPosteriorAssignment dataPosterior, DynamicDataInstance dynamicDataInstance) throws Exception {
                        return new DataPosteriorInstance(dataPosterior, dynamicDataInstance);
                    }
                });

        return this.translate(dataJoined);
    }

    public void setGlobalThreshold(double globalThreshold) {
        this.globalThreshold = globalThreshold;
    }

    public void setMaximumGlobalIterations(int maximumGlobalIterations) {
        this.maximumGlobalIterations = maximumGlobalIterations;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }


    public <E extends UnivariateDistribution> E getParameterPosteriorTime0(Variable parameter) {
        if (parameter.isParameterVariable()) {
            Variable newVar =this.parallelVBTime0.getSVB().getPlateuStructure().getEFLearningBN().getParametersVariables().getVariableByName(parameter.getName());
            return this.parallelVBTime0.getParameterPosterior(newVar);
        }else {
            Variable newVar =this.dagTime0.getVariables().getVariableByName(parameter.getName());
            return this.parallelVBTime0.getParameterPosterior(newVar);
        }
    }

    public <E extends UnivariateDistribution> E getParameterPosteriorTimeT(Variable parameter) {
        if (parameter.isParameterVariable()) {
            Variable newVar =this.svbTimeT.getPlateuStructure().getEFLearningBN().getParametersVariables().getVariableByName(parameter.getName());
            return this.svbTimeT.getParameterPosterior(newVar);
        }else {
            Variable newVar =this.dagTimeT.getVariables().getVariableByName(parameter.getName());
            return this.svbTimeT.getParameterPosterior(newVar);
        }
    }

    @Override
    public void initLearning() {


        this.noLatentVariablesNames =this.dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(p -> p.getMainVar().isObservable())
                .map(p -> p.getMainVar().getName())
                .collect(Collectors.toList());

        this.latentVariablesNames = this.dynamicDAG.getParentSetsTimeT()
                .stream()
                .flatMap(p -> p.getParents().stream())
                .filter(v -> v.isInterfaceVariable())
                .map( v -> this.dynamicDAG.getDynamicVariables().getVariableFromInterface(v))
                .map(v -> v.getName())
                .collect(Collectors.toList());

        this.latentInterfaceVariablesNames = this.dynamicDAG.getParentSetsTimeT()
                .stream()
                .flatMap(p -> p.getParents().stream())
                .filter(v -> v.isInterfaceVariable())
                .map(v -> v.getName())
                .collect(Collectors.toList());


        this.parallelVBTime0 = new eu.amidst.flinklink.core.learning.parametric.ParallelVB();
        this.parallelVBTime0.setPlateuStructure(Serialization.deepCopy(plateuStructure));
        if (transitionMethod!=null)
            this.parallelVBTime0.setTransitionMethod(Serialization.deepCopy(transitionMethod));
        this.parallelVBTime0.setLocalThreshold(this.localThreshold);
        this.parallelVBTime0.setMaximumLocalIterations(this.maximumLocalIterations);
        this.parallelVBTime0.setBatchSize(this.batchSize);
        this.parallelVBTime0.setGlobalThreshold(this.globalThreshold);
        this.parallelVBTime0.setMaximumGlobalIterations(this.maximumGlobalIterations);
        this.parallelVBTime0.setOutput(this.output);
        this.parallelVBTime0.setSeed(this.seed);
        this.parallelVBTime0.setDAG(this.dagTime0);
        this.parallelVBTime0.setIdenitifableModelling(this.idenitifableModelling);
        this.parallelVBTime0.initLearning();

        this.svbTimeT = new SVB();
        this.svbTimeT.setPlateuStructure(Serialization.deepCopy(plateuStructure));
        if (transitionMethod!=null)
            this.svbTimeT.setTransitionMethod(Serialization.deepCopy(transitionMethod));
        this.svbTimeT.getPlateuStructure().getVMP().setMaxIter(this.maximumLocalIterations);
        this.svbTimeT.getPlateuStructure().getVMP().setThreshold(this.localThreshold);
        this.svbTimeT.setWindowsSize(this.batchSize);
        this.svbTimeT.setOutput(this.output);
        this.svbTimeT.setSeed(this.seed);
        this.svbTimeT.setDAG(this.dagTimeT);
        this.svbTimeT.initLearning();
    }

    @Override
    public DataSet<DataPosterior> computePosterior(List<Variable> latentVariables) {
        return null;
    }

    @Override
    public void setDAG(DynamicDAG dag) {
        this.dynamicDAG = dag;
        this.dagTime0=this.dynamicDAG.toDAGTime0();
        this.dagTimeT=this.dynamicDAG.toDAGTimeT();


    }

    @Override
    public void setSeed(int seed) {
        this.seed=seed;
    }

    @Override
    public DynamicBayesianNetwork getLearntDynamicBayesianNetwork() {

        BayesianNetwork bnTime0 = this.parallelVBTime0.getLearntBayesianNetwork();
        BayesianNetwork bnTimeT = this.svbTimeT.getLearntBayesianNetwork();
        DynamicVariables dynamicVariables = this.dynamicDAG.getDynamicVariables();

        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(this.dynamicDAG);

        for (Variable dynamicVariable : dynamicVariables) {
            ConditionalDistribution dynamicDist = dbn.getConditionalDistributionTime0(dynamicVariable);
            ConditionalDistribution staticDist = bnTime0.getConditionalDistribution(bnTime0.getVariables().getVariableByName(dynamicVariable.getName()));

            staticDist.setVar(dynamicVariable);
            staticDist.setConditioningVariables(dynamicDist.getConditioningVariables());

            dbn.setConditionalDistributionTime0(dynamicVariable,staticDist);
        }

        for (Variable dynamicVariable : dynamicVariables) {
            ConditionalDistribution dynamicDist = dbn.getConditionalDistributionTimeT(dynamicVariable);
            ConditionalDistribution staticDist = bnTimeT.getConditionalDistribution(bnTimeT.getVariables().getVariableByName(dynamicVariable.getName()));

            staticDist.setVar(dynamicVariable);
            staticDist.setConditioningVariables(dynamicDist.getConditioningVariables());

            dbn.setConditionalDistributionTimeT(dynamicVariable,staticDist);
        }


        return dbn;
    }

    @Override
    public void setOutput(boolean activateOutput) {
        this.output=activateOutput;
    }


    public static class DataPosteriorInstance {
        final DataPosteriorAssignment dataPosterior;
        final DynamicDataInstance dataInstance;

        public DataPosteriorInstance(DataPosteriorAssignment dataPosterior, DynamicDataInstance dataInstance) {
            this.dataPosterior = dataPosterior;
            this.dataInstance = dataInstance;
        }

        public DataPosteriorAssignment getDataPosterior() {
            return dataPosterior;
        }

        public DynamicDataInstance getDataInstance() {
            return dataInstance;
        }
    }

    public static Assignment filterAssignment(Assignment assignment, List<Variable> variables, List<Variable> variablesInterface){
        HashMapAssignment assignmentnew = new HashMapAssignment();
        for (int i = 0; i < variables.size(); i++) {
            assignmentnew.setValue(variablesInterface.get(i), assignment.getValue(variables.get(i)));
        }
        return assignmentnew;
    }

    public static class CajaMarLearnMapInferenceAssignment extends RichFlatMapFunction<Batch<DataPosteriorAssignment>, DataPosteriorAssignment> {

        List<Variable> latentVariables;
        List<Variable> latentInterfaceVariables;

        SVB svb;

        Attributes attributes;
        List<Variable> variables;

        public CajaMarLearnMapInferenceAssignment(Attributes attributes, List<Variable> variables) {
            this.attributes = attributes;
            this.variables = variables;
        }

        @Override
        public void flatMap(Batch<DataPosteriorAssignment> data, Collector<DataPosteriorAssignment> out) {

            for (int i = 0; i < data.getElements().size(); i++) {
                for (Variable latentVariable : latentInterfaceVariables) {
                    DataPosteriorAssignment dataPosteriorAssignment = data.getElements().get(i);
                    if (!dataPosteriorAssignment.isObserved(latentVariable)){
                        UnivariateDistribution dist = dataPosteriorAssignment.getPosterior().getPosterior(latentVariable);
                        Variable interfaceVariable = this.svb.getDAG().getVariables().getVariableByName(latentVariable.getName() + DynamicVariables.INTERFACE_SUFFIX);
                        this.svb.getPlateuStructure().getNodeOfVar(latentVariable, i).setPDist(dist.toEFUnivariateDistribution().deepCopy(interfaceVariable));
                        this.svb.getPlateuStructure().getNodeOfVar(latentVariable, i).setAssignment(null);
                    }
                }
            }

            DataOnMemory<DataInstance> dataBatch = new DataOnMemoryListContainer<DataInstance>(
                    attributes,
                    data.getElements().stream()
                            .map(d ->
                                    new DataInstanceFromAssignment(d.getPosterior().getId(), d.getAssignment(), attributes, variables))
                            .collect(Collectors.toList())
            );

            List<DataPosteriorAssignment> posteriorAssignments = svb.computePosteriorAssignment(dataBatch, latentVariables);
            for (DataPosteriorAssignment posterior: posteriorAssignments){
                out.collect(posterior);
            }
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            svb = Serialization.deserializeObject(parameters.getBytes(eu.amidst.flinklink.core.learning.parametric.ParallelVB.SVB, null));
            svb.initLearning();
            List<String> variableNames = Serialization.deserializeObject(parameters.getBytes(LATENT_VARIABLE_NAMES, null));
            List<String> interfaceVariablenames = Serialization.deserializeObject(parameters.getBytes(LATENT_INTERFACE_VARIABLE_NAMES, null));

            latentVariables = variableNames.stream().map(name -> svb.getDAG().getVariables().getVariableByName(name)).collect(Collectors.toList());
            latentInterfaceVariables = interfaceVariablenames.stream().map(name -> svb.getDAG().getVariables().getVariableByName(name)).collect(Collectors.toList());

        }
    }

    public static class DataInstanceFromAssignment implements DataInstance, Serializable {

        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = -3436599636425587512L;

        long seq_Id;
        Assignment assignment;
        Attributes attributes;
        List<Variable> variables;

        public DataInstanceFromAssignment(long seq_Id, Assignment assignment1, Attributes atts, List<Variable> variables){
            this.seq_Id = seq_Id;
            this.assignment=assignment1;
            this.attributes = atts;
            this.variables = variables;
        }

        @Override
        public double getValue(Variable var) {
            return this.assignment.getValue(var);
        }

        @Override
        public void setValue(Variable var, double value) {
            this.assignment.setValue(var, value);
        }

        @Override
        public Attributes getAttributes() {
            return attributes;
        }

        @Override
        public Set<Variable> getVariables(){
            return assignment.getVariables();
        }

        @Override
        public double getValue(Attribute att) {
            return seq_Id;
        }

        @Override
        public void setValue(Attribute att, double value) {
        }

        @Override
        public double[] toArray() {
            throw new UnsupportedOperationException("Operation not supported for an Assignment object");
        }

        @Override
        public String toString(){
            return this.outputString();
        }
    }

    public static class ParallelVBTranslate extends RichMapPartitionFunction<DataPosteriorInstance, DataPosteriorAssignment> {

        List<Variable> latentVariables;
        List<Variable> latentInterfaceVariables;
        List<Variable> allVariables;


        public ParallelVBTranslate(DAG dag, List<String> latentNames, List<String> latentInterfaceNames, List<String> noLatentVariablesName) {
            latentVariables = latentNames.stream().map(name -> dag.getVariables().getVariableByName(name)).collect(Collectors.toList());
            latentInterfaceVariables = latentInterfaceNames.stream().map(name -> dag.getVariables().getVariableByName(name)).collect(Collectors.toList());
            allVariables = noLatentVariablesName.stream().map(name -> dag.getVariables().getVariableByName(name)).collect(Collectors.toList());

        }

        @Override
        public void mapPartition(Iterable<DataPosteriorInstance> values, Collector<DataPosteriorAssignment> out) throws Exception {


            for (DataPosteriorInstance value : values) {
                HashMapAssignment assignment = new HashMapAssignment();

                for (Variable variable : allVariables) {
                    assignment.setValue(variable, value.getDataInstance().getValue(variable));
                }

                for (int i = 0; i < this.latentVariables.size(); i++) {
                    Variable staticVar = this.latentVariables.get(i);
                    Variable interfaceVar = this.latentInterfaceVariables.get(i);
                    if (value.getDataPosterior().isObserved(staticVar))
                        assignment.setValue(interfaceVar, value.getDataPosterior().getAssignment().getValue(staticVar));
                }
                out.collect(new DataPosteriorAssignment(value.getDataPosterior().getPosterior(), assignment));
            }
        }
    }
    private static class AssignmentParallelVBImpl implements Assignment{

        Assignment oldassingment;
        HashMap<Variable, Variable> hashMap;

        public AssignmentParallelVBImpl(Assignment oldassingment, HashMap<Variable, Variable> hashMap) {
            this.oldassingment = oldassingment;
            this.hashMap = hashMap;
        }

        @Override
        public double getValue(Variable var) {
            return this.oldassingment.getValue(this.hashMap.get(var));
        }

        @Override
        public void setValue(Variable var, double value) {
            this.setValue(this.hashMap.get(var),value);
        }

        @Override
        public Set<Variable> getVariables() {
            return null;
        }

    }
    public static class ParallelVBMap extends RichMapFunction<Batch<DataPosteriorAssignment>, CompoundVector> {
        double basedELBO = 0;
        DoubleSumAggregator elbo;
        SVB svb;

        List<Variable> latentInterfaceVariables;
        Attributes attributes;
        List<Variable> variables;



        CompoundVector prior;

        CompoundVector initialPosterior;

        CompoundVector updatedPrior;

        Map<Double,CompoundVector> partialVectors;

        IdenitifableModelling idenitifableModelling;

        boolean randomStart;


        public ParallelVBMap(Attributes attributes, List<Variable> variables, boolean randomStart, IdenitifableModelling idenitifableModelling) {
            this.attributes = attributes;
            this.variables = variables;
            this.randomStart = randomStart;
            this.idenitifableModelling = idenitifableModelling;
        }

        @Override
        public CompoundVector map(Batch<DataPosteriorAssignment> data) throws Exception {

            int superstep = getIterationRuntimeContext().getSuperstepNumber() - 1;

            double batchID = data.getBatchID();

            if (data.getElements().size()==0) {
                elbo.aggregate(basedELBO);
                return prior;
            }else{

                //Prepare the data
                for (int i = 0; i < data.getElements().size(); i++) {
                    for (Variable latentVariable : latentInterfaceVariables) {
                        DataPosteriorAssignment dataPosteriorAssignment = data.getElements().get(i);
                        if (!dataPosteriorAssignment.isObserved(latentVariable)) {
                            UnivariateDistribution dist = dataPosteriorAssignment.getPosterior().getPosterior(latentVariable);
                            Variable interfaceVariable = this.svb.getDAG().getVariables().getVariableByName(latentVariable.getName() + DynamicVariables.INTERFACE_SUFFIX);
                            this.svb.getPlateuStructure().getNodeOfVar(latentVariable, i).setPDist(dist.toEFUnivariateDistribution().deepCopy(interfaceVariable));
                            this.svb.getPlateuStructure().getNodeOfVar(latentVariable, i).setAssignment(null);
                        }
                    }
                }

                DataOnMemory<DataInstance> dataBatch = new DataOnMemoryListContainer<DataInstance>(
                        attributes,
                        data.getElements().stream()
                                .map(d ->
                                        new DataInstanceFromAssignment(d.getPosterior().getId(), d.getAssignment(), attributes, variables))
                                .collect(Collectors.toList())
                );
                ///////////////////




                //Compute ELBO
                this.svb.updateNaturalParameterPrior(updatedPrior);
                this.svb.updateNaturalParameterPosteriors(updatedPrior);
                svb.getPlateuStructure().getNonReplictedNodes().forEach(node -> node.setActive(false));
                SVB.BatchOutput outElbo = svb.updateModelOnBatchParallel(dataBatch);


                if (Double.isNaN(outElbo.getElbo()))
                    throw new IllegalStateException("NaN elbo");

                elbo.aggregate(outElbo.getElbo());


                //elbo.aggregate(svb.getPlateuStructure().getReplicatedNodes().filter(node-> node.isActive()).mapToDouble(node -> svb.getPlateuStructure().getVMP().computeELBO(node)).sum());


                //Set Active Parameters
                svb.getPlateuStructure()
                        .getNonReplictedNodes()
                        .filter(node -> this.idenitifableModelling.isActiveAtEpoch(node.getMainVariable(), superstep))
                        .forEach(node -> node.setActive(true));


                if (partialVectors.containsKey(batchID)) {
                    CompoundVector newVector = Serialization.deepCopy(updatedPrior);
                    newVector.substract(partialVectors.get(batchID));
                    this.svb.updateNaturalParameterPrior(newVector);
                    this.svb.updateNaturalParameterPosteriors(updatedPrior);
                }else {
                    this.svb.updateNaturalParameterPrior(this.prior);
                    this.svb.updateNaturalParameterPosteriors(this.initialPosterior);
                }

                //System.out.println("PRIOR");
                //System.out.println(svb.getLearntBayesianNetwork());

                SVB.BatchOutput out = svb.updateModelOnBatchParallel(dataBatch);



                partialVectors.put(batchID,out.getVector());

                //this.svb.updateNaturalParameterPrior(this.svb.getPlateuStructure().getPlateauNaturalParameterPosterior());
                //System.out.println("POSTERIOR");
                //System.out.println(svb.getLearntBayesianNetwork());

                //System.out.println(out.getVector().getVectorByPosition(0).get(0));

                svb.getPlateuStructure().getNonReplictedNodes().forEach(node -> node.setActive(true));


                return out.getVector();
            }
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            String bnName = parameters.getString(eu.amidst.flinklink.core.learning.parametric.ParallelVB.BN_NAME, "");
            svb = Serialization.deserializeObject(parameters.getBytes(eu.amidst.flinklink.core.learning.parametric.ParallelVB.SVB, null));
            svb.initLearning();
            Collection<CompoundVector> collection = getRuntimeContext().getBroadcastVariable("VB_PARAMS_" + bnName);
            updatedPrior = collection.iterator().next();

            if (prior!=null) {
                svb.updateNaturalParameterPrior(prior);
                svb.updateNaturalParameterPosteriors(updatedPrior);
                basedELBO = svb.getPlateuStructure().getNonReplictedNodes().mapToDouble(node -> svb.getPlateuStructure().getVMP().computeELBO(node)).sum();
                //svb.initLearning();
                //System.out.println("BaseELBO:"+ basedELBO);
            }else{
                this.prior=Serialization.deepCopy(updatedPrior);
                this.svb.updateNaturalParameterPrior(prior);
                if (randomStart) {
                    this.svb.getPlateuStructure().setSeed(this.svb.getSeed());
                    this.svb.getPlateuStructure().resetQs();
                    initialPosterior = Serialization.deepCopy(this.svb.getPlateuStructure().getPlateauNaturalParameterPosterior());
                    initialPosterior.sum(prior);
                }else{
                    initialPosterior=Serialization.deepCopy(svb.getNaturalParameterPrior());
                }

                this.svb.updateNaturalParameterPosteriors(initialPosterior);

                basedELBO = svb.getPlateuStructure().getNonReplictedNodes().mapToDouble(node -> svb.getPlateuStructure().getVMP().computeELBO(node)).sum();

            }

            svb.updateNaturalParameterPrior(updatedPrior);

            List<String> names = Serialization.deserializeObject(parameters.getBytes(LATENT_INTERFACE_VARIABLE_NAMES, null));
            latentInterfaceVariables = names.stream().map(name -> svb.getDAG().getVariables().getVariableByName(name)).collect(Collectors.toList());

            elbo = getIterationRuntimeContext().getIterationAggregator("ELBO_" + bnName);

            if (partialVectors==null){
                partialVectors = new HashMap();
            }

        }
    }





}
