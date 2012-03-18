import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

final class BadCode extends Exception{
    String msg;
	//Exception for a bad code
	public BadCode(String msg){
		this.msg = msg;
	}

    public String toString() {
        return msg;
    }
}

public class BarCodes3 {
	public static HashMap<String,String> bMap;// bitString -> codeCharacter
	public static HashMap<String,String> bMapRev;// reversedBitString -> codeCharacter
	public static HashMap<Character,Integer> charWeights;// character -> character weight
	public static int nmax = -1;		//max narrow bar width encountered
	public static int nmin = Integer.MAX_VALUE;	//min narrow bar width encountered
	public static int wmax = -1;		//max wide bar width encountered
	public static int wmin = Integer.MAX_VALUE;	//min wide bar width encountered
	public static double[] I_n;			//Approximating interval for narrow bars
	public static double[] I_w;			//Approximating interval for wide bars
	public static int barCount;				//bar count for current case
	public static int b0;				//first bar width used to define I_n and I_w
	public static double r = 0.05;		//error tolerance
	public static boolean reversed = false;	//indicates whether code is backwards in input
	public static boolean startConsumed = false; //indicates whether the first character has been consumed
	public static String curr = "";		//string to hold the bit string of the current character
	public static String code = "";		//string to hold the code
	public static Object currChar = null; //character used by consume current
	public static int codeLen;			//length of the code in characters, computed from bar count c
	
	public static void makeMaps(){
		/* populate the mappings 
		 * bMap: bitString -> code character
		 * bMapRev: reversedBitString -> code character
		 * charWeights: character -> character weight for C and K check values*/
		bMap = new HashMap<String, String>();
		bMapRev = new HashMap<String, String>();
		charWeights = new HashMap<Character, Integer>();
		bMap.put("00001","0");
		bMapRev.put("10000", "0");
		charWeights.put('0', 0);
		
		bMap.put("10001", "1");
		bMapRev.put("10001", "1");
		charWeights.put('1' ,1);
		
		bMap.put("01001","2");
		bMapRev.put("10010","2");
		charWeights.put('2' ,2);
		
		bMap.put("11000", "3");
		bMapRev.put("00011", "3");
		charWeights.put('3' ,3);
		
		bMap.put("00101", "4");
		bMapRev.put("10100", "4");
		charWeights.put('4' ,4);

		bMap.put("10100", "5");
		bMapRev.put("00101", "5");
		charWeights.put('5' ,5);

		bMap.put("01100", "6");
		bMapRev.put("00110", "6");
		charWeights.put('6' ,6);

		bMap.put("00011", "7");
		bMapRev.put("11000", "7");
		charWeights.put('7' ,7);

		bMap.put("10010", "8");
		bMapRev.put("01001", "8");
		charWeights.put('8' ,8);

		bMap.put("10000", "9");
		bMapRev.put("00001", "9");
		charWeights.put('9' ,9);

		bMap.put("00100", "-");
		bMapRev.put("00100", "-");
		charWeights.put('-' ,10);
 
		bMap.put("00110", "S");
		bMapRev.put("01100", "S");
	}
	
	
	public static char getC(String s){
		//Build the C check value from code string s
		int sum = 0;
		int n = s.length();
		for (int i=1; i<=n; i++){
			sum += ((n-i) % 10 + 1) * charWeights.get(s.charAt(i-1));
		}
		int val = sum % 11;
		if (val==10) return '-';
		else return Integer.toString(val).charAt(0);
		
		
	}
	public static char getK(String s){
		//Build the K check value from code string s, which includes the C value
		int sum = 0;
		int n = s.length();
		for (int i=1; i<=n; i++){
			sum += ((n-i) % 9 + 1)*charWeights.get(s.charAt(i-1));
		}
		int val = sum % 11;
		if (val == 10) return '-';
		else return Integer.toString(val).charAt(0);
	}
	
	public static boolean inInterval(double[] interval, int value){
		//returns true if value lies in interval
		return (value >= interval[0] && value <= interval[1]);
	}
	
	public static int max(int a, int b){
		//returns the max of inputs a and b
		if (a >= b) return a;
		else return b;
	}
    
	public static int min(int a, int b){
		//returns the min of inputs a and b
		if (a <= b) return a;
		else return b;
	}
    
	public static void consumeCurrent(int bi) throws BadCode{
		//this method consumes the current bit string by generating the corresponding
		//character and appending it to String code
		
		//validate that the current bar is a spacer
		if (!inInterval(I_n,bi)) {throw new BadCode("bad code");}
		nmax = max(bi,nmax);//check for narrow max/min
		nmin = min(bi, nmin);
		if (!startConsumed){//First consumption, expect start character
			currChar = bMap.get(curr);//get the character
			if (currChar != null && (String)currChar == "S"){ //It's a valid,forward, S symbol
				startConsumed = true;//start has been consumed
				reversed = false;//the code is not reversed
			}
			else if ((currChar = bMapRev.get(curr)) != null && (String)currChar == "S"){//code is reversed
				startConsumed = reversed = true;//start consumed and code is reversed
			}
			else throw new BadCode("bad code");//start symbol is invalid
		}
		if (reversed) code += bMapRev.get(curr);//reversed -> use reversed mapping
		else code += bMap.get(curr);//not reversed -> use forward mapping
		curr = "";//reset current bit string curr to begin receiving bits for next character
	}

    private static boolean checkBarCount(int barCount) {
        return ((barCount + 1) % 6 == 0 && barCount >= 29 && barCount <= 150);
    }
    
	public static void main(String[] args) throws IOException, BadCode {
		makeMaps();//compile the bitString to character maps, and character weight map
        long start = System.nanoTime();
		String fileName = args[0];
		Scanner scnr = new Scanner(new File(fileName));
		int currCase = 1;//start with case 1
		barCount = scnr.nextInt();//read Case 1 input length
        
		while(barCount!=0){//barCount will be 0 after last case and while will exit
            if (!checkBarCount(barCount)) throw new BadCode("bad code");
			//reset parameters for next case
			int onBar = 0; //tracks bar currently being processed in case an error is thrown
			
			//max/min observed values for narrow and wide bars, resp.
			nmax = -1;
			nmin = Integer.MAX_VALUE;
			wmax = -1;
			wmin = Integer.MAX_VALUE;
			reversed = false;  //indicates whether code is backwards, set on start symbol consumption
			startConsumed = false; //indicates whether start character has been consumed
			curr = ""; //holds the bitstring of the current character whose bars are being processed
			code = ""; //holds the code
			currChar = null; // used by consumeCurrent to store the character representation of current bit string
			try{
				codeLen = (int)((barCount+1)/6.0);//compute code length(in characters) from bar count c
				int b0 = scnr.nextInt(); //get first bar width
				
				//Define intervals for valid narrow and wide bar widths, resp.
				I_n = new double[] {b0*(1-r)/(1+r), b0*(1+r)/(1-r)};
				I_w = new double[] {2*b0*(1-r)/(1+r), 2*b0*(1+r)/(1-r)};
				
				int bi = b0; // used in for loop to store width of ith bar, set to first bar width
				for (int i = 0;i< barCount;i++){//loop over remaining bars of this case
					if ((i+1) % 6 == 0){//This is a spacing character, consume curr
						consumeCurrent(bi);//consumeCurrent also validates spacer bar bi
					}
					else{//This is not a spacing character
						if (inInterval(I_n,bi)){//this is a narrow bar
							nmax = max(nmax, bi);//check for new max/min
							nmin = min(nmin, bi);
							curr += "0";//append 0 to current bit string
						}
						else if (inInterval(I_w,bi)){//this is a wide bar
							wmax = max(wmax, bi);//check for new max/min
							wmin = min(wmin, bi);
							curr += "1";//append 1 to current bit string
						}
						else throw new BadCode("bad code");//bar not wide or narrow, bad
					
					}
					if (i < barCount-1) {//if we aren't on the last bar (i==barcount-1)
						bi = scnr.nextInt();//set bi for next run
						onBar++;//increment onBar in case error occurs, allows remaining bars to be consumed
					}
					
				}
				
				//still need to consume last current... this is because there is no trailing space to trigger consumption
				consumeCurrent(b0);//no spacer to validate, just using known narrow bar b0
				
				//post loop validation of observed max/min
				if ((nmax/(1+r) > nmin/(1-r)) /* there are n candidates*/&&
					(wmax/(1+r) > wmin/(1-r))   /* there are w candidates*/|| 
					(wmax/(1+r) > 2*nmin/(1-r)) /* check for w=2n candidates*/|| 
					(2*nmax/(1+r) > wmin/(1-r)) /* check for w=2n candidates*/){
					throw new BadCode("bad code");// Error Tolerance violated
				}
				//System.out.println("nmin:" + Integer.toString(nmin) + " nmax:" + Integer.toString(nmax));
				//System.out.println("wmin:" + Integer.toString(wmin) + " wmax:" + Integer.toString(wmax));
				
				//reverse the code if it was read backwards
				if (reversed) code = new StringBuffer(code).reverse().toString();
				
				//verify start/stop symbols, one was already checked when startConsumed set to true
				if (code.charAt(0) != 'S' || code.charAt(codeLen-1) != 'S'){
					throw new BadCode("bad code"); // Invalid Start/Stop Symbol
                } 
				
				//get the C and K values from the code
				char checkC = code.charAt(codeLen-3);
				char checkK = code.charAt(codeLen-2);
				
				//compare C value
				if (checkC != getC(code.substring(1,codeLen-3)))
					throw new BadCode("bad C");
				
				//compare K value
				if (checkK != getK(code.substring(1,codeLen-2)))
					throw new BadCode("bad K");

				//Print the code
				System.out.println("Case " + Integer.toString(currCase) + ": "+code.substring(1,codeLen-3));
				currCase++;//increment to the next case
				
				barCount = scnr.nextInt();//read next counter, will be 0 at end and break while loop
			}
			catch (BadCode b){//handle bad code throws
				//may still have cases waiting, need to consume the rest of the offender
				for (int i = onBar+1;i< barCount;i++){scnr.nextInt();}//read remainder of the offending code
				barCount = scnr.nextInt();//get next bar count
				//Print case results
				System.out.println("Case " + Integer.toString(currCase) + ": "+ b.toString());
				currCase++;//increment to next case
			}
		}
        //System.out.println("Version 3: " + (System.nanoTime() - start) + " ns");        
	}
}




