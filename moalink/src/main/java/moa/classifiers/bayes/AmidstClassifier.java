package moa.classifiers.bayes;

import eu.amidst.corestatic.datastream.Attributes;
import eu.amidst.corestatic.datastream.DataInstance;
import eu.amidst.corestatic.datastream.DataOnMemoryListContainer;
import eu.amidst.corestatic.datastream.filereaders.DataInstanceImpl;
import eu.amidst.corestatic.distribution.Multinomial;
import eu.amidst.corestatic.inference.InferenceAlgorithm;
import eu.amidst.corestatic.inference.messagepassing.VMP;
import eu.amidst.corestatic.learning.parametric.MaximumLikelihood;
import eu.amidst.corestatic.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.corestatic.learning.parametric.bayesian.StreamingVariationalBayesVMP;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.models.DAG;
import eu.amidst.corestatic.variables.Variable;
import eu.amidst.corestatic.variables.Variables;
import eu.amidst.moalink.converterFromMoaToAmidst.Converter;
import eu.amidst.moalink.converterFromMoaToAmidst.DataRowWeka;
import moa.classifiers.AbstractClassifier;
import moa.core.InstancesHeader;
import moa.core.Measurement;
import moa.options.FlagOption;
import moa.options.IntOption;
import weka.core.Instance;

import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 18/06/15.
 */
public class AmidstClassifier  extends AbstractClassifier {

    public IntOption batchSizeOption = new IntOption("batchSize",
            'w', "Size of the batch in which to perform learning (significant if hidden variables)",
            100);

    public int getBatchSize_() {
        return batchSize_;
    }

    public void setBatchSize_(int batchSize_) {
        this.batchSize_ = batchSize_;
    }

    public int getnOfGaussianHiddenVars_() {
        return nOfGaussianHiddenVars_;
    }

    public void setnOfGaussianHiddenVars_(int nOfGaussianHiddenVars_) {
        this.nOfGaussianHiddenVars_ = nOfGaussianHiddenVars_;
    }

    public int getnOfStatesMultHiddenVar_() {
        return nOfStatesMultHiddenVar_;
    }

    public void setnOfStatesMultHiddenVar_(int nOfStatesMultHiddenVar_) {
        this.nOfStatesMultHiddenVar_ = nOfStatesMultHiddenVar_;
    }

    public boolean isParallelMode_() {
        return parallelMode_;
    }

    public void setParallelMode_(boolean parallelMode_) {
        this.parallelMode_ = parallelMode_;
    }

    protected  int batchSize_ = 100;

    public IntOption nOfGaussianHiddenVars = new IntOption("nOfGaussHiddenVars",
            'g', "Number of Gaussian hidden super-parent variables",
            0);
    protected  int nOfGaussianHiddenVars_ = 0;

    public IntOption nOfStatesMultHiddenVar = new IntOption("nOfStatesMultHiddenVar",
            'm', "Number of states for the multinomial hidden super-parent variable",
            0);
    protected  int nOfStatesMultHiddenVar_ = 0;

    public FlagOption parallelMode = new FlagOption("parallelMode", 'p',
            "Learn parameters in parallel mode when possible (e.g. ML)");

    protected boolean parallelMode_ = true;

    private DAG dag = null;
    private Variable classVar_;
    private DataOnMemoryListContainer<DataInstance> batch_;
    private ParameterLearningAlgorithm parameterLearningAlgorithm_;
    private BayesianNetwork bnModel_;
    InferenceAlgorithm predictions_;


    @Override
    public void resetLearningImpl() {

    }

    @Override
    public void setModelContext(InstancesHeader ih) {
        super.setModelContext(ih);

        setBatchSize_(batchSizeOption.getValue());
        setnOfGaussianHiddenVars_(nOfGaussianHiddenVars.getValue());
        setnOfStatesMultHiddenVar_(nOfStatesMultHiddenVar.getValue());
        setParallelMode_(parallelMode.isSet());

        Attributes attributes = Converter.convertAttributes(this.modelContext);
        Variables modelHeader = new Variables(attributes);
        classVar_ = modelHeader.getVariableByName(modelContext.classAttribute().name());

        batch_ = new DataOnMemoryListContainer(attributes);
        predictions_ = new VMP();

        /* Create hidden variables*/
        IntStream.rangeClosed(0, nOfGaussianHiddenVars_).forEach(i -> modelHeader.newGaussianVariable("HG_" + i));
        modelHeader.newMultionomialVariable("HM", nOfStatesMultHiddenVar_);

        dag = new DAG(modelHeader);

        /* Set DAG structure */
        if (isParallelMode_()) {
            dag.getParentSets().parallelStream()
                    .filter(w -> w.getMainVar().getVarID() != classVar_.getVarID())
                    .forEach(w -> {w.addParent(classVar_);
                            if(nOfStatesMultHiddenVar_!=0)
                                w.addParent(modelHeader.getVariableByName("HM"));
                            if(nOfGaussianHiddenVars_!=0)
                                IntStream.rangeClosed(0, nOfGaussianHiddenVars_).parallel()
                                        .forEach(hv -> w.addParent(modelHeader.getVariableByName("HG_" + hv)));});

        }
        else {
            dag.getParentSets().stream()
                    .filter(w -> w.getMainVar().getVarID() != classVar_.getVarID())
                    .forEach(w -> {w.addParent(classVar_);
                        if(nOfStatesMultHiddenVar_!=0)
                            w.addParent(modelHeader.getVariableByName("HM"));
                        if(nOfGaussianHiddenVars_!=0)
                            IntStream.rangeClosed(0, nOfGaussianHiddenVars_)
                                    .forEach(hv -> w.addParent(modelHeader.getVariableByName("HG_" + hv)));});
        }


        if(nOfStatesMultHiddenVar_ == 0 && nOfGaussianHiddenVars_ == 0){   //ML can be used
            parameterLearningAlgorithm_ = new MaximumLikelihood();
        }else
            parameterLearningAlgorithm_ = new StreamingVariationalBayesVMP();
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        DataInstance dataInstance = new DataInstanceImpl(new DataRowWeka(instance));
        if(batch_.getNumberOfDataInstances() < batchSize_) {  //store
            batch_.add(dataInstance);
        }else{                                                  //store & learn
            batch_.add(dataInstance);
            parameterLearningAlgorithm_.setParallelMode(isParallelMode_());
            parameterLearningAlgorithm_.setDAG(dag);
            parameterLearningAlgorithm_.setDataStream(batch_);
            parameterLearningAlgorithm_.initLearning();
            parameterLearningAlgorithm_.runLearning();
            bnModel_ = parameterLearningAlgorithm_.getLearntBayesianNetwork();
            predictions_.setModel(bnModel_);
        }
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {

        if(bnModel_ == null) {
            double[] votes = new double[classVar_.getNumberOfStates()];
            for (int i = 0; i < votes.length; i++) {
                votes[i] = 1.0/votes.length;
            }
            return votes;
        }

        DataInstance dataInstance = new DataInstanceImpl(new DataRowWeka(instance));
        this.predictions_.setEvidence(dataInstance);
        Multinomial multinomial = this.predictions_.getPosterior(classVar_);
        return multinomial.getParameters();
    }

    @Override
    public void getModelDescription(StringBuilder stringBuilder, int i) {

    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }
}
