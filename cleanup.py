import os
import sys

GHIDRA_BIN_FILE = "ghidra_bin_location.txt"

dir = open(GHIDRA_BIN_FILE, "rb")
dir_str = dir.read().decode("utf-8").rstrip("\n\r")
pwd = os.path.dirname(os.path.realpath(__file__))
if(len(dir_str) < 1) :
    print("empty string in file")
    exit(0)
files = []
p = os.listdir(pwd)
for item in p:
    if(item.endswith(".java")):
        files.append(str(item[:-5]))
o = os.listdir(dir_str)
for item in o:
    if(item.endswith(".class") and item[:-6] in files):
        os.remove(dir_str + item)
        print("deleted: "+item)
