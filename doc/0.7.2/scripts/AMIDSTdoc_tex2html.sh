#!/bin/bash
#



cd "$(dirname "$0")";



root='..';
src='tex';
tag='html';
version=`cat ../version.txt`;
useTemplates=0;
removeTitle=0;
imgLinks=1;

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

	echo ${origin}' to '${dest};


    ./preprocessTex.py ${origin} | cat > ${origin}.aux;

	if [ $useTemplates -eq 1 ]; then
	    cat templates/header.html > ${dest};
	fi;
    iconv -t utf-8 ${origin}.aux  | pandoc --to html --from latex  >> ${dest};


    if [ $useTemplates -eq 1 ]; then
	    cat templates/footer.html >> ${dest};
	fi;

	sed -i '' 's/<p class="caption">/<p class="caption" style="text-align:center">/g' ${dest};
	sed -i '' 's/class="figure"/class="figure" style="text-align:center"/g' ${dest};
	

	sed -i '' "s/(\*\\\\amidstversion\*)/${version}/g" ${dest};	
	
    if [ $removeTitle -eq 1 ]; then
	    sed -i '' '1d' ${dest};
	fi;

	if [ $imgLinks -eq 1 ]; then
	    ./replaceImgPath.py ${dest} | cat > ${dest}.aux;
	    rm ${dest};
	    mv ${dest}.aux ${dest};
	fi;


	rm ${origin/'.tex'/'.pdf'};
	rm ${origin}.aux;
	rm $origin;
	
#	read -p "Press [Enter] key to start backup...";
done




find ${root}/${tag} -name "properties.html" -type f -delete;

