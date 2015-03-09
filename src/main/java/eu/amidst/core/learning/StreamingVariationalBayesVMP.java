package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.VMP;
import eu.amidst.core.inference.VMP_.Node;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * TODO By iterating several times over the data we can get better approximations. Trick. Initialize the Q's of the parameters variables with the final posterios in the previous iterations.
 *
 *
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public class StreamingVariationalBayesVMP implements BayesianLearningAlgorithmForBN {

    EF_BayesianNetwork ef_extendedBN;
    VMP vmp = new VMP();
    DAG dag;
    ParameterVariables parametersVariables;
    DataStream<DataInstance> dataStream;
    double elbo;
    boolean parallelMode=false;

    @Override
    public double updateModel(DataInstance dataInstance){
        return updateModel(this.vmp,dataInstance);
    }

    @Override
    public double getLogMarginalProbability() {
        return elbo;
    }

    @Override
    public void runLearning() {
        if (!parallelMode) {
            this.elbo = this.dataStream.stream().sequential().mapToDouble(this::updateModel).sum();
        }else {
            //Creeat EF_ExtendedBN which returns ParameterVariable object
            //Paremter variable car
            BatchOutput finalout = this.dataStream.streamOfBatches(100).map(this::updateModelOnBatch).reduce(BatchOutput::sum).get();
        }
    }

    public static double updateModel(VMP localVMP, DataInstance dataInstance){
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

    private BatchOutput updateModelOnBatch(DataStream<DataInstance> batch) {

        VMP localvmp = new VMP();
        localvmp.setEFModel(this.ef_extendedBN);

        List<Vector> naturalParameters = parametersVariables.getListOfVariables().stream()
                .map(var -> {
                    NaturalParameters parameter = this.ef_extendedBN.getDistribution(var).getNaturalParameters();
                    NaturalParameters copy = new ArrayVector(parameter.size());
                    copy.copy(parameter);
                    return copy;
                }).collect(Collectors.toList());

        CompoundVector compoundVectorInit = new CompoundVector(naturalParameters);


        double elbo = batch.stream().mapToDouble(dist -> updateModel(localvmp, dist)).sum();

        naturalParameters = parametersVariables.getListOfVariables().stream()
                .map(var -> this.ef_extendedBN.getDistribution(var).getNaturalParameters()).collect(Collectors.toList());


        CompoundVector compoundVectorEnd = new CompoundVector(naturalParameters);

        compoundVectorEnd.substract(compoundVectorInit);

        return new BatchOutput(compoundVectorEnd, elbo);
    }

    @Override
    public void setDAG(DAG dag) {

        this.dag = dag;
        parametersVariables = new ParameterVariables(dag.getStaticVariables());

        List<EF_ConditionalDistribution> ef_condDistList = dag.getStaticVariables().getListOfVariables().stream().
                map(var -> var.getDistributionType().newEFConditionalDistribution(dag.getParentSet(var).getParents()).
                        toExtendedLearningDistribution(parametersVariables))
                .flatMap(listOfDist -> listOfDist.stream())
                .sorted((a,b) -> a.getVariable().getVarID() - b.getVariable().getVarID()).collect(Collectors.toList());

        this.ef_extendedBN = new EF_BayesianNetwork();
        ef_extendedBN.setDistributionList(ef_condDistList);
        vmp.setEFModel(ef_extendedBN);
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
        List<ConditionalDistribution> condDistList = new ArrayList<>();

        for (EF_ConditionalDistribution dist: this.ef_extendedBN.getDistributionList()) {
            if (dist.getVariable().isParameterVariable())
                continue;

            EF_ConditionalLearningDistribution distLearning = (EF_ConditionalLearningDistribution)dist;
            Map<Variable, Vector> expectedParameters = new HashMap<>();
            for(Variable var: distLearning.getParameterParentVariables()){
                EF_UnivariateDistribution uni =  ((EF_BaseDistribution_MultinomialParents)this.ef_extendedBN.getDistribution(var)).getBaseEFUnivariateDistribution(0);
                expectedParameters.put(var, uni.getExpectedParameters());
            }
            condDistList.add(distLearning.toConditionalDistribution(expectedParameters));
        }


        condDistList = condDistList.stream().sorted((a, b) -> a.getVariable().getVarID() - b.getVariable().getVarID()).collect(Collectors.toList());


        return BayesianNetwork.newBayesianNetwork(dag,condDistList);
    }

    static class BatchOutput{
        Vector vector;
        double elbo;

        BatchOutput(Vector vector_, double elbo_) {
            this.vector = vector_;
            this.elbo = elbo;
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
