#!/usr/bin/python





import sys
import re
import os






def preprocessTex( file, fileFolder):

    print "preprocessTex"

    initialPath=os.getcwd();

    pattern = re.compile('\includejavasource{(.*)}')

    newCode="";

    for i, line in enumerate(file):
        matchObj = re.search(pattern, line,flags=0)
        if matchObj:
            codePath=matchObj.group(1);
            print codePath


            print os.getcwd()
            os.chdir(fileFolder);
            print os.getcwd()

            with open(codePath, 'r') as content_file:
                code = content_file.read()

            os.chdir(initialPath);


            replacement="\\begin{lstlisting}\n"+code+"\n\\end{lstlisting}";
            #print replacement;
            newCode=newCode+replacement;

        else:
            newCode=newCode+line;

    return newCode;
        





def main():

    filePath="../tex/examples/flinklink.tex";
    match=re.search('(.*)/(.*)\.(.*)', filePath,flags=0);

    fileFolder=match.group(1);
    fileName=match.group(2);
    fileExt=match.group(3);


    file=open(filePath);

    texCode = preprocessTex(file, fileFolder);
# my code here

if __name__ == "__main__":
    main()