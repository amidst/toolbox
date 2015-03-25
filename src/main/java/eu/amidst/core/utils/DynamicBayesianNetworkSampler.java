package eu.amidst.core.utils;

import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.*;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataWriter;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.variables.*;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

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
    private Stream<Assignment> sampleStream;
    private Map<Variable, Boolean> hiddenVars = new HashMap();
    private Random random = new Random(seed);

    private Map<Variable, Double> marNoise = new HashMap();

    public DynamicBayesianNetworkSampler(DynamicBayesianNetwork network1){
        network=network1;
        this.causalOrderTime0 = Utils.getCausalOrderTime0(network.getDynamicDAG());
        this.causalOrderTimeT = Utils.getCausalOrderTimeT(network.getDynamicDAG());
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

    public Stream<DynamicAssignment> getSampleStream(int nSequences, int sequenceLength) {
        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);
        return IntStream.range(0,nSequences).mapToObj(Integer::new)
                .flatMap(i -> sample(network, causalOrderTime0, causalOrderTimeT, randomGenerator.current(), i, sequenceLength))
                .map(this::filter);
    }

    public List<DynamicAssignment> getSampleList(int nSequences, int sequenceLength){
        return this.getSampleStream(nSequences,sequenceLength).collect(Collectors.toList());
    }

    public Iterable<DynamicAssignment> getSampleIterator(int nSequences, int sequenceLength){
        class I implements Iterable<DynamicAssignment>{
            public Iterator<DynamicAssignment> iterator(){
                return getSampleStream(nSequences,sequenceLength).iterator();
            }
        }
        return new I();
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
                    double sampledValue = network.getDistributionsTime0().get(var.getVarID()).getUnivariateDistribution(dataPresent).sample(random);
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
                    double sampledValue = network.getDistributionsTimeT().get(var.getVarID()).getUnivariateDistribution(d).sample(random);
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
        ARFFDataWriter.writeToARFFFile(dataStream, "./data/dnb-samples.arff");

        System.out.println(watch.stop());


        for (DynamicAssignment dynamicdataassignment : sampler.getSampleIterator(3, 2)){
            System.out.println("\nSequence ID" + dynamicdataassignment.getSequenceID());
            System.out.println("\nTime ID" + dynamicdataassignment.getTimeID());
            System.out.println(dynamicdataassignment.toString(network.getDynamicVariables().getListOfDynamicVariables()));
        }

        //for (DynamicDataInstance dynamicdatainstance : sampler.getSampleList(2, 10)){
        //    System.out.println(dynamicdatainstance.toString(network.getDynamicVariables().getListOfDynamicVariables()));
        //}
        //System.out.println();

        //sampler.getSampleStream(2, 10).forEach( e -> System.out.println(e.toString(network.getDynamicVariables().getListOfDynamicVariables())));


    }
}
