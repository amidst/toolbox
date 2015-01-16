package eu.amidst.core.exponentialfamily;

import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public interface SufficientStatistics extends Vector {
    // TODO: This is not stateless operation!!!!. However it works if we accumulate in the second argument!!! TEST!!!
    // Look at the code below
    /*
             List<ArrayVector> vectorList = IntStream.range(0,10).mapToObj(i -> {
            ArrayVector vec = new ArrayVector(2);
            vec.set(0, 1);
            vec.set(1, 1);
            return vec;
        }).collect(Collectors.toList());


        Vector out1 = vectorList.parallelStream()
                .reduce(new ArrayVector(2), (u, v) -> {
                    ArrayVector outvec = new ArrayVector(2);
                    outvec.sum(v);
                    outvec.sum(u);
                    return outvec;});


        Vector out2 = vectorList.parallelStream().reduce(new ArrayVector(2), (u, v) -> {u.sum(v); return u;});
        Vector out3 = vectorList.parallelStream().reduce(new ArrayVector(2), (u, v) -> {v.sum(u); return v;});

        System.out.println(out1.get(0) + ", " + out1.get(1));
        System.out.println(out2.get(0) + ", " + out2.get(1));
        System.out.println(out3.get(0) + ", " + out3.get(1));
     */
    public static SufficientStatistics sumSS(SufficientStatistics vec1, SufficientStatistics vec2){
        vec2.sum(vec1);
        return vec2;
    }
}
