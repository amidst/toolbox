package eu.amidst.core.learning.dynamic;

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * TODO By iterating several times over the data we can get better approximations. Trick. Initialize the Q's of the parameters variables with the final posterios in the previous iterations.
 *
 *
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public class StreamingVariationalBayesVMPForDBN implements BayesianLearningAlgorithmForDBN {

    EF_LearningBayesianNetwork ef_extendedBNTime0;
    EF_LearningBayesianNetwork ef_extendedBNTimeT;

    PlateuVMPDBN plateuVMPDBN = new PlateuVMPDBN();

    DynamicDAG dag;

    ParameterVariables parametersVariablesTime0;
    ParameterVariables parametersVariablesTimeT;

    DataStream<DynamicDataInstance> dataStream;

    double elbo;
    boolean parallelMode=false;
    int windowsSize=100;
    int seed = 0;

    public PlateuVMPDBN getPlateuVMPDBN() {
        return plateuVMPDBN;
    }

    public StreamingVariationalBayesVMPForDBN(){
        plateuVMPDBN = new PlateuVMPDBN();
        plateuVMPDBN.setNRepetitions(windowsSize);

    }


    //public PlateuVMP getPlateuVMP() {
    //    return plateuVMP;
    //}

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
        this.plateuVMPDBN.setNRepetitions(windowsSize);
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
            //BatchOutput finalout = this.dataStream.streamOfBatches(100).map(this::updateModelOnBatchParallel).reduce(BatchOutput::sum).get();
        }
    }

    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    @Override
    public double updateModel(DataOnMemory<DynamicDataInstance> batch) {
        //System.out.println("\n Batch:");

        List<DynamicDataInstance> data = batch.getList();
        double logprob = 0;
        if (batch.getDataInstance(0).getTimeID()==0){
            logprob+=this.updateModelTime0(batch.getDataInstance(0));
            data.remove(0);
            if (data.size()==0)
                return logprob;

        }

        logprob+=this.updateModelTimeT(data);

        return logprob;
    }

    private double updateModelTime0(DynamicDataInstance dataInstance) {
        this.plateuVMPDBN.setEvidenceTime0(dataInstance);
        this.plateuVMPDBN.runInferenceTime0();
        for (EF_ConditionalDistribution dist: plateuVMPDBN.getEFLearningBNTime0().getDistributionList()){
            if (dist.getVariable().isParameterVariable()) {
                ((EF_BaseDistribution_MultinomialParents) dist).setBaseEFDistribution(0, plateuVMPDBN.getEFParameterPosteriorTime0(dist.getVariable()).deepCopy());
            }
        }
        //this.plateuVMP.resetQs();
        return this.plateuVMPDBN.getLogProbabilityOfEvidenceTime0();
    }

    private double updateModelTimeT(List<DynamicDataInstance> batch) {
        this.plateuVMPDBN.setEvidenceTimeT(batch);
        this.plateuVMPDBN.runInferenceTimeT();
        for (EF_ConditionalDistribution dist: plateuVMPDBN.getEFLearningBNTimeT().getDistributionList()){
            if (dist.getVariable().isParameterVariable()) {
                ((EF_BaseDistribution_MultinomialParents) dist).setBaseEFDistribution(0, plateuVMPDBN.getEFParameterPosteriorTimeT(dist.getVariable()).deepCopy());
            }
        }
        //this.plateuVMPDBN.resetQs();
        return this.plateuVMPDBN.getLogProbabilityOfEvidenceTimeT();
    }


    @Override
    public void setDynamicDAG(DynamicDAG dag) {
        this.dag = dag;
    }

    public void initLearning(){

        List<EF_ConditionalDistribution> distTim0 = this.dag.getParentSetsTime0().stream().map(pSet -> pSet.getMainVar().getDistributionType().<EF_ConditionalDistribution>newEFConditionalDistribution(pSet.getParents())).collect(Collectors.toList());
        this.ef_extendedBNTime0 = new EF_LearningBayesianNetwork(distTim0, this.dag.getDynamicVariables());

        List<EF_ConditionalDistribution> distTimT = this.dag.getParentSetsTimeT().stream().map(pSet -> pSet.getMainVar().getDistributionType().<EF_ConditionalDistribution>newEFConditionalDistribution(pSet.getParents())).collect(Collectors.toList());
        this.ef_extendedBNTimeT = new EF_LearningBayesianNetwork(distTimT, this.dag.getDynamicVariables());


        this.plateuVMPDBN.setSeed(seed);
        this.plateuVMPDBN.setDBNModel(this.dag);
        this.plateuVMPDBN.setPlateuModelTime0(this.ef_extendedBNTime0);
        this.plateuVMPDBN.setPlateuModelTimeT(this.ef_extendedBNTimeT);
        this.plateuVMPDBN.resetQs();
    }

    @Override
    public void setDataStream(DataStream<DynamicDataInstance> data) {
        this.dataStream=data;
    }

    @Override
    public DynamicBayesianNetwork getLearntDBN() {
        return DynamicBayesianNetwork.newDynamicBayesianNetwork(this.dag, this.ef_extendedBNTime0.toConditionalDistribution(), this.ef_extendedBNTimeT.toConditionalDistribution());
    }




}
