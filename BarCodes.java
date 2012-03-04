import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Arrays;

class BarCodes {

    // Convenience class to hold arrays of bytes
    // To be used as map keys (since arrays use object equality for comparison)
    private static class Encoding {
        public byte[] encoding;
        
        public Encoding(byte[] encoding) {
            this.encoding = encoding;
        }

        public boolean equals(Object aThat) {
            if (this == aThat) return true;
            if (!(aThat instanceof Encoding)) return false;
            Encoding that = (Encoding)aThat;

            return Arrays.equals(this.encoding, that.encoding);
        }
    }

    // initialize the encoding->character map
    static final Map<Encoding, Character> encodings;
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
    }

    byte[] byteEncodedInputs;
    
    BarCodes(String[] args) {
        int[] inputs = argsToInputs(args);
        if (inputs == null) badCode(1);        
        byteEncodedInputs = convertToByteEncoding(inputs);
        if (byteEncodedInputs == null) badCode(1);
        for (int i = 0; i < byteEncodedInputs.length; i++) {
            System.out.print(byteEncodedInputs[i] + " ");
        }
        System.out.println("");
    }

    int[] argsToInputs(String[] args) {
        int numInputs = Integer.valueOf(args[0]);
        String[] strInputs = Arrays.copyOfRange(args, 1, args.length);
        int[] intInputs = convertToIntArray(strInputs);
        if (numInputs != intInputs.length) return null;
        if (!checkRange(intInputs)) return null;
        return intInputs;
    }

    boolean checkRange(int[] inputs) {
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] < 1 || inputs[i] > 200) {
                return false;
            }
        }
        return true;
    }
    
    // convert an the original input ints to an array of 0's and 1's
    byte[] convertToByteEncoding(int[] intInputs) {
        int[] ranges = getRanges(intInputs);
        // TODO: return null if either low range or high range deviates
        //       from their center by more than 5%
        byte[] byteEncoded = new byte[intInputs.length];
        for (int i = 0; i < intInputs.length; i++) {
            if (intInputs[i] >= ranges[0] &&
                intInputs[i] <= ranges[1]) {
                byteEncoded[i] = 0; // narrow bar
            } else if (intInputs[i] >= ranges[2] &&
                       intInputs[i] <= ranges[3]) {
                byteEncoded[i] = 1; // wide bar
            } else {
                System.out.println("Oops.  something wrong with range function!");
                System.exit(2);
            }
        }
        return byteEncoded;
    }

    /* Returns an array of four ints, with the format
       [narrowMin, narrowMax, wideMin, highMax],
       where [narrowMin, narrowMax] is the range of the narrow values,
       and [wideMin, wideMax] is the range of the wide values
       These are calculated by sorting the inputs and finding the
       largest gap between adjacent elements, which is the gap between
       narrowMax and wideMin
    */
    int[] getRanges(int[] intInputs) {
        if (intInputs == null || intInputs.length == 0) return null;
        int[] sortedCpy = Arrays.copyOf(intInputs, intInputs.length);
        Arrays.sort(sortedCpy);
        // find the largest gap between two adjascent numbers
        // this will separate the narrow bars from the wide bars
        int largestGap = Integer.MIN_VALUE;
        int gapIndex = -1;
        for (int i = 0; i < sortedCpy.length - 1; i++) {
            int gap = sortedCpy[i + 1] - sortedCpy[i];
            if (gap > largestGap) {
                largestGap = gap;
                gapIndex = i;
            }
        }
        // something is very wrong if there are no gaps in the input data
        if (gapIndex == -1) badCode(1);
        int narrowMin = sortedCpy[0];
        int narrowMax = sortedCpy[gapIndex];
        int wideMin = sortedCpy[gapIndex + 1];
        int wideMax = sortedCpy[sortedCpy.length - 1];
        return new int[] {narrowMin, narrowMax, wideMin, wideMax};
    }

    // Arg: string array (assumed to be integer strings)
    // Returns: int array
    int[] convertToIntArray(String[] stringArray) {
        int[] intArray = new int[stringArray.length];
        for (int i = 0; i < intArray.length; i++) {
            intArray[i] = Integer.parseInt(stringArray[i]);
        }
        return intArray;
    }

    void run() {
    }
    
    static void badCode(int caseNum) {
        System.out.println("Case " + caseNum + ": bad code");
        System.exit(1);
    }

    
    public static void main(String[] args) {
        // args.length must be >= 18
        // 1 int for number of regions
        // 10 for the start and stop encodings
        // 5 for the minimum one encoded char,
        // at least 2 separating bars
        if (args.length < 18) badCode(0);
        BarCodes barCodes = new BarCodes(args);
        barCodes.run();
    }
}