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

package eu.amidst.flinklink.core.utils;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.core.data.DataFlink;
import org.apache.flink.api.common.functions.RichMapPartitionFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 24/9/15.
 */
public class DBNSampler {


    Random random;
    DynamicBayesianNetwork dbn;
    int nSamples=1000;
    Attributes newAttributes;
    BayesianNetwork bnTime0;
    BayesianNetwork bnTimeT;
    int seed = 0;
    private int batchSize;

    /** Represents a {@code Map} containing the hidden variables. */
    private Map<Variable, Boolean> hiddenVars = new HashMap();

    /** Represents a {@code Map} containing the noisy variables. */
    private Map<Variable, Double> marNoise = new HashMap();


    public DBNSampler(DynamicBayesianNetwork dbn) {
        this.dbn = dbn;
        this.bnTime0 = dbn.toBayesianNetworkTime0();
        this.bnTimeT = dbn.toBayesianNetworkTimeT();
        this.random = new Random(seed);
    }

    public void setDBN(DynamicBayesianNetwork dbn) {
        this.dbn = dbn;
        this.bnTime0 = dbn.toBayesianNetworkTime0();
        this.bnTimeT = dbn.toBayesianNetworkTimeT();
    }

    /**
     * Sets a given {@link Variable} object as hidden.
     * @param var a given {@link Variable} object.
     */
    public void setHiddenVar(Variable var) {
        this.hiddenVars.put(var,true);
    }

    /**
     * Sets a given {@link Variable} object as noisy.
     * @param var a given {@link Variable} object.
     * @param noiseProb a double that represents the noise probability.
     */
    public void setMARVar(Variable var, double noiseProb){ this.marNoise.put(var,noiseProb);}

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setSeed(int seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    public void setNSamples(int nSamples) {
        this.nSamples = nSamples;
    }

    public DataFlink<DynamicDataInstance> cascadingSample(ExecutionEnvironment env, DataFlink<DynamicDataInstance> previousSample){
        if (previousSample==null){
            BayesianNetworkSampler sampler = new BayesianNetworkSampler(this.bnTime0);
            this.hiddenVars.keySet().stream().forEach(var -> sampler.setHiddenVar(bnTime0.getVariables().getVariableByName(var.getName())));
            this.marNoise.entrySet().stream().forEach(e -> sampler.setMARVar(bnTime0.getVariables().getVariableByName(e.getKey().getName()),e.getValue()));

            sampler.setSeed(this.seed);
            sampler.setBatchSize(this.batchSize);
            DataFlink<DataInstance> data= sampler.sampleToDataFlink(env, this.nSamples);

            Attribute attseq = new Attribute(data.getAttributes().getNumberOfAttributes(),Attributes.SEQUENCE_ID_ATT_NAME, new RealStateSpace());
            Attribute atttime = new Attribute(data.getAttributes().getNumberOfAttributes()+1,Attributes.TIME_ID_ATT_NAME, new RealStateSpace());

            List<Attribute> attributeList = data.getAttributes().getFullListOfAttributes();
            List<Attribute> att2 =attributeList.stream().map(at -> at).collect(Collectors.toList());
            att2.add(attseq);
            att2.add(atttime);
            newAttributes = new Attributes(att2);


            DataFlinkWrapper dataFlinkWrapper = new DataFlinkWrapper();
            dataFlinkWrapper.setName(this.dbn.getName());
            dataFlinkWrapper.setAttributes(newAttributes);
            dataFlinkWrapper.setData(data.getDataSet());
            dataFlinkWrapper.setnSamples(nSamples);
            dataFlinkWrapper.setTimeId(0);

            return dataFlinkWrapper;

        }else{

            DataSet<DynamicDataInstance> data = previousSample.getDataSet().mapPartition(new MAPDynamicInstancesSampler(this.bnTimeT, newAttributes, this.hiddenVars, this.marNoise, seed));

            return new DataFlink<DynamicDataInstance>() {

                @Override
                public String getName() {
                    return dbn.getName();
                }

                @Override
                public Attributes getAttributes() {
                    return newAttributes;
                }

                @Override
                public DataSet<DynamicDataInstance> getDataSet() {
                    return data;
                }
            };

        }
    }


    public DataFlink<DynamicDataInstance> cascadingSampleConceptDrift(ExecutionEnvironment env, DataFlink<DynamicDataInstance> previousSample, boolean drift){
        if (previousSample==null){
            BayesianNetworkSampler sampler = new BayesianNetworkSampler(this.bnTime0);
            this.hiddenVars.keySet().stream().forEach(var -> sampler.setHiddenVar(bnTime0.getVariables().getVariableByName(var.getName())));
            this.marNoise.entrySet().stream().forEach(e -> sampler.setMARVar(bnTime0.getVariables().getVariableByName(e.getKey().getName()),e.getValue()));

            sampler.setSeed(this.seed);
            sampler.setBatchSize(this.batchSize);
            DataFlink<DataInstance> data= sampler.sampleToDataFlink(env,this.nSamples);

            Attribute attseq = new Attribute(data.getAttributes().getNumberOfAttributes(),Attributes.SEQUENCE_ID_ATT_NAME, new RealStateSpace());
            Attribute atttime = new Attribute(data.getAttributes().getNumberOfAttributes()+1,Attributes.TIME_ID_ATT_NAME, new RealStateSpace());

            List<Attribute> attributeList = data.getAttributes().getFullListOfAttributes();
            List<Attribute> att2 =attributeList.stream().map(at -> at).collect(Collectors.toList());
            att2.add(attseq);
            att2.add(atttime);
            newAttributes = new Attributes(att2);


            DataFlinkWrapper dataFlinkWrapper = new DataFlinkWrapper();
            dataFlinkWrapper.setName(this.dbn.getName());
            dataFlinkWrapper.setAttributes(newAttributes);
            dataFlinkWrapper.setData(data.getDataSet());
            dataFlinkWrapper.setnSamples(nSamples);
            dataFlinkWrapper.setTimeId(0);

            return dataFlinkWrapper;

        }else{
            if (drift) {
                this.bnTimeT.randomInitialization(this.random);
            }

            DataSet<DynamicDataInstance> data = previousSample.getDataSet().mapPartition(new MAPDynamicInstancesSampler(this.bnTimeT, newAttributes, this.hiddenVars, this.marNoise, seed));

            return new DataFlink<DynamicDataInstance>() {

                @Override
                public String getName() {
                    return dbn.getName();
                }

                @Override
                public Attributes getAttributes() {
                    return newAttributes;
                }

                @Override
                public DataSet<DynamicDataInstance> getDataSet() {
                    return data;
                }
            };

        }
    }

    /**
     *
     */
    static class MAPDynamicInstancesSampler extends RichMapPartitionFunction<DynamicDataInstance,DynamicDataInstance>{

        BayesianNetwork bn;
        List<Variable> causalOrder;
        Random random;
        Attributes attributes;
        int seed;
        Map<Variable,Boolean> hiddenVars;
        Map<Variable,Double> marVars;


        public MAPDynamicInstancesSampler(BayesianNetwork bn, Attributes attributes, Map<Variable,Boolean> hiddenVars, Map<Variable,Double> marVars,  int seed) {
            this.bn = bn;
            this.seed = seed;
            this.attributes = attributes;
            this.hiddenVars = hiddenVars;
            this.marVars = marVars;
            this.causalOrder = Utils.getTopologicalOrder(this.bn.getDAG());
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            random = new Random(this.seed + this.getRuntimeContext().getIndexOfThisSubtask());
        }

        @Override
        public void mapPartition(Iterable<DynamicDataInstance> values, Collector<DynamicDataInstance> out) throws Exception {

            values.forEach(d -> {

                HashMapAssignment pastAssignment = new HashMapAssignment();
                for (Attribute att : d.getAttributes().getListOfNonSpecialAttributes()){
                    pastAssignment.setValue(bn.getVariables().getVariableByName(att.getName()+ DynamicVariables.INTERFACE_SUFFIX),d.getValue(att));
                }
                Assignment assignment = sample(bn, causalOrder, random, pastAssignment);
                hiddenVars.keySet().stream().forEach(var -> assignment.setValue(bn.getVariables().getVariableByName(var.getName()),Utils.missingValue()));
                marVars.entrySet().forEach(e -> {
                    if (random.nextDouble()<e.getValue())
                        assignment.setValue(bn.getVariables().getVariableByName(e.getKey().getName()),Utils.missingValue());
                });
                DataInstance dataInstanceNew = new DataStreamFromStreamOfAssignments.DataInstanceFromAssignment(assignment,attributes, bn.getVariables().getListOfVariables());
                DynamicDataInstanceWrapper wrapper = new DynamicDataInstanceWrapper(dataInstanceNew,attributes,d.getSequenceID(), d.getTimeID()+1);
                out.collect(wrapper);
            });
        }

    }

    /**
     * Samples an {@link Assignment} randomly from a {@link BayesianNetwork} conditioned to a given Assignment.
     * The assignment must only contains assignments from head nodes.
     * @param random, a random number generator
     * @param conditionAssignment, an {@link Assignment} object.
     * @return A sampled Assignment
     */
    public static Assignment sample(BayesianNetwork network, List<Variable> causalOrder, Random random, Assignment conditionAssignment) {

        HashMapAssignment assignment = new HashMapAssignment(network.getNumberOfVars());
        for (Variable var : causalOrder) {
            if (Utils.isMissingValue(conditionAssignment.getValue(var))) {
                double sampledValue = network.getConditionalDistribution(var).getUnivariateDistribution(assignment).sample(random);
                assignment.setValue(var, sampledValue);
            }else{
                assignment.setValue(var,conditionAssignment.getValue(var));
            }
        }
        return assignment;
    }

    static class DataFlinkWrapper implements DataFlink<DynamicDataInstance>, Serializable{
        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = 4107783324901370839L;

        Attributes attributes;
        DataSet<DataInstance> data;
        int timeId;
        int nSamples;
        private String name;

        public void setAttributes(Attributes attributes) {
            this.attributes = attributes;
        }

        public void setData(DataSet<DataInstance> data) {
            this.data = data;
        }

        public void setTimeId(int timeId) {
            this.timeId = timeId;
        }

        public void setnSamples(int nSamples) {
            this.nSamples = nSamples;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Attributes getAttributes() {
            return attributes;
        }

        @Override
        public DataSet<DynamicDataInstance> getDataSet() {
            return data.mapPartition(new MAPDynamicToStaticWrapper(this.timeId, this.nSamples, this.attributes));
        }
    }

    static class MAPDynamicToStaticWrapper extends RichMapPartitionFunction<DataInstance,DynamicDataInstance>{

        int time_id;
        int nSamples;
        Attributes attributes;
        public MAPDynamicToStaticWrapper(int time_id, int nSamples, Attributes atts) {
            this.time_id = time_id;
            this.nSamples = nSamples;
            this.attributes = atts;
        }

        @Override
        public void mapPartition(Iterable<DataInstance> values, Collector<DynamicDataInstance> out) throws Exception {

            Iterator<DataInstance> it = values.iterator();

            long count = (long) this.getRuntimeContext().getIndexOfThisSubtask()*nSamples;
            while (it.hasNext()){
                out.collect(new DynamicDataInstanceWrapper(it.next(),this.attributes,count,time_id));
                count++;
            }
        }

    }

    static class DynamicDataInstanceWrapper implements DynamicDataInstance, Serializable{
        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = 4107783324901370839L;

        DataInstance dataInstance;
        long seq_id;
        long time_id;
        Attributes attributes;

        public DynamicDataInstanceWrapper(DataInstance dataInstance, Attributes attributes, long seq_id, long time_id) {
            this.dataInstance = dataInstance;
            this.seq_id = seq_id;
            this.time_id = time_id;
            this.attributes=attributes;
        }

        @Override
        public long getSequenceID() {
            return this.seq_id;
        }

        @Override
        public long getTimeID() {
            return this.time_id;
        }

        @Override
        public double getValue(Attribute att, boolean present) {
            if (!present)
                throw new UnsupportedOperationException("This implementation can not query past evidence");

            if (att.isSeqId()){
                return this.seq_id;
            }if (att.isTimeId()){
                return this.time_id;
            }else {
                return this.dataInstance.getValue(att);
            }
        }

        @Override
        public void setValue(Attribute att, double val, boolean present) {
            if (!present)
                throw new UnsupportedOperationException("This implementation can not query past evidence");

            if (att.isSpecialAttribute()){
                throw new UnsupportedOperationException("Special Attributes can not be modified");
            }else {
                this.dataInstance.setValue(att,val);
            }
        }

        @Override
        public Attributes getAttributes() {
            return attributes;
        }

        @Override
        public double[] toArray() {
            return this.dataInstance.toArray();
        }

        @Override
        public String toString(){
            return this.outputString();
        }
    }
}
