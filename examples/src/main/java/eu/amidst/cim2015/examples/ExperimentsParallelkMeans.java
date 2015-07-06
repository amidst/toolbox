package eu.amidst.cim2015.examples;

import eu.amidst.core.datastream.*;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.OptionParser;

import java.io.IOException;
import java.util.Comparator;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 01/07/15.
 */
public class ExperimentsParallelkMeans {

    static int k = 2;
    static int numDiscVars = 5;
    static int numGaussVars = 5;
    static int numStates = 2;
    static int sampleSize = 10000;
    static boolean sampleData = true;
    static int batchSize = 100;

    static Attributes atts;
    /*Need to store the centroids*/
    static DataOnMemoryListContainer<DataInstance> centroids;

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

        DataStream<DataInstance> data;
        if(isSampleData()) {
            BayesianNetworkGenerator.setNumberOfGaussianVars(getNumGaussVars());
            BayesianNetworkGenerator.setNumberOfMultinomialVars(getNumDiscVars(), getNumStates());
            BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();
            data = new BayesianNetworkSampler(bn).sampleToDataStream(getSampleSize());
            DataStreamWriter.writeDataToFile(data, "./datasets/tmp.arff");
        }

        data = DataStreamLoader.openFromFile("datasets/tmp.arff");
        atts = data.getAttributes();

        /*Need to store the centroids*/
        centroids = new DataOnMemoryListContainer(data.getAttributes());

        data.stream().limit(getK()).forEach(dataInstance -> centroids.add(dataInstance));
        data.restart();

        boolean change = true;
        while(change){

            /*
            data.parallelStream(batchSize)
                    .map(Pair::getMinDistance)
                    .collect(
                            Collectors.groupingBy(
                                    Pair::getCentroid
                            ))
                    .entrySet()
                    .stream()
                    .flatMap(listEntry->listEntry.getValue().stream().map(pair -> pair.getDataInstance().toArray()))
                    .collect(Averager::new, Averager::accept, Averager::combine).average();
            */

            /*
            Map<DataInstance, DataInstance> oldAndNewCentroids = data.parallelStream(batchSize)
                    .map(Pair::getMinDistance)
                    .collect(
                            Collectors.groupingBy(
                                    Pair::getCentroid,
                                    Collectors.reducing(
                                            new double[atts.getNumberOfAttributes()],
                                            Pair::getDataInstanceToArray,
                                            Pair::getAverage)));
                                            */

            /*
            Map<DataInstance,List<Pair>> cluster = data.parallelStream(batchSize)
                    .map(Pair::getMinDistance)
                    .collect(
                            Collectors.groupingBy(
                                    Pair::getCentroid));
            */
        }


    }

    /*Calculate Euclidean Distance*/
    public static double getED(DataInstance e1, DataInstance e2){

        double sum = IntStream.rangeClosed(0,atts.getNumberOfAttributes()-1).mapToDouble(i ->
        {Attribute att = atts.getList().get(i); return Math.pow(e1.getValue(att)-e2.getValue(att),2);}).sum();

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

    public static class Pair
    {
        private DataInstance centroid;
        private DataInstance dataInstance;

        public DataInstance getCentroid() {
            return centroid;
        }

        public void setCentroid(DataInstance centroid) {
            this.centroid = centroid;
        }

        public DataInstance getDataInstance() {
            return dataInstance;
        }

        public void setDataInstance(DataInstance dataInstance) {
            this.dataInstance = dataInstance;
        }

        public Pair(DataInstance centroid_, DataInstance dataInstance_){
            centroid = centroid_;
            dataInstance = dataInstance_;
        }

        public static Pair getMinDistance(DataInstance dataInstance){
            DataInstance centroid = centroids.stream().min(Comparator.comparing(c -> getED(c, dataInstance))).get();
            return new Pair(centroid,dataInstance);
        }

        public double[] getDataInstanceToArray(){
            return dataInstance.toArray();
        }

        public double[] getCentroidToArray(){
            return centroid.toArray();
        }

        public double[] getAverage(double[] aux){
            return new double[atts.getNumberOfAttributes()];
        }


    }


    public static void main(String[] args) throws Exception {
        runParallelKMeans();
    }
}
