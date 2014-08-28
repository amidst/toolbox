package eu.amidst.core.Potential;


import java.util.List;

/**
 * Created by afa on 03/07/14.
 */
public class MultivariateGaussianCF implements Potential {
    public void setParameters(MultivariateGaussian prob) {
    }

    public double[][] getKParameter() {
        return null;
    }

    public double[] getHParameter() {
        return null;
    }

    public double getGParameter() {
        return 0;
    }

    public MultivariateGaussian getMG() {
        return null;
    }

    @Override
    public void setVariables(List variables) {

    }

    @Override
    public List getVariables() {
        return null;
    }

    @Override
    public void combine(Potential pot) {

    }

    @Override
    public void marginalize(List variables) {

    }
}
