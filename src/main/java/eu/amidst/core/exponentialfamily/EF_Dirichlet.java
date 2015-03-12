package eu.amidst.core.exponentialfamily;

import org.apache.commons.math3.special.Gamma;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 26/02/15.
 */
public class EF_Dirichlet extends EF_UnivariateDistribution {

    int nOfStates;


    public EF_Dirichlet(Variable var1) {
        if (!var1.isDirichletParameter())
            throw new IllegalArgumentException("Non Dirichlet var");
        this.var=var1;
        this.nOfStates = var.getNumberOfStates();
        this.naturalParameters = this.createZeroedNaturalParameters();
        this.momentParameters = this.createZeroedMomentParameters();

        for (int i = 0; i < nOfStates; i++) {
            this.naturalParameters.set(i,1.0);
        }

        updateMomentFromNaturalParameters();

        //this.setMomentParameters(momentParameters);
    }

    public EF_Dirichlet(Variable var1, double scale) {
        if (!var1.isDirichletParameter())
            throw new IllegalArgumentException("Non Dirichlet var");
        this.var=var1;
        this.nOfStates = var.getNumberOfStates();
        this.naturalParameters = this.createZeroedNaturalParameters();
        this.momentParameters = this.createZeroedMomentParameters();

        for (int i = 0; i < nOfStates; i++) {
            this.naturalParameters.set(i,scale - 1);
        }

        updateMomentFromNaturalParameters();
    }


    @Override
    public double computeLogBaseMeasure(double val) {
        return 0;
    }

    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics vec = this.createZeroedSufficientStatistics();
        vec.set((int)val, Math.log(val));
        return vec;
    }

    @Override
    public Vector getExpectedParameters() {

        double sum =0;

        for (int i = 0; i < nOfStates; i++) {
            sum+=this.naturalParameters.get(i)+1;
        }

        Vector vector = new ArrayVector(this.nOfStates);
        for (int i = 0; i < nOfStates; i++) {
            vector.set(i,(this.naturalParameters.get(i)+1)/sum);
        }

        return vector;
    }

    @Override
    public void fixNumericalInstability() {

    }

    @Override
    public EF_UnivariateDistribution deepCopy() {
        EF_Dirichlet copy = new EF_Dirichlet(this.getVariable());
        copy.getNaturalParameters().copy(this.getNaturalParameters());
        copy.getMomentParameters().copy(this.getMomentParameters());
        return copy;
    }

    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {

        for (int i = 0; i < this.nOfStates; i++) {
            this.getNaturalParameters().set(i, random.nextDouble() + 1e-5);
        }
        this.updateMomentFromNaturalParameters();

        return this;
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("No Implemented. EF_Dirichlet distribution should (right now) only be used for learning.");
    }

    @Override
    public void updateMomentFromNaturalParameters() {

        double sumOfU_i = 0;
        for (int i = 0; i < nOfStates; i++) {
            sumOfU_i += this.naturalParameters.get(i);
        }

        for (int i = 0; i < nOfStates; i++) {
            this.momentParameters.set(i, Gamma.digamma(this.naturalParameters.get(i)) - Gamma.digamma(sumOfU_i));
        }
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return nOfStates;
    }

    @Override
    public double computeLogNormalizer() {

        double sumOfU_i = 0;
        double sumLogGammaOfU_i = 0;
        for (int i = 0; i < nOfStates; i++) {
            sumOfU_i += naturalParameters.get(i);
            sumLogGammaOfU_i += Gamma.logGamma(naturalParameters.get(i));
        }

        return sumLogGammaOfU_i - Gamma.logGamma(sumOfU_i);
    }

    @Override
    public Vector createZeroedVector() {
        return new ArrayVector(nOfStates);
    }

}
