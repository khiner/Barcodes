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
        encodings = Collections.unmodifiableMap(map);
    }

    int[] inputs;

    BarCodes(String[] args) {
        int numRegions = Integer.valueOf(args[0]);
        inputs = new int[numRegions];
        for (int i = 0; i < numRegions; i++) {
            inputs[i] = Integer.valueOf(args[i + 1]);
        }
    }

    void run() {
    }
    
    static void badCode(int caseNum) {
        System.out.println("Case " + caseNum + ": bad code");
        System.exit(1);
    }

    
    public static void main(String[] args) {
        if (args.length < 2)
            badCode(0);
        BarCodes barCodes = new BarCodes(args);
        barCodes.run();
    }

}