package eu.amidst.core.learning;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_DistributionBuilder;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.utils.Vector;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public final class MaximumLikelihood {

    private static int batchSize = 1000;

    public static int getBatchSize() {
        return batchSize;
    }

    public static void setBatchSize(int batchSize) {
        MaximumLikelihood.batchSize = batchSize;
    }

    public static BayesianNetwork serialLearnStatic(DAG dag, DataBase dataBase) {

        EF_BayesianNetwork efBayesianNetwork = new EF_BayesianNetwork(dag);

        dataBase.stream().map(efBayesianNetwork::getSufficientStatistics).reduce(efBayesianNetwork.createZeroedSufficientStatistics(), SufficientStatistics::sum);

        return efBayesianNetwork.toBayesianNetwork(dag);

    }

    public static BayesianNetwork parallelLearnStatic(DAG dag, DataBase dataBase) {

        EF_BayesianNetwork efBayesianNetwork = new EF_BayesianNetwork(dag);

        dataBase.parallelStream(batchSize).map(efBayesianNetwork::getSufficientStatistics).reduce(efBayesianNetwork.createZeroedSufficientStatistics(), SufficientStatistics::sum);

        return efBayesianNetwork.toBayesianNetwork(dag);

    }

    public static DynamicBayesianNetwork learnDynamic(DynamicDAG dag, DataBase dataBase) {

        return null;
    }
}
