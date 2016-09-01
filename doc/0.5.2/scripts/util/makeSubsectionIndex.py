#!/usr/bin/python


import sys
import re
import os






def getSubsectionIndex( file, fileFolder):


    initialPath=os.getcwd();

    pattern1 = re.compile('(.*)subsection{(.*)}(.*)label{(.*)}(.*)')

    newCode="";

    for i, line in enumerate(file):
        match1 = re.search(pattern1,line,flags=0);



        if match1:
            codePath=match1.group(1);

            print '\item \hyperref['+match1.group(4)+']{'+match1.group(2)+'}';


    return newCode;






def main(argv):


    if len(argv)>1:
        filePath=sys.argv[1];
    else:
        filePath="../../tex/examples/bnetworks.tex";

    match=re.search('(.*)/(.*)\.(.*)', filePath,flags=0);

    fileFolder=match.group(1);
    fileName=match.group(2);
    fileExt=match.group(3);


    file=open(filePath);

    texCode = getSubsectionIndex(file, fileFolder);

    print texCode;
    # my code here

if __name__ == "__main__":


    main(sys.argv)

