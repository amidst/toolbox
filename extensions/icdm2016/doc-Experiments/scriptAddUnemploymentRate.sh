join -t, unemploymentRateAlmeria.csv data.csv
awk -F "," 'BEGIN { OFS="," } {print $1,$3,$2,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15}' joinedData.csv > joinedDataReordered.csv
head -n 17 datosWekaUnemploymentRate.arff > header.txt
cat header.txt joinedDataReordered.csv > dataWekaUnemploymentRate.arff

#Add column of zeros:
awk '{print $1,$2,1,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14}' FS=, OFS=, data.csv > dataExtraDumbColumn1.csv
cat header.txt dataExtraDumbColumn1.csv > dataWekaDummyAttribute1.arff

#Add column of series of increments of 1:
awk 'BEGIN{t=0} {print $1,$2,t,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14;t+=1}' FS=, OFS=, F='0' data.csv > dataExtraIncDumbColumn.csv
cat header.txt dataExtraIncDumbColumn.csv > dataWekaDummyIncAttribute.arff