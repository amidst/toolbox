package eu.amidst.kbspaper;


import COM.hugin.HAPI.*;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.DataSetGenerator;

import eu.amidst.latentvariablemodels.staticmodels.FactorAnalysis;
import eu.amidst.latentvariablemodels.staticmodels.GaussianMixture;
import eu.amidst.latentvariablemodels.staticmodels.MixtureOfFactorAnalysers;


import java.io.IOException;

/**
 * Created by rcabanas on 08/08/2018.
 */



public class GenerateData {



    public static void main(String[] args) throws IOException, ClassNotFoundException, ExceptionHugin {

      //  build_mog(conf.numvars);
        build_mfa(conf.numvars);
      //  build_fa(conf.numvars);


      //  generateData(conf.numvars);


    }


    public static void build_mog(int numvars) throws IOException {

        DataStream<DataInstance> data = DataSetGenerator.generate(1234,500, 0, numvars);

        GaussianMixture m = new GaussianMixture(data.getAttributes());
        m.setDiagonal(true);
        m.setNumStatesHiddenVar(conf.pca_numStatesHiddenVar);

        m.updateModel(data);

        BayesianNetworkWriter.save(m.getModel(), conf.netfolder +"mog.bn");
        System.out.println("saving "+  conf.netfolder +"mog.bn");

    }



    public static void build_mfa(int numvars) throws IOException {

        DataStream<DataInstance> data = DataSetGenerator.generate(1234,500, 0, numvars);

        MixtureOfFactorAnalysers m = new MixtureOfFactorAnalysers(data.getAttributes())
                .setNumberOfLatentVariables(conf.mfa_numLatent)
                .setNumberOfStatesLatentDiscreteVar(conf.mfa_numStatesLatent);


        m.updateModel(data);

        BayesianNetworkWriter.save(m.getModel(), conf.netfolder +"mfa.bn");
        System.out.println("saving "+  conf.netfolder +"mfa.bn");

    }



    public static void build_fa(int numvars) throws IOException {

        DataStream<DataInstance> data = DataSetGenerator.generate(1234,500, 0, numvars);

        FactorAnalysis m = new FactorAnalysis(data.getAttributes())
                .setNumberOfLatentVariables(conf.fa_numLatent);

        m.updateModel(data);

        BayesianNetworkWriter.save(m.getModel(), conf.netfolder +"fa.bn");
        System.out.println("saving "+  conf.netfolder +"fa.bn");

    }






    public static void generateData(int numvars) throws IOException, ClassNotFoundException {



        for(long s = conf.s_init; s<=conf.s_stop; s=s+conf.s_step ) {

            System.out.println("sampling "+s+" instances");

            DataStream<DataInstance> dataStream = DataSetGenerator.generate(1234,(int)s, 0, numvars);

            //We finally save the sampled data set to a arff file.
            DataStreamWriter.writeDataToFile(dataStream, conf.datafolder +"data_"+(s/1000)+"k.arff");

            System.out.println("saving "+ conf.datafolder +"data_"+(s/1000)+"k.arff");

        }
    }



}
