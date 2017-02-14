/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_Normal;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.DoubleStream;

//import eu.amidst.corestatic.variables.StaticVariables;

/**
 * This class extends the abstract class {@link UnivariateDistribution} and defines the Gaussian Mixture distribution.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 *
 */
public class GaussianMixture extends UnivariateDistribution {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 3362372347079403247L;

    /** Represents the list of {@link Normal} distributions in the linear combination. */
    private List<Normal> terms;

    /** Represents the set of coefficients in the linear combination. */
    private double[] coefficients;

    private class WeightedTerm {
        private double weight;
        private Normal normal;

        public WeightedTerm(double coeff, Normal normal) {
            this.weight =coeff;
            this.normal=normal;
        }

        public double getWeight() {
            return weight;
        }

        public Normal getNormal() {
            return normal;
        }

        public double getMean() {
            return normal.getMean();
        }

        public double getVar() {
            return normal.getVariance();
        }

        public void setNormal(Normal normal) {
            this.normal = normal;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }
    }

    /**
     * Creates a new GaussianMixture distribution for a given variable.
     * @param var1 a {@link Variable} object.
     */
    public GaussianMixture(Variable var1) {
        this.var=var1;

        terms = new ArrayList<Normal>();
//        Normal aux=new Normal(var1);
//        aux.setMean(0);
//        aux.setVariance(1);
//        terms.add(aux);

//        coefficients=new double[1];
//        coefficients[0]=1;
    }

    /**
     * Creates a new GaussianMixture distribution given a list of {@link Normal} distributions and a set of coefficients.
     * @param list a list of {@link Normal} distributions.
     * @param coeffs a set of coefficients (i.e., weights).
     */
    public GaussianMixture(List<Normal> list, double[] coeffs) {
        this.var=list.get(0).getVariable();
        this.terms=list;
        this.coefficients=coeffs;
    }

    /**
     * Creates a new GaussianMixture distribution given a set of parameters.
     * @param params a list of parameters (must be of length multiple of 3).
     */
    public GaussianMixture(double[] params) {
        terms = new ArrayList<Normal>();
        this.setParameters(params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GaussianMixture deepCopy(Variable variable) {
        GaussianMixture copy = new GaussianMixture(variable);
        copy.setParameters(this.getParameters());
        return copy;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EF_Normal toEFUnivariateDistribution() {
        throw new UnsupportedOperationException("Method not supported for a GaussianMixture object");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogProbability(double value) {
        double prob=0;

        int index=0;
        for(Normal normal : this.terms) {
            prob=prob + this.coefficients[index] * normal.getProbability(value);
            index++;
        }
        return Math.log(prob);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double sample(Random rand) {

        //int term = rand.nextInt(this.coefficients.length);
        double prob = rand.nextDouble();

        double sumcoefs=0;
        int term = 0;
        while(prob>=sumcoefs) {
            term++;
            sumcoefs=sumcoefs+this.coefficients[term];

        }

        return this.terms.get(term).sample(rand);
        /*
        DoubleStream aux = Arrays.stream(this.coefficients);
        aux.
        double suma=aux.sumNonStateless();
        aux = Arrays.stream(this.coefficients);
        this.coefficients = aux.map(x -> x/suma).toArray();
        return Double.NaN;*/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getParameters() {

        int numParameters=3*coefficients.length;
        double[] parameters = new double[numParameters];

        int index=0;
        for(Normal normal : this.terms) {
            parameters[3*index]=this.coefficients[index];
            parameters[3*index+1]=normal.getMean();
            parameters[3*index+2]=normal.getVariance();
            index++;
        }
        return parameters;
    }

    /**
     * Sets the parameters of this GaussianMixture.
     * @param params an Array of doubles containing the GaussianMixture parameters.
     */
    public void setParameters(double[] params) {
        if (params.length % 3!=0) {
            throw new UnsupportedOperationException("The number of parameters for the Gaussian mixture is not valid");
        }
        else {
            int numTerms = params.length / 3;

            this.coefficients = new double[numTerms];
            this.terms = new ArrayList<>(numTerms);


            for (int index = 0; index<numTerms; index++) {
                this.coefficients[index] = params[3 * index];
                Normal aux=new Normal(this.var);
                aux.setMean(params[3 * index + 1]);
                aux.setVariance(params[3 * index + 2]);
                this.terms.add(aux);
            }

        }
    };


    public void addTerm(Normal normal, double weight) {
        if(this.coefficients==null) {
            this.coefficients = new double[1];
            this.coefficients[0] = weight;
        }
        else {
            double[] newCoefficients = Arrays.copyOf(this.coefficients, this.coefficients.length + 1);
            newCoefficients[newCoefficients.length - 1] = weight;
            this.coefficients = newCoefficients;
        }
        this.terms.add(normal);
    }

    public void normalizeWeights() {
        double sumWeights = Arrays.stream(this.coefficients).sum();
        for (int i = 0; i < this.coefficients.length; i++) {
            this.coefficients[i]=this.coefficients[i]/sumWeights;
        }
    }

    public void filterTerms(double minWeightThreshold) {
        List<Normal> newListOfTerms = new ArrayList<>();
        List<Double> newListOfCoefficients = new ArrayList<>();;

        for (int i = 0; i < this.coefficients.length; i++) {
            if (this.coefficients[i] >= minWeightThreshold) {
                newListOfTerms.add( this.terms.get(i) );
                newListOfCoefficients.add( this.coefficients[i] );
            }
        }

        this.coefficients = newListOfCoefficients.stream().mapToDouble(Double::doubleValue).toArray();
        this.terms = newListOfTerms;

        this.normalizeWeights();
    }

    public void sortByMean() {
        List<WeightedTerm> termList = new ArrayList<>();
        for (int i = 0; i < coefficients.length; i++) {
            termList.add(new WeightedTerm(coefficients[i],terms.get(i)));
        }

        termList.sort((w1,w2) -> w1.getMean() > w2.getMean() ? 1 : -1);

        List<Normal> newTerms = new ArrayList<>();
        double [] newCoeffs = new double[coefficients.length];

        for (int i = 0; i < coefficients.length; i++) {
            newTerms.add(termList.get(i).getNormal());
            newCoeffs[i]=termList.get(i).getWeight();
        }

        this.terms = newTerms;
        this.coefficients = newCoeffs;
    }

    public void sortByWeight() {
        List<WeightedTerm> termList = new ArrayList<>();
        for (int i = 0; i < coefficients.length; i++) {
            termList.add(new WeightedTerm(coefficients[i],terms.get(i)));
        }

        termList.sort((w1,w2) -> w1.getWeight() > w2.getWeight() ? -1 : 1);

        List<Normal> newTerms = new ArrayList<>();
        double [] newCoeffs = new double[coefficients.length];

        for (int i = 0; i < coefficients.length; i++) {
            newTerms.add(termList.get(i).getNormal());
            newCoeffs[i]=termList.get(i).getWeight();
        }

        this.terms = newTerms;
        this.coefficients = newCoeffs;
    }

    public void mergeTerms(double threshold) {
        this.sortByMean();

        List<WeightedTerm> termList = new ArrayList<>();
        for (int i = 0; i < coefficients.length; i++) {
            termList.add(new WeightedTerm(coefficients[i],terms.get(i)));
        }

        int i=0;
        while (i<termList.size()) {
            Normal distrP = termList.get(i).getNormal();
            boolean ended=false;
            while(!ended) {
                ended=true;
                for (int j = i + 1; j < termList.size(); j++) {
                    Normal distrQ = termList.get(j).getNormal();
                    double KL_distance = distrP.toEFUnivariateDistribution().kl(distrQ.toEFUnivariateDistribution().getNaturalParameters(), 0);
                    if (KL_distance < threshold) {
                        ended = false;

                        double mean1 = distrP.getMean();
                        double mean2 = distrQ.getMean();

                        double var1 = distrP.getVariance();
                        double var2 = distrQ.getVariance();

                        double newMean = (mean1 + mean2)/2;
                        double newVar = (var1+Math.pow(mean1,2) + var2+Math.pow(mean2,2))/2 - Math.pow(newMean,2);

                        double newWeight = termList.get(i).getWeight() + termList.get(j).getWeight();
                        Normal newTerm = new Normal(distrP.getVariable());
                        newTerm.setMean(newMean);
                        newTerm.setVariance(newVar);

                        termList.get(i).setNormal(newTerm);
                        termList.get(i).setWeight(newWeight);

                        termList.remove(j);
                    }
                }
            }
            i=i+1;
        }

        List<Normal> newTerms = new ArrayList<>();
        double [] newCoeffs = new double[termList.size()];

        for (int k = 0; k < termList.size(); k++) {
            newTerms.add(termList.get(k).getNormal());
            newCoeffs[k]=termList.get(k).getWeight();
        }

        this.terms = newTerms;
        this.coefficients = newCoeffs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfParameters() {
        return 3*coefficients.length;
    };

    public int getNumberOfComponents() {
        return coefficients.length;
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public Variable getVariable() {
        return this.var;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String label(){
        return "Gaussian Mixture";
    }

    /**
     * Randomly initializes this GaussianMixture for a given number of terms.
     * @param random a {@link java.util.Random} object.
     * @param numTerms a number of terms.
     */
    public void randomInitialization(Random random,int numTerms) {

        this.coefficients= new double[numTerms];
        this.terms = new ArrayList<>(numTerms);
        for(int k=0; k<numTerms; k++) {
            this.coefficients[k]=random.nextDouble();

            Normal aux=new Normal(this.var);
            aux.setMean(5*random.nextGaussian());
            aux.setVariance(random.nextDouble());

            this.terms.add(aux);

        };
        DoubleStream aux = Arrays.stream(this.coefficients);
        double suma = aux.sum();
        aux = Arrays.stream(this.coefficients);
        this.coefficients = aux.map(x -> x/suma).toArray();

        //System.out.println(coefficients);
        //this.coefficients = this.coefficients / .map().sumNonStateless();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialization(Random random) {
        int numTerms = random.nextInt(5);
        this.randomInitialization(random,numTerms);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist.getClass().getName().equals("eu.amidst.core.distribution.GaussianMixture"))
            return this.equalDist((Normal)dist,threshold);
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String text = "";
        for(int k=0; k<coefficients.length; k++) {
            text = text + String.format("%.10f", coefficients[k]) + " " + terms.get(k).toString();
            if (k<coefficients.length-1) {
                text = text + " + ";
            }
        }
        return text;
    }

    /*
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        double[] params = new double[] {0.1,0,1,0.9,-1,1.2};

        Random seed = new Random(1234);

        StaticVariables stvar = new StaticVariables();
        Variable var = stvar.newGaussianVariable("Mixture");

        GaussianMixture gm = new GaussianMixture(var);
        System.out.println("Label: " + gm.label());
        System.out.println("Initial Gaussian Mixture: ");
        System.out.println(gm.toString());

        gm.randomInitialization(seed,4);
        System.out.println("Random Gaussian Mixture: ");
        System.out.println(gm.toString());

        System.out.println("Number of parameters: " + gm.getNumberOfParameters());
        System.out.println("Array of parameters: " + Arrays.toString(gm.getParameters()));

        gm.setParameters(params);
        System.out.println("Gaussian Mixture given by parameters: ");
        System.out.println(gm.toString());

        double sample = gm.sample(new Random(123456));
        System.out.println("Sample: " + sample);
    }
    */
}
