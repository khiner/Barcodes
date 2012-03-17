import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Arrays;
import java.io.File;
import java.util.Scanner;

class BarCodes2 {
    /*
      Wrapper class to hold arrays of bytes
      To be used as map keys (since arrays use object equality
      for comparison)
    */
    private static class Encoding {
        public byte[] bytes;

        public Encoding() {
            bytes = new byte[5];
        }
        
        public Encoding(byte[] bytes) {
            this.bytes = bytes;
        }

        public boolean equals(Object aThat) {
            if (this == aThat) return true;
            if (!(aThat instanceof Encoding)) return false;
            Encoding that = (Encoding)aThat;

            return Arrays.equals(this.bytes, that.bytes);
        }

        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }

    // initialize the encoding->character map
    public static final Map<Encoding, Character> encodings;
    // backwards encoding->character map
    public static final Map<Encoding, Character> bEncodings;
    public static final Map<Character, Integer> weights;
    static {
        Map<Encoding, Character> map = new HashMap<Encoding, Character>();
        map.put(new Encoding(new byte[] {0,0,0,0,1}), '0');
        map.put(new Encoding(new byte[] {1,0,0,0,1}), '1');
        map.put(new Encoding(new byte[] {0,1,0,0,1}), '2');
        map.put(new Encoding(new byte[] {1,1,0,0,0}), '3');
        map.put(new Encoding(new byte[] {0,0,1,0,1}), '4');
        map.put(new Encoding(new byte[] {1,0,1,0,0}), '5');
        map.put(new Encoding(new byte[] {0,1,1,0,0}), '6');
        map.put(new Encoding(new byte[] {0,0,0,1,1}), '7');
        map.put(new Encoding(new byte[] {1,0,0,1,0}), '8');
        map.put(new Encoding(new byte[] {1,0,0,0,0}), '9');
        map.put(new Encoding(new byte[] {0,0,1,0,0}), '-');
        map.put(new Encoding(new byte[] {0,0,1,1,0}), 's');
        encodings = Collections.unmodifiableMap(map);
        Map<Encoding, Character> bMap = new HashMap<Encoding, Character>();
        bMap.put(new Encoding(new byte[] {1,0,0,0,0}), '0');
        bMap.put(new Encoding(new byte[] {1,0,0,0,1}), '1');
        bMap.put(new Encoding(new byte[] {1,0,0,1,0}), '2');
        bMap.put(new Encoding(new byte[] {0,0,0,1,1}), '3');
        bMap.put(new Encoding(new byte[] {1,0,1,0,0}), '4');
        bMap.put(new Encoding(new byte[] {0,0,1,0,1}), '5');
        bMap.put(new Encoding(new byte[] {0,0,1,1,0}), '6');
        bMap.put(new Encoding(new byte[] {1,1,0,0,0}), '7');
        bMap.put(new Encoding(new byte[] {0,1,0,0,1}), '8');
        bMap.put(new Encoding(new byte[] {0,0,0,0,1}), '9');
        bMap.put(new Encoding(new byte[] {0,0,1,0,0}), '-');
        bMap.put(new Encoding(new byte[] {0,1,1,0,0}), 's');
        bEncodings = Collections.unmodifiableMap(bMap);
        Map<Character, Integer> wMap = new HashMap<Character, Integer>();
        wMap.put('0', 0);        
        wMap.put('1', 1);
        wMap.put('2', 2);
        wMap.put('3', 3);
        wMap.put('4', 4);
        wMap.put('5', 5);
        wMap.put('6', 6);
        wMap.put('7', 7);
        wMap.put('8', 8);
        wMap.put('9', 9);
        wMap.put('-', 10);
        weights = Collections.unmodifiableMap(wMap);        
    }

    private static int[] argsToInputs(Scanner scanner) {
        int numInputs = scanner.nextInt();
        // if first int is 0, this is the end symbol. return 0
        if (numInputs == 0) return new int[] {0};
        
        /* args.length must be >= 30:
           1 int for number of regions
           10 for the start and stop encodings
           10 for C and K
           5 for the minimum one encoded char,
           and at least 4 separating bars
        */        
        if ((numInputs + 1) % 6 != 0 || numInputs < 29 /*|| numInputs > 150*/) {
            // bad input, skip scanner foward to next input case
            for (int i = 0; i < numInputs; i++)
                scanner.nextInt();
            return null;
        }
        int[] inputs = new int[numInputs];
        for (int i = 0; i < numInputs; i++) {
            inputs[i] = scanner.nextInt();
        }
        return inputs;
    }
    
    /*
      convert the original input ints to an array of 0's and 1's.
      returns null for bad code
    */
    private static char[] convertToCharacters(int[] b) {
        int c = b.length;
        float r = 0.05f;
        int[] I_n = {(int)(b[0]*(1 - r)/(1 + r)), (int)(b[0]*(1 + r)/(1 - r))};
        int[] I_w = {2*I_n[0], 2*I_n[1]};
        int n_min = Integer.MAX_VALUE, w_min = Integer.MAX_VALUE;        
        int n_max = 0, w_max = 0;        
        boolean startConsumed = false;
        boolean reversed = false;
        Encoding curr = new Encoding(new byte[5]);
        char[] code = new char[(c + 1)/6];
        int codeIndex = 0;
        
        for (int i = 0; i < c; i++) {
            if ((i + 1)%6 == 0 || i == c - 1) { //This is a spacing character, consume curr
                // space is supposed to be a narrow bar
                if (b[i] < I_n[0] || b[i] > I_n[1]) return null;
                n_max = Math.max(n_max, b[i]);
                n_min = Math.min(n_min, b[i]);
                if (!startConsumed) { //validate start/stop symbol and check for code reversa
                    
                    if (encodings.containsKey(curr) && encodings.get(curr) == 's') {
                        // the code is not reversed
                        startConsumed = true;
                        reversed = false;                        
                    } else if (bEncodings.containsKey(curr) && bEncodings.get(curr) == 's') {
                        // the code is reversed
                        startConsumed = true;
                        reversed = true;
                    } else return null; // bad code - start/stop symbol is bad
                }
                if (reversed) {  // The code is backwards, use the reversed bit string to character mapping
                    if (bEncodings.containsKey(curr)) {
                        char character = bEncodings.get(curr);
                        code[codeIndex] = character;
                        codeIndex++;
                    } else return null; // bad code - no character for curr
                } else { // The code is not backwards, use the regular bit string character mapping
                    if (encodings.containsKey(curr)) {
                        char character = encodings.get(curr);
                        code[codeIndex] = character;
                        codeIndex++;
                    } else return null; // bad code - no character for curr
                }
                Arrays.fill(curr.bytes, (byte)0);  //purge curr to prepare for next character
            } else {  //This is not a spacing character
                if (b[i] >= I_n[0] && b[i] <= I_n[1]) { // narrow
                    n_max = Math.max(n_max, b[i]);
                    n_min = Math.min(n_min, b[i]);
                    curr.bytes[i%6] = 0;
                } else if (b[i] >= I_w[0] && b[i] <= I_w[1]) { // wide
                    w_max = Math.max(w_max, b[i]);
                    w_min = Math.min(w_min, b[i]);
                    curr.bytes[i%6] = 1;
                } else return null; // bad code - bar is neither wide nor narrow
            }
        }

        // Post-Loop validation
        if (n_max/(1 + r) > n_min/(1 - r) || w_max/(1 + r) > w_min/(1 - r)
            || w_min/(1 - r) < 2*n_max/(1 + r)) {
            return null;  // observed max/mins violate tolerance r and/or criterion w = 2n
        }
        if (reversed) {
            code = getReversedChars(code);
        }
        
        if (code[0] != 's' || code[code.length - 1] != 's') {
            return null; // bad code - invalid start/stop
        }

        return Arrays.copyOfRange(code, 1, code.length - 1);
    }

    private static char[] getReversedChars(char[] chars) {
        char[] reversed = new char[chars.length];
        for (int i = 0, j = chars.length - 1; i < chars.length; i++, j--) {
            reversed[i] = chars[j];
        }
        return reversed;
    }
        
    private static boolean checkC(char[] characters) {
        int n = characters.length - 2;
        int c = weights.get(characters[n]);
        int sum = 0;
        for (int i = 1; i < n + 1; i++) {
            sum += ((n - i)%10 + 1)*weights.get(characters[i - 1]);
        }
        sum %= 11;
        return (c == sum);
    }

    private static boolean checkK(char[] characters) {
        int n = characters.length - 1;
        int k = weights.get(characters[n]);
        int sum = 0;        
        for (int i = 1; i < n + 1; i++) {
            sum += ((n - i)%9 + 1)*weights.get(characters[i - 1]);
        }
        sum %= 11;
        return (k == sum);
    }
    
    /*
      Output correctly formatted error with the given case number and type
      and exit w/o error
      Type should be 'code', 'c', or 'k'      
    */
    private static String bad(String type, int caseNum) {
        return new String("Case " + caseNum + ": bad " + type);
    }

    private static String good(char[] characters, int caseNum) {
        return new String("Case " + caseNum + ": " + String.valueOf(characters));
    }

    private static String runInputCase(int[] inputs, int caseNum) {
        char[] characters = convertToCharacters(inputs);
        if (characters == null) return bad("code", caseNum);
        if (!checkC(characters)) return bad("C", caseNum);
        if (!checkK(characters)) return bad("K", caseNum);
        characters = Arrays.copyOfRange(characters, 0, characters.length - 2);
        return good(characters, caseNum);        
    }

    public static void run(Scanner scanner) {        
        int caseNum = 1;
        while (true) {
            int[] inputs = argsToInputs(scanner);
            if (inputs == null) {
                System.out.println(bad("code", caseNum));
                continue;
            } else if (inputs[0] == 0) break; // end of input signal
            
            runInputCase(inputs, caseNum);
            caseNum++;
        }
    }
    
    public static void main(String[] args) {
       long start = System.nanoTime();
       String fileName = args[0];
       try {
           Scanner scanner = new Scanner(new File(fileName));       
           BarCodes2.run(scanner);
           System.out.println("Version 2: " + (System.nanoTime() - start) + " ns");
       }
       catch (Exception e) {
           e.printStackTrace();
       }         
    }
}