package eu.amidst.ecai2016;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by dario on 22/4/16.
 */
public class NIPS_ConvertNFSAbstractFilesToARFF {

    public static void main(String[] args) throws FileNotFoundException, IOException {

        String originalDataFolder = "/Users/dario/Desktop/AMIDST otros/datos/datosNSFAbstracts/";
        String outputFolder = "/Users/dario/Desktop/AMIDST otros/datos/datosNSFAbstracts/abstractsFormatoARFF/";

        String abstractIdentifiersFile = "identificadoresTodos.txt";
        String wordsIdentifiersFile = "words.txt";

        String line;

        // TRANSFORM THE DICTIONARY OF WORDS INTO WEKA SPARSE FORMAT
        FileReader reader; //= new FileReader(originalDataFolder + wordsIdentifiersFile);

        FileInputStream fileInputStream = new FileInputStream(originalDataFolder + wordsIdentifiersFile);
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "Windows-1252");
        BufferedReader input =  new BufferedReader(inputStreamReader);

        FileWriter writer;

        FileOutputStream fileOutputStream = new FileOutputStream(outputFolder  + wordsIdentifiersFile);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "Windows-1252");
        BufferedWriter output =  new BufferedWriter(outputStreamWriter);


        output.write("@RELATION NSFAbstracts");
        output.newLine();
        output.newLine();


        int lineNumber = 0;
        while ( (line=input.readLine()) != null ) {

            lineNumber++;

            StringTokenizer tokenizer = new StringTokenizer(line);
            int word_index = Integer.parseInt(tokenizer.nextToken());
            String word = tokenizer.nextToken();

            output.write("@ATTRIBUTE " + word + " integer");
            output.newLine();

            if(lineNumber>30783) {
                System.out.println(word);
            }

        }

        output.close();
        outputStreamWriter.close();
        fileOutputStream.close();
        //writer.close();
        input.close();
        inputStreamReader.close();
        fileInputStream.close();
        //reader.close();

        // GENERATE AN ARFF FILE FOR EACH YEAR AND SET THE ATTRIBUTE DEFINITIONS
        for (int i = 1990; i <= 2003; i++) {

            //reader = new FileReader(outputFolder + wordsIdentifiersFile);
            fileInputStream = new FileInputStream(outputFolder + wordsIdentifiersFile);
            inputStreamReader = new InputStreamReader(fileInputStream, "Windows-1252");
            input =  new BufferedReader(inputStreamReader);

            //writer = new FileWriter(outputFolder  + "year" + i + ".arff");
            fileOutputStream = new FileOutputStream(outputFolder  + "year" + i + ".arff");
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, "Windows-1252");
            output =  new BufferedWriter(outputStreamWriter);

            while( (line = input.readLine())!=null ) {
                output.write(line);
                output.newLine();
            }

            output.newLine();
            output.write("@DATA");
            output.newLine();
            output.newLine();

            output.close();
            outputStreamWriter.close();
            fileOutputStream.close();
            input.close();
            inputStreamReader.close();
            fileInputStream.close();
        }



        // READ THE CORRESPONDENCE BETWEEN ABSTRACT INDICES AND YEARS
        reader = new FileReader(originalDataFolder + abstractIdentifiersFile);
        input =  new BufferedReader(reader);

        Map<Integer,Integer> abstractToYearCorrespondence = new HashMap<>();

        while( (line = input.readLine())!=null ) {

            StringTokenizer tokenizer = new StringTokenizer(line);
            int abstract_index = Integer.parseInt(tokenizer.nextToken());
            String abstract_code = tokenizer.nextToken();

            int year2digits = Integer.parseInt(abstract_code.substring(1,3));
            int year = ( (year2digits>=90) ? 1900+year2digits : 2000+year2digits);

            abstractToYearCorrespondence.put(abstract_index,year);
        }
        reader.close();
        input.close();




        // INTRODUCE WORDS DATA IN EACH OF THE PREVIOUSLY GENERATED ARFF FILES

        for (int i = 1; i <= 3; i++) {
            String wordsFile = "nsfabs_part" + i + "_out/docwords.txt";

            reader = new FileReader(originalDataFolder + wordsFile);
            input =  new BufferedReader(reader);

            StringBuilder dataLine  = new StringBuilder();
            int previous_abstract_index = 0;
            int previous_year = 0;
            int year = 0;

            while( (line = input.readLine())!=null ) {

                StringTokenizer tokenizer = new StringTokenizer(line);
                int abstract_index = Integer.parseInt(tokenizer.nextToken());
                int word_index = Integer.parseInt(tokenizer.nextToken());
                int word_frequency = Integer.parseInt(tokenizer.nextToken());

                year = abstractToYearCorrespondence.get(abstract_index);

                if (previous_abstract_index==0) {

                    // Change this line for cheking:
                    // dataLine.append(abstract_index + "{");
                    dataLine.append("{");

                    previous_abstract_index = abstract_index;
                    previous_year = year;
                }
                else if (previous_abstract_index!=abstract_index) {

                    dataLine.replace(dataLine.lastIndexOf(","),dataLine.lastIndexOf(",")+1,"");
                    dataLine.append("}");

                    writer = new FileWriter(outputFolder  + "year" + previous_year + ".arff",true);
                    output =  new BufferedWriter(writer);

                    output.write(dataLine.toString());
                    output.newLine();

                    output.close();
                    writer.close();


                    dataLine = new StringBuilder();

                    // Change this line for cheking:
                    // dataLine.append(abstract_index + "{");
                    dataLine.append("{");

                    previous_abstract_index = abstract_index;
                    previous_year = year;
                }

                dataLine.append(word_index-1);
                dataLine.append(" ");
                dataLine.append(word_frequency);
                dataLine.append(",");
            }

            dataLine.replace(dataLine.lastIndexOf(","),dataLine.lastIndexOf(",")+1,"");
            dataLine.append("}");

            writer = new FileWriter(outputFolder  + "year" + year + ".arff",true);
            output =  new BufferedWriter(writer);

            output.write(dataLine.toString());
            output.newLine();

            output.close();
            writer.close();

            input.close();
            reader.close();

        }
    }
}
