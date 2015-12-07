package eu.amidst.dataGeneration;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;

import java.util.concurrent.TimeUnit;

/**
 * The script to generate this data can be found in Dropbox under (as it should not be made public)
 *
 * Created by ana@cs.aau.dk on 03/12/15.
 */
public class GenerateCajaMarLikeData {

    public void testGeneratedDataWithRScript(){
        DataStream<DataInstance> dataIDA = DataStreamLoader.openFromFile(
                "./extensions/uai2016/doc-experiments/dataGenerationForFlink/IDAlikeData/MONTH1.arff");
        Attributes attributes = dataIDA.stream().findFirst().get().getAttributes();
        System.out.println(attributes.toString());
        System.out.println("DEFAULT[0] = "+dataIDA.stream().findFirst().get().getValue(
                attributes.getAttributeByName("DEFAULT")));

        DataStream<DataInstance> dataSCAI = DataStreamLoader.openFromFile(
                "./extensions/uai2016/doc-experiments/dataGenerationForFlink/SCAIlikeData/MONTH2.arff");
        attributes = dataSCAI.stream().findFirst().get().getAttributes();
        System.out.println(attributes.toString());
        System.out.println("AGE[0] = "+dataSCAI.stream().findFirst().get().getValue(
                attributes.getAttributeByName("AGE")));
    }
    public static void main(String[] args) throws Exception{

        GenerateCajaMarLikeData generateCajaMarLikeData = new GenerateCajaMarLikeData();

        /*
         * The 1st parameter is the number of files (per month)
         * The 2nd parameter is the length of the sequence, i.e., # of clients
         */
        Runtime.getRuntime().exec("Rscript ./extensions/uai2016/doc-experiments/dataGenerationForFlink/" +
                "data_generator_IDA.R 3 10");

        Runtime.getRuntime().exec("Rscript ./extensions/uai2016/doc-experiments/dataGenerationForFlink/" +
                "data_generator_SCAI.R 3 10");

        TimeUnit.SECONDS.sleep(5);

        generateCajaMarLikeData.testGeneratedDataWithRScript();


    }
}
