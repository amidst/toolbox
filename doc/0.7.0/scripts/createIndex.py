#!/usr/bin/python


import sys
import re
import os
import string







def createIndex( file):


    initialPath=os.getcwd();


    pattern1 = re.compile('(.*)section{(.*)}\\\\label{(.*)}')

    tex="";
    prevLevel=0;

    for i, line in enumerate(file):
        match1 = re.search(pattern1,line,flags=0);


        if match1:
            levelStr=match1.group(1);
            name=match1.group(2);
            label=match1.group(3);

            if levelStr=="\\":
                level=0;
            elif levelStr=="\\sub":
                level=1;
            else:
                level=2;


            if(prevLevel<level):
                tex = tex+"\\begin{itemize}\n";
            if(prevLevel>level):
                tex = tex+"\\end{itemize}\n";

            if level > 0:
                tex = tex+ "\item \hyperref["+label+"]{"+name+"}\n";


            prevLevel=level;

    while level>0:
        tex = tex+"\\end{itemize}\n";
        level = level-1;

    return tex;






def main(argv):

    path = '../tex/examples/dbnetworks.tex'
    file=open(path);
    texCode = createIndex(file);



    print texCode;
    # my code here

if __name__ == "__main__":


    main(sys.argv)

