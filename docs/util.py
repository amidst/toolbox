# -*- coding: utf-8 -*-

import os
import sys

# function that extract a String with the content of ../version.txt
def get_version():
    # Open file
    file = open("../version.txt")
    return file.read()
    





if __name__ == "__main__":
	print(get_version())
	
