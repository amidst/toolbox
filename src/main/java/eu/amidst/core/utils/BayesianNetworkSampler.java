package eu.amidst.core.utils;


import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.*;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataWriter;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * Created by andresmasegosa on 11/12/14.
 */
public class BayesianNetworkSampler implements AmidstOptionsHandler {

    private BayesianNetwork network;

    private List<Variable> causalOrder;

    private int seed = 0;

    private Random random = new Random(seed);

    private Stream<Assignment> sampleStream;

    private Map<Variable, Boolean> hiddenVars = new HashMap();

    private Map<Variable, Double> marNoise = new HashMap();
    public BayesianNetworkSampler(){}

    public BayesianNetworkSampler(BayesianNetwork network1){
        network=network1;
        this.causalOrder=Utils.getCausalOrder(network.getDAG());
    }

    private Stream<Assignment> getSampleStream(int nSamples) {
        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);
        return IntStream.range(0, nSamples)
                .mapToObj(i -> sample(network, causalOrder, randomGenerator.current()))
                .map(this::filter);
    }

    public void setHiddenVar(Variable var) {
        this.hiddenVars.put(var,true);
    }

    public void setMARVar(Variable var, double noiseProb){ this.marNoise.put(var,noiseProb);}

    private Assignment filter(Assignment assignment){
        hiddenVars.keySet().stream().forEach(var -> assignment.setValue(var,Utils.missingValue()));
        marNoise.entrySet().forEach(e -> {
            if (random.nextDouble()<e.getValue())
                assignment.setValue(e.getKey(),Utils.missingValue());
        });

        return assignment;
    }

    private List<Assignment> getSampleList(int nSamples){
        return this.getSampleStream(nSamples).collect(Collectors.toList());
    }

    private Iterable<Assignment> getSampleIterator(int nSamples){
        class I implements Iterable<Assignment>{
            public Iterator<Assignment> iterator(){
                return getSampleStream(nSamples).iterator();
            }
        }
        return new I();
    }

    public void setSeed(int seed) {
        this.seed = seed;
        random = new Random(seed);
    }



    public DataStream<DataInstance> sampleToDataBase(int nSamples){
        class TemporalDataStream implements DataStream<DataInstance> {
            Attributes atts;
            BayesianNetworkSampler sampler;
            int nSamples;

            TemporalDataStream(BayesianNetworkSampler sampler1, int nSamples1){
                this.sampler=sampler1;
                this.nSamples = nSamples1;
                List<Attribute> list = this.sampler.network.getStaticVariables().getListOfVariables().stream()
                        .map(var -> new Attribute(var.getVarID(), var.getName(), var.getStateSpaceType())).collect(Collectors.toList());
                this.atts= new Attributes(list);
            }

            @Override
            public Attributes getAttributes() {
                return atts;
            }

            @Override
            public Stream<DataInstance> stream() {
                class TemporalDataInstance implements DataInstance{

                    Assignment assignment;
                    TemporalDataInstance(Assignment assignment1){
                        this.assignment=assignment1;
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
                    public double getValue(Attribute att) {
                        return this.assignment.getValue(sampler.network.getStaticVariables().getVariableByName(att.getName()));
                    }

                    @Override
                    public void setValue(Attribute att, double value) {
                        this.assignment.setValue(sampler.network.getStaticVariables().getVariableByName(att.getName()), value);
                    }
                }
                return this.sampler.getSampleStream(this.nSamples).map(a -> new TemporalDataInstance(a));
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

        random = new Random(seed);

        return new TemporalDataStream(this,nSamples);

    }

    private static Assignment sample(BayesianNetwork network, List<Variable> causalOrder, Random random) {

        HashMapAssignment assignment = new HashMapAssignment(network.getNumberOfVars());
        for (Variable var : causalOrder) {
            double sampledValue = network.getConditionalDistribution(var).getUnivariateDistribution(assignment).sample(random);
            assignment.setValue(var, sampledValue);
        }
        return assignment;
    }

    public String listOptions(){
        return  classNameID() +",\\"+
                "-seed, 0, seed for random number generator\\";
    }

    @Override
    public String listOptionsRecursively() {
        return this.listOptions()
                + "\n" + network.listOptionsRecursively();
    }

    public void loadOptions(){
        seed = getIntOption("-seed");
    }

    public String classNameID(){
        return "eu.amidst.core.utils.BayesianNetworkSampler";
    }


    public static void main(String[] args) throws Exception{

        Stopwatch watch = Stopwatch.createStarted();

        BayesianNetwork network = BayesianNetworkLoader.loadFromFile("networks/asia.bn");

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(network);
        sampler.setSeed(0);

        DataStream<DataInstance> dataStream = sampler.sampleToDataBase(10);
        DataStreamWriter.writeDataToFile(dataStream,"data/asisa-samples.arff");

        System.out.println(watch.stop());

        for (Assignment assignment : sampler.getSampleIterator(2)){
            System.out.println(assignment.toString(network.getStaticVariables().getListOfVariables()));
        }
        System.out.println();

        for (Assignment assignment : sampler.getSampleList(2)){
            System.out.println(assignment.toString(network.getStaticVariables().getListOfVariables()));
        }
        System.out.println();

        sampler.getSampleStream(2).forEach( e -> System.out.println(e.toString(network.getStaticVariables().getListOfVariables())));


        //VariableList is expensive to compute!!
    }

}
