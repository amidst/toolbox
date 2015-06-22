/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.utils;

import com.google.common.base.Stopwatch;
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
import eu.amidst.dynamic.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Hanen on 14/01/15.
 */
public class DynamicBayesianNetworkSampler {

    private DynamicBayesianNetwork network;
    private List<Variable> causalOrderTime0;
    private List<Variable> causalOrderTimeT;
    private int seed = 0;
    private Map<Variable, Boolean> hiddenVars = new HashMap();
    private Random random = new Random(seed);

    private Map<Variable, Double> marNoise = new HashMap();

    public DynamicBayesianNetworkSampler(DynamicBayesianNetwork network1){
        network=network1;
        this.causalOrderTime0 = eu.amidst.dynamic.utils.Utils.getCausalOrderTime0(network.getDynamicDAG());
        this.causalOrderTimeT = eu.amidst.dynamic.utils.Utils.getCausalOrderTimeT(network.getDynamicDAG());
    }


    public void setHiddenVar(Variable var) {
        if (var.isInterfaceVariable())
            throw new IllegalArgumentException();
        this.hiddenVars.put(var,true);
    }

    public void setMARVar(Variable var, double noiseProb){
        if (var.isInterfaceVariable())
            throw new IllegalArgumentException();

        this.marNoise.put(var,noiseProb);
    }

    private DynamicDataInstance filter(DynamicDataInstance assignment){
        hiddenVars.keySet().stream().forEach(var -> assignment.setValue(var,Utils.missingValue()));
        marNoise.entrySet().forEach(e -> {
            if (random.nextDouble()<e.getValue())
                assignment.setValue(e.getKey(),Utils.missingValue());
        });

        return assignment;
    }

    private Stream<DynamicAssignment> getSampleStream(int nSequences, int sequenceLength) {
        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);
        return IntStream.range(0,nSequences).mapToObj(Integer::new)
                .flatMap(i -> sample(network, causalOrderTime0, causalOrderTimeT, randomGenerator.current(), i, sequenceLength))
                .map(this::filter);
    }

    public void setSeed(int seed) {
        this.seed = seed;
        random = new Random(seed);
    }

    public DataStream<DynamicDataInstance> sampleToDataBase(int nSequences, int sequenceLength){
        random = new Random(seed);
        return new TemporalDataStream(this,nSequences,sequenceLength);
    }


    // Sample a stream of assignments of length "sequenceLength" for a given sequence "sequenceID"
    private static Stream<DynamicDataInstance> sample(DynamicBayesianNetwork network, List<Variable> causalOrderTime0, List<Variable> causalOrderTimeT, Random random, int sequenceID, int sequenceLength) {


        final HashMapAssignment[] data = new HashMapAssignment[2];

        return IntStream.range(0, sequenceLength).mapToObj( k ->
        {
            if (k==0) {
                HashMapAssignment dataPresent = new HashMapAssignment(network.getNumberOfVars());

                for (Variable var : causalOrderTime0) {
                    double sampledValue = network.getConditionalDistributionsTime0().get(var.getVarID()).getUnivariateDistribution(dataPresent).sample(random);
                    dataPresent.setValue(var, sampledValue);
                }

                DynamicDataInstanceImpl d = new DynamicDataInstanceImpl(network, null, dataPresent, sequenceID, 0);

                HashMapAssignment dataPast = new HashMapAssignment(network.getNumberOfVars());
                for (Variable var : network.getDynamicVariables().getListOfDynamicVariables()) {
                    dataPast.setValue(network.getDynamicVariables().getInterfaceVariable(var), dataPresent.getValue(var));
                }
                data[0] = dataPast;
                data[1] = dataPresent;

                return d;
            }else {
                HashMapAssignment dataPresent = new HashMapAssignment(network.getNumberOfVars());

                DynamicDataInstance d = new DynamicDataInstanceImpl(network, data[0], dataPresent, sequenceID, k);

                for (Variable var : causalOrderTimeT) {
                    double sampledValue = network.getConditionalDistributionsTimeT().get(var.getVarID()).getUnivariateDistribution(d).sample(random);
                    dataPresent.setValue(var, sampledValue);
                }

                HashMapAssignment dataPast = new HashMapAssignment(network.getNumberOfVars());
                for (Variable var : network.getDynamicVariables().getListOfDynamicVariables()) {
                    dataPast.setValue(network.getDynamicVariables().getInterfaceVariable(var), dataPresent.getValue(var));
                }
                data[0] = dataPast;
                data[1] = dataPresent;

                return d;
            }
        });
    }

    static class TemporalDataStream implements DataStream {
        Attributes atts;
        DynamicBayesianNetworkSampler sampler;
        int sequenceLength;
        int nSequences;

        TemporalDataStream(DynamicBayesianNetworkSampler sampler1, int nSequences1, int sequenceLength1){
            this.sampler=sampler1;
            this.nSequences = nSequences1;
            this.sequenceLength = sequenceLength1;
            List<Attribute> list = new ArrayList<>();

            list.add(new Attribute(0,Attributes.SEQUENCE_ID_ATT_NAME, new RealStateSpace()));
            list.add(new Attribute(1,Attributes.TIME_ID_ATT_NAME, new RealStateSpace()));
            list.addAll(this.sampler.network.getDynamicVariables().getListOfDynamicVariables().stream()
                    .map(var -> new Attribute(var.getVarID() + 2, var.getName(), var.getStateSpaceType())).collect(Collectors.toList()));
            this.atts= new Attributes(list);
        }

        @Override
        public Attributes getAttributes() {
            return atts;
        }

        @Override
        public Stream<DynamicDataInstance> stream() {
            return this.sampler.getSampleStream(this.nSequences,this.sequenceLength).map( e -> (DynamicDataInstanceImpl)e);
        }

        @Override
        public void close() {

        }

        @Override
        public boolean isRestartable() {
            return false;
        }

        @Override
        public void restart() {

        }
    }

    static class DynamicDataInstanceImpl implements DynamicDataInstance {

        DynamicBayesianNetwork dbn;
        private HashMapAssignment dataPresent;
        private HashMapAssignment dataPast;
        private int sequenceID;
        private int timeID;

        public DynamicDataInstanceImpl(DynamicBayesianNetwork dbn_, HashMapAssignment dataPast1, HashMapAssignment dataPresent1, int sequenceID1, int timeID1){
            this.dbn=dbn_;
            dataPresent = dataPresent1;
            dataPast =  dataPast1;
            this.sequenceID = sequenceID1;
            this.timeID = timeID1;
        }



        @Override
        public String toString(List<Variable> vars) {
            StringBuilder builder = new StringBuilder(vars.size()*2);
            vars.stream().limit(vars.size()-1).forEach(var -> builder.append(this.getValue(var)+","));
            builder.append(this.getValue(vars.get(vars.size()-1)));
            return builder.toString();
        }

        @Override
        public int getSequenceID() {
            return sequenceID;
        }

        @Override
        public int getTimeID() {
            return timeID;
        }

        @Override
        public double getValue(Variable var) {
            if (var.isInterfaceVariable()) {
                return dataPast.getValue(var);
            } else {
                return dataPresent.getValue(var);
            }
        }

        @Override
        public void setValue(Variable var, double val) {
            if (var.isInterfaceVariable()) {
                dataPast.setValue(var, val);
            } else {
                dataPresent.setValue(var, val);
            }
        }

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
        DataStreamWriter.writeDataToFile(dataStream, "./datasets/dnb-samples.arff");

        System.out.println(watch.stop());


        for (DynamicAssignment dynamicdataassignment : sampler.sampleToDataBase(3, 2)){
            System.out.println("\nSequence ID" + dynamicdataassignment.getSequenceID());
            System.out.println("\nTime ID" + dynamicdataassignment.getTimeID());
            System.out.println(dynamicdataassignment.toString(network.getDynamicVariables().getListOfDynamicVariables()));
        }

    }
}
