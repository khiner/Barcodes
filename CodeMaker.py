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
    return c%11                              # return summation mod 11, C value

def K(code):                                # function to generate K check value
    k = 0                                   # set k to zero
    n = len(code)                           # get length of code string
    for i in range(1,n+1):                  # loop over code string length, summation
        k += (((n-i)%9) +1) * w(code[i-1])  # compute this step of sum, using (n-i) not (n-i +1) because n redefined to include C
    return k%11                             # return summation mod 11, K value

def randCode():
    length = random.randint(1,10)
    return ''.join(random.choice(string.digits + '-') for i in xrange(length))

def getValues(dirtyCode):
    code = re.sub(r"[^0-9\-]","",dirtyCode)     # remove invalid characters from input

    #compute and append C to code string
    codeC = code + str(C(code))

    #compute and append K to code string
    codeCK = codeC + str(K(codeC))

    #compute the bitstring for this code
    codeCKBitStr = bitStrs["Start"] + "0"   # begin with start char and spacer
    for char in codeCK:                     # loop over all chars in code
        codeCKBitStr += bitStrs[char] + "0" # append bitstring for this char and spacer
    codeCKBitStr += bitStrs["Stop"]         # append stop character

    wide = random.randint(1,200) # get some value for wide width
    narrow = int(wide/2.0)       # compute narrow width
    width = {"0":narrow,"1":wide} # mapping bit character -> bar width

    fudge = lambda val: int(val+val*(-0.05+0.1*random.random())) # function to apply random error to a value, +/- 5%
    values = [] # list to hold integer values for final representation

    for b in codeCKBitStr:              #loop over bits characters in code bit string
        values.append(fudge(width[b]))  #get width for this bit, fudge it, then append to the list

    if random.random() > 0.50:          #the code could be backwards, reverse it half the time
        values.reverse()
        
    return values

parser = ArgumentParser()
parser.add_argument('-r', '--randNum', type=int, default=0, help='if you want randomly generated cases, specify the number of cases.')
parser.add_argument('-c', '--code', nargs='+', help="specify specific codes you'd like to test.  If random cases are also generated, these will be the first cases.")
parser.add_argument('-f', '--file', help="Optionally print output to this file.")
parser.add_argument('-v', '--verbose', action='store_true', help="print the values to command line")
args = parser.parse_args()

if not args.code and args.randNum == 0:
    args.randNum = 1 # send at least one output
    
allValues = []
if args.code:
    for code in args.code:
        dirtyCode = code  # get input code from command line arg
        values = getValues(dirtyCode)
        values.insert(0,len(values))
        allValues.append(values)
    
# generate the desired number of random cases.
# if the random arg was not set, numRand will be 0
for i in xrange(args.randNum):
    dirtyCode = randCode()
    values = getValues(dirtyCode)
    values.insert(0, len(values))
    allValues.append(values)

cmd = ["java"]
cmd.append("BarCodes")
for values in allValues:
    cmd += [str(v) for v in values]
call(cmd)

# print to command line as well if verbose option is set
if args.verbose:
    for values in allValues:
        print str(values[0])
        for v in values[1:]:    
            print str(v),
        print ''

# file ouput if filename given in command line
if args.file:
    f = open(args.file, 'w')
    for values in allValues:
        f.write(str(values[0]) + '\n')
        for v in values[1:]:
            f.write(str(v) + " ")
        f.write('\n')
    f.close()

