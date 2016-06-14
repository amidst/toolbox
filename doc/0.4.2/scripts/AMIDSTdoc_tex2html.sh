#!/bin/bash
#



cd "$(dirname "$0")";



root='..';
src='tex';
tag='html';


pandflag=`which pandoc | wc -l`; 


if [ $pandflag -eq 0 ]
then
	echo 'ERROR: pandoc is not installed in the system'
	exit;
fi;


if [ ! -d ${root}/${src}  ]; then
	echo 'ERROR: source folder '${root}/${src}' not found';
	exit;
fi






rm -rf ${root}/${tag};
cp -R ${root}/${src} ${root}/${tag};
cp -R ${root}/scripts/stylesheets/ ${root}/${src}/stylesheets


find ${root}/${tag} -name "*.aux" -type f -delete;
find ${root}/${tag} -name "*.log" -type f -delete;
find ${root}/${tag} -name "*.out" -type f -delete;
find ${root}/${tag} -name "*.synctex.gz" -type f -delete;

for f in $(find ${root}/${tag} -name '*.tex'); 
do  
	#${root}/scripts/tex2AMIDSThtml $f;
	
	
	#### file conversion ####
	origin=$f;
	dest=${origin/'.tex'/'.html'};

	./importCode.py ${origin} | cat > ${origin}.aux;

	echo ${origin}' to '${dest};
	cat templates/header.html > ${dest};
	pandoc --to html --from latex ${origin} >> ${dest};
	#iconv -t utf-8 ${origin}  | pandoc --to html --from latex  >> ${dest};
	cat templates/footer.html >> ${dest};

	sed -i '' 's/<p class="caption">/<p class="caption" style="text-align:center">/g' ${dest};
	sed -i '' 's/class="figure"/class="figure" style="text-align:center"/g' ${dest};
	
	########################
	
	
	rm ${f/'.tex'/'.pdf'};
	rm $f;
done

