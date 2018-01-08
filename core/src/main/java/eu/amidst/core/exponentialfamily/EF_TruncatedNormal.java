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

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.Normal;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.RobustOperations;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * This class extends the abstract class {@link EF_UnivariateDistribution} and defines a Normal distribution in exponential family canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox, take a look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 */
public class EF_TruncatedNormal extends EF_TruncatedUnivariateDistribution {


    public static final int EXPECTED_MEAN = 0;
    public static final int EXPECTED_SQUARE = 1;

    /**
     * Creates a new EF_Normal distribution for a given variable.
     *
     * @param var1 a {@link Variable} object with a Normal distribution type.
     */
    public EF_TruncatedNormal(Variable var1) {
        this.parents = new ArrayList();

        this.var = var1;
        this.naturalParameters = new ArrayVector(2);
        this.momentParameters = new ArrayVector(2);

        this.setNaturalWithMeanPrecision(0,1);
    }


    public double getMean() {
        return this.naturalParameters.get(0)/this.getPrecision();
    }

    public double getPrecision() {
        return -2*this.naturalParameters.get(1);
    }

    public void setNaturalWithMeanPrecision(double mean, double precision) {
        this.naturalParameters.set(0, mean * precision);
        this.naturalParameters.set(1, -0.5 * precision);

        this.updateMomentFromNaturalParameters();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(double val) {
        return -0.5 * Math.log(2 * Math.PI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {
        double beta = (this.upperInterval - this.getMean()) * this.getPrecision();
        double alpha = (this.lowerInterval - this.getMean()) * this.getPrecision();
        double normalization_constant = 0.5 * org.apache.commons.math3.special.Erf.erf(alpha/Math.sqrt(2), beta/Math.sqrt(2));
        //normalization_constant = 0.5 * (1.0+org.apache.commons.math3.special.Erf.erf(beta/Math.sqrt(2))) - 0.5 * (1.0+org.apache.commons.math3.special.Erf.erf(alpha/Math.sqrt(2)));

        //System.out.println(0.5*(1.0+org.apache.commons.math3.special.Erf.erf(beta/Math.sqrt(2))));
        //System.out.println(0.5*(1.0+org.apache.commons.math3.special.Erf.erf(alpha/Math.sqrt(2))));

        //System.out.println(this.upperInterval + ", " + this.lowerInterval + "," + this.getMean() + ", " + this.getPrecision());
        //System.out.println(beta + ", " + alpha + ", " + normalization_constant);
        return -0.5 * Math.log(this.getPrecision()) + 0.5 * this.getPrecision() * Math.pow(this.getMean(), 2) - Math.log(normalization_constant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector createZeroVector() {
        return new ArrayVector(2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters createZeroNaturalParameters() {
        return new ArrayVector(2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics createInitSufficientStatistics() {

        SufficientStatistics vector = this.createZeroSufficientStatistics();

        double mean = 0;
        double meansquare = 0.1;

        vector.set(0, mean);
        vector.set(1, meansquare);

        return vector;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics vec = this.createZeroSufficientStatistics();
        vec.set(EXPECTED_MEAN, val);
        vec.set(EXPECTED_SQUARE, val * val);
        return vec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector getExpectedParameters() {
        Vector vec = new ArrayVector(1);
        vec.set(0, this.momentParameters.get(0));
        return vec;
    }

    @Override
    public void fixNumericalInstability() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution deepCopy(Variable var) {

        EF_TruncatedNormal copy = new EF_TruncatedNormal(var);
        copy.getNaturalParameters().copy(this.getNaturalParameters());
        copy.getMomentParameters().copy(this.getMomentParameters());

        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution deepCopy() {
        return this.deepCopy(this.var);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {

        double mean = random.nextGaussian() * 10;
        double var = random.nextDouble() * 10 + 1;

        this.setNaturalWithMeanPrecision(mean, 1 / var);

        this.fixNumericalInstability();

        this.updateMomentFromNaturalParameters();

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Normal toUnivariateDistribution() {

        Normal normal = new Normal(this.getVariable());

        normal.setMean(this.getMean());
        normal.setVariance(1/this.getPrecision());

        return normal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private double expectedXOfTruncatedNormal(double mu, double sigma, double lowerEndpoint, double upperEndpoint) {

//        if(mu <= -50) {
//            return (1-expectedXOfTruncatedNormal(1-mu, sigma, lowerEndpoint, upperEndpoint));
//        }
        double beta = (upperEndpoint - mu) / sigma;
        double alpha = (lowerEndpoint - mu) / sigma;

        AuxiliaryNormalDistribution auxNormal = new AuxiliaryNormalDistribution();

        double log_phi_alpha = Math.log(1.0/Math.sqrt(2.0*Math.PI)) - 0.5 * Math.pow(alpha,2);
        double log_phi_beta = Math.log(1.0/Math.sqrt(2.0*Math.PI)) - 0.5 * Math.pow(beta,2);
        double log_phi_diff = RobustOperations.robustDifferenceOfLogarithms(log_phi_alpha,log_phi_beta);

        double factor = (log_phi_beta>log_phi_alpha) ? -1.0 : 1.0;

        double log_PHI_alpha, log_PHI_beta, log_PHI_diff;

        if (alpha>10 && beta>10) {
            log_PHI_alpha = auxNormal.logpnorm(-alpha, 0, 1);
            log_PHI_beta = auxNormal.logpnorm(-beta, 0, 1);
            log_PHI_diff = RobustOperations.robustDifferenceOfLogarithms(log_PHI_alpha, log_PHI_beta);
        }
        else {
            log_PHI_alpha = auxNormal.logpnorm(alpha, 0, 1);
            log_PHI_beta = auxNormal.logpnorm(beta, 0, 1);
            log_PHI_diff = RobustOperations.robustDifferenceOfLogarithms(log_PHI_beta, log_PHI_alpha);
        }

        //double newExpectedX = this.getMean() + (aux.density(alpha) - aux.density(beta))/normalization_constant * 1/this.getPrecision();
        double newExpectedX = mu + factor * Math.exp( log_phi_diff - log_PHI_diff ) * sigma;

        if(newExpectedX>upperEndpoint) newExpectedX = upperEndpoint;
        if(newExpectedX<lowerEndpoint) newExpectedX = lowerEndpoint;

        return newExpectedX;
    }

    private double expectedXSquaredOfTruncatedNormal(double mu, double sigma, double lowerEndpoint, double upperEndpoint) {

        double beta = (upperEndpoint - mu) / sigma;
        double alpha = (lowerEndpoint - mu) / sigma;

        AuxiliaryNormalDistribution auxNormal = new AuxiliaryNormalDistribution();

        double log_phi_alpha = Math.log(1.0/Math.sqrt(2.0*Math.PI)) - 0.5 * Math.pow(alpha,2);
        double log_phi_beta = Math.log(1.0/Math.sqrt(2.0*Math.PI)) - 0.5 * Math.pow(beta,2);
        double log_phi_diff = RobustOperations.robustDifferenceOfLogarithms(log_phi_alpha,log_phi_beta);

        double factor = (log_phi_beta>log_phi_alpha) ? -1.0 : 1.0;

        double log_PHI_alpha, log_PHI_beta, log_PHI_diff;

        if (alpha>10 && beta >10) {
            log_PHI_alpha = auxNormal.logpnorm(-alpha, 0, 1);
            log_PHI_beta = auxNormal.logpnorm(-beta, 0, 1);
            log_PHI_diff = RobustOperations.robustDifferenceOfLogarithms(log_PHI_alpha, log_PHI_beta);
        }
        else {
            log_PHI_alpha = auxNormal.logpnorm(alpha, 0, 1);
            log_PHI_beta = auxNormal.logpnorm(beta, 0, 1);
            log_PHI_diff = RobustOperations.robustDifferenceOfLogarithms(log_PHI_beta, log_PHI_alpha);
        }

        double newExpectedX = mu + factor * Math.exp( log_phi_diff - log_PHI_diff ) * sigma;

        double varianceTerm1 =  alpha * Math.exp( log_phi_alpha-log_PHI_diff) - beta * Math.exp( log_phi_beta-log_PHI_diff);            // (alpha * aux.density(alpha) - beta * aux.density(beta))/normalization_constant;
        double varianceTerm2 =  Math.exp( 2*(log_phi_diff - log_PHI_diff) );                                                            // Math.pow( (aux.density(alpha) - aux.density(beta))/normalization_constant, 2);

        double newVariance = Math.pow(sigma,2) * (1.0 + varianceTerm1 - varianceTerm2);
        double newExpectedXSquared = newVariance + Math.pow(newExpectedX,2);

        double maxAbsEndpoints = Math.max(Math.abs(lowerEndpoint),Math.abs(upperEndpoint));
        if(newExpectedXSquared>Math.pow(maxAbsEndpoints,2)) newExpectedXSquared = Math.pow(maxAbsEndpoints,2);
        if(newExpectedXSquared<0) newExpectedXSquared = 0;

        return newExpectedXSquared;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {

        double mu = this.getMean();
        double sigma = Math.sqrt(1.0/this.getPrecision());


        double newExpectedX = expectedXOfTruncatedNormal(mu, sigma, lowerInterval, upperInterval);
        this.momentParameters.set(EXPECTED_MEAN, newExpectedX);

        double newExpectedXSquared = expectedXSquaredOfTruncatedNormal(mu, sigma, lowerInterval, upperInterval);

        //if (newExpectedXSquared <= 0)
        //    throw new IllegalStateException("Zero or Negative expected square value");

        //if (Double.isNaN(newExpectedXSquared))
        //    throw new IllegalStateException("NaN expected square value");

        this.momentParameters.set(EXPECTED_SQUARE, newExpectedXSquared);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        return this.getSufficientStatistics(data.getValue(this.var));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sizeOfSufficientStatistics() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return this.computeLogBaseMeasure(dataInstance.getValue(this.var));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables, String nameSuffix) {
        List<EF_ConditionalDistribution> conditionalDistributions = new ArrayList<>();

        Variable varNormalGamma = variables.newNormalGamma(this.var.getName()+"_NormalGamma_Parameter_"+nameSuffix+"_"+variables.getNumberOfVars());

        conditionalDistributions.add(varNormalGamma.getDistributionType().newEFUnivariateDistribution());


        EF_NormalGivenJointNormalGamma dist = new EF_NormalGivenJointNormalGamma(this.var,varNormalGamma);
        conditionalDistributions.add(dist);

        return conditionalDistributions;
    }

/*
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables, String nameSuffix) {
        List<EF_ConditionalDistribution> conditionalDistributions = new ArrayList<>();

        Variable varGamma = variables.newGammaParameter(this.var.getName()+"_Gamma_Parameter_"+nameSuffix+"_"+variables.getNumberOfVars());
        Variable normalMean = variables.newGaussianParameter(this.var.getName() + "_Mean_Parameter_"+nameSuffix+"_"+variables.getNumberOfVars());

        conditionalDistributions.add(varGamma.getDistributionType().newEFUnivariateDistribution());

        conditionalDistributions.add(normalMean.getDistributionType().newEFUnivariateDistribution());

        EF_NormalGivenIndependentNormalGamma dist = new EF_NormalGivenIndependentNormalGamma(this.var, normalMean, varGamma);
        conditionalDistributions.add(dist);

        return conditionalDistributions;
    }
*/

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        NaturalParameters out = new ArrayVector(2);
        out.copy(this.getNaturalParameters());
        return out;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public double kl(NaturalParameters naturalParameters, double logNormalizer) {
        double precisionQ = -2*naturalParameters.get(1);
        double meanQ = naturalParameters.get(0)/precisionQ;

        double kl =  0.5*Math.log(this.getPrecision()) - 0.5*Math.log(precisionQ) + 0.5*precisionQ/this.getPrecision()
                + 0.5*precisionQ*Math.pow(this.getMean()-meanQ,2) -0.5;

        //double factor = precisionQ/this.getPrecision();
        //double kl = 0.5*(-Math.log(factor) + factor + precisionQ*Math.pow(this.getMean()-meanQ,2) -1);

        if (Double.isNaN(kl)){
            throw new IllegalStateException("NaN KL");
        }

        if (kl<0) {
            kl=0;
        }


        return kl;
    }

/*    public static void main(String[] args) throws IOException {
        AuxiliaryNormalDistribution aux = new AuxiliaryNormalDistribution();

        double [] vectorMu = {-10000,-1000,-100,-50,-10,-5,0,5,10,50,100,1000,10000};
        double [] vectorSD = {1000, 100, 10, 1, 0.1, 0.01, 0.001, 0.000001};

//        double [] vectorMu = {100,1000};
//        double [] vectorSD = {0.01, 0.1};

        FileWriter fileWriter = new FileWriter("/Users/dario/Downloads/salidaEFNormal.csv");
        double mu, sd;
        for (int i_mu = 0; i_mu < vectorMu.length; i_mu++) {
            for (int i_SD = 0; i_SD < vectorSD.length; i_SD++) {
                mu = vectorMu[i_mu];
                sd = vectorSD[i_SD];

                org.apache.commons.math3.distribution.NormalDistribution aux2 = new org.apache.commons.math3.distribution.NormalDistribution(mu, sd);

                for (int i = 0; i <= 1000; i++) {
                    double x = ((double) i)/1000.0;

                    double logPNorm1 = aux.logpnorm(x,mu,sd);
                    double PNorm1 = Math.exp(logPNorm1);

                    double PNorm2 = aux2.cumulativeProbability(x);
                    double logPNorm2 = Math.log(PNorm2);

                    String output = mu + "\t" + sd + "\t" + x + "\t" + logPNorm1 + "\t" + logPNorm2 + "\t" + (logPNorm1-logPNorm2) + "\t" + (PNorm1-PNorm2) + "\n";

                    fileWriter.write(output);
                }
            }
        }

        fileWriter.flush();
        fileWriter.close();
    }*/

    private static class AuxiliaryNormalDistribution {

        double logpnorm(double x, double mu, double sigma) {
            boolean lower_tail=true;
            boolean log_p=true;
            double p, cp;

                                                                                /* "DEFAULT" */
                                                                                /* --------- */
            final double R_D__0	= (log_p ? Double.NEGATIVE_INFINITY : 0.);		/* 0 */
            final double R_D__1	= (log_p ? 0. : 1.);			                /* 1 */
            final double R_DT_0	= (lower_tail ? R_D__0 : R_D__1);		        /* 0 */
            final double R_DT_1	= (lower_tail ? R_D__1 : R_D__0);		        /* 1 */

            /* Note: The structure of these checks has been carefully thought through.
             * For example, if x == mu and sigma == 0, we get the correct answer 1.
             */
//            #ifdef IEEE_754
//                    if(ISNAN(x) || ISNAN(mu) || ISNAN(sigma))
//                        return x + mu + sigma;
//            #endif

            if(!Double.isFinite(x) && mu == x) return Double.NaN;/* x-mu is NaN */
            if (sigma <= 0) {
                if(sigma < 0)
                    return Double.NaN;
	            /* sigma = 0 : */
                return (x < mu) ? R_DT_0 : R_DT_1;
            }
            p = (x - mu) / sigma;
            if(!Double.isFinite(p))
                return (x < mu) ? R_DT_0 : R_DT_1;
            x = p;

            double [] result = pnorm_both(x);
            p=result[0];
            cp=result[1];

            return(lower_tail ? p : cp);
        }

        private final int SIXTEEN = 16;   /* Cutoff allowing exact "*" and "/" */
        private final double M_SQRT_32 = 5.656854249492380195206754896838;	/* sqrt(32) */
        private final double M_1_SQRT_2PI = 0.398942280401432677939946059934;	/* 1/sqrt(2pi) */

        public double [] pnorm_both(double x) {

            int i_tail = 0;
            boolean log_p=true;


            final double [] a = {
                2.2352520354606839287,
                161.02823106855587881,
                1067.6894854603709582,
                18154.981253343561249,
                0.065682337918207449113
            };
            final double [] b = {
                47.20258190468824187,
                976.09855173777669322,
                10260.932208618978205,
                45507.789335026729956
            };
            final double [] c = {
                0.39894151208813466764,
                8.8831497943883759412,
                93.506656132177855979,
                597.27027639480026226,
                2494.5375852903726711,
                6848.1904505362823326,
                11602.651437647350124,
                9842.7148383839780218,
                1.0765576773720192317e-8
            };
            final double [] d = {
                22.266688044328115691,
                235.38790178262499861,
                1519.377599407554805,
                6485.558298266760755,
                18615.571640885098091,
                34900.952721145977266,
                38912.003286093271411,
                19685.429676859990727
            };
            final double [] p = {
                0.21589853405795699,
                0.1274011611602473639,
                0.022235277870649807,
                0.001421619193227893466,
                2.9112874951168792e-5,
                0.02307344176494017303
            };
            final double [] q = {
                1.28426009614491121,
                0.468238212480865118,
                0.0659881378689285515,
                0.00378239633202758244,
                7.29751555083966205e-5
            };

            double xden, xnum, temp, del, eps, xsq, y;
//            #ifdef NO_DENORMS
//                  double min = DBL_MIN;
//            #endif
            int i;
            boolean lower, upper;

            /* Final probability or log-probability result;
               ccum won't be used here */
            double cum = 0.0, ccum=0.0;

//            #ifdef IEEE_754
//                  if(ISNAN(x)) { *cum = *ccum = x; return; }
//            #endif

            /* Consider changing these : */
            final double DBL_EPSILON = java.lang.Math.ulp(1.0);
            eps = DBL_EPSILON * 0.5;

            /* i_tail in {0,1,2} =^= {lower, upper, both} */
            lower = i_tail != 1;
            upper = i_tail != 0;

            y = java.lang.Math.abs(x);
            if (y <= 0.67448975) { /* qnorm(3/4) = .6744.... -- earlier had 0.66291 */
                if (y > eps) {
                    xsq = x * x;
                    xnum = a[4] * xsq;
                    xden = xsq;
                    for (i = 0; i < 3; ++i) {
                        xnum = (xnum + a[i]) * xsq;
                        xden = (xden + b[i]) * xsq;
                    }
                } else xnum = xden = 0.0;

                temp = x * (xnum + a[3]) / (xden + b[3]);
                if(lower)   cum = 0.5 + temp;
                if(upper)   ccum = 0.5 - temp;
                if(log_p) {
                    if(lower)  cum = Math.log(cum);
                    if(upper)  ccum = Math.log(ccum);
                }
            }
            else if (y <= M_SQRT_32) {

	            /* Evaluate pnorm for 0.674.. = qnorm(3/4) < |x| <= sqrt(32) ~= 5.657 */

                xnum = c[8] * y;
                xden = y;
                for (i = 0; i < 7; ++i) {
                    xnum = (xnum + c[i]) * y;
                    xden = (xden + d[i]) * y;
                }
                temp = (xnum + c[7]) / (xden + d[7]);



                /*      DO_DEL(y)     MACRO */
                xsq = (int) ((y * SIXTEEN)) / SIXTEEN;
                del = (y - xsq) * (y + xsq);
                if(log_p) {
                    cum = (-xsq * xsq * 0.5) + (-del * 0.5) + Math.log(temp);
                    if((lower && x > 0.) || (upper && x <= 0.))
                        ccum = Math.log1p(-Math.exp(-xsq * xsq * 0.5) * Math.exp(-del * 0.5) * temp);
                }
                else {
                    cum = Math.exp(-xsq * xsq * 0.5) * Math.exp(-del * 0.5) * temp;
                    ccum = 1.0 - cum;
                }



                /*      SWAP_TAIL      MACRO */
                if (x > 0.) {/* swap  ccum <--> cum */
                    temp = cum;
                    if(lower) cum = ccum;
                        ccum = temp;
                }



            }


            /* else	  |x| > sqrt(32) = 5.657 :
             * the next two case differentiations were really for lower=T, log=F
             * Particularly	 *not*	for  log_p !
             * Cody had (-37.5193 < x  &&  x < 8.2924) ; R originally had y < 50
             *
             * Note that we do want symmetry(0), lower/upper -> hence use y
             */

            else if((log_p && y < 1e170) /* avoid underflow below */
                /*  ^^^^^ MM FIXME: can speedup for log_p and much larger |x| !
                 * Then, make use of  Abramowitz & Stegun, 26.2.13, something like
                 xsq = x*x;
                 if(xsq * DBL_EPSILON < 1.)
                    del = (1. - (1. - 5./(xsq+6.)) / (xsq+4.)) / (xsq+2.);
                 else
                    del = 0.;
                 *cum = -.5*xsq - M_LN_SQRT_2PI - log(x) + log1p(-del);
                 *ccum = log1p(-exp(*cum)); /.* ~ log(1) = 0 *./
                 swap_tail;
                 [Yes, but xsq might be infinite.]
                */
                || (lower && -37.5193 < x  &&  x < 8.2924)
                || (upper && -8.2924  < x  &&  x < 37.5193)
                ) {

	            /* Evaluate pnorm for x in (-37.5, -5.657) union (5.657, 37.5) */
                xsq = 1.0 / (x * x); /* (1./x)*(1./x) might be better */
                xnum = p[5] * xsq;
                xden = xsq;
                for (i = 0; i < 4; ++i) {
                    xnum = (xnum + p[i]) * xsq;
                    xden = (xden + q[i]) * xsq;
                }
                temp = xsq * (xnum + p[4]) / (xden + q[4]);
                temp = (M_1_SQRT_2PI - temp) / y;

                /*      DO_DEL(x)     MACRO */
                xsq = (int) ((x * SIXTEEN)) / SIXTEEN;
                del = (x - xsq) * (x + xsq);
                if(log_p) {
                    cum = (-xsq * xsq * 0.5) + (-del * 0.5) + Math.log(temp);
                    if((lower && x > 0.) || (upper && x <= 0.))
                        ccum = Math.log1p(-Math.exp(-xsq * xsq * 0.5) * Math.exp(-del * 0.5) * temp);
                }
                else {
                    cum = Math.exp(-xsq * xsq * 0.5) * Math.exp(-del * 0.5) * temp;
                    ccum = 1.0 - cum;
                }



                /*      SWAP_TAIL      MACRO */
                if (x > 0.) {/* swap  ccum <--> cum */
                    temp = cum;
                    if(lower) cum = ccum;
                    ccum = temp;
                }


            } else { /* large x such that probs are 0 or 1 */
                                                                                    /* "DEFAULT" */
                                                                                    /* --------- */
                final double R_D__0	= (log_p ? Double.NEGATIVE_INFINITY : 0.);		/* 0 */
                final double R_D__1	= (log_p ? 0. : 1.);			                /* 1 */

                if(x > 0) {
                    cum = R_D__1;
                    ccum = R_D__0;
                }
                else {
                    cum = R_D__0;
                    ccum = R_D__1;
                }
            }


//            #ifdef NO_DENORMS
//                /* do not return "denormalized" -- we do in R */
//                        if(log_p) {
//                            if(cum > -min)	 *cum = -0.;
//                            if(ccum > -min)*ccum = -0.;
//                        }
//                        else {
//                            if(*cum < min)	 *cum = 0.;
//                            if(*ccum < min)	*ccum = 0.;
//                        }
//            #endif

            double [] result = {cum,ccum};
            return result;
        }

        }



}