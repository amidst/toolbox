package eu.amidst.flinklink.core.inference;

import eu.amidst.core.distribution.GaussianMixture;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.ImportanceSamplingCLG_new;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.RobustOperations;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.log4j.BasicConfigurator;

import java.io.InvalidObjectException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Created by dario on 27/4/16.
 */
public class DistributedImportanceSamplingCLG {

    private static final long serialVersionUID = 369279637623979372L;

    private BayesianNetwork model;
    private BayesianNetwork samplingModel;
    private boolean sameSamplingModel;


    private int seed = 0;
    private int sampleSize = 10000;

    private List<Variable> variablesOfInterest;

    private Assignment evidence;

    private boolean useGaussianMixtures = false;

    private double logProbOfEvidence;

    private List<Tuple3<List<UnivariateDistribution>,Double, Double>> posteriors;


    private double mixtureOfGaussiansInitialVariance = 50;
    private double mixtureOfGaussiansNoveltyRate = 0.0001;

    private int numberOfCoresToUse = -1;

    Function<Double,Double> queryingFunction;
    Variable queryingVariable;
    double queryResult;

    public void setModel(BayesianNetwork model) {

        if(model.getVariables().getListOfVariables().stream().anyMatch(variable -> !(variable.isMultinomial() || variable.isNormal()))) {
            throw new UnsupportedOperationException("All the Variable objects must be Multinomial or Normal in a CLG network");
        }

        this.model = model;
        this.samplingModel = model;
        this.sameSamplingModel = true;
        this.variablesOfInterest = model.getVariables().getListOfVariables();
    }

    public void setSamplingModel(BayesianNetwork samplingModel) {

        if(this.model.getVariables().getListOfVariables().stream().anyMatch(variable -> !(variable.isMultinomial() || variable.isNormal()))) {
            throw new UnsupportedOperationException("All the Variable objects must be Multinomial or Normal in a CLG network");
        }

        this.samplingModel = samplingModel;
        this.sameSamplingModel = samplingModel.equalBNs(this.model,1E-10);
        this.variablesOfInterest = model.getVariables().getListOfVariables();
    }

    public void setEvidence(Assignment evidence) {
        this.evidence = evidence;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }


    public void setSeed(int seed) {
        this.seed = seed;
    }


    public void setGaussianMixturePosteriors(boolean useGaussianMixtures) {
        this.useGaussianMixtures = useGaussianMixtures;
    }


    public void setVariablesOfInterest(List<Variable> variablesAPosterior) throws InvalidObjectException {

        if (variablesAPosterior.stream().anyMatch(variable -> !model.getVariables().getListOfVariables().contains(variable))) {
            throw new InvalidObjectException("All variables in the list must be included in the Bayesian network");
        }
        this.variablesOfInterest = variablesAPosterior;

    }


    public void setMixtureOfGaussiansInitialVariance(double mixtureOfGaussiansInitialVariance) {
        this.mixtureOfGaussiansInitialVariance = mixtureOfGaussiansInitialVariance;
    }

    public void setMixtureOfGaussiansNoveltyRate(double mixtureOfGaussiansNoveltyRate) {
        this.mixtureOfGaussiansNoveltyRate = mixtureOfGaussiansNoveltyRate;
    }


    public void setNumberOfCores(int numberOfCoresToUse) {
        this.numberOfCoresToUse = numberOfCoresToUse;
    }

    public void runInference() throws Exception {

        BasicConfigurator.configure();
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        int maxParallelism = env.getParallelism();

        int parallelism;
        if(this.numberOfCoresToUse != -1) {
            parallelism = this.numberOfCoresToUse;
        }
        else {
            parallelism = maxParallelism;
        }

        final int numberOfFlinkNodes = parallelism;
        env.setParallelism(numberOfFlinkNodes);

        System.out.println("Flink parallel nodes: " + numberOfFlinkNodes);

        posteriors = env.generateSequence(0,numberOfFlinkNodes-1)
                .map(new LocalImportanceSampling(model, variablesOfInterest, evidence, sampleSize/numberOfFlinkNodes, seed, useGaussianMixtures, mixtureOfGaussiansInitialVariance, mixtureOfGaussiansNoveltyRate, queryingVariable, queryingFunction))
                .collect();

        this.logProbOfEvidence = posteriors.stream().mapToDouble(aux -> aux.getField(1)).reduce(RobustOperations::robustSumOfLogarithms).getAsDouble() - Math.log(numberOfFlinkNodes);

        this.queryResult = posteriors.stream().mapToDouble(aux -> aux.getField(2)).average().getAsDouble();

    }

    static class LocalImportanceSampling implements MapFunction<Long, Tuple3<List<UnivariateDistribution>, Double, Double>> {

        private BayesianNetwork model;
        private List<Variable> variablesAPosteriori;
        private Assignment evidence;
        private int seed;
        private int numberOfSamples;
        private boolean useGaussianMixtures;
        private double mixtureOfGaussiansInitialVariance;
        private double mixtureOfGaussiansNoveltyRate;
        private Variable queryingVariable;
        private Function<Double,Double> queryingFunction;

        LocalImportanceSampling(BayesianNetwork model, List<Variable> variablesAPosteriori, Assignment evidence, int numberOfSamples, int seed, boolean useGaussianMixtures, double GMInitialVariance, double GMNoveltyRate, Variable queryingVariable, Function<Double,Double> queryingFunction) {
            this.model = model;
            this.variablesAPosteriori = variablesAPosteriori;
            this.evidence = evidence;
            this.numberOfSamples = numberOfSamples;
            this.seed = seed;
            this.useGaussianMixtures = useGaussianMixtures;
            this.mixtureOfGaussiansInitialVariance = GMInitialVariance;
            this.mixtureOfGaussiansNoveltyRate = GMNoveltyRate;
            this.queryingVariable=queryingVariable;
            this.queryingFunction=queryingFunction;
        }

        @Override
        public Tuple3<List<UnivariateDistribution>, Double, Double> map(Long value) throws Exception {

            ImportanceSamplingCLG_new localImportanceSampling = new ImportanceSamplingCLG_new();
            localImportanceSampling.setModel(model);
            localImportanceSampling.setSeed(seed + value.intValue());
            localImportanceSampling.setSampleSize(numberOfSamples);

            localImportanceSampling.setVariablesOfInterest(variablesAPosteriori);

            localImportanceSampling.setGaussianMixturePosteriors(useGaussianMixtures);
            localImportanceSampling.setMixtureOfGaussiansInitialVariance(mixtureOfGaussiansInitialVariance);
            localImportanceSampling.setMixtureOfGaussiansNoveltyRate(mixtureOfGaussiansNoveltyRate);
            localImportanceSampling.setParallelMode(true);

            localImportanceSampling.setEvidence(evidence);

            localImportanceSampling.setQuery(queryingVariable,queryingFunction);

            localImportanceSampling.runInference();

            double queryResult = localImportanceSampling.getQueryResult();

            List<UnivariateDistribution> listPosteriors = new ArrayList<>();
            variablesAPosteriori.forEach(var -> listPosteriors.add(localImportanceSampling.getPosterior(var)));

            //double logProbEvidence = localImportanceSampling.getLogProbabilityOfEvidence();
            double logSumWeights = localImportanceSampling.getLogSumWeights();

            Tuple3<List<UnivariateDistribution>, Double, Double> result = new Tuple3<>();
            result.setField(listPosteriors,0);
            result.setField(logSumWeights,1);
            result.setField(queryResult,2);

            return result;
        }
    }

    public void setQuery(Variable var, Function<Double,Double> function) {

        if( !this.variablesOfInterest.contains(var) ) {
            throw new UnsupportedOperationException("The querying variable must be a variable of interest");
        }
        this.queryingVariable = var;
        this.queryingFunction = (Function<Double,Double> & Serializable)function;
    }

    public double getQueryResult() {
        return queryResult;
    }

    public <E extends UnivariateDistribution> E getPosterior(Variable variable) {

        //int parallelNodes = posteriors.size();

        if (variable.isMultinomial()) {

            double [] logParameters = new double[variable.getNumberOfStates()];
            UnivariateDistribution posteriorVar;

            for (int i = 0; i < posteriors.size(); i++) {
                List<UnivariateDistribution> posteriors_i = posteriors.get(i).getField(0);
                posteriorVar = posteriors_i.stream().filter(posterior -> posterior.getVariable().equals(variable)).findFirst().get();
                double logWeight_i = posteriors.get(i).getField(1);

                for (int j = 0; j < logParameters.length; j++) {
                    logParameters[j] = RobustOperations.robustSumOfLogarithmsWithZeros(logParameters[j], logWeight_i + Math.log(posteriorVar.getParameters()[j]));
                }
            }
            Multinomial finalDistribution = new Multinomial(variable);
            double [] parameters = Arrays.stream(logParameters).map(a -> Math.exp(a)).toArray();
            finalDistribution.setProbabilities(Utils.normalize(parameters));

            return ((E)finalDistribution);
        }
        else if(variable.isNormal()) {
            if(!useGaussianMixtures) {

                double logSumWeights = posteriors.stream().map(aux -> ((Double)aux.getField(1)).doubleValue() ).reduce(RobustOperations::robustSumOfLogarithms).get();
                double[] parameters = new double[2];
                UnivariateDistribution posteriorVar;


                for (int i = 0; i < posteriors.size(); i++) {
                    List<UnivariateDistribution> posteriors_i = posteriors.get(i).getField(0);
                    posteriorVar = posteriors_i.stream().filter(posterior -> posterior.getVariable().equals(variable)).findFirst().get();

                    double logWeight_i = posteriors.get(i).getField(1);

                    double meanEstimate_i = posteriorVar.getParameters()[0];
                    double varEstimate_i = posteriorVar.getParameters()[1];

                    parameters[0] = parameters[0] + Math.exp(logWeight_i-logSumWeights) * meanEstimate_i;
                    parameters[1] = parameters[1] + Math.exp(logWeight_i-logSumWeights) * (varEstimate_i + Math.pow(meanEstimate_i,2));

                    //System.out.println(meanEstimate_i + ", " + varEstimate_i + ", " + logWeight_i + ", " + Math.exp(logWeight_i-logSumWeights));

                }

//                for (int i = 0; i < posteriors.size(); i++) {
//                    List<UnivariateDistribution> posteriors_i = posteriors.get(i).getField(0);
//                    posteriorVar = posteriors_i.stream().filter(posterior -> posterior.getVariable().equals(variable)).findFirst().get();
//                    double logWeight_i = posteriors.get(i).getField(1);
//
//                    // CHECK SIGNS OF MEANS
//                    if( posteriorVar.getParameters()[0]>=0 ) {
//                        if(logSumPositiveTerms==0) {
//                            logSumPositiveTerms = logWeight_i + Math.log(posteriorVar.getParameters()[0]);
//                        }
//                        else {
//                            logSumPositiveTerms = RobustOperations.robustSumOfLogarithms(logSumPositiveTerms, logWeight_i + Math.log(posteriorVar.getParameters()[0]));
//                        }
//                    }
//                    else {
//                        if (logSumNegativeTerms==0) {
//                            logSumNegativeTerms = logWeight_i + Math.log(-posteriorVar.getParameters()[0]);
//                        }
//                        else {
//                            logSumNegativeTerms = RobustOperations.robustSumOfLogarithms(logSumNegativeTerms, logWeight_i + Math.log(-posteriorVar.getParameters()[0]));
//                        }
//                    }
////                    parameters[0] = RobustOperations.robustSumOfLogarithms(parameters[0], logWeight_i + Math.log(posteriorVar.getParameters()[0]));
//                    parameters[1] = RobustOperations.robustSumOfLogarithms(parameters[1], logWeight_i + Math.log(posteriorVar.getParameters()[1]));
//
//                }
//
//                parameters[0] = Math.exp( -logSumWeights + ((logSumPositiveTerms>logSumNegativeTerms) ? (RobustOperations.robustDifferenceOfLogarithms(logSumPositiveTerms,logSumNegativeTerms)) : RobustOperations.robustDifferenceOfLogarithms(logSumNegativeTerms,logSumPositiveTerms)));
//                parameters[1] = Math.exp(parameters[1] - logSumWeights);

                Normal finalDistribution = new Normal(variable);

                double finalMeanEstimate = parameters[0];
                double finalVarEstimate = parameters[1] - Math.pow(finalMeanEstimate,2);
                finalDistribution.setMean(finalMeanEstimate);
                finalDistribution.setVariance(finalVarEstimate);

                return ((E) finalDistribution);
            }
            else {

                GaussianMixture finalDistribution = new GaussianMixture(variable);

                UnivariateDistribution posteriorVar;

                for (int i = 0; i < posteriors.size(); i++) {
                    List<UnivariateDistribution> posteriors_i = posteriors.get(i).getField(0);
                    posteriorVar = posteriors_i.stream().filter(posterior -> posterior.getVariable().equals(variable)).findFirst().get();
                    double logWeight_i = posteriors.get(i).getField(1);

                    GaussianMixture posteriorDistr = ((GaussianMixture)posteriorVar);

                    double [] parametersPosterior = posteriorDistr.getParameters();
                    for (int j = 0; j < (parametersPosterior.length)/3; j++) {
                        double weightThisTerm = parametersPosterior[3*j+0];
                        double meanThisTerm =   parametersPosterior[3*j+1];
                        double varThisTerm =    parametersPosterior[3*j+2];

                        double relativeWeight = Math.exp(logWeight_i - logProbOfEvidence);
                        double newWeight = relativeWeight * weightThisTerm;

                        if(newWeight > 0.00001) {
                            Normal normal = new Normal(variable);
                            normal.setMean(meanThisTerm);
                            normal.setVariance(varThisTerm);
                            finalDistribution.addTerm(normal, newWeight);
                        }
                    }
                }

                finalDistribution.normalizeWeights();
                finalDistribution.filterTerms(0.0001);
                finalDistribution.sortByMean();
                finalDistribution.mergeTerms(0.1);
                return ((E) finalDistribution);
            }
        }
        else {
            throw new UnsupportedOperationException("Invalid variable type in DistributedImportanceSamplingCLG");
        }
    }

//    private Tuple2<UnivariateDistribution,Double> reduceMultinomial(Tuple2<UnivariateDistribution,Double> distr1, Tuple2<UnivariateDistribution,Double> distr2) {
//
//        Multinomial multinomialDistr1 = ((Multinomial)distr1.getField(0));
//        Multinomial multinomialDistr2 = ((Multinomial)distr2.getField(0));
//
//        double weight1 = distr1.getField(1);
//        double weight2 = distr2.getField(1);
//
//        Variable variable = multinomialDistr1.getVariable();
//
//        double [] parametersDistr1 = multinomialDistr1.getParameters();
//        double [] parametersDistr2 = multinomialDistr2.getParameters();
//
//
//        Multinomial finalDistribution = multinomialDistr1.toEFUnivariateDistribution().toUnivariateDistribution();
//        finalDistribution.setProbabilities(Utils.normalize(finalDistribution.getParameters()));
//
//        double finalWeight = 0;
//
//        return new Tuple2<>(finalDistribution,finalWeight);
//    }
//
//    private Tuple2<UnivariateDistribution,Double> reduceNormal(Tuple2<UnivariateDistribution,Double> distr1, Tuple2<UnivariateDistribution,Double> distr2) {
////        double [] paramDistr1 = distr1.getLogParameters();
////        double [] paramDistr2 = distr2.getLogParameters();
////        double [] paramNewDistr = new double[5];
////
////        for (int i = 0; i < paramDistr1.length; i++) {
////            paramNewDistr[i] = RobustOperations.robustSumOfLogarithms(paramDistr1[i],paramDistr2[i]);
////        }
////
////        StreamingUpdateableRobustNormal newDistr = new StreamingUpdateableRobustNormal(distr1.getNormal().getVariable());
////        newDistr.setLogParameters(paramNewDistr);
////        return newDistr;
//        return distr1;
//    }
//
//    private Tuple2<UnivariateDistribution,Double> reduceGaussianMixture(Tuple2<UnivariateDistribution,Double> distr1, Tuple2<UnivariateDistribution,Double> distr2) {
//
////        double[] paramDistr1 = distr1.getGaussianMixture().getParameters();
////        double[] paramDistr2 = distr2.getGaussianMixture().getParameters();
////
////        StreamingUpdateableGaussianMixture newDistr = new StreamingUpdateableGaussianMixture();
////
////        double logWeightDistr1 = distr1.getLogSumWeights();
////        double logWeightDistr2 = distr2.getLogSumWeights();
////
////        double logSumWeights = RobustOperations.robustSumOfLogarithms(logWeightDistr1,logWeightDistr2);
////
////        for (int i = 0; i < (paramDistr1.length)/3; i++) {
////            double weightThisTerm = paramDistr1[3*i+0];
////            double meanThisTerm =   paramDistr1[3*i+1];
////            double varThisTerm =    paramDistr1[3*i+2];
////
////            double relativeWeight = Math.exp(logWeightDistr1 - logSumWeights);
////            double newWeight = relativeWeight * weightThisTerm;
////
////            if(newWeight > 0.0001) {
////                Normal normal = new Normal(distr1.getGaussianMixture().getVariable());
////                normal.setMean(meanThisTerm);
////                normal.setVariance(varThisTerm);
////                newDistr.add(newWeight, normal);
////            }
////        }
////
////        for (int i = 0; i < (paramDistr2.length)/3; i++) {
////            double weightThisTerm = paramDistr2[3*i+0];
////            double meanThisTerm =   paramDistr2[3*i+1];
////            double varThisTerm =    paramDistr2[3*i+2];
////
////            double relativeWeight = Math.exp(logWeightDistr2 - logSumWeights);
////            double newWeight = relativeWeight * weightThisTerm;
////
////            if(newWeight > 0.0001) {
////                Normal normal = new Normal(distr1.getGaussianMixture().getVariable());
////                normal.setMean(meanThisTerm);
////                normal.setVariance(varThisTerm);
////                newDistr.add(newWeight, normal);
////            }
////        }
//
//        return distr1;
//    }



    public static void main(String[] args) throws Exception {

        // Bayesian network model
        BayesianNetworkGenerator.setNumberOfMultinomialVars(8, 2);
        BayesianNetworkGenerator.setNumberOfGaussianVars(30);
        BayesianNetwork model = BayesianNetworkGenerator.generateNaiveBayes(2);
        System.out.println(model);

        DistributedImportanceSamplingCLG distributedImportanceSampling =  new DistributedImportanceSamplingCLG();
        distributedImportanceSampling.setModel(model);

        // Variables for querying the a posteriori distribution

        Variable classVar = model.getVariables().getVariableByName("ClassVar");
        Variable discreteVar1 = model.getVariables().getVariableByName("DiscreteVar1");

        List<Variable> variablesAPosteriori = new ArrayList<>(1);
        variablesAPosteriori.add(classVar);
        variablesAPosteriori.add(discreteVar1);

        distributedImportanceSampling.setVariablesOfInterest(variablesAPosteriori);

        // Evidence
        Assignment evidence = new HashMapAssignment(2);
        Variable discreteVar0 = model.getVariables().getVariableByName("DiscreteVar0");
        Variable gaussianVar0 = model.getVariables().getVariableByName("GaussianVar0");

        evidence.setValue(discreteVar0,0);
        evidence.setValue(gaussianVar0,-6);

        distributedImportanceSampling.setEvidence(evidence);


        // Set parameters and run inference
        distributedImportanceSampling.setSeed(28235);
        distributedImportanceSampling.setSampleSize(50000);

        distributedImportanceSampling.runInference();

        // Show results
        System.out.println(distributedImportanceSampling.getPosterior(classVar));
        System.out.println(distributedImportanceSampling.getPosterior(discreteVar1));


    }
}
