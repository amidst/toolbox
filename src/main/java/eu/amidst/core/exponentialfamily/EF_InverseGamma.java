package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.apache.commons.math3.special.Gamma;

/**
 * Created by ana@cs.aau.dk on 23/02/15.
 */
public class EF_InverseGamma extends EF_UnivariateDistribution {

    public static final int LOGX = 0;
    public static final int INVX = 1;
    public static final double DELTA = 0.0001;

    public EF_InverseGamma(Variable var1) {

        this.var = var1;
        this.naturalParameters = this.createZeroedNaturalParameters();
        this.momentParameters = this.createZeroedMomentParameters();

        this.momentParameters.set(LOGX, 0);
        this.momentParameters.set(INVX, 1);
        this.setMomentParameters(momentParameters);
    }

    @Override
    public double computeLogBaseMeasure(double val) {
        return 0;
    }

    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics vec = this.createZeroedSufficientStatistics();
        vec.set(LOGX, Math.log(val));
        vec.set(INVX, 1.0 / val);
        return vec;
    }

    @Override
    public EF_UnivariateDistribution deepCopy() {
        EF_InverseGamma copy = new EF_InverseGamma(this.getVariable());
        copy.getNaturalParameters().copy(this.getNaturalParameters());

        return copy;
    }

    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {
        double alpha = random.nextGaussian() * 10 + 0.1;
        double beta = random.nextDouble() * 10 + 0.1;

        this.getNaturalParameters().set(LOGX, -alpha - 1);
        this.getNaturalParameters().set(INVX, -beta);

        this.updateMomentFromNaturalParameters();

        return this;
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        double m0 = this.getMomentParameters().get(0);
        double m1 = this.getMomentParameters().get(1);
        // Coordinate ascent until convergence
        double newalpha = 0.0, alpha = 1.0;
        double newbeta = 0.0, beta = 1.0;
        while (Math.abs(newalpha - alpha) > DELTA || Math.abs(newbeta - beta) > DELTA) {
            newalpha = Utils.invDigamma(Math.log(beta) - m0);
            newbeta = newalpha / m1;
        }
        this.naturalParameters.set(0, newalpha);
        this.naturalParameters.set(1, newbeta);
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        double alpha = this.naturalParameters.get(0) - 1;
        double beta = -this.naturalParameters.get(1);
        this.momentParameters.set(0, Math.log(beta) - Gamma.digamma(alpha));
        this.momentParameters.set(1, alpha / beta);
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return 2;
    }

    @Override
    public double computeLogNormalizer() {
        double alpha = -this.momentParameters.get(0) - 1;
        double beta = -this.momentParameters.get(1);
        return Math.log(Gamma.gamma(alpha)) - alpha * Math.log(beta);
    }

    @Override
    public Vector createZeroedVector() {
        return new ArrayVector(2);
    }


}