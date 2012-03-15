import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

final class BadCode extends Exception{
	public BadCode(String msg){
		super(msg);
	}
}

public class BarCodes3 {
	public static HashMap<String,String> bMap;
	public static HashMap<String,String> bMapRev;
	public static HashMap<Character,Integer> charWeights;
	public static int nmax = -1;
	public static int nmin = 9999999;
	public static int wmax = -1;
	public static int wmin = 9999999;
	public static double[] I_n;
	public static double[] I_w;
	public static int c;
	public static int b0;
	public static double r = 0.05;
	public static boolean reversed = false;
	public static boolean startConsumed = false;
	public static String curr = "";
	public static String code = "";
	public static Object currChar = null;
	public static int codeLen;
	
	public static void makeMaps(){
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
		int sum = 0;
		int n = s.length();
		for (int i=1; i<=n; i++){
			sum += ((n-i) % 9 + 1)* charWeights.get(s.charAt(i-1));
		}
		int val = sum % 11;
		if (val==10) return '-';
		else return Integer.toString(val).charAt(0);
	}
	public static int getInt(BufferedReader scnr) throws IOException{
		String s = "";
		char r;
		int x;
		while((x = scnr.read()) != -1 && (r = (char)x) != ' '){
			s += r;
		}
		try{
			x = Integer.parseInt(s);
			return x;
		}
		catch (Exception e){
			throw new IOException("End of file encountered");
		}
	}
	
	public static boolean inInterval(double[] interval, int value){
		return (value >= interval[0] && value <= interval[1]);
	}
	
	public static int max(int a, int b){
		if (a>b) return a;
		else if (b > a) return b;
		else return a;
	}
	public static int min(int a, int b){
		if (a<b) return a;
		else if (b < a) return b;
		else return a;
	}
	public static void consumeCurrent(int bi) throws BadCode{
		//this method consumes the current bit string by generating the corresponding
		//character and appending it to String code
		
		//validate that the current bar is a spacer
		if (!inInterval(I_n,bi)) {throw new BadCode("Invalid spacer bar width");}
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
			else throw new BadCode("Invalid Start/Stop symbol");//start symbol is invalid
		}
		if (reversed) code += bMapRev.get(curr);//reversed -> use reversed mapping
		else code += bMap.get(curr);//not reversed -> use forward mapping
		curr = "";//reset current bit string curr to begin receiving bits for next character
	}
	
	public static void main(String[] args) throws IOException, BadCode {
		makeMaps();//combile the bitString to character maps, and character weight map
		String fileName = args[0];//get input file name
		
		Scanner scnr = new Scanner(new File(fileName));//build a scanner for this file
		int currCase = 1;//start with case 1
		c = scnr.nextInt();//read Case 1 input length
		
		while(c!=0){//c will be 0 after last case and while will exit
			//reset parameters for next case
			int onBar = 0; //tracks bar currently being processed in case an error is thrown
			
			//max/min observed values for narrow and wide bars, resp.
			nmax = -1;
			nmin = 9999999;
			wmax = -1;
			wmin = 9999999;
			reversed = false;  //indicates whether code is backwards, set on start symbol consumption
			startConsumed = false; //indicates whether start character has been consumed
			curr = ""; //holds the bitstring of the current character whos bars are being processed
			code = ""; //holds the code
			currChar = null; //
			try{
				codeLen = (int)((c+1)/6.0);//compute code length(in characters) from bar count c
				int b0 = scnr.nextInt(); //get first bar width
				
				//Define intervals for valid narrow and wide bar widths, resp.
				I_n = new double[] {b0*(1-r)/(1+r), b0*(1+r)/(1-r)};
				I_w = new double[] {2*b0*(1-r)/(1+r), 2*b0*(1+r)/(1-r)};
				
				int bi = -1; // used in for loop to store width of ith bar
				bi = b0; //set ith bar to first bar already read by scanner
				for (int i = 0;i<c;i++){//loop over remaining bars
					if ((i+1) % 6 == 0){//This is a spacing character, consume curr
						consumeCurrent(bi);
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
						else throw new BadCode("Invalid bar width encountered");//bar not wide or narrow, bad
					
					}
					if (i<c-1) {//if we aren't on the last bar (i==c-1)
						bi = scnr.nextInt();//set bi for next run
						onBar++;//increment onBar in case error occurs, allows remaining bars to be consumed
					}
					
				}
				
				//still need to consume last current... this is because there is no trailing space
				consumeCurrent(b0);
				
				//post loop validation of observed max/min
				if (!((nmax/(1+r) <= nmin/(1-r)) &&
					(wmax/(1+r) <= wmin/(1-r)) && 
					(wmax/(1+r) <= 2*nmin/(1-r)) && 
					(2*nmax/(1+r) <= wmin/(1-r)))){
					
					
					throw new BadCode("Error Tolerance violated");
				}
				System.out.println("nmin:" + Integer.toString(nmin) + " nmax:" + Integer.toString(nmax));
				System.out.println("wmin:" + Integer.toString(wmin) + " wmax:" + Integer.toString(wmax));
				
				//reverse the code if it was read backwards
				if (reversed) code = new StringBuffer(code).reverse().toString();
				
				//verify start/stop symbols, one was already checked when startConsumed set to true
				if (!(code.charAt(0) == 'S' && code.charAt(codeLen-1)=='S')){
					throw new BadCode("Invalid Start/Stop Symbol");}
				
				//get the C and K values from the code
				char checkC = code.charAt(codeLen-3);
				char checkK = code.charAt(codeLen-2);
				
				//compare C value
				if (checkC != getC(code.substring(1,codeLen-3)))
					throw new BadCode("Bad C value");
				
				//compare K value
				if (checkK != getK(code.substring(1,codeLen-2)))
					throw new BadCode("Bad K value");

				//Print the code
				System.out.println("Case " + Integer.toString(currCase) + ": "+code.substring(1,codeLen-3));
				currCase++;//increment to the next case
				
				c = scnr.nextInt();//read next counter, will be 0 at end and break while loop
			}
			catch (BadCode b){//handle bad code throws
				//may still have cases waiting, need to consume the rest of the offender
				for (int i = onBar+1;i<c;i++){scnr.nextInt();}//read remainder of the offending code
				c = scnr.nextInt();//get next bar count
				//Print case results
				System.out.println("Case " + Integer.toString(currCase) + ": "+ b.toString());
				currCase++;//increment to next case
			}
		}
	}
}
