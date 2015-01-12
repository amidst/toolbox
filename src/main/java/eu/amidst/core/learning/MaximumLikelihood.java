package eu.amidst.core.learning;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_DistributionBuilder;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.utils.Vector;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public final class MaximumLikelihood {

    private static int batchSize = 1000;
    private static boolean parallelMode = true;

    public static int getBatchSize() {
        return batchSize;
    }

    public static void setBatchSize(int batchSize) {
        MaximumLikelihood.batchSize = batchSize;
    }

    public static boolean isParallelMode() {
        return parallelMode;
    }

    public static void setParallelMode(boolean parallelMode) {
        MaximumLikelihood.parallelMode = parallelMode;
    }

    public static BayesianNetwork learnParametersStaticModel(DAG dag, DataBase dataBase) {

        EF_BayesianNetwork efBayesianNetwork = new EF_BayesianNetwork(dag);


        Stream<DataInstance> stream = null;
        if (parallelMode){
            stream = dataBase.parallelStream(batchSize);
        }else{
            stream = dataBase.stream();
        }

        AtomicInteger dataInstanceCount = new AtomicInteger(0);

        SufficientStatistics sumSS = stream
                .peek(w -> {
                    dataInstanceCount.getAndIncrement();
                })
                .map(efBayesianNetwork::getSufficientStatistics)
                .reduce(efBayesianNetwork.createZeroedSufficientStatistics(), SufficientStatistics::sumSS);

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efBayesianNetwork.setMomentParameters(sumSS);
        return efBayesianNetwork.toBayesianNetwork(dag);

    }

    public static DynamicBayesianNetwork learnDynamic(DynamicDAG dag, DataBase dataBase) {

        return null;
    }
}
