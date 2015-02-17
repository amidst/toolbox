package eu.amidst.core.inference;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DynamicDataInstance;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.VMP_.Message;
import eu.amidst.core.inference.VMP_.Node;
import eu.amidst.core.learning.DynamicNaiveBayesClassifier;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.DynamicAssignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 12/02/15.
 */
public class DynamicVMP implements InferenceAlgorithmForDBN {


    DynamicBayesianNetwork model;
    EF_DynamicBayesianNetwork ef_model;
    DynamicAssignment assignment = new HashMapAssignment(0);

    List<Node>  nodesTimeT;
    List<Node>  nodesClone;

    VMP vmpTime0;
    VMP vmpTimeT;

    int timeID;

    public DynamicVMP(){
        this.vmpTime0 = new VMP();
        this.vmpTimeT = new VMP();
        this.setParallelMode(false);
        this.setSeed(0);
        this.timeID=-1;
    }
    public boolean isParallelMode() {
        return this.vmpTimeT.isParallelMode();
    }

    public void setParallelMode(boolean parallelMode) {
        this.vmpTime0.setParallelMode(parallelMode);
        this.vmpTimeT.setParallelMode(parallelMode);
    }

    public int getSeed() {
        return this.vmpTime0.getSeed();
    }

    public void setSeed(int seed) {
        this.vmpTime0.setSeed(seed);
        this.vmpTimeT.setSeed(seed);
    }

    @Override
    public void setModel(DynamicBayesianNetwork model_) {
        model = model_;
        ef_model = new EF_DynamicBayesianNetwork(this.model);

        this.vmpTime0.setEFModel(ef_model.getBayesianNetworkTime0());

        nodesTimeT = this.ef_model.getBayesianNetworkTimeT().getDistributionList()
                .stream()
                .map(dist -> new Node(dist))
                .collect(Collectors.toList());

        nodesClone = this.ef_model.getBayesianNetworkTime0().getDistributionList()
                .stream()
                .map(dist -> {
                    EF_ConditionalDistribution pDist = new EF_BaseDistribution_MultinomialParents(new ArrayList<Variable>(),
                            Arrays.asList(dist.getNewBaseEFUnivariateDistribution()));
                    Node node = new Node(pDist);
                    node.setMainVar(this.model.getDynamicVariables().getTemporalClone(pDist.getVariable()));
                    node.setActive(false);
                    return node;
                })
                .collect(Collectors.toList());

        List<Node> allNodes = new ArrayList();
        allNodes.addAll(nodesTimeT);
        allNodes.addAll(nodesClone);
        this.vmpTimeT.setNodes(allNodes);

    }

    @Override
    public DynamicBayesianNetwork getModel() {
        return this.model;
    }


    @Override
    public void addDynamicEvidence(DynamicAssignment assignment_) {
        this.assignment = assignment_;
    }

    @Override
    public <E extends UnivariateDistribution> E getFilteredPosterior(Variable var) {
        return (getTimeIDOfPosterior()==0)? this.vmpTime0.getPosterior(var): this.vmpTimeT.getPosterior(var);
    }

    @Override
    public <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead) {
        return null;
    }

    @Override
    public int getTimeIDOfPosterior() {
        return this.timeID;
    }

    @Override
    public int getTimeIDOfLastEvidence(){
        return this.assignment.getTimeID();
    }

    @Override
    public void runInference(){

        if (assignment.getTimeID()==0) {
            this.vmpTime0.setEvidence(this.assignment);
            this.vmpTime0.runInference();
            this.timeID=0;

            this.vmpTime0.getNodes().stream().filter(node -> !node.isObserved()).forEach(node -> {
                Variable temporalClone = this.model.getDynamicVariables().getTemporalClone(node.getMainVariable());
                ((EF_BaseDistribution_MultinomialParents) this.vmpTimeT.getNodeOfVar(temporalClone).getPDist()).setBaseEFDistribution(0, node.getQDist().deepCopy());
            });

        }else{
            this.timeID=this.assignment.getTimeID();
            this.vmpTimeT.setEvidence(this.assignment);
            this.vmpTimeT.runInference();
            this.vmpTimeT.getNodes().stream()
                    .filter(node -> !node.getMainVariable().isTemporalClone() && !node.isObserved())
                    .forEach(node -> {
                        Variable temporalClone = this.model.getDynamicVariables().getTemporalClone(node.getMainVariable());
                        ((EF_BaseDistribution_MultinomialParents) this.vmpTimeT.getNodeOfVar(temporalClone).getPDist()).setBaseEFDistribution(0, node.getQDist().deepCopy());
                    });
        }

    }

    public static void main(String[] arguments) throws IOException, ClassNotFoundException {

        String file = "./datasets/bank_data_train.arff";
        DataBase<DynamicDataInstance> data = new DynamicDataOnDiskFromFile(new ARFFDataReader(file));

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 3);//We set -3 to account for time id and seq_id
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork bn = model.getDynamicBNModel();

        file = "./datasets/bank_data_predict.arff";
        data = new DynamicDataOnDiskFromFile(new ARFFDataReader(file));

        // The value of the timeWindow must be sampleSize-1 at maximum
        int timeSlices = 9;


        System.out.println("Computing Probabilities of Defaulting for 10 clients using Hugin API:\n");

        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new DynamicVMP());
        InferenceEngineForDBN.setModel(bn);
        Variable defaultVar = bn.getDynamicVariables().getVariableByName("DEFAULT");
        UnivariateDistribution dist = null;
        for(DynamicDataInstance instance: data){
            if (instance.getTimeID()==0 && dist != null)
                System.out.println(dist.toString());
            instance.setValue(defaultVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            dist = InferenceEngineForDBN.getFilteredPosterior(defaultVar);
        }

    }
}
