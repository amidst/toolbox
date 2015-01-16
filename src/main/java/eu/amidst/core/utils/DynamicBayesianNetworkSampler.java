package eu.amidst.core.utils;

import com.google.common.base.Stopwatch;
import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.filereaders.DynamicDataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicBayesianNetworkLoader;
import eu.amidst.core.variables.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
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
    private boolean parallelMode = true;
    private int seed = 0;
    private Stream<Assignment> sampleStream;


    public DynamicBayesianNetworkSampler(DynamicBayesianNetwork network1){
        network=network1;
        this.causalOrderTime0 = Utils.getCausalOrderTime0(network.getDynamicDAG());
        this.causalOrderTimeT = Utils.getCausalOrderTimeT(network.getDynamicDAG());
    }


    public Stream<DataInstance> getSampleStream(int nSequences, int sequenceLength) {
        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);

        List<DataInstance> dbnsample = new ArrayList(nSequences);

        for(int i=0; i< nSequences;i++){
            List<DynamicDataInstance> oneseq = sample(network, causalOrderTime0, causalOrderTimeT, randomGenerator.current(), i, sequenceLength);
            for(int j=0; j< sequenceLength;j++) {
                dbnsample.add(i, oneseq.get(j));
            }
        }
        //sampleStream = sampleStream.forEach(i -> sample(network, causalOrderTime0, causalOrderTimeT, randomGenerator.current(), i, sequenceLength));

        return dbnsample.stream();
    }

    public List<DataInstance> getSampleList(int nSequences, int sequenceLength){
        return this.getSampleStream(nSequences,sequenceLength).collect(Collectors.toList());
    }

    public Iterable<DataInstance> getSampleIterator(int nSequences, int sequenceLength){
        class I implements Iterable<DataInstance>{
            public Iterator<DataInstance> iterator(){
                return getSampleStream(nSequences,sequenceLength).iterator();
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

    public void sampleToAnARFFFile(String path, int nSequences, int sequenceLength) throws IOException {

        List<Variable> variables = network.getDynamicVariables().getListOfDynamicVariables();

        FileWriter fw = new FileWriter(path);
        fw.write("@relation dataset\n\n");

        for (Variable v : variables){
            fw.write(v.toARFFString()+"\n");
        }

        fw.write("\n\n@data\n\n");

        this.getSampleStream(nSequences,sequenceLength).forEach(e -> {
            try {
                fw.write(e.toARFFString(variables) + "\n");
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        fw.close();
    }


    public DataBase sampleToDataBase(int nSequences, int sequenceLength){

        class TemporalDataBase implements DataBase{
            Attributes atts;
            DynamicBayesianNetworkSampler sampler;
            int sequenceLength;
            int nSequences;

            TemporalDataBase(DynamicBayesianNetworkSampler sampler1, int nSequences1, int sequenceLength1){
                this.sampler=sampler1;
                this.nSequences = nSequences1;
                this.sequenceLength = sequenceLength1;
                List<Attribute> list = this.sampler.network.getDynamicVariables().getListOfDynamicVariables().stream()
                        .map(var -> new Attribute(var.getVarID(), var.getName(), var.getStateSpace())).collect(Collectors.toList());
                this.atts= new Attributes(list);
            }

            @Override
            public Attributes getAttributes() {
                return atts;
            }

            @Override
            public Stream<DataInstance> stream() {
/*
                class TemporalDynamicDataInstance implements DataInstance{

                    HashMapAssignment dataPast;
                    HashMapAssignment dataPresent;
                    int sequenceID;
                    int timeID;

                    TemporalDynamicDataInstance(HashMapAssignment dataPast1, HashMapAssignment dataPresent1, int sequenceID1, int timeID1){
                        this.dataPast=dataPast1;
                        this.dataPresent = dataPresent1;
                        this.sequenceID = sequenceID1;
                        this.timeID = timeID1;
                    }

                    @Override
                    public double getValue(Variable var) {
                        if (var.isTemporalClone()){
                            return dataPast.getValue(var);
                        }else {
                            return dataPresent.getValue(var);
                        }
                    }

                    @Override
                    public int getSequenceID() {
                        return sequenceID;
                    }

                    @Override
                    public int getTimeID() {
                        return timeID;
                    }
                }
*/
                return this.sampler.getSampleStream(this.nSequences,this.sequenceLength);//.map((a,b) -> new TemporalDynamicDataInstance(a));
            }
        }

        return new TemporalDataBase(this,nSequences,sequenceLength);
    }


    // Sample a stream of assignments of length "sequenceLength" for a given sequence "sequenceID"
    private static List<DynamicDataInstance> sample(DynamicBayesianNetwork network, List<Variable> causalOrderTime0, List<Variable> causalOrderTimeT, Random random, int sequenceID, int sequenceLength) {

        List<DynamicDataInstance> allAssignments = new ArrayList(sequenceLength);

        HashMapAssignment dataPast = new HashMapAssignment(network.getNumberOfVars());
        HashMapAssignment dataPresent = new HashMapAssignment(network.getNumberOfVars());

        for (Variable var : causalOrderTime0) {
            double sampledValue = network.getDistributionsTime0().get(var.getVarID()).getUnivariateDistribution(dataPresent).sample(random);
            dataPresent.putValue(var, sampledValue);
        }

        DynamicDataInstance d = new DynamicDataInstance(null,dataPresent, sequenceID, 0);

        allAssignments.add(0,d);

        dataPast = dataPresent;

        for(int i=1; i< sequenceLength;i++) {
            for (Variable var : causalOrderTimeT) {
                double sampledValue = network.getDistributionsTimeT().get(var.getVarID()).getUnivariateDistribution(dataPresent).sample(random);
                dataPresent.putValue(var, sampledValue);
            }
            DynamicDataInstance d2 = new DynamicDataInstance(dataPast, dataPresent, sequenceID, i);

            allAssignments.add(i,d2);

            dataPast = dataPresent;
           }
        return allAssignments;
        //return allAssignments.stream();
    }


    static class DynamicDataInstance implements DataInstance{

        private HashMapAssignment dataPresent;
        private HashMapAssignment dataPast;
        private int sequenceID;
        private int timeID;

        public DynamicDataInstance(HashMapAssignment dataPast1, HashMapAssignment dataPresent1, int sequenceID1, int timeID1){
            dataPresent = dataPresent1;
            dataPast =  dataPast1;
            this.sequenceID = sequenceID1;
            this.timeID = timeID1;
        }

        @Override
        public double getValue(Variable var) {
            if (var.isTemporalClone()){
                return dataPast.getValue(var);
            }else {
                return dataPresent.getValue(var);
            }
        }

        @Override
        public String toString(List<Variable> vars) {
            StringBuilder builder = new StringBuilder(vars.size()*2);
            vars.stream().limit(vars.size()-1).forEach(var -> builder.append(this.getValue(var)+","));
            builder.append(this.getValue(vars.get(vars.size()-1)));
            return builder.toString();
        }

        @Override
        public String toARFFString(List<Variable> vars) {
            StringBuilder builder = new StringBuilder(vars.size()*2);

            for(int i=0; i<vars.size()-1;i++) {
                if (vars.get(i).getStateSpace().getStateSpaceType() == StateSpaceType.FINITE_SET) {
                    FiniteStateSpace stateSpace = vars.get(i).getStateSpace();
                    String nameState = stateSpace.getStatesName((int) this.getValue(vars.get(i)));
                    builder.append(nameState + ",");
                }
                else{
                    builder.append(this.getValue(vars.get(i))+ ",");
                }
            }

            if(vars.get(vars.size()-1).getStateSpace().getStateSpaceType()  == StateSpaceType.FINITE_SET) {
                FiniteStateSpace stateSpace = vars.get(vars.size() - 1).getStateSpace();
                String nameState = stateSpace.getStatesName((int) this.getValue(vars.get(vars.size() - 1)));
                builder.append(nameState);
            }
            else{
                builder.append(this.getValue(vars.get(vars.size() - 1)));
            }
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
    }


    public static void main(String[] args) throws Exception{

        Stopwatch watch = Stopwatch.createStarted();

        DynamicBayesianNetwork network = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2);

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(network);
        sampler.setSeed(0);
        sampler.setParallelMode(true);

        sampler.sampleToAnARFFFile("./data/dnb-samples.arff", 2, 10);

        System.out.println(watch.stop());

        for (DataInstance dynamicdatainstance : sampler.getSampleIterator(2, 10)){
            System.out.println(dynamicdatainstance.toString(network.getDynamicVariables().getListOfDynamicVariables()));
        }
        System.out.println();

        for (DataInstance dynamicdatainstance : sampler.getSampleList(2, 10)){
            System.out.println(dynamicdatainstance.toString(network.getDynamicVariables().getListOfDynamicVariables()));
        }
        System.out.println();

        sampler.getSampleStream(2, 10).forEach( e -> System.out.println(e.toString(network.getDynamicVariables().getListOfDynamicVariables())));


    }
}
