package eu.amidst.kbspaper;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by rcabanas on 14/08/2018.
 */
public class LogResults {

    private CSVPrinter printer = null;

    public LogResults(String filename) throws IOException {

        BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename));

        printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("tool", "network", "instances", "numvars", "size", "time"));


    }

    public void record(String tool, String network, long instances, int numvars, double size, long time) throws IOException {
        printer.printRecord(tool, network, instances,numvars,size,time);
        printer.flush();

    }

    public CSVPrinter getPrinter() {
        return printer;
    }

    public static void main(String[] args) throws IOException {
        LogResults log = new LogResults("extensions/KBS-paper/res.csv");

        log.record("hugin", "pca", 1000, 10, 300, 1234);
        log.record("hugin", "mog", 2000, 10, 300, 2113);




    }




}
