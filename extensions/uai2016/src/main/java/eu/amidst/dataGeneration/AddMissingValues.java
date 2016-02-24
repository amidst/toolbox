package eu.amidst.dataGeneration;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.utils.Utils;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.util.List;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 23/02/16.
 */
public class AddMissingValues {

    public static void main(String[] args) throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        String inputFileName = args[0];
        String outputFileName = args[1];

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env,inputFileName, false);

        DataSet<DataInstance> dataFlinkWithMissing = dataFlink.getDataSet().
                map(new MissValuesMap(dataFlink.getAttributes()));

        DataFlinkWriter.writeDataToARFFFolder(new DataFlink<DataInstance>() {
            @Override
            public String getName() {
                return dataFlink.getName();
            }

            @Override
            public Attributes getAttributes() {
                return dataFlink.getAttributes();
            }

            @Override
            public DataSet<DataInstance> getDataSet() {
                return dataFlinkWithMissing;
            }
        }, outputFileName);
    }

    public static void addMissingValuesToFile (String inputFileName,String outputFileName)  throws Exception {

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env,inputFileName, false);

        DataSet<DataInstance> dataFlinkWithMissing = dataFlink.getDataSet().
                map(new MissValuesMap(dataFlink.getAttributes()));

        DataFlinkWriter.writeDataToARFFFolder(new DataFlink<DataInstance>() {
            @Override
            public String getName() {
                return dataFlink.getName();
            }

            @Override
            public Attributes getAttributes() {
                return dataFlink.getAttributes();
            }

            @Override
            public DataSet<DataInstance> getDataSet() {
                return dataFlinkWithMissing;
            }
        }, outputFileName);
    }

    public static class MissValuesMap extends RichMapFunction<DataInstance, DataInstance> {

        Random random;
        List<Attribute> attsList;

        public MissValuesMap(Attributes atts){

            attsList = atts.getListOfNonSpecialAttributes();
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            random = new Random(getRuntimeContext().getIndexOfThisSubtask());
        }

        @Override
        public DataInstance map(DataInstance dataInstance) throws Exception {

            attsList.stream().forEach(attribute -> {
                if(!attribute.getName().equalsIgnoreCase("Default")){
                    double probMissing = random.nextDouble();
                    if((attribute.getIndex()%2==0 && probMissing<0.3) ||
                            (attribute.getIndex()%2==1 && probMissing<0.8))
                        dataInstance.setValue(attribute, Utils.missingValue());
                }
            });
            return dataInstance;
        }
    }


}
