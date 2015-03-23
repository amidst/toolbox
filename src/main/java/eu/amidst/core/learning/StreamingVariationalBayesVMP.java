package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.BaseDistribution_MultinomialParents;
import eu.amidst.core.distribution.ConditionalLinearGaussian;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;
import spire.math.Natural;

import java.util.Iterator;
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
    DataStream<DataInstance> dataStream;
    double elbo;
    boolean parallelMode=false;
    boolean randomRestart=false;
    int windowsSize=100;
    int seed = 0;
    double fading = 1;

    public StreamingVariationalBayesVMP(){
        plateuVMP = new PlateuVMP();
        plateuVMP.setNRepetitions(windowsSize);
    }

    public void setRandomRestart(boolean randomRestart) {
        this.randomRestart = randomRestart;
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

    public void setFading(double fading) {
        this.fading = fading;
    }

    @Override
    public void runLearning() {
        this.initLearning();
        if (!parallelMode) {
            //this.elbo = this.dataStream.stream().sequential().mapToDouble(this::updateModel).sum();
            this.elbo = this.dataStream.streamOfBatches(this.windowsSize).mapToDouble(this::updateModel).sum();
        }else {

            List<Vector> naturalParametersPriors =  this.ef_extendedBN.getParametersVariables().getListOfVariables().stream()
                    .map(var -> {
                        NaturalParameters parameter =((EF_BaseDistribution_MultinomialParents)this.ef_extendedBN.getDistribution(var)).getBaseEFUnivariateDistribution(0).getNaturalParameters();
                        NaturalParameters copy = new ArrayVector(parameter.size());
                        copy.copy(parameter);
                        return copy;
                    }).collect(Collectors.toList());

            CompoundVector compoundVectorPrior = new CompoundVector(naturalParametersPriors);

            //BatchOutput finalout = this.dataStream.streamOfBatches(this.windowsSize).map(batch -> this.updateModelOnBatchParallel(batch, compoundVectorPrior)).reduce(BatchOutput::sum).get();

            List<Vector> naturalParametersPriors2 =  this.ef_extendedBN.getParametersVariables().getListOfVariables().stream()
                    .map(var -> {
                        NaturalParameters parameter =((EF_BaseDistribution_MultinomialParents)this.ef_extendedBN.getDistribution(var)).getBaseEFUnivariateDistribution(0).getNaturalParameters();
                        NaturalParameters copy = new ArrayVector(parameter.size());
                        copy.copy(parameter);
                        return copy;
                    }).collect(Collectors.toList());

            BatchOutput finalout = new BatchOutput(new CompoundVector(naturalParametersPriors2), 0);

            for (DataOnMemory<DataInstance> batch : this.dataStream.iterableOverBatches(this.windowsSize)){

                BatchOutput out = this.updateModelOnBatchParallel(batch, compoundVectorPrior);

                BatchOutput.sum(out, finalout);

                if(!randomRestart) {
                    for (int i = 0; i < this.ef_extendedBN.getParametersVariables().getListOfVariables().size(); i++) {
                        Variable var = this.ef_extendedBN.getParametersVariables().getListOfVariables().get(i);
                        CompoundVector vector = (CompoundVector) finalout.getVector();
                        NaturalParameters orig = (NaturalParameters) vector.getVectorByPosition(i);
                        NaturalParameters copy = new ArrayVector(orig.size());
                        copy.copy(orig);
                        this.plateuVMP.getEFParameterPosterior(var).setNaturalParameters(copy);
                    }
                }
            }

            this.elbo = finalout.getElbo();

            CompoundVector total = (CompoundVector)finalout.getVector();

            //total.sum(compoundVectorPrior);



            List<Variable> parameters = this.ef_extendedBN.getParametersVariables().getListOfVariables();

            for (int i = 0; i <parameters.size(); i++) {
                Variable var = parameters.get(i);
                EF_BaseDistribution_MultinomialParents dist = (EF_BaseDistribution_MultinomialParents) this.ef_extendedBN.getDistribution(var);
                EF_UnivariateDistribution uni = plateuVMP.getEFParameterPosterior(var).deepCopy();
                uni.setNaturalParameters((NaturalParameters)total.getVectorByPosition(i));
                dist.setBaseEFDistribution(0,uni);
            }
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

        this.ef_extendedBN.getParametersVariables().getListOfVariables().stream().forEach(var -> {
            EF_BaseDistribution_MultinomialParents dist = (EF_BaseDistribution_MultinomialParents) this.ef_extendedBN.getDistribution(var);
            EF_UnivariateDistribution prior  =plateuVMP.getEFParameterPosterior(var).deepCopy();
            NaturalParameters naturalParameters = prior.getNaturalParameters();
            naturalParameters.multiplyBy(fading);
            prior.setNaturalParameters(naturalParameters);
            dist.setBaseEFDistribution(0, prior);
        });

        /*this.getPlateuVMP().getEFLearningBN().getParametersVariables().forEach(var -> {
            if (!var.isGammaParameter())
                System.out.println(this.getPlateuVMP().getEFParameterPosterior(var).toUnivariateDistribution().toString());
            else
                System.out.println(this.getPlateuVMP().getEFParameterPosterior(var).getNaturalParameters().get(0)+ ", "+ 1/this.getPlateuVMP().getEFParameterPosterior(var).getMomentParameters().get(1));

        });*/


        //this.plateuVMP.resetQs();
        return this.plateuVMP.getLogProbabilityOfEvidence();
    }

    private BatchOutput updateModelOnBatchParallel(DataOnMemory<DataInstance> batch,  CompoundVector compoundVectorPrior) {

        this.plateuVMP.setEvidence(batch.getList());
        this.plateuVMP.runInference();

        List<Vector> naturalParametersPosterior =  this.ef_extendedBN.getParametersVariables().getListOfVariables().stream()
                .map(var -> plateuVMP.getEFParameterPosterior(var).deepCopy().getNaturalParameters()).collect(Collectors.toList());


        CompoundVector compoundVectorEnd = new CompoundVector(naturalParametersPosterior);

        compoundVectorEnd.substract(compoundVectorPrior);

        return new BatchOutput(compoundVectorEnd, this.plateuVMP.getLogProbabilityOfEvidence());
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
