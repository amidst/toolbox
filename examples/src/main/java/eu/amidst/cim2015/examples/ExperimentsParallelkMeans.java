package eu.amidst.cim2015.examples;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.OptionParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 01/07/15.
 */
public class ExperimentsParallelkMeans {

    static int k = 2;
    static int numDiscVars = 5000;
    static int numGaussVars = 5000;
    static int numStates = 10;
    static int sampleSize = 1000000;
    static boolean sampleData = true;
    static int batchSize = 1000;

    public static int getNumStates() {
        return numStates;
    }

    public static void setNumStates(int numStates) {
        ExperimentsParallelML.numStates = numStates;
    }

    public static int getNumDiscVars() {
        return numDiscVars;
    }

    public static void setNumDiscVars(int numDiscVars) {
        ExperimentsParallelML.numDiscVars = numDiscVars;
    }

    public static int getNumGaussVars() {
        return numGaussVars;
    }

    public static void setNumGaussVars(int numGaussVars) {
        ExperimentsParallelML.numGaussVars = numGaussVars;
    }

    public static int getSampleSize() {
        return sampleSize;
    }

    public static void setSampleSize(int sampleSize) {
        ExperimentsParallelML.sampleSize = sampleSize;
    }

    public static boolean isSampleData() {
        return sampleData;
    }

    public static void setSampleData(boolean sampleData) {
        ExperimentsParallelML.sampleData = sampleData;
    }

    public static int getK() {
        return k;
    }

    public static void setK(int k) {
        ExperimentsParallelkMeans.k = k;
    }

    public static void runParallelKMeans() throws IOException {

        BayesianNetworkGenerator.setNumberOfGaussianVars(getNumGaussVars());
        BayesianNetworkGenerator.setNumberOfMultinomialVars(getNumDiscVars(), getNumStates());
        BayesianNetwork bn  = BayesianNetworkGenerator.generateBayesianNetwork();
        DataStream<DataInstance> data = new BayesianNetworkSampler(bn).sampleToDataStream(getSampleSize());
        if(isSampleData())
            DataStreamWriter.writeDataToFile(data, "./datasets/tmp.arff");

        /*Need to store the centroids*/
        final DataOnMemoryListContainer<DataInstance> centroids = new DataOnMemoryListContainer(data.getAttributes());

        data.stream().limit(getK()).forEach(dataInstance -> centroids.add(dataInstance));
        data.restart();

        boolean change = true;
        while(change){

        }


    }

    /*Calculate Euclidean Distance*/
    public double getED(DataInstance e1, DataInstance e2){

        double sum = IntStream.rangeClosed(0,e1.getAttributes().getNumberOfAttributes()).mapToDouble(i ->
        {Attribute att = e1.getAttributes().getList().get(i); return Math.pow(e1.getValue(att)-e2.getValue(att),2);}).sum();

        return Math.sqrt(sum);
    }

    public static String classNameID(){
        return "eu.amidst.cim2015.examples.batchSizeComparisonsML";
    }

    public static String getOption(String optionName) {
        return OptionParser.parse(classNameID(), listOptions(), optionName);
    }

    public static int getIntOption(String optionName){
        return Integer.parseInt(getOption(optionName));
    }

    public static boolean getBooleanOption(String optionName){
        return getOption(optionName).equalsIgnoreCase("true") || getOption(optionName).equalsIgnoreCase("T");
    }

    public static String listOptions(){

        return  classNameID() +",\\"+
                "-sampleSize, 1000000, Sample size of the dataset\\" +
                "-numStates, 10, Num states of all disc. variables (including the class)\\"+
                "-GV, 5000, Num of gaussian variables\\"+
                "-DV, 5000, Num of discrete variables\\"+
                "-k, 2, Num of clusters\\"+
                "-sampleData, true, Sample arff data (if not read datasets/sampleBatchSize.arff by default)\\";
    }

    public static void loadOptions() {
        setNumGaussVars(getIntOption("-GV"));
        setNumDiscVars(getIntOption("-DV"));
        setNumStates(getIntOption("-numStates"));
        setSampleSize(getIntOption("-sampleSize"));
        setK(getIntOption("-k"));
        setSampleData(getBooleanOption("-sampleData"));
    }

    private class pair
    {

    }

    private class Averager
    {
        private double[] total = new double[getNumDiscVars()+getNumGaussVars()+1];
        private int count = 0;

        public double[] average() {
            return count > 0? Arrays.stream(total).map(val -> val / count).toArray() : new double[0];
        }

        public void accept(double[] instance) {
            total = IntStream.rangeClosed(0,instance.length-1).mapToDouble(i -> total[i] + instance[i]).toArray();
            count++;
        }

        public void combine(Averager other) {
            total = IntStream.rangeClosed(0,other.total.length-1).mapToDouble(i->total[i]+other.total[i]).toArray();
            count += other.count;
        }
    }


    public static void main(String[] args) throws Exception {
        runParallelKMeans();
    }
}
