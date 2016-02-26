package eu.amidst.modelExperiments;

import com.sun.javafx.binding.StringFormatter;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataFolderReader;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.GaussianMixture;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.MissingAssignment;
import eu.amidst.core.variables.Variable;
import scala.runtime.StringFormat;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Instances;

import java.io.IOException;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by dario on 25/02/16.
 */
public class ObtainGaussianMixtures {

    public static void main(String[] args) throws IOException, ClassNotFoundException, ParseException {

        //String networkFile = "/Users/dario/Desktop/UAI/networks/MixtureSVI_4129_100_1_2000_0_41293_0.55_.bn";
        String networkFile2 = "/Users/dario/Desktop/UAI/networks/MixtureVMP_550_100_1_100_1_2000_0_.bn";

        BayesianNetwork model = BayesianNetworkLoader.loadFromFile(networkFile2);

        String dataFile = "/Users/dario/Desktop/UAI/data/totalWeka-ContinuousReducedFolderTrain.arff";

        System.out.println(model.toString());

        Variable classVariable = model.getVariables().getVariableByName("Default");
        Variable globalHidden = model.getVariables().getVariableByName("GlobalHidden");

        System.out.println("\nUNIVARIATE GAUSSIAN MIXTURES FOR OBSERVABLE VARIABLES\n");

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(8);

        model.getVariables().getListOfVariables().forEach(variable -> {
            if(variable.isNormal()) {
                ConditionalDistribution varDistribution = model.getConditionalDistribution(variable);

                List<Variable> nonClassMultinomialParents = varDistribution.getConditioningVariables().stream().filter(variable1 -> !variable1.equals(classVariable)).collect(Collectors.toList());
                int nStatesMultinomialParents = (int) Math.round(Math.exp(nonClassMultinomialParents.stream().mapToDouble(parent1 -> Math.log(parent1.getNumberOfStates())).sum()));


                // For class=0
                StringBuilder gaussianMixtureDistribution = new StringBuilder();
                gaussianMixtureDistribution.append("gaussianMixtureDensity <- function(x) { ");
                for(int m=0; m<nStatesMultinomialParents; m++) {
                    Assignment parentsConfiguration = MultinomialIndex.getVariableAssignmentFromIndex(nonClassMultinomialParents, m);
                    parentsConfiguration.setValue(classVariable,0);

                    Variable thirdParent = nonClassMultinomialParents.stream().filter(parent -> !parent.equals(globalHidden)).findFirst().get();

                    //double prob1 = model.getConditionalDistribution(classVariable).getLogProbability(parentsConfiguration);
                    double prob2 = model.getConditionalDistribution(globalHidden).getLogProbability(parentsConfiguration);
                    double prob3 = model.getConditionalDistribution(thirdParent).getLogProbability(parentsConfiguration);

                    double probParentAssignment = Math.exp(prob2 + prob3);

                    gaussianMixtureDistribution.append( nf.format(probParentAssignment) + " * ");
                    //gaussianMixtureDistribution.append( String.format("%.5f", probParentAssignment) + " x ");
                    //gaussianMixtureDistribution.append(varDistribution.getUnivariateDistribution(parentsConfiguration));
                    UnivariateDistribution normalComponent=varDistribution.getUnivariateDistribution(parentsConfiguration);
                    gaussianMixtureDistribution.append( "dnorm(x,mean="+normalComponent.getParameters()[0]+",sd=sqrt("+normalComponent.getParameters()[1]+"))");
                    gaussianMixtureDistribution.append(" + ");
                }
                gaussianMixtureDistribution.replace(gaussianMixtureDistribution.lastIndexOf(" + "),gaussianMixtureDistribution.lastIndexOf(" + ")+2,"");
                gaussianMixtureDistribution.append(" }");

                System.out.println("Variable " + variable.getName() + " for class = 0");
                System.out.println(gaussianMixtureDistribution.toString()+ "\n");


                List<Map<Double,UnivariateDistribution>> normalComponents = new ArrayList<>();

                // For class=1
                gaussianMixtureDistribution = new StringBuilder();
                gaussianMixtureDistribution.append("gaussianMixtureDensity <- function(x) { ");
                for(int m=0; m<nStatesMultinomialParents; m++) {
                    Assignment parentsConfiguration = MultinomialIndex.getVariableAssignmentFromIndex(nonClassMultinomialParents, m);
                    parentsConfiguration.setValue(classVariable,1);

                    Variable thirdParent = nonClassMultinomialParents.stream().filter(parent -> !parent.equals(globalHidden)).findFirst().get();

                    //double prob1 = model.getConditionalDistribution(classVariable).getLogProbability(parentsConfiguration);
                    double prob2 = model.getConditionalDistribution(globalHidden).getLogProbability(parentsConfiguration);
                    double prob3 = model.getConditionalDistribution(thirdParent).getLogProbability(parentsConfiguration);

                    double probParentAssignment = Math.exp(prob2 + prob3);

                    gaussianMixtureDistribution.append( nf.format(probParentAssignment) + " * ");
                    //gaussianMixtureDistribution.append( String.format("%.5f", probParentAssignment) + " x ");
                    //gaussianMixtureDistribution.append(varDistribution.getUnivariateDistribution(parentsConfiguration));

                    //probabilities.add(probParentAssignment);

                    UnivariateDistribution normalComponent=varDistribution.getUnivariateDistribution(parentsConfiguration);

                    Map<Double,UnivariateDistribution> thisComponent = new HashMap<Double, UnivariateDistribution>();
                    thisComponent.put(probParentAssignment,normalComponent);

                    normalComponents.add(thisComponent);

                    gaussianMixtureDistribution.append( "dnorm(x,mean="+normalComponent.getParameters()[0]+",sd=sqrt("+normalComponent.getParameters()[1]+"))");
                    gaussianMixtureDistribution.append(" + ");
                }
                gaussianMixtureDistribution.replace(gaussianMixtureDistribution.lastIndexOf(" + "),gaussianMixtureDistribution.lastIndexOf(" + ")+2,"");
                gaussianMixtureDistribution.append(" }");

                System.out.println("Variable " + variable.getName() + " for class = 1");
                System.out.println(gaussianMixtureDistribution.toString());


                ARFFDataFolderReader arffDataReader = new ARFFDataFolderReader();
                arffDataReader.loadFromFile(dataFile);
                double logLikelyhoodThisVariable = arffDataReader.stream().mapToDouble(dataRow -> {

                    Assignment variableValueThisRow = new HashMapAssignment();
                    variableValueThisRow.setValue(variable, dataRow.getValue(variable.getAttribute()));

                    double totalDensity =  normalComponents.stream().mapToDouble(normalComponent -> {
                        double weight = normalComponent.keySet().stream().findFirst().get();
                        UnivariateDistribution thisComponent = normalComponent.entrySet().stream().findFirst().get().getValue();
                        double density = thisComponent.getLogProbability(variableValueThisRow);
                        return weight * Math.exp(density);
                    }).sum();
                    return Double.isFinite(Math.log(totalDensity)) ? Math.log(totalDensity) : 0;
                }).sum();
                System.out.println("logLikelyhoodThisVar: " + logLikelyhoodThisVariable + "\n");

            }
        });

    }


}
