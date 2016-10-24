#!/usr/bin/python


import sys
import re
import os
import string







def preprocessHtml( file ):

   # print("changing paths to images...");

    domain="http://www.amidsttoolbox.com";
    pathimg="wp-content/uploads";

    from datetime import date
    year=str(date.today().year);
    month=str(date.today().month);
    if date.today().month<10:
        month="0"+month;

    pattern1 = re.compile('<img src="(.*)/(.*)" alt(.*)')
    pattern2 = re.compile('(.*)<a href="(.*).html">(.*)');


    newCode="";

    for i, line in enumerate(file):
        match1 = re.search(pattern1,line,flags=0);
        match2 = re.search(pattern2,line,flags=0);

        if match1:
            name=match1.group(2);
            endline=match1.group(3);
            newCode=newCode+'<img src="'+domain+'/'+pathimg+'/'+year+'/'+month+'/'+name+'" alt'+endline;

        elif match2:
            begLine = match2.group(1);
            endline = match2.group(3);
            post = match2.group(2);
            if "http" not in post:
                newCode = newCode + begLine+'<a href="'+post+'/">'+endline;
            else:
                newCode=newCode+line;

        else:


            newCode=newCode+line;

    return newCode;






def main(argv):


    if len(argv)>1:
        filePath=sys.argv[1];
    else:
        filePath="../html/first-steps/getting-started.html";


    file=open(filePath);

    texCode = preprocessHtml(file);

    print texCode;
    # my code here

if __name__ == "__main__":


    main(sys.argv)

