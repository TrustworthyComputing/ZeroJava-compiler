#!/usr/bin/env python

import os
import argparse
import subprocess

# Parse arguments
def parseArgs():
    parser = argparse.ArgumentParser(description='Zilch to C')
    parser.add_argument('--filename', help='path to Zilch file (.zl)', required=True)
    parser.add_argument('--pubtape', help='path to primary tape file')
    parser.add_argument('--auxtape', help='path to auxiliary/private tape file')
    args = parser.parse_args()
    if not args.filename.endswith(".zl"):
        print("Zilch code filename should end with '.zl' extension.")
        exit(-1)
    if args.pubtape is not None:
        if not os.path.isfile(args.pubtape):
            print("Public tape file '" + args.pubtape + "' does not exist.")
            exit(-2)
    if args.auxtape is not None:
        if not os.path.isfile(args.auxtape):
            print("Auxiliary (private) tape file '" + args.auxtape + "' does not exist.")
            exit(-2)
    return args.filename, args.pubtape, args.auxtape


def getArraySizes(filename):
    array_sizes = {}
    with open(filename) as ifp:
        for line in ifp:
            str = line.strip()
            if 'new int[' in str:
                var = str.split('=')[0].strip()
                size = str.split('new int[')[1][:-2]
                array_sizes[var] = size
    return array_sizes


def main(filename, pubtape, auxtape, array_sizes):
    c_outputfile = filename[:-3] + ".c"
    with open(c_outputfile, 'w') as ofp:
        ofp.write('#include <stdio.h>\n')
        with open(filename) as ifp:
            for line in ifp:
                str = line.strip()
                if 'void main(' in str:
                    while '{' not in str:
                        line = next(ifp)
                        str = line.strip()
                    ofp.write('void main(void) {\n')
                    if pubtape is not None:
                        ofp.write('FILE *pubfp = fopen("' + pubtape + '", "r");\n')
                    if auxtape is not None:
                        ofp.write('FILE *auxfp = fopen("' + auxtape + '", "r");\n')
                elif str.startswith('Prover.answer('):
                    ofp.write('printf("%d\\n", ' + str[len('Prover.answer('):] + "\n")
                elif str.startswith('int[]'):  # int[] arr; --> int arr[size];
                    var = str.split('int[]')[1][:-1].strip()
                    ofp.write('int ' + var + '[' + array_sizes[var] + '];\n')
                elif 'new int[' in str:
                    continue
                elif str.startswith('PrimaryTape.read('):
                    if pubtape is None:
                        print("Cannot use PrimaryTape.read without providing a public tape file.")
                        exit(-1)
                    ofp.write('fscanf(auxfp, "%d", &' + str[len('PrimaryTape.read('):] + "\n")
                elif str.startswith('PrivateTape.read('):
                    if auxtape is None:
                        print("Cannot use PrivateTape.read without providing a auxiliary tape file.")
                        exit(-1)
                    ofp.write('fscanf(pubfp, "%d", &' + str[len('PrivateTape.read('):] + "\n")
                else:
                    ofp.write(line)
    return c_outputfile

  
if __name__== "__main__":
    filename, pubtape, auxtape = parseArgs()
    array_sizes = getArraySizes(filename)
    c_outputfile = main(filename, pubtape, auxtape, array_sizes)
    c_out = c_outputfile + '.out'
    cmd_result = subprocess.call(['bash','-c', 'gcc ' + c_outputfile + ' -o ' + c_out])
    if cmd_result != 0:
        os.remove(c_outputfile)
        exit(-1)
    cmd_result = subprocess.check_output(['bash','-c', c_out])
    print cmd_result
    os.remove(c_outputfile)
