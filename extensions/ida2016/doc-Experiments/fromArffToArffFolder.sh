for i in {0..83};
 do 
cat datosWeka.arff | grep "^$i,"> data/$i;
done