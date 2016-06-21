package eu.amidst.sparklink.core.data;

import eu.amidst.core.datastream.Attributes;

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.sparklink.core.io.SchemaConverter;
import org.apache.commons.lang.NotImplementedException;
import org.apache.spark.sql.DataFrame;


/**
 * Created by jarias on 20/06/16.
 */


public class DataSpark {

    private final DataFrame data;

    private Attributes attributes;

    public DataSpark(DataFrame d) throws Exception {

        data = d.cache();
        attributes = SchemaConverter.getAttributes(data);

    }

    public Attributes getAttributes() {

        return attributes;
    }
}
