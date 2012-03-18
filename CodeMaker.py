"""This script generates a simulated code-11 barcode reading
The process is as follows:
    1. get a string representation of the code
    2. remove invalid characters from the code string
    3. append the C check value to the code string
    4. append the K check value to the code string
    5. convert the code to a bit string where:
        0= narrow bar
        1= wide bar
        code character substrings are separated by a 0
    6. Compute a random wide bar width in [1,200] and the corresponding narrow bar width
    7. convert the bit string to a list of integer widths where each width is fudged by up to +/- 5%
    8. randomly reverse the integer list or not"""

import sys,re,random,string
from subprocess import call
from argparse import ArgumentParser

# mapping from code char -> bit String
bitStrs={"0":"00001","1":"10001","2":"01001","3":"11000","4":"00101", \
             "5":"10100","6":"01100","7":"00011","8":"10010","9":"10000", \
             "-":"00100", "Start":"00110", "Stop":"00110"}

def w(char):             #function to return character weight
    try:
        return int(char) # try making the char an int weight
    except Exception, e: # must not be an integer character
        if char == '-':  # make sure it's a hyphen
            return 10    # return hyphen weight
        else:
            raise e      # bad character

def C(code):                                 # function to generate C check value
    c = 0                                    # set c to zero
    n = len(code)                            # get length of code string
    for i in range(1,n+1):                   # loop over code string length, perform summation
        c += (((n-i)%10) + 1) * w(code[i-1]) # compute this step of sum
    c %= 11
    if c == 10:
        # we want one character for c, 10 is two characters        
        c = '-'
    return c                              # return summation mod 11, C value

def K(code):                                # function to generate K check value
    k = 0                                   # set k to zero
    n = len(code)                           # get length of code string
    for i in range(1,n+1):                  # loop over code string length, summation
        k += (((n-i)%9) +1) * w(code[i-1])  # compute this step of sum, using (n-i) not (n-i +1) because n redefined to include C
    k %= 11
    if k == 10:
        # we want one character for k, 10 is two characters
        k = '-'    
    return k                             # return summation mod 11, K value

def randCode(maxLength):
    length = random.randint(1, maxLength)
    return ''.join(random.choice(string.digits + '-') for i in xrange(length))

def code(length):
    return ''.join(random.choice(string.digits + '-') for i in xrange(length))
    
def getCodeCK(dirtyCode, badC=False, badK=False):
    code = re.sub(r"[^0-9\-]","",dirtyCode)     # remove invalid characters from input
    chars = string.digits + '-'
    #compute and append C to code string
    c = str(C(code))    
    if badC:
        c = random.choice(chars.replace(c, ''))
        
    codeC = code + c

    #compute and append K to code string
    k = str(K(codeC))    
    if badK:
        k = random.choice(chars.replace(k, ''))
        
    return codeC + k

def genBadC(dirtyCode):
    codeCK = getCodeCK(dirtyCode, badC=True)
    return genValues(codeCK)

def genBadK(dirtyCode):
    codeCK = getCodeCK(dirtyCode, badK=True)
    return genValues(codeCK)
    
def genGood(dirtyCode):
    codeCK = getCodeCK(dirtyCode)
    return genValues(codeCK)
    
def genValues(codeCK):
    #compute the bitstring for this code
    codeCKBitStr = bitStrs["Start"] + "0"   # begin with start char and spacer
    for char in codeCK:                     # loop over all chars in code
        codeCKBitStr += bitStrs[char] + "0" # append bitstring for this char and spacer
    codeCKBitStr += bitStrs["Stop"]         # append stop character

    fudge = lambda val: val+int(val*(-0.05+0.1*random.random())) # function to apply random error to a value, +/- 5%
    
    # get some value for narrow width
    # max is 95, since 92*2 = 190, and max(fudge(190)) == 200 (199.5 rounded up)
    narrow = random.randint(1,95)
    wide = narrow*2       # compute wide width    
        
    width = {"0":narrow,"1":wide} # mapping bit character -> bar width

    values = [] # list to hold integer values for final representation

    for b in codeCKBitStr:              #loop over bits characters in code bit string
        values.append(fudge(width[b]))  #get width for this bit, fudge it, then append to the list
    if random.random() > 0.50:          #the code could be backwards, reverse it half the time
        values.reverse()
        
    return values

parser = ArgumentParser()
parser.add_argument('-r', '--numRand', type=int, default=0, help='if you want randomly generated cases, specify the number of cases.')
parser.add_argument('-c', '--code', nargs='+', help='specific codes you would like to test.  If random cases are also generated, these will be the first cases.')
parser.add_argument('-m', '--maxLength', type=int, help="maximum length of the test strings. \
                    default is 21 for max input length of 150 (150/6 = 25, sub 4 for start/stop/c/k).", default=21)
parser.add_argument('-f', '--file', default='testInput', help="file name to write test input to.  default is 'testInput'")
parser.add_argument('-t', '--testFile', help='optionally print the expected java output to a the specified file')
parser.add_argument('-v', '--verbose', action='store_true', help='print the values to command line')
args = parser.parse_args()

if not args.code and args.numRand == 0:
    args.numRand = 1 # send at least one output
    
allValues = []
allCodes = []
if args.code: # get input code from command line arg
    for code in args.code:
        allCodes.append(code)
        values = genGood(code)
        values.insert(0,len(values)) # first number is length of input
        allValues.append(values)
    
# generate the desired number of random cases.
# if the random arg was not set, numRand will be 0
for i in xrange(args.numRand):
    code = randCode(args.maxLength)
    rand = random.random()
    if rand < 0.33:
        allCodes.append(code)
        values = genGood(code)
    elif rand < 0.66:
        allCodes.append('badC')
        values = genBadC(code)
    else:
        allCodes.append('badK')
        values = genBadK(code)                            
    values.insert(0, len(values)) # first number is length of input
    allValues.append(values)

# output to file and run BarCodes
f = open(args.file, 'w')
for values in allValues:
    f.write(str(values[0]) + '\n')
    for v in values[1:]:
        f.write(str(v) + " ")
    f.write('\n')
f.write('0\n')
f.close()

# run BarCodes using the file
cmd = ["java", "BarCodes2", args.file]
call(cmd)
    
# print to command line as well if verbose option is set
if args.verbose:
    for line in open(args.file, 'r').readlines():
        print line    
    
if args.testFile:
    f = open(args.testFile, 'w')
    # write expected output of each case to the file
    for i in xrange(len(allCodes)):
        if allCodes[i] == 'badC':
            f.write('Case ' + str(i + 1) + ': bad C\n')
        elif allCodes[i] == 'badK':
            f.write('Case ' + str(i + 1) + ': bad K\n')
        else:
            f.write('Case ' + str(i + 1) + ': ' + allCodes[i] + '\n')
    f.close()
    
