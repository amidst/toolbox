package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Vector;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * TODO By iterating several times over the data we can get better approximations. Trick. Initialize the Q's of the parameters variables with the final posterios in the previous iterations.
 *
 *
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public class StreamingVariationalBayesVMP implements BayesianLearningAlgorithmForBN {

    EF_LearningBayesianNetwork ef_extendedBN;
    PlateuVMP plateuVMP = new PlateuVMP();
    DAG dag;
    ParameterVariables parametersVariables;
    DataStream<DataInstance> dataStream;
    double elbo;
    boolean parallelMode=false;
    int windowsSize=100;
    int seed = 0;

    public StreamingVariationalBayesVMP(){
        plateuVMP = new PlateuVMP();
        plateuVMP.setNRepetitions(windowsSize);
    }


    public PlateuVMP getPlateuVMP() {
        return plateuVMP;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    @Override
    public double getLogMarginalProbability() {
        return elbo;
    }

    public void setWindowsSize(int windowsSize) {
        this.windowsSize = windowsSize;
        this.plateuVMP.setNRepetitions(windowsSize);
    }

    @Override
    public void runLearning() {
        this.initLearning();
        if (!parallelMode) {
            //this.elbo = this.dataStream.stream().sequential().mapToDouble(this::updateModel).sum();
            this.elbo = this.dataStream.streamOfBatches(this.windowsSize).sequential().mapToDouble(this::updateModel).sum();
        }else {
            //Creeat EF_ExtendedBN which returns ParameterVariable object
            //Paremter variable car
            BatchOutput finalout = this.dataStream.streamOfBatches(100).map(this::updateModelOnBatchParallel).reduce(BatchOutput::sum).get();
        }
    }

    public static double updateModelTmp(VMP localVMP, DataInstance dataInstance){
        localVMP.setEvidence(dataInstance);
        localVMP.runInference();
        for (EF_ConditionalDistribution dist: localVMP.getEFModel().getDistributionList()){
            if (dist.getVariable().isParameterVariable()){
                ((EF_BaseDistribution_MultinomialParents)dist).setBaseEFDistribution(0, localVMP.getEFPosterior(dist.getVariable()).deepCopy());
            }
        }
        return localVMP.getLogProbabilityOfEvidence();
    }

    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    @Override
    public double updateModel(DataOnMemory<DataInstance> batch) {
        //System.out.println("\n Batch:");
        this.plateuVMP.setEvidence(batch.getList());
        this.plateuVMP.runInference();
        for (EF_ConditionalDistribution dist: plateuVMP.getEFLearningBN().getDistributionList()){
            if (dist.getVariable().isParameterVariable()) {
                ((EF_BaseDistribution_MultinomialParents) dist).setBaseEFDistribution(0, plateuVMP.getEFParameterPosterior(dist.getVariable()).deepCopy());
            }
        }
        //this.plateuVMP.resetQs();
        return this.plateuVMP.getLogProbabilityOfEvidence();
    }

    private BatchOutput updateModelOnBatchParallel(DataStream<DataInstance> batch) {

        VMP localvmp = new VMP();
        localvmp.setEFModel(this.ef_extendedBN.toEFBayesianNetwork());

        List<Vector> naturalParameters = parametersVariables.getListOfVariables().stream()
                .map(var -> {
                    NaturalParameters parameter = this.ef_extendedBN.getDistribution(var).getNaturalParameters();
                    NaturalParameters copy = new ArrayVector(parameter.size());
                    copy.copy(parameter);
                    return copy;
                }).collect(Collectors.toList());

        CompoundVector compoundVectorInit = new CompoundVector(naturalParameters);


        double elbo = batch.stream().mapToDouble(dist -> updateModelTmp(localvmp, dist)).sum();

        naturalParameters = parametersVariables.getListOfVariables().stream()
                .map(var -> this.ef_extendedBN.getDistribution(var).getNaturalParameters()).collect(Collectors.toList());


        CompoundVector compoundVectorEnd = new CompoundVector(naturalParameters);

        compoundVectorEnd.substract(compoundVectorInit);

        return new BatchOutput(compoundVectorEnd, elbo);
    }

    @Override
    public void setDAG(DAG dag) {
        this.dag = dag;
    }

    public void initLearning(){

        List<EF_ConditionalDistribution> dists = this.dag.getParentSets().stream()
                .map(pSet -> pSet.getMainVar().getDistributionType().<EF_ConditionalDistribution>newEFConditionalDistribution(pSet.getParents()))
                .collect(Collectors.toList());

        this.ef_extendedBN = new EF_LearningBayesianNetwork(dists, dag.getStaticVariables());
        this.plateuVMP.setSeed(seed);
        plateuVMP.setPlateuModel(ef_extendedBN);
        this.plateuVMP.resetQs();
    }

    @Override
    public void setDataStream(DataStream<DataInstance> data) {
        this.dataStream=data;
    }

    private static EF_BayesianNetwork convertDAGToExtendedEFBN(DAG dag){
        return null;
    }

    @Override
    public BayesianNetwork getLearntBayesianNetwork() {
        return BayesianNetwork.newBayesianNetwork(this.dag, ef_extendedBN.toConditionalDistribution());
    }

    static class BatchOutput{
        Vector vector;
        double elbo;

        BatchOutput(Vector vector_, double elbo_) {
            this.vector = vector_;
            this.elbo = elbo_;
        }

        public Vector getVector() {
            return vector;
        }

        public double getElbo() {
            return elbo;
        }

        public void setElbo(double elbo) {
            this.elbo = elbo;
        }

        public static BatchOutput sum(BatchOutput batchOutput1, BatchOutput batchOutput2){
            batchOutput2.getVector().sum(batchOutput1.getVector());
            batchOutput2.setElbo(batchOutput2.getElbo()+batchOutput1.getElbo());
            return batchOutput2;
        }
    }



}
