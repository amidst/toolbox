package eu.amidst.core.database;

import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 19/02/15.
 */
public interface DynamicDataBase extends DataBase<DynamicDataInstance> {

    Stream<DataSequence> streamOfDataSequences();

    Stream<DataSequence> parallelStreamOfDataSequences();

}
