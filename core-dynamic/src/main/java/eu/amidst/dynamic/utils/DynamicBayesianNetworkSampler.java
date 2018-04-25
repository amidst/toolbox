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

package eu.amidst.dynamic.utils;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.core.utils.LocalRandomGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
/**
 * This class implements the interface {@link eu.amidst.core.utils.AmidstOptionsHandler}.
 * It defines a sampler of data from a {@link DynamicBayesianNetwork}.
 */
public class DynamicBayesianNetworkSampler {

    /** Represents the {@link DynamicBayesianNetwork} object from which the data will be sampled. */
    private DynamicBayesianNetwork network;

    /** Represents the list of variables given in a causal order at Time 0. */
    private List<Variable> causalOrderTime0;

    /** Represents the list of variables given in a causal order at Time T. */
    private List<Variable> causalOrderTimeT;

    /** Represents the initial seed for random sampling. */
    private int seed = 0;

    /** Represents a {@code Map} containing the hidden variables. */
    private Map<Variable, Boolean> hiddenVars = new HashMap();

    /** Represents a {@link java.util.Random} object. */
    private Random random = new Random(seed);

    /** Represents a {@code Map} containing the noisy variables. */
    private Map<Variable, Double> marNoise = new HashMap();

    /** Represents a {@code Map} containing the latent variables. */
    private Map<Variable, Boolean> latentVars = new HashMap();

    /**
     * Creates a new DynamicBayesianNetworkSampler given an input {@link DynamicBayesianNetwork} object.
     * @param network1 an input {@link DynamicBayesianNetwork} object.
     */
    public DynamicBayesianNetworkSampler(DynamicBayesianNetwork network1){
        network=network1;
        this.causalOrderTime0 = eu.amidst.dynamic.utils.Utils.getCausalOrderTime0(network.getDynamicDAG());
        this.causalOrderTimeT = eu.amidst.dynamic.utils.Utils.getCausalOrderTimeT(network.getDynamicDAG());
    }

    /**
     * Sets a given {@link Variable} object as latent. A latent variable doesn't contain an attribute and therefore
     * doesn't generate a sampling value.
     * @param var a given {@link Variable} object.
     */
    public void setLatentVar(Variable var){
        this.latentVars.put(var, true);
    }

    /**
     * Gets the set of latent variables. A latent variable doesn't contain an attribute and therefore
     * doesn't generate a sampling value.
     */
    public Set<Variable> getLatentVars() {
        return latentVars.keySet();
    }

    /**
     * Sets a given {@link Variable} object as hidden.
     * @param var a given {@link Variable} object.
     */
    public void setHiddenVar(Variable var) {
        if (var.isInterfaceVariable())
            throw new IllegalArgumentException();
        this.hiddenVars.put(var,true);
    }

    /**
     * Sets a given {@link Variable} object as noisy.
     * @param var a given {@link Variable} object.
     * @param noiseProb a double that represents the noise probability.
     */
    public void setMARVar(Variable var, double noiseProb){
        if (var.isInterfaceVariable())
            throw new IllegalArgumentException();

        this.marNoise.put(var,noiseProb);
    }

    /**
     * Filters a given {@link HashMapAssignment} object, i.e., sets the values assigned to either missing or noisy variables to Double.NaN.
     * @param assignment a given {@link HashMapAssignment} object.
     * @return a filtered {@link HashMapAssignment} object.
     */
    private HashMapAssignment filter(HashMapAssignment assignment){
        hiddenVars.keySet().stream().forEach(var -> assignment.setValue(var,Utils.missingValue()));
        marNoise.entrySet().forEach(e -> {
            if (random.nextDouble()<e.getValue())
                assignment.setValue(e.getKey(),Utils.missingValue());
        });

        if (!latentVars.isEmpty()){
            HashMapAssignment newassignment = new HashMapAssignment();
            for (Variable variable : assignment.getVariables()) {
                if (!this.latentVars.containsKey(variable))
                    newassignment.setValue(variable,assignment.getValue(variable));
            }
        }

        return assignment;
    }

    /**
     * Returns a {@code Stream} of randomly sampled {@link DynamicAssignment}s.
     * @param nSequences an {@code int} that represents the number of sequences.
     * @param sequenceLength an {@code int} that represents the length of each sequence.
     * @return a {@code Stream} of randomly sampled {@link DynamicAssignment}s.
     */
    private Stream<DynamicAssignment> getSampleStream(int nSequences, int sequenceLength) {
        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);
        return IntStream.range(0,nSequences).mapToObj(Integer::new)
                .flatMap(i -> sample(network, causalOrderTime0, causalOrderTimeT, randomGenerator.current(), i, sequenceLength));
    }

    /**
     * Sets the seed.
     * @param seed an {@code int} that represents the seed value.
     */
    public void setSeed(int seed) {
        this.seed = seed;
        random = new Random(seed);
    }

    /**
     * Returns a {@code DataStream} of randomly sampled {@link DynamicAssignment}s.
     * @param nSequences an {@code int} that represents the number of sequences.
     * @param sequenceLength an {@code int} that represents the length of each sequence.
     * @return a {@code DataStream} of randomly sampled {@link DynamicAssignment}s.
     */
    public DataStream<DynamicDataInstance> sampleToDataBase(int nSequences, int sequenceLength){
        random = new Random(seed);
        return new TemporalDataStream(this,nSequences,sequenceLength);
    }

    /**
     * Returns a {@code Stream} of randomly sampled {@link DynamicDataInstance} for a given sequence sequenceID of length sequenceLength.
     * @param network a {@link DynamicBayesianNetwork} object.
     * @param causalOrderTime0 a list of variables given in a causal order at Time 0.
     * @param causalOrderTimeT a list of variables given in a causal order at Time T.
     * @param random an object of type {@link java.util.Random}.
     * @param sequenceID an {@code int} that represents the sequence ID.
     * @param sequenceLength an {@code int} that represents the length of the sequence.
     * @return an object of the class Stream<DynamicDataInstance>
     */
    private Stream<DynamicDataInstance> sample(DynamicBayesianNetwork network, List<Variable> causalOrderTime0, List<Variable> causalOrderTimeT, Random random, int sequenceID, int sequenceLength) {

        final HashMapAssignment[] data = new HashMapAssignment[2];

        return IntStream.range(0, sequenceLength).mapToObj( k ->
        {
            if (k==0) {
                HashMapAssignment dataPresent = new HashMapAssignment(network.getNumberOfVars());

                for (Variable var : causalOrderTime0) {
                    double sampledValue = network.getConditionalDistributionsTime0().get(var.getVarID()).getUnivariateDistribution(dataPresent).sample(random);
                    dataPresent.setValue(var, sampledValue);
                }
                data[0] = replicateOnPast(dataPresent);
                data[1] = filter(dataPresent);

                return new DynamicDataInstanceImpl(network, null, data[1], sequenceID, 0);
            }else {
                HashMapAssignment dataPresent = new HashMapAssignment(network.getNumberOfVars());

                DynamicDataInstance d = new DynamicDataInstanceImpl(network, data[0], dataPresent, sequenceID, k);

                for (Variable var : causalOrderTimeT) {
                    double sampledValue = network.getConditionalDistributionsTimeT().get(var.getVarID()).getUnivariateDistribution(d).sample(random);
                    dataPresent.setValue(var, sampledValue);
                }
                data[0] = replicateOnPast(dataPresent);
                dataPresent = filter(dataPresent);

                d = new DynamicDataInstanceImpl(network, replicateOnPast(data[1]), dataPresent, sequenceID, k);

                data[1] = filter(dataPresent);

                return d;
            }
        });
    }

    /**
     * Replicates the present data in the past time.
     * @param dataPresent a {@link HashMapAssignment} object that represents the present data.
     * @return a {@link HashMapAssignment} object that represents the past data.
     */
    private HashMapAssignment replicateOnPast(HashMapAssignment dataPresent){
        HashMapAssignment dataPast = new HashMapAssignment(network.getNumberOfVars());
        for (Variable var : network.getDynamicVariables().getListOfDynamicVariables()) {
            dataPast.setValue(network.getDynamicVariables().getInterfaceVariable(var), dataPresent.getValue(var));
        }
        return dataPast;
    }

    /**
     * This class defines Temporal Data Stream and implements the {@link DataStream} interface.
     */
    static class TemporalDataStream implements DataStream {
        Attributes atts;
        DynamicBayesianNetworkSampler sampler;
        int sequenceLength;
        int nSequences;

        /**
         * Creates a new temporal data stream.
         * @param sampler1 a {@link DynamicBayesianNetworkSampler} object.
         * @param nSequences1 an {@code int} that represents the number of sequences in the data stream to be sampled.
         * @param sequenceLength1 an {@code int} that represents the length of each sequence.
         */
        TemporalDataStream(DynamicBayesianNetworkSampler sampler1, int nSequences1, int sequenceLength1){
            this.sampler=sampler1;
            this.nSequences = nSequences1;
            this.sequenceLength = sequenceLength1;
            List<Attribute> list = new ArrayList<>();

            list.add(new Attribute(0,Attributes.SEQUENCE_ID_ATT_NAME, new RealStateSpace()));
            list.add(new Attribute(1,Attributes.TIME_ID_ATT_NAME, new RealStateSpace()));
            list.addAll(this.sampler.network.getDynamicVariables().getListOfDynamicVariables().stream()
                    .filter(var -> !sampler1.getLatentVars().contains(var))
                    .map(var -> new Attribute(var.getVarID() + 2, var.getName(), var.getStateSpaceType())).collect(Collectors.toList()));
            this.atts= new Attributes(list);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Attributes getAttributes() {
            return atts;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Stream<DynamicDataInstance> stream() {
            return this.sampler.getSampleStream(this.nSequences,this.sequenceLength).map( e -> (DynamicDataInstanceImpl)e);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isRestartable() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void restart() {

        }
    }

    /**
     * This class implements the {@link DynamicDataInstance} interface.
     */
    static class DynamicDataInstanceImpl implements DynamicDataInstance, Serializable {

        DynamicBayesianNetwork dbn;
        private HashMapAssignment dataPresent;
        private HashMapAssignment dataPast;
        private long sequenceID;
        private long timeID;

        /**
         * Creates a new DynamicDataInstance.
         * @param dbn_ a {@link DynamicBayesianNetwork} object.
         * @param dataPast1 a {@link HashMapAssignment} object representing the past data.
         * @param dataPresent1 a {@link HashMapAssignment} object representing the present data.
         * @param sequenceID1 an {@code int} that represents the sequence ID.
         * @param timeID1 an {@code int} that represents the time ID.
         */
        public DynamicDataInstanceImpl(DynamicBayesianNetwork dbn_, HashMapAssignment dataPast1, HashMapAssignment dataPresent1, int sequenceID1, int timeID1){
            this.dbn=dbn_;
            dataPresent = dataPresent1;
            dataPast =  dataPast1;
            this.sequenceID = sequenceID1;
            this.timeID = timeID1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getSequenceID() {
            return sequenceID;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getTimeID() {
            return timeID;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getValue(Variable var) {
            if (var.isInterfaceVariable()) {
                return dataPast.getValue(var);
            } else {
                return dataPresent.getValue(var);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValue(Variable var, double val) {
            if (var.isInterfaceVariable()) {
                dataPast.setValue(var, val);
            } else {
                dataPresent.setValue(var, val);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Attributes getAttributes() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double[] toArray() {
            throw new UnsupportedOperationException("Method not currently supported for (HashMap)Assigment objects");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Variable> getVariables(){
            return Sets.union(dataPresent.getVariables(), dataPast.getVariables());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getValue(Attribute att, boolean present) {
            if (att.getIndex() == 0) {
                return this.sequenceID;
            } else if (att.getIndex() == 1){
                return this.timeID;
            }else{
                return this.getValue(dbn.getDynamicVariables().getVariableById(att.getIndex() - 2));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValue(Attribute att, double val, boolean present) {
            if (att.getIndex() == 0) {
                this.sequenceID = (int)val;
            } else if (att.getIndex() == 1){
                this.timeID = (int) val;
            }else {
                this.setValue(dbn.getDynamicVariables().getVariableById(att.getIndex() - 2), val);
            }
        }

    }


    public static void main(String[] args) throws Exception{

        Stopwatch watch = Stopwatch.createStarted();

        DynamicBayesianNetworkGenerator dbnGenerator = new DynamicBayesianNetworkGenerator();
        dbnGenerator.setNumberOfContinuousVars(0);
        dbnGenerator.setNumberOfDiscreteVars(3);
        dbnGenerator.setNumberOfStates(2);

        DynamicBayesianNetwork network = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(network);
        sampler.setSeed(0);
        DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(3,2);
        DataStreamWriter.writeDataToFile(dataStream, "./datasets/simulated/dnb-samples.arff");

        System.out.println(watch.stop());

        for (DynamicAssignment dynamicdataassignment : sampler.sampleToDataBase(3, 2)){
            System.out.println("\n Sequence ID" + dynamicdataassignment.getSequenceID());
            System.out.println("\n Time ID" + dynamicdataassignment.getTimeID());
            System.out.println(dynamicdataassignment.outputString());
        }

    }
}
