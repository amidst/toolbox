package eu.amidst.core.database.filereaders.arffFileReader;

import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.filereaders.DataFileReader;
import eu.amidst.core.database.filereaders.DataRow;
import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;
import eu.amidst.core.variables.FiniteStateSpace;
import eu.amidst.core.variables.RealStateSpace;
import eu.amidst.core.variables.StateSpaceType;
import org.eclipse.jetty.io.UncheckedIOException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 17/12/14.
 */
public class ARFFDataReader implements DataFileReader {
    String relationName;
    private Attributes attributes;
    private int dataLineCount;
    private Path pathFile;
    private StateSpaceType[] stateSpace;

    public ARFFDataReader(String pathString) {
        pathFile = Paths.get(pathString);
        try {
            Optional<String> atRelation = Files.lines(pathFile)
                    .map(String::trim)
                    .limit(0)
                    .filter(line -> line.startsWith("@relation"))
                    .findFirst();

            if (!atRelation.isPresent())
                throw new IllegalArgumentException("ARFF file does not start with a @relation line.");

            relationName = atRelation.get().split(" ")[1];

            final int[] count = {0};
            Optional<String> atData = Files.lines(pathFile)
                    .map(String::trim)
                    .peek(line -> count[0]++)
                    .filter(line -> line.startsWith("@data"))
                    .findFirst();

            if (!atData.isPresent())
                throw new IllegalArgumentException("ARFF file does not contain @data line.");

            dataLineCount = count[0]+1;

            List<String> attLines = Files.lines(pathFile)
                    .map(String::trim)
                    .limit(count[0])
                    .filter(line -> line.startsWith("@attribute"))
                    .collect(Collectors.toList());

            List<Attribute> atts = IntStream.range(0,attLines.size())
                    .mapToObj( i -> createAttributeFromLine(i, attLines.get(i)))
                    .collect(Collectors.toList());

            this.attributes = new Attributes(atts);


            //
            stateSpace=new StateSpaceType[atts.size()];

            for (Attribute att: atts){
                stateSpace[att.getIndex()] = att.getStateSpace().getStateSpaceType();
            }
        }catch (IOException ex){
            throw new UncheckedIOException(ex);
        }
    }

    private static Attribute createAttributeFromLine(int index, String line){
        String[] parts = line.split(" ");

        if (!parts[0].trim().startsWith("@attribute"))
            throw new IllegalArgumentException("Attribute line does not start with @attribute");

        String name = parts[1].trim();
        parts[2]=parts[2].trim();

        if (parts[2].equals("real")){
            return new Attribute(index, name, new RealStateSpace());
        }else if (parts[2].startsWith("{")){
            String[] states = parts[2].substring(1,parts[2].length()-1).split(",");

            List<String> statesNames = Arrays.stream(states).map(String::trim).collect(Collectors.toList());

            return new Attribute(index, name, new FiniteStateSpace(statesNames));
        }else{
            throw new UnsupportedOperationException("We can not create an attribute from this line: "+line);
        }

    }

    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    @Override
    public boolean doesItReadThisFileExtension(String fileExtension) {
        return fileExtension.equals(".arff");
    }


    @Override
    public Stream<DataRow> stream() {
        Stream<String> streamString =null;
        try{
            streamString = Files.lines(pathFile);
        }catch (IOException ex){
            throw new UncheckedIOException(ex);
        }
        return streamString.skip(this.dataLineCount).map(line -> new DataRowWeka(this.attributes, line));
    }


    private static class DataRowWeka implements DataRow{
        double[] data;

        public DataRowWeka(Attributes atts, String line){
            data = new double[atts.getNumberOfAttributes()];
            String[] parts = line.split(",");
            for (int i = 0; i < parts.length; i++) {
                switch (atts.getList().get(i).getStateSpace().getStateSpaceType()){
                    case REAL:
                        data[i]=Double.parseDouble(parts[i]);
                        break;
                    case FINITE_SET:
                        FiniteStateSpace finiteStateSpace = atts.getList().get(i).getStateSpace();
                        data[i]=finiteStateSpace.getIndexOfState(parts[i]);
                }
            }
        }

        @Override
        public double getValue(Attribute att) {
            return data[att.getIndex()];
        }
    }
}
