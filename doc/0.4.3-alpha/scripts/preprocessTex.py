#!/usr/bin/python


import sys
import re
import os






def preprocessTex( file, fileFolder):


    initialPath=os.getcwd();

    pattern1 = re.compile('\includejavasource{(.*)}')
    pattern2 = re.compile('(.*)\hyperref\[sec:(.*)\]\{(.*)\}(.*)')

    newCode="";

    for i, line in enumerate(file):
        match1 = re.search(pattern1,line,flags=0);
        match2 = re.search(pattern2,line,flags=0);


        if match1:
            codePath=match1.group(1);



            os.chdir(fileFolder);


            with open(codePath, 'r') as content_file:
                code = content_file.read()

            os.chdir(initialPath);

            replacement="\\begin{lstlisting}\n"+code+"\n\\end{lstlisting}";
            #print replacement;
            newCode=newCode+replacement;


        elif match2:



            codePath=match2.group(0);
            replacement=match2.group(1)+"href{#sec:"+match2.group(2)+"}{"+match2.group(3)+"}"+match2.group(4)
            newCode=newCode+replacement;

        else:


            newCode=newCode+line;

    return newCode;






def main(argv):


    if len(argv)>1:
        filePath=sys.argv[1];
    else:
        filePath="../tex/integration/flinklink.tex";

    match=re.search('(.*)/(.*)\.(.*)', filePath,flags=0);

    fileFolder=match.group(1);
    fileName=match.group(2);
    fileExt=match.group(3);


    file=open(filePath);

    texCode = preprocessTex(file, fileFolder);

    print texCode;
    # my code here

if __name__ == "__main__":


    main(sys.argv)

