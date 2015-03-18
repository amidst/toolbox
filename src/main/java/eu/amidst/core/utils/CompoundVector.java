package eu.amidst.core.utils;

import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.exponentialfamily.NaturalParameters;
import eu.amidst.core.exponentialfamily.SufficientStatistics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CompoundVector implements SufficientStatistics, MomentParameters, NaturalParameters {

    int size;
    List<IndexedVector> baseVectors;

    public CompoundVector(int nVectors, int size1){
        baseVectors = new ArrayList(nVectors);
        this.size = size1;
        for (int i = 0; i < nVectors; i++) {
            baseVectors.add(i, new IndexedVector(i, null));
        }
    }

    public CompoundVector(List<Vector> vectors) {
        baseVectors = new ArrayList(vectors.size());

        for (int i = 0; i < vectors.size(); i++) {
            baseVectors.add(i, new IndexedVector(i, vectors.get(i)));
            size += vectors.get(i).size();
        }
    }

    public void setVectorByPosition(int position, Vector vec) {
        baseVectors.get(position).setVector(vec);
    }

    public Vector getVectorByPosition(int position) {
        return this.baseVectors.get(position).getVector();
    }

    @Override
    public double get(int i) {
        int total = 0;
        for (int j = 0; j < this.baseVectors.size(); j++) {
            if (i < (total + this.baseVectors.get(j).getVector().size())) {
                return this.baseVectors.get(j).getVector().get(i - total);
            } else {
                total += this.baseVectors.get(j).getVector().size();
            }
        }
        return Double.NaN;
    }

    @Override
    public void set(int i, double val) {
        int total = 0;
        for (int j = 0; j < this.baseVectors.size(); j++) {
            if (i < (total + this.baseVectors.get(j).getVector().size())) {
                this.baseVectors.get(j).getVector().set(i - total, val);
                return;
            } else {
                total += this.baseVectors.get(j).getVector().size();
            }
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void sum(Vector vector) {
        this.sum((CompoundVector) vector);
    }

    @Override
    public void copy(Vector vector) {
        this.copy((CompoundVector) vector);
    }

    @Override
    public void divideBy(double val) {
        this.baseVectors.stream().forEach(w -> w.getVector().divideBy(val));
    }

    @Override
    public double dotProduct(Vector vec) {
        return this.dotProduct((CompoundVector) vec);
    }

    public double dotProduct(CompoundVector vec) {
        return this.baseVectors.stream().mapToDouble(w -> w.getVector().dotProduct(vec.getVectorByPosition(w.getIndex()))).sum();
    }

    public void copy(CompoundVector vector) {
        if (vector.size() != this.size())
            throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

        this.baseVectors.stream().forEach(w -> w.getVector().copy(vector.getVectorByPosition(w.getIndex())));

    }

    public void sum(CompoundVector vector) {
        this.baseVectors.stream().forEach(w -> w.getVector().sum(vector.getVectorByPosition(w.getIndex())));
    }


    static class IndexedVector {
        Vector vector;
        int index;

        IndexedVector(int index1, Vector vec1) {
            this.vector = vec1;
            this.index = index1;
        }

        public Vector getVector() {
            return vector;
        }

        public int getIndex() {
            return index;
        }

        public void setVector(Vector vector) {
            this.vector = vector;
        }
    }

    @FunctionalInterface
    public interface VectorBuilder {
        public Vector createZeroedVector();
    }

}