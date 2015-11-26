package eu.amidst.dynamic.examples.inference;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.utils.AmidstOptionsHandler;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.inference.FactoredFrontierForDBN;
import eu.amidst.dynamic.inference.InferenceEngineForDBN;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 25/11/15.
 */
public class DynamicIS_Scalability implements AmidstOptionsHandler {

    int numberOfDiscreteVars = 5;
    int numberOfContinuousVars = 5;
    int numberOfStates = 2;
    int sequenceLength = 1000;
    int numOfSequences = 3;
    boolean connectChildrenTemporally = false;
    boolean activateMiddleLayer = true;


    public int getNumOfSequences() {
        return numOfSequences;
    }

    public void setNumOfSequences(int numOfSequences) {
        this.numOfSequences = numOfSequences;
    }

    public int getNumberOfContinuousVars() {
        return numberOfContinuousVars;
    }

    public void setNumberOfContinuousVars(int numberOfContinuousVars) {
        this.numberOfContinuousVars = numberOfContinuousVars;
    }

    public int getNumberOfStates() {
        return numberOfStates;
    }

    public void setNumberOfStates(int numberOfStates) {
        this.numberOfStates = numberOfStates;
    }

    public int getSequenceLength() {
        return sequenceLength;
    }

    public void setSequenceLength(int sequenceLength) {
        this.sequenceLength = sequenceLength;
    }

    public int getNumberOfDiscreteVars() {
        return numberOfDiscreteVars;
    }

    public void setNumberOfDiscreteVars(int numberOfDiscreteVars) {
        this.numberOfDiscreteVars = numberOfDiscreteVars;
    }

    public boolean isConnectChildrenTemporally() {
        return connectChildrenTemporally;
    }

    public void setConnectChildrenTemporally(boolean connectChildrenTemporally) {
        this.connectChildrenTemporally = connectChildrenTemporally;
    }

    public boolean isActivateMiddleLayer() {
        return activateMiddleLayer;
    }

    public void setActivateMiddleLayer(boolean activateMiddleLayer) {
        this.activateMiddleLayer = activateMiddleLayer;
    }

    public void runExperiment(){
        Random random = new Random(1);

        //We first generate a dynamic Bayesian network with two latent nodes
        DynamicVariables dynamicVariables  = new DynamicVariables();

        //Upper layer
        Variable varH1 = dynamicVariables.newMultinomialDynamicVariable("varH1",numberOfStates);

        //Middle layer
        Variable varH2, varH3;
        if(activateMiddleLayer) {
            varH2 = dynamicVariables.newMultinomialDynamicVariable("varH2", numberOfStates);
            varH3 = dynamicVariables.newMultinomialDynamicVariable("varH3", numberOfStates);
        }else{
            varH2 = null;
            varH3 = null;
        }

        //Discrete leaf variables (lower layer)
        IntStream.range(1, numberOfDiscreteVars+1)
                .forEach(i -> dynamicVariables.newMultinomialDynamicVariable("DiscreteVar" + i, numberOfStates));

        //Continuous leaf variables (lower layer)
        IntStream.range(1,numberOfContinuousVars+1)
                .forEach(i -> dynamicVariables.newGaussianDynamicVariable("ContinuousVar" + i));

        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        //Connect varH1 as parent of hidden nodes in the second layer
        if(activateMiddleLayer) {
            dag.getParentSetTimeT(varH2).addParent(varH1);
            dag.getParentSetTimeT(varH3).addParent(varH1);
        }
        //Connect hidden nodes temporally
        dag.getParentSetTimeT(varH1).addParent(varH1.getInterfaceVariable());
        if(activateMiddleLayer) {
            dag.getParentSetTimeT(varH2).addParent(varH2.getInterfaceVariable());
            dag.getParentSetTimeT(varH3).addParent(varH2.getInterfaceVariable());
        }


        //Connect hidden vars in middle layer (varH2 and varH3) with all leaves
        //connect leaf variables in time if set.
        if(activateMiddleLayer) {
            dag.getParentSetsTimeT().stream()
                    .filter(var -> var.getMainVar().getVarID()!=varH1.getVarID() &&
                    var.getMainVar().getVarID()!=varH2.getVarID()&&
                    var.getMainVar().getVarID()!=varH3.getVarID())
                    .forEach(w -> {
                                w.addParent(varH2);
                                w.addParent(varH3);
                                if(this.connectChildrenTemporally)
                                    w.addParent(dynamicVariables.getInterfaceVariable(w.getMainVar()));
                            }
                    );
        }else{
            //Connect hidden var in top layer (varH1) with all leaves
            //Connect leaf variables in time if set
            dag.getParentSetsTimeT().stream()
                    .filter(var -> var.getMainVar().getVarID()!=varH1.getVarID())
                    .forEach(w -> {
                                w.addParent(varH1);
                                if(this.connectChildrenTemporally)
                                    w.addParent(dynamicVariables.getInterfaceVariable(w.getMainVar()));
                            }
                    );
        }

        System.out.println(dag);

        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dag);

        //We initialize the parameters of the network randomly
        dbn.randomInitialization(random);

        //We create a dynamic dataset with 3 sequences for prediction
        DynamicBayesianNetworkSampler dynamicSampler = new DynamicBayesianNetworkSampler(dbn);
        dynamicSampler.setHiddenVar(varH1);
        if(activateMiddleLayer) {
            dynamicSampler.setHiddenVar(varH2);
            dynamicSampler.setHiddenVar(varH3);
        }
        DataStream<DynamicDataInstance> dataPredict = dynamicSampler.sampleToDataBase(this.numOfSequences,
                this.sequenceLength);


        //********************************************************************************************
        //                   DYNAMIC IS WITH FACTORED FRONTIER ALGORITHM
        //********************************************************************************************

        //We select DynamicVMP as the Inference Algorithm
        ImportanceSampling importanceSampling = new ImportanceSampling();
        importanceSampling.setParallelMode(true);
        importanceSampling.setKeepDataOnMemory(true);
        FactoredFrontierForDBN factoredFrontierForDBN = new FactoredFrontierForDBN(importanceSampling);
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(factoredFrontierForDBN);
        //Then, we set the DBN model
        InferenceEngineForDBN.setModel(dbn);

        UnivariateDistribution posterior = null;
        int time = 0 ;

        long start = System.nanoTime();
        for (DynamicDataInstance instance : dataPredict) {
            //The InferenceEngineForDBN must be reset at the begining of each Sequence.
            if (instance.getTimeID()==0 && posterior != null) {
                InferenceEngineForDBN.reset();
                time=0;
            }
            //We also set the evidence.
            InferenceEngineForDBN.addDynamicEvidence(instance);

            //Then we run inference
            InferenceEngineForDBN.runInference();

            //Then we query the posterior of the target variable
            posterior = InferenceEngineForDBN.getFilteredPosterior(varH1);

            //We show the output
            System.out.println("P(varH1|e[0:"+(time++)+"]) = "+posterior);
        }
        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        System.out.println("Time for Dynamic IS = "+seconds+" secs");
    }

    public static void main(String[] args) throws IOException {
        DynamicIS_Scalability exp = new DynamicIS_Scalability();
        exp.runExperiment();

    }

    @Override
    public String listOptions() {
        return  this.classNameID() +",\\"+
                "-d, 5, Number of discrete leaf vars\\" +
                "-c, 5, Number of continuous leaf vars\\" +
                "-s, 2, Number of states for all the discrete vars\\" +
                "-l 1000, Length for each sequence\\" +
                "-q, 3, Number of sequences\\" +
                "-linkNodes, false, Connects leaf nodes in consecutive time steps.\\"+
                "-activateMiddleLayer, true, Create middle layer with two (temporaly connected) " +
                                            "discrete hidden nodes.\\";
    }

    @Override
    public String listOptionsRecursively() {
        return this.listOptions();
    }

    @Override
    public void loadOptions() {
        this.setNumberOfDiscreteVars(this.getIntOption("d"));
        this.setNumberOfContinuousVars(this.getIntOption("c"));
        this.setNumberOfStates(this.getIntOption("s"));
        this.setSequenceLength(this.getIntOption("-l"));
        this.setNumOfSequences(this.getIntOption("q"));
        this.setConnectChildrenTemporally(getBooleanOption("-linkNodes"));
        this.setActivateMiddleLayer(getBooleanOption("-activateMiddleLayer"));
    }
}
