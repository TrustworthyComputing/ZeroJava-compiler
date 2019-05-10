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
    if not os.path.isfile(args.filename):
        print("Input file '" + args.filename + "' does not exist.")
        exit(-2)
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
                    ofp.write('int main(void) {\n')
                    if pubtape is not None:
                        ofp.write('FILE *pubfp = fopen("' + pubtape + '", "r");\n')
                        ofp.write('int pub_tape[1000];\n')
                        ofp.write('int pub_tape_idx = 0;\n')
                        ofp.write('while (!feof(pubfp)) {\n')
                        ofp.write('    fscanf(pubfp, "%d", &pub_tape[pub_tape_idx]);\n')
                        ofp.write('    pub_tape_idx++;\n')
                        ofp.write('}\n')
                        ofp.write('fclose(pubfp);\n')
                        ofp.write('pub_tape_idx = 0;\n')
                    if auxtape is not None:
                        ofp.write('FILE *auxfp = fopen("'+ auxtape + '", "r");\n')
                        ofp.write('int priv_tape[1000];\n')
                        ofp.write('int priv_tape_idx = 0;\n')
                        ofp.write('while (!feof(auxfp)) {\n')
                        ofp.write('    fscanf(auxfp, "%d", &priv_tape[priv_tape_idx]);\n')
                        ofp.write('    priv_tape_idx++;\n')
                        ofp.write('}\n')
                        ofp.write('fclose(auxfp);\n')
                        ofp.write('priv_tape_idx = 0;\n')
                elif str.startswith('Prover.answer('):
                    ofp.write('printf("%d\\n", ' + str[len('Prover.answer('):] + "\n")
                    ofp.write('return 0;\n')
                elif str.startswith('int[]'):  # int[] arr; --> int arr[size];
                    continue
                    var = str.split('int[]')[1][:-1].strip()
                    ofp.write('int ' + var + '[' + array_sizes[var] + '];\n')
                elif 'new int[' in str:
                    var = str.split('=')[0].strip()
                    ofp.write('int ' + var + '[' + array_sizes[var] + '];\n')
                elif str.startswith('PrimaryTape.read('):
                    if pubtape is None:
                        print("Cannot use PrimaryTape.read without providing a public tape file.")
                        exit(-1)
                    var = str[len('PrimaryTape.read('):-2]
                    ofp.write(var + ' = pub_tape[pub_tape_idx++];\n')
                elif str.startswith('PrivateTape.read('):
                    if auxtape is None:
                        print("Cannot use PrivateTape.read without providing a auxiliary tape file.")
                        exit(-1)
                    var = str[len('PrivateTape.read('):-2]
                    ofp.write(var + ' = priv_tape[priv_tape_idx++];\n')
                elif str.startswith('PrimaryTape.seek('):
                    if pubtape is None:
                        print("Cannot use PrimaryTape.seek without providing a public tape file.")
                        exit(-1)
                    parts = str.split(',')
                    idx = parts[1].strip()[:-2]
                    var = parts[0][len('PrimaryTape.seek('):]
                    ofp.write(var + ' = pub_tape[' + idx + '];\n')
                elif str.startswith('PrivateTape.seek('):
                    if auxtape is None:
                        print("Cannot use PrivateTape.seek without providing a auxiliary tape file.")
                        exit(-1)
                    parts = str.split(',')
                    idx = parts[1].strip()[:-2]
                    var = parts[0][len('PrivateTape.seek('):]
                    ofp.write(var + ' = priv_tape[' + idx + '];\n')
                elif str.startswith('Out.print('):
                    var = str[len('Out.print('):-2]
                    ofp.write('printf("'+var+': %d\\n", '+var+');\n')
                else:
                    ofp.write(line)
    return c_outputfile

  
if __name__== "__main__":
    filename, pubtape, auxtape = parseArgs()
    array_sizes = getArraySizes(filename)
    c_outputfile = main(filename, pubtape, auxtape, array_sizes)
    c_out = c_outputfile + '.out'
    c_out = os.path.abspath(c_out)
    cmd_result = subprocess.call(['bash','-c', 'gcc ' + c_outputfile + ' -o ' + c_out])
    if cmd_result != 0:
        os.remove(c_outputfile)
        exit(-1)
    cmd_result = subprocess.check_output(['bash','-c', c_out])
    print cmd_result
    os.remove(c_out)
    os.remove(c_outputfile)
