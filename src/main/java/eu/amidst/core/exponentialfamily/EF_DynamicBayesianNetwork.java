package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.utils.Vector;

/**
 * Created by andresmasegosa on 13/01/15.
 */
public class EF_DynamicBayesianNetwork extends EF_Distribution {

    EF_BayesianNetwork bayesianNetworkTime0;

    EF_BayesianNetwork bayesianNetworkTimeT;


    public EF_DynamicBayesianNetwork(DynamicDAG dag) {
        this.bayesianNetworkTime0 = new EF_BayesianNetwork(dag.getParentSetsTime0());
        this.bayesianNetworkTimeT = new EF_BayesianNetwork(dag.getParentSetsTimeT());
    }

    @Override
    public void updateNaturalFromMomentParameters() {

    }

    @Override
    public void updateMomentFromNaturalParameters() {

    }

    @Override
    public SufficientStatistics getSufficientStatistics(DataInstance data) {
        return null;
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return 0;
    }

    @Override
    public double computeLogBaseMeasure(DataInstance dataInstance) {
        return 0;
    }

    @Override
    public double computeLogNormalizer() {
        return 0;
    }

    @Override
    public Vector createZeroedVector() {
        return null;
    }

    static class CompoundVector implements SufficientStatistics, MomentParameters, NaturalParameters {

        VectorBuilder vectorBuilder;

        Vector vectorTime0;
        Vector vectorTimeT;

        public CompoundVector(VectorBuilder vectorBuilder1){
            this.vectorBuilder = vectorBuilder1;
            this.vectorTime0 = vectorBuilder.createZeroedVector();
            this.vectorTimeT = vectorBuilder.createZeroedVector();
        }
        @Override
        public double get(int i) {
            if (i<vectorTime0.size())
                return this.vectorTime0.get(i);
            else
                return this.vectorTimeT.get(i-vectorTime0.size());
        }

        @Override
        public void set(int i, double val) {

        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void sum(Vector vector) {

        }

        @Override
        public void copy(Vector vector) {

        }

        @Override
        public void divideBy(double val) {

        }

        @Override
        public double dotProduct(Vector vec) {
            return 0;
        }
    }

    @FunctionalInterface
    public interface VectorBuilder {
        public Vector createZeroedVector();
    }
}
