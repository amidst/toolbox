package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
//import eu.amidst.corestatic.variables.StaticVariables;

import javax.naming.OperationNotSupportedException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.DoubleStream;

/**
 * <h2>This class implements an univariate mixture of Gaussian distributions.</h2>
 *
 * @author Darío Ramos
 * @version 0.3
 * @since 2015-05-12
 */
public abstract class GaussianMixture extends UnivariateDistribution {

    private static final long serialVersionUID = 3362372347079403247L;

    /**
     * List of Normal distributions in the linear combination
     */
    private List<Normal> terms;

    /**
     * List of coefficients in the linear combination
     */
    private double[] coefficients;




    /**
     * The class constructor.
     * @param var1 The variable of the distribution.
     */
    public GaussianMixture(Variable var1) {
        this.var=var1;

        terms = new ArrayList<Normal>();
        Normal aux=new Normal(var1);
        aux.setMean(0);
        aux.setVariance(1);
        terms.add(aux);

        coefficients=new double[1];
        coefficients[0]=1;
    }

    /**
     * The class constructor.
     * @param list A list of Normal distributions
     * @param coeffs A list of coefficients (weights) in the mixture
     */
    public GaussianMixture(List<Normal> list, double[] coeffs) {
        this.var=list.get(0).getVariable();
        this.terms=list;
        this.coefficients=coeffs;
    }


    /**
     * The class constructor.
     * @param params The list of parameters (must be of length multiple of 3)
     */
    public GaussianMixture(double[] params) {

        this.setParameters(params);
    }

    public double getLogProbability(double value) {
        double prob=0;

        int index=0;
        for(Normal normal : this.terms) {
            prob=prob + this.coefficients[index] * normal.getProbability(value);
            index++;
        }
        return Math.log(prob);
    }

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
        double suma=aux.sum();
        aux = Arrays.stream(this.coefficients);
        this.coefficients = aux.map(x -> x/suma).toArray();
        return Double.NaN;*/
    }


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

    public int getNumberOfParameters() {
        return 3*coefficients.length;
    };

    public Variable getVariable() {
        return this.var;
    }

    public String label(){
        return "Gaussian Mixture";
    }

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
        double suma=aux.sum();
        aux = Arrays.stream(this.coefficients);
        this.coefficients = aux.map(x -> x/suma).toArray();

        //System.out.println(coefficients);
        //this.coefficients = this.coefficients / .map().sum();

    }

    public void randomInitialization(Random random) {
        int numTerms = random.nextInt(5);
        this.randomInitialization(random,numTerms);

    }

    public boolean equalDist(Distribution dist, double threshold) {
        if (dist.getClass().getName().equals("eu.amidst.core.distribution.GaussianMixture"))
            return this.equalDist((Normal)dist,threshold);
        return false;
    }

    @Override
    public abstract <E extends EF_UnivariateDistribution> E toEFUnivariateDistribution();

    public String toString() {
        String text = "";
        for(int k=0; k<coefficients.length; k++) {
            text = text + String.format("%.3f", coefficients[k]) + " " + terms.get(k).toString();
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
