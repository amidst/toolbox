package eu.amidst.multicore;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DAGGenerator;

import java.io.IOException;

/**
 * Created by rcabanas on 16/06/16.
 */
public class ParallelMLExample {
    public static void main(String[] args) throws IOException, ClassNotFoundException {


        int batchSize = 100;

        DataStream<DataInstance> data = DataStreamLoader.open("datasets/simulated/WasteIncineratorSample.arff");;
        //We can load a Bayesian network using the static class BayesianNetworkLoader
         DAG dag = DAGGenerator.getNaiveBayesStructure(data.getAttributes(), "B");

        BayesianNetwork bn = new BayesianNetwork(dag);

        data.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));

        //Now we print the loaded model
        System.out.println(bn.toString());


        EF_BayesianNetwork efbn = new EF_BayesianNetwork(bn);


        SufficientStatistics sumSS = data.parallelStream(batchSize)
                .map(efbn::getSufficientStatistics) //see Program 6
                .reduce(SufficientStatistics::sumVectorNonStateless).get();
                //.reduce((v1,v2) -> {v1.sum(v2); return v1;}).get();


        sumSS.divideBy(data.stream().count());

        for(int i=0; i<sumSS.size(); i++) {
            System.out.println(sumSS.get(i));
        }

    }




}
/*

0.8467
0.1533
0.8467
0.1533
0.8091
0.0376
0.1457
0.0076
0.8467
0.1533
0.2362
0.6105
0.0433
0.11
0.8467
0.1533
-2.7646811744767565
9.414227380471274
-0.4986826913188683
1.6984520776480738
0.8467
0.1533
2.4330129957374433
7.340233249391847
0.5954958853052369
2.3913434984083635
0.8467
0.1533
-1.6903399593649104
3.458556357483668
-0.1519460265508319
0.19678141499760016
0.8467
0.1533
1.3298241846024756
2.3818202524670085
0.16146879462297856
0.22784556337480985
0.8467
0.1533
-0.18723851894853738
0.21670989973191396
-0.03368063578992722
0.03910376190697502
0.8467
0.1533
2.2455048081284525
6.423828852852779
0.5617703685238481
2.1605943035145754
 */