for i in {0..83};
 do 
cat datosWeka.arff | grep "^$i,"> data/$i;
done

for i in {0..83};  do cp dataWekaFolder.arff/data/$i dataWekaFolder.arff/MONTH$i.arff/data/$i; done

for i in {0..83};
 do 
mkdir dataWekaFolder.arff/MONTH$i.arff/data
done

for i in {0..83};  do cp dataWekaFolder.arff/attributes.txt  dataWekaFolder.arff/MONTH$i.arff/; done