package eu.amidst.core.utils;

import com.google.common.base.Stopwatch;
import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * Created by andresmasegosa on 11/12/14.
 */
public class BayesianNetworkSampler  {

    private BayesianNetwork network;

    private List<Variable> causalOrder;

    private boolean parallelMode = true;

    private int seed = 0;

    private Stream<Assignment> sampleStream;

    public BayesianNetworkSampler(BayesianNetwork network1){
        network=network1;
        this.causalOrder=BayesianNetworkSampler.getCausalOrder(network.getDAG());
    }


    /*public Stream<Assignment> getSampleStream(int nSamples) {
        if (parallelMode){
            LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);
            sampleStream = IntStream.range(0, nSamples).parallel().mapToObj(i -> sample(network, causalOrder, randomGenerator.current()));
        }else{
            Random random  = new Random(seed);
            sampleStream =  IntStream.range(0, nSamples).mapToObj(e -> sample(network, causalOrder, random));
        }
        return sampleStream;
    }*/




    public Stream<Assignment> getSampleStream(int nSamples) {
        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);
        sampleStream =  IntStream.range(0, nSamples).mapToObj(i -> sample(network, causalOrder, randomGenerator.current()));
        return (parallelMode)? sampleStream.parallel() : sampleStream;
    }

    public List<Assignment> getSampleList(int nSamples){
        return this.getSampleStream(nSamples).collect(Collectors.toList());
    }

    public Iterable<Assignment> getSampleIterator(int nSamples){
        class I implements Iterable<Assignment>{
            public Iterator<Assignment> iterator(){
                return getSampleStream(nSamples).iterator();
            }
        }
        return new I();
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    public void sampleToAnARFFFile(String path, int nSamples) throws IOException {

        List<Variable> variables = network.getStaticVariables().getListOfVariables();

        FileWriter fw = new FileWriter(path);
        fw.write("@relation dataset\n\n");

        for (Variable v : variables){
            fw.write(v.toARFFString()+"\n");
        }

        fw.write("\n\n@data\n\n");


        this.getSampleStream(nSamples).forEach(e -> {
            try {
                fw.write(e.toARFFString(variables) + "\n");
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        fw.close();

    }

    public DataBase sampleToDataBase(int nSamples){
        class TemporalDataBase implements DataBase{
            Attributes atts;
            BayesianNetworkSampler sampler;
            int nSamples;

            TemporalDataBase(BayesianNetworkSampler sampler1, int nSamples1){
                this.sampler=sampler1;
                this.nSamples = nSamples1;
                List<Attribute> list = this.sampler.network.getStaticVariables().getListOfVariables().stream()
                        .map(var -> new Attribute(var.getVarID(), var.getName(), var.getStateSpace())).collect(Collectors.toList());
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
                    public int getSequenceID() {
                        return 0;
                    }

                    @Override
                    public int getTimeID() {
                        return 0;
                    }
                }
                return this.sampler.getSampleStream(this.nSamples).map(a -> new TemporalDataInstance(a));
            }
        }

        return new TemporalDataBase(this,nSamples);

    }

    private static Assignment sample(BayesianNetwork network, List<Variable> causalOrder, Random random) {

        HashMapAssignment assignment = new HashMapAssignment(network.getNumberOfVars());
        for (Variable var : causalOrder) {
            double sampledValue = network.getDistribution(var).getUnivariateDistribution(assignment).sample(random);
            assignment.putValue(var, sampledValue);
        }
        return assignment;
    }

    private static List<Variable> getCausalOrder(DAG dag){
        StaticVariables variables = dag.getStaticVariables();
        int nNrOfAtts = variables.getNumberOfVars();
        List<Variable> order = new ArrayList();
        boolean[] bDone = new boolean[variables.getNumberOfVars()];

        for (Variable var: variables){
            bDone[var.getVarID()] = false;
        }

        for (int iAtt = 0; iAtt < nNrOfAtts; iAtt++) {
                boolean allParentsDone = false;
                for (Variable var2 : variables){
                    if (!bDone[var2.getVarID()]) {
                        allParentsDone = true;
                        int iParent = 0;
                        for (Variable parent: dag.getParentSet(var2))
                            allParentsDone = allParentsDone && bDone[parent.getVarID()];

                        if (allParentsDone){
                            order.add(var2);
                            bDone[var2.getVarID()] = true;
                        }
                    }
                }
            }
            return order;
    }


    public static void main(String[] args) throws Exception{

        Stopwatch watch = Stopwatch.createStarted();

        BayesianNetwork network = BayesianNetworkLoader.loadFromHugin("./networks/asia.net");

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(network);
        sampler.setSeed(0);
        sampler.setParallelMode(true);

        sampler.sampleToAnARFFFile("./data/asisa-samples.arff", 10);

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
