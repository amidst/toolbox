package eu.amidst.sparklink.core.data;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;

import static eu.amidst.core.variables.StateSpaceTypeEnum.REAL;

/**
 * Created by jarias on 20/06/16.
 */
public class SchemaConverter {


    static Attributes getAttributes(DataFrame data) throws Exception {

        ArrayList<Attribute> attributesList = new ArrayList();

        scala.Tuple2<String, String>[] schema = data.dtypes();

        // Parse the different fields to AMIDST attributes:
        for (int i = 0; i < schema.length; i++) {

            String name = schema[i]._1;
            String type = schema[i]._2;

            // Check the state space:
            StateSpaceType stateSpace;

            switch (type) {
                // Numeric Types:
                case "ByteType":
                case "ShortType":
                case "IntType":
                case "LongType":
                case "FloatType":
                case "DoubleType":
                    stateSpace = new RealStateSpace();
                    break;

                // Categorical Types
                case "StringType":
                    // FIXME: Can we analise the whole data just once?
                    ArrayList<String> states = getColumnStates(data, name);
                    stateSpace = new FiniteStateSpace(states);
                    break;

                case "BooleanType":
                default:
                    // FIXME: Create custom exception
                    throw new Exception("Unsupported Format " + type);
            }

            attributesList.add( new Attribute(i, name, stateSpace) );
        }

        return new Attributes(attributesList);
    }


    private static ArrayList<String> getColumnStates(DataFrame data, String name) {

        ArrayList<String> states = new ArrayList();

        final Row[] statesRow = data.select(name).distinct().collect();

        for (Row r : statesRow)
            states.add( r.getString(0) );

        return states;
    }


    static StructType getSchema(Attributes atts) {

        // Generate the schema based on the list of attributes and depending on their type:
        List<StructField> fields = new ArrayList<StructField>();

        for (Attribute att: atts.getFullListOfAttributes()) {

            if (att.getStateSpaceType().getStateSpaceTypeEnum() == REAL)
                fields.add(DataTypes.createStructField(att.getName(), DataTypes.DoubleType, true));
            else
                fields.add(DataTypes.createStructField(att.getName(), DataTypes.StringType, true));

        }
        return DataTypes.createStructType(fields);
    }

}
