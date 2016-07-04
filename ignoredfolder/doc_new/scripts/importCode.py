#!/usr/bin/python

import sys
import re
import os

#algun problema con el path

initialPath=os.getcwd();



#return

file=sys.argv[1];

match=re.search("(.*)/(.*)\.(.*)", file,flags=0);
filePath=match.group(1);
fileName=match.group(2);
fileExt=match.group(3);


pattern = re.compile("\includejavasource{(.*)}")

newCode="";

for i, line in enumerate(open(file)):
	matchObj = re.search(pattern, line,flags=0)
	if matchObj:
		codePath=matchObj.group(1);
		
		
		os.chdir(filePath);
		
		with open(codePath, 'r') as content_file:
			code = content_file.read()	
			
		os.chdir(initialPath);
		
				
		replacement="\\begin{lstlisting}\n"+code+"\n\\end{lstlisting}"; 
		#print replacement;	
		newCode=newCode+replacement;
		
	else:
   		newCode=newCode+line;
   		
print newCode;
   
   



