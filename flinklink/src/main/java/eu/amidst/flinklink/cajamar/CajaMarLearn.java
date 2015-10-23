/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.flinklink.cajamar;

import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.learning.parametric.bayesian.DataPosterior;
import eu.amidst.core.learning.parametric.bayesian.DataPosteriorAssignment;
import eu.amidst.core.learning.parametric.bayesian.SVB;
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
import eu.amidst.flinklink.core.learning.parametric.ParallelVB;
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
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 21/09/15.
 */
public class CajaMarLearn implements ParameterLearningAlgorithm, Serializable{

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    static String LATENT_VARIABLE_NAMES = "LATENT_VARIABLE_NAMES";
    static String LATENT_INTERFACE_VARIABLE_NAMES = "LATENT_INTERFACE_VARIABLE_NAMES";



    DynamicDAG dynamicDAG;
    DAG dagTimeT;
    DAG dagTime0;
    int seed;
    int batchSize;
    boolean output;
    protected int maximumGlobalIterations = 10;

    protected double globalThreshold = 0.0001;

    protected ParallelVB parallelVBTime0;
    protected SVB svbTimeT;

    transient DataSet<DataPosteriorAssignment> dataPosteriorDataSet;


    protected List<String> latentVariablesNames;
    protected List<String> latentInterfaceVariablesNames;
    protected List<String> noLatentVariablesNames;

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
        this.parallelVBTime0.setDataFlink(newdata);
        List<Variable> vars = this.latentVariablesNames
                                        .stream()
                                        .map(name -> this.dagTime0.getVariables().getVariableByName(name))
                                        .collect(Collectors.toList());

        this.dataPosteriorDataSet = this.parallelVBTime0.computePosteriorAssignment(vars);
    }

    public DataSet<DataPosteriorAssignment> getDataPosteriorDataSet() {
        return dataPosteriorDataSet;
    }

    private void updateTimeT(DataFlink<DynamicDataInstance> data){
        try{

            //System.out.println("DATA SET: " + data.getDataSet().count());
            //System.out.println("DATA POSTERIOR: " + dataPosteriorDataSet.count());

            /********************************  JOIN DATA ************************************/
            DataSet<DataPosteriorAssignment> dataPosteriorInstanceDataSet = this.joinData(data.getDataSet());
            /**************************************************************************/

            //System.out.println("DATA INSTANCE: " + dataPosteriorInstanceDataSet.count());

            /********************************  ITERATIVE VMP ************************************/
            CompoundVector parameterPrior = this.svbTimeT.getNaturalParameterPrior();
            CompoundVector zeroedVector = CompoundVector.newZeroedVector(parameterPrior);

            DataSet<CompoundVector> paramSet = data.getDataSet().getExecutionEnvironment().fromElements(zeroedVector);


            // set number of bulk iterations for KMeans algorithm
            IterativeDataSet<CompoundVector> loop = paramSet.iterate(maximumGlobalIterations)
                    .registerAggregationConvergenceCriterion("ELBO_" + this.dagTimeT.getName(), new DoubleSumAggregator(), new ParallelVB.ConvergenceELBO(this.globalThreshold));

            Configuration config = new Configuration();
            config.setString(eu.amidst.flinklink.core.learning.parametric.ParameterLearningAlgorithm.BN_NAME, this.dagTimeT.getName());
            config.setBytes(ParallelVB.SVB, Serialization.serializeObject(svbTimeT));
            config.setBytes(ParallelVB.PRIOR, Serialization.serializeObject(parameterPrior));
            config.setBytes(LATENT_INTERFACE_VARIABLE_NAMES, Serialization.serializeObject(this.latentInterfaceVariablesNames));

            DataSet<CompoundVector> newparamSet = ConversionToBatches.toBatches(dataPosteriorInstanceDataSet, this.batchSize)
                    .map(new CajaMarLearn.ParallelVBMap(data.getAttributes(), this.dagTimeT.getVariables().getListOfVariables()))
                    .withParameters(config)
                    .withBroadcastSet(loop, "VB_PARAMS_" + this.dagTimeT.getName())
                    .reduce(new ParallelVB.ParallelVBReduce());

            // feed new centroids back into next iteration
            DataSet<CompoundVector> finlparamSet = loop.closeWith(newparamSet);

            parameterPrior.sum(finlparamSet.collect().get(0));
            this.svbTimeT.updateNaturalParameterPrior(parameterPrior);
            /**************************************************************************/

            /******************************* UPDATE DATA POSTERIORS********************/

            config = new Configuration();
            config.setString(eu.amidst.flinklink.core.learning.parametric.ParameterLearningAlgorithm.BN_NAME, this.dagTimeT.getName());
            config.setBytes(ParallelVB.SVB, Serialization.serializeObject(svbTimeT));
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
        DataSet<DataPosteriorInstance>  dataJoined = data.join(dataPosteriorDataSet, JoinOperatorBase.JoinHint.REPARTITION_SORT_MERGE)
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

    public void setGlobalThreshold(double globalThreshold) {
        this.globalThreshold = globalThreshold;
    }

    public void setMaximumGlobalIterations(int maximumGlobalIterations) {
        this.maximumGlobalIterations = maximumGlobalIterations;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public void initLearning() {
        this.parallelVBTime0 = new ParallelVB();
        this.parallelVBTime0.setBatchSize(this.batchSize);
        this.parallelVBTime0.setGlobalThreshold(this.globalThreshold);
        this.parallelVBTime0.setMaximumGlobalIterations(this.maximumGlobalIterations);
        this.parallelVBTime0.setOutput(this.output);
        this.parallelVBTime0.setSeed(this.seed);
        this.parallelVBTime0.setDAG(this.dagTime0);
        this.parallelVBTime0.initLearning();

        this.svbTimeT = new SVB();
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

        this.noLatentVariablesNames =this.dynamicDAG.getParentSetsTimeT()
                .stream()
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

    public static class CajaMarLearnMapInferenceAssignment extends RichFlatMapFunction<List<DataPosteriorAssignment>, DataPosteriorAssignment> {

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
        public void flatMap(List<DataPosteriorAssignment> data, Collector<DataPosteriorAssignment> out) {

            for (int i = 0; i < data.size(); i++) {
                for (Variable latentVariable : latentInterfaceVariables) {
                    DataPosteriorAssignment dataPosteriorAssignment = data.get(i);
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
                    data.stream()
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
            svb = Serialization.deserializeObject(parameters.getBytes(ParallelVB.SVB, null));
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
    public static class ParallelVBMap extends RichMapFunction<List<DataPosteriorAssignment>, CompoundVector> {

        DoubleSumAggregator elbo;
        SVB svb;
        List<Variable> latentInterfaceVariables;
        Attributes attributes;
        List<Variable> variables;

        public ParallelVBMap(Attributes attributes, List<Variable> variables) {
            this.attributes = attributes;
            this.variables = variables;
        }

        @Override
        public CompoundVector map(List<DataPosteriorAssignment> data) throws Exception {


            for (int i = 0; i < data.size(); i++) {
                for (Variable latentVariable : latentInterfaceVariables) {
                    DataPosteriorAssignment dataPosteriorAssignment = data.get(i);
                    if (!dataPosteriorAssignment.isObserved(latentVariable)){
                        UnivariateDistribution dist = dataPosteriorAssignment.getPosterior().getPosterior(latentVariable);
                        Variable interfaceVariable = this.svb.getDAG().getVariables().getVariableByName(latentVariable.getName() + DynamicVariables.INTERFACE_SUFFIX);
                        this.svb.getPlateuStructure().getNodeOfVar(latentVariable, i).setPDist(dist.toEFUnivariateDistribution().deepCopy(interfaceVariable));
                        this.svb.getPlateuStructure().getNodeOfVar(latentVariable, i).setAssignment(null);
                    }
                }
            }

            data.get(0).getAssignment().getVariables();

            DataOnMemory<DataInstance> dataBatch = new DataOnMemoryListContainer<DataInstance>(
                    attributes,
                    data.stream()
                            .map(d ->
                            new DataInstanceFromAssignment(d.getPosterior().getId(), d.getAssignment(),attributes,variables))
                            .collect(Collectors.toList())
            );
            SVB.BatchOutput out = svb.updateModelOnBatchParallel(dataBatch);
            elbo.aggregate(out.getElbo());
            return out.getVector();
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            String bnName = parameters.getString(ParallelVB.BN_NAME, "");
            svb = Serialization.deserializeObject(parameters.getBytes(ParallelVB.SVB, null));
            CompoundVector parameterPrior = Serialization.deserializeObject(parameters.getBytes(ParallelVB.PRIOR, null));
            Collection<CompoundVector> collection = getRuntimeContext().getBroadcastVariable("VB_PARAMS_" + bnName);
            CompoundVector updatedPrior = collection.iterator().next();
            parameterPrior.sum(updatedPrior);
            svb.updateNaturalParameterPrior(parameterPrior);

            List<String> names = Serialization.deserializeObject(parameters.getBytes(LATENT_INTERFACE_VARIABLE_NAMES, null));
            latentInterfaceVariables = names.stream().map(name -> svb.getDAG().getVariables().getVariableByName(name)).collect(Collectors.toList());

            elbo = getIterationRuntimeContext().getIterationAggregator("ELBO_" + bnName);

        }
    }

}
