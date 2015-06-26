package moa.clusterers;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.filereaders.DataInstanceImpl;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.learning.parametric.bayesian.StreamingVariationalBayesVMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.moalink.converterFromMoaToAmidst.Converter;
import eu.amidst.moalink.converterFromMoaToAmidst.DataRowWeka;
import moa.cluster.Clustering;
import moa.core.Measurement;
import moa.options.FlagOption;
import moa.options.IntOption;
import weka.core.*;

import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 18/06/15.
 */
public class AmidstClusteringAlgorithm extends AbstractClusterer {


    public IntOption timeWindowOption = new IntOption("timeWindow",
            't', "Rang of the window.", 100);

    public IntOption numberClustersOption = new IntOption("numberClusters",
            'c', "Number of Clusters.", 2);

    public FlagOption parallelModeOption = new FlagOption("parallelMode", 'p',
            "Learn parameters in parallel mode when possible (e.g. ML)");

    public boolean isParallelMode_() {
        return parallelMode_;
    }

    public void setParallelMode_(boolean parallelMode_) {
        this.parallelMode_ = parallelMode_;
    }

    public int getNumClusters() {
        return numClusters;
    }

    public void setNumClusters(int numClusters) {
        this.numClusters = numClusters;
    }

    protected boolean parallelMode_ = false;

    /** number of clusters desired in clustering **/
    protected int numClusters = 2;

    private DataOnMemoryListContainer<DataInstance> batch_;
    private int windowCounter;
    private Clustering sourceClustering = null;

    private DAG dag = null;
    private BayesianNetwork bnModel_;
    private ParameterLearningAlgorithm parameterLearningAlgorithm_;
    InferenceAlgorithm predictions_;
    Attributes attributes_;
    Variable clusterVar_;

    @Override
    public void resetLearningImpl() {
        batch_ = null;
        windowCounter = 0;
    }


    @Override
    public void trainOnInstanceImpl(Instance instance) {
        if(batch_ == null){
            setParallelMode_(parallelModeOption.isSet());
            setNumClusters(numberClustersOption.getValue());

            attributes_ = Converter.convertAttributes(instance.enumerateAttributes(),instance.classAttribute());
            Variables modelHeader = new Variables(attributes_);
            clusterVar_ = modelHeader.newMultionomialVariable("clusterVar", getNumClusters());

            batch_ = new DataOnMemoryListContainer(attributes_);
            predictions_ = new VMP();
            predictions_.setSeed(this.randomSeed);


            dag = new DAG(modelHeader);

            /* Set DAG structure */

            /* *.- Add hidden cluster variable as parents of all predictive attributes */
            if (isParallelMode_()) {
                dag.getParentSets().parallelStream()
                        .filter(w -> w.getMainVar().getVarID() != clusterVar_.getVarID())
                        .forEach(w -> w.addParent(clusterVar_));

            }
            else {
                dag.getParentSets().stream()
                        .filter(w -> w.getMainVar().getVarID() != clusterVar_.getVarID())
                        .filter(w -> w.getMainVar().isObservable())
                        .forEach(w -> w.addParent(clusterVar_));
            }

            System.out.println(dag.toString());

            parameterLearningAlgorithm_ = new StreamingVariationalBayesVMP();
        }

        if(windowCounter >= timeWindowOption.getValue()){
            batch_ = new DataOnMemoryListContainer(attributes_);
            windowCounter = 0;
        }
        DataInstance dataInstance = new DataInstanceImpl(new DataRowWeka(instance, attributes_));
        windowCounter++;
        batch_.add(dataInstance);
    }

    @Override
    public Clustering getClusteringResult() {
        sourceClustering = new Clustering();

        Instances dataset = getDataset(attributes_.getNumberOfAttributes(), getNumClusters());
        Instances newInstances = new Instances(dataset);

        if(bnModel_==null) {
            //parameterLearningAlgorithm_.setParallelMode(isParallelMode_());
            parameterLearningAlgorithm_.setDAG(dag);
            parameterLearningAlgorithm_.initLearning();
            parameterLearningAlgorithm_.updateModel(batch_);
        }else {
            parameterLearningAlgorithm_.updateModel(batch_);
        }

        bnModel_ = parameterLearningAlgorithm_.getLearntBayesianNetwork();
        predictions_.setModel(bnModel_);


        for (DataInstance dataInstance: batch_) {
            this.predictions_.setEvidence(dataInstance);
            this.predictions_.runInference();
            Multinomial multinomial = this.predictions_.getPosterior(clusterVar_);

            double[] results =  multinomial.getProbabilities();

            int cnum = IntStream.rangeClosed(0, getNumClusters() - 1).reduce((a, b) -> (results[a] > results[b])? a: b).getAsInt();

            double[] attValues = dataInstance.getAttributes().getList().stream().mapToDouble(att -> dataInstance.getValue(att)).toArray();
            Instance newInst = new DenseInstance(1.0, attValues);
            newInst.insertAttributeAt(attributes_.getNumberOfAttributes());
            newInst.setDataset(dataset);
            newInst.setClassValue(cnum);
            newInstances.add(newInst);
        }
        clustering = new Clustering(newInstances);

        return sourceClustering;

    }

    private Instances getDataset(int numdim, int numclass) {
        FastVector attributes = new FastVector();
        for (int i = 0; i < numdim; i++) {
            attributes.addElement(new Attribute("att" + (i + 1)));
        }

        if(numclass > 0){
            FastVector classLabels = new FastVector();
            for (int i = 0; i < numclass; i++) {
                classLabels.addElement("class" + (i + 1));
            }
            attributes.addElement(new Attribute("class", classLabels));
        }

        Instances myDataset = new Instances("horizion", attributes, 0);
        if(numclass > 0){
            myDataset.setClassIndex(myDataset.numAttributes() - 1);
        }

        return myDataset;
    }

    @Override
    public boolean  keepClassLabel(){
        return true;
    }


    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getModelDescription(StringBuilder stringBuilder, int i) {

    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        return null;
    }

}
