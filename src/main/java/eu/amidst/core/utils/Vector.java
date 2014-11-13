package eu.amidst.core.utils;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public class Vector {

    double[] array;

    public Vector(int size){
        this.array = new double[size];
    }

    public Vector(double[] vec){
        this.array=vec;
    }

    public double get(int i){
        return this.array[i];
    }

    public void set(int i, double val){
        this.array[i]=val;
    }

    public int size(){
        return this.array.length;
    }

    public void dotProduct(Vector vec){
        if (vec.size()!=this.size())
            throw new IllegalArgumentException("Error in variable Vector. Method dotProduct. The parameter vec has a different size. ");

        for (int i=0; i<this.array.length; i++){
            this.array[i]*=vec.get(i);
        }
    }

    public static double dotProduct(Vector vec1, Vector vec2){
        if (vec1.size()!=vec2.size())
            throw new IllegalArgumentException("Error in variable Vector. Method dotProduct. Parameters have a different size. ");

        double sum=0;
        for (int i=0; i<vec1.size(); i++){
            sum += vec1.get(i)*vec2.size();
        }

        return sum;
    }

}
