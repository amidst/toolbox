package moa.classifiers.bayes;

import moa.classifiers.AbstractClassifier;
import moa.core.Measurement;
import weka.core.Instance;

/**
 * Created by ana@cs.aau.dk on 18/06/15.
 */
public class AmidstClassifier  extends AbstractClassifier {
    @Override
    public void resetLearningImpl() {

    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {

    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[0];
    }

    @Override
    public void getModelDescription(StringBuilder stringBuilder, int i) {

    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        return new double[0];
    }
}
