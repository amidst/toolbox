join -t, unemploymentRateAlmeria.csv data.csv
awk -F "," 'BEGIN { OFS="," } {print $1,$3,$2,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15}' joinedData.csv > joinedDataReordered.csv
head -n 17 datosWekaUnemploymentRate.arff > header.txt
cat header.txt joinedDataReordered.csv > dataWekaUnemploymentRate.arff
