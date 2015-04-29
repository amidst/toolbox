package eu.amidst.ida2015;


import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.filereaders.DataStreamFromFile;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class scriptRemoveInstancesWithMissing{


    public static void removeInstancesWithMissing(String path)  throws IOException {
        ARFFDataReader reader= new ARFFDataReader();
        reader.loadFromFile(path);

        String newPath = path.replace(".arff", "INDICATORS.arff");

        FileWriter fw = new FileWriter(newPath);
        fw.write("@relation dataset\n\n");

        for (Attribute att : reader.getAttributes()){
            fw.write(ARFFDataWriter.attributeToARFFString(att)+"\n");
        }

        fw.write("\n\n@data\n\n");

        DataStreamFromFile data = new DataStreamFromFile(reader);

        data.stream().forEach(e -> {
            boolean missing = false;
            for (Attribute att : reader.getAttributes()) {
                if(Double.isNaN(e.getValue(att))){
                    missing = true;
                    break;
                }
            }
            try {
                if(!missing)
                fw.write(ARFFDataWriter.dataInstanceToARFFString(reader.getAttributes(), e) + "\n");
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        fw.close();

    }
    public static void main(String[] args) {
        try {
            //addIndicatorVarsToCajamar(args[0]);
            removeInstancesWithMissing("/Users/ana/Documents/core/datasets/dynamicDataOnlyContinuous.arff");
        }catch (IOException ex){}
    }
}