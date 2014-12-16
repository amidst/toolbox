package eu.amidst.core.utils;

import java.util.Random;

/**
 * Created by andresmasegosa on 15/12/14.
 */
public class LocalRandomGenerator {

    final Random random;

    public LocalRandomGenerator(int seed){
        random = new Random(seed);
    }

    public Random current(){
        return new Random(random.nextInt());
    }
}
