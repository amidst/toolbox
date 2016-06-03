#!/bin/bash




read -p "Have you manually updated the version in README.md and in documentation folder? (y/n)" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Nn]$ ]]
then
	echo "Do it and run again the script";
    exit 1
fi


cd ../toolbox;
export JAVA_HOME=$(/usr/libexec/java_home);


version=$1;
mvnargs=$2;




undoStep=0;



if [ `cat ~/.m2/settings.xml | grep "<gpg.executable>gpg2" | wc -l` -eq 1 ]
then

	echo 'checking mvn gpg profile... ok';

else

	echo 'checking up-to-date branch... FAILED: add to ~/.m2/settings.xml a gpg2 profile adding this:';
	printf "<profiles> \n\
    <profile> \n\
      <id>gpg</id> \n\
      <properties> \n\
        <gpg.executable>gpg2<\gpg.executable>\n\
        <gpg.passphrase>[password]</gpg.passphrase> \n\
      </properties> \n\
    </profile> \n\
  </profiles> \n\
  <activeProfiles> \n\
    <activeProfile>gpg</activeProfile> \n\
  </activeProfiles> \n\
";
	exit;

fi;


if [ `cat ~/.m2/settings.xml | grep "<server>" -A 5 | grep github -A 3 -B 3 | grep username -A 3 -B 3 | grep password | wc -l` -eq 1 ]
then
	echo 'checking mvn server configuration... ok '
else
	echo 'checking mvn server configuration... FAILED: add to ~/.m2/settings.xml a github server called github adding this:';
	printf " <servers> \n\
        <server>\n\
            <id>github</id>\n\
            <username>[github_user]</username>\n\
            <password>[github_pass]</password>\n\
        </server>\n\
    </servers>\n\
    ";
    
    exit;

fi;





if [ `git rev-parse --abbrev-ref HEAD` == "develop" ] 
then
	echo 'checking current branch... ok';
else
	echo 'checking current branch... FAILED (not in develop)';
	exit;
fi;


if [ `git pull | grep up-to-date | wc -l` -eq 1 ]
then

	echo 'checking up-to-date branch... ok';

else

	echo 'checking up-to-date branch... FAILED';
	exit;

fi;



if [ `git status | grep working\ directory\ clean | wc -l` -eq 1 ]
then

	echo 'checking changes to commit... ok';

else

	echo 'checking up-to-date branch... FAILED (there are changes to commit)';
	exit;

fi;



if [ `git branch -a | grep release-${version} | wc -l` -eq 0 ]
then

	echo 'checking remote branches... ok';

else

	echo 'checking remote branches... FAILED (existing branch for the release)';
	exit;


fi;


#Create new branch

read -p "A new branch release-"${version}" is going to be created, are you sure? " -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Nn]$ ]]
then
    exit 1
fi


git branch release-${version};
((undoStep++));

git checkout release-${version};
((undoStep++));





if [ `git rev-parse --abbrev-ref HEAD` == "release-"${version} ] 
then
	echo 'checking new branch... ok';
else
	echo 'checking new branch... FAILED (not switched to new branch)';
	exit;
fi;






#Replace version macro

sed -i '' 's/<AMIDSTver>/<!--AMIDSTver>/g' pom.xml; 
sed -i '' 's/<\/AMIDSTver>/<\/AMIDSTver-->/g' pom.xml; 
sed -i '' 's/<AMIDSTver>/<!--AMIDSTver>/g' */pom.xml; 
sed -i '' 's/<\/AMIDSTver>/<\/AMIDSTver-->/g' */pom.xml; 
sed -i '' 's/<AMIDSTver>/<!--AMIDSTver>/g' */*/pom.xml; 
sed -i '' 's/<\/AMIDSTver>/<\/AMIDSTver-->/g' */*/pom.xml; 

sed -i '' 's/${AMIDSTver}/'$version'/g' pom.xml;
sed -i '' 's/${AMIDSTver}/'$version'/g' */pom.xml;
sed -i '' 's/${AMIDSTver}/'$version'/g' */*/pom.xml;

declare -a mblist
let i=0
while IFS=$'\n' read -r line_data; do
    mblist[i]="${array_element}" # Populate array.
    
    
    echo 'removing module '${line_data};
    
    rm -rf ${line_data}; 
    git rm -r ${line_data};
    
    
    line_data=${line_data/\//\\\/}
 	sed -i '' 's/<module>'${line_data}'<\/module>//g' pom.xml;   
    
    ((++i))
done < ../modules_blacklist.txt

echo "removing unnecessary folders"

rm -rf doc_old; 
git rm -r doc_old;

rm -rf scripts; 
git rm -r scripts;


echo "committing and pushing new brach";
git add ./*;
git commit -m "created branch for release "${version};
git push -u origin release-${version};

echo $undoStep;


sed -i '' 's/<!--plugin>/<plugin>/g' pom.xml;   
sed -i '' 's/<\/plugin-->/<\/plugin>/g' pom.xml;  



mvncmd='mvn clean deploy '${mvnargs};

echo "The command "${mvncmd}" is going to be run...";
read -p  "are you sure? " -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Nn]$ ]]
then
    exit 1
fi


$mvncmd;

git reset --hard;
git checkout develop;




exit;

if [ $undoStep -eq 2 ];
then
git reset --hard;
git checkout develop;
((undoStep--));

fi;

if [ $undoStep -eq 1 ];
then
git branch release-${version};
((undoStep--));
fi;