package eu.amidst.sparklink.core.io;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;

import java.util.ArrayList;

/**
 * Created by jarias on 20/06/16.
 */
public class SchemaConverter {


    public static Attributes getAttributes(DataFrame data) throws Exception {

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

}
