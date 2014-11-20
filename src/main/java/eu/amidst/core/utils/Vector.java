package eu.amidst.core.utils;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public interface Vector {

    public double get(int i);

    public void set(int i, double val);

    public int size();

    public default void dotProduct(Vector vec){
        if (vec.size()!=this.size())
            throw new IllegalArgumentException("Error in variable Vector. Method dotProduct. The parameter vec has a different size. ");

        for (int i=0; i<this.size(); i++){
            this.set(i, this.get(i)*vec.get(i));
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
