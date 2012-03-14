import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class BadCode extends Exception{
	public BadCode(String msg){
		super(msg);
	}
}

public class Barcodes3 {
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
	public static int getInt(BufferedReader br) throws IOException{
		String s = "";
		char r;
		int x;
		while((x = br.read()) != -1 && (r = (char)x) != ' '){
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
		if (!inInterval(I_n,bi)) {throw new BadCode("Invalid spacer bar width");}
		nmax = max(bi,nmax);
		nmin = min(bi, nmin);
		if (!startConsumed){
			currChar = bMap.get(curr);
			if (currChar != null && (String)currChar == "S"){ //Code is not reversed
				startConsumed = true;
				reversed = false;
			}
			else if ((currChar = bMapRev.get(curr)) != null && (String)currChar == "S"){//code is reversed
				startConsumed = reversed = true;
			}
			else throw new BadCode("Invalid Start/Stop symbol");
		}
		if (reversed) code += bMapRev.get(curr);//reversed -> use reversed mapping
		else code += bMap.get(curr);//not reversed -> use forward mapping
		curr = "";
	}
	
	public static void main(String[] args) throws IOException, BadCode {
		makeMaps();
		String fileName = args[0];
		
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		c = Integer.parseInt(br.readLine());
		codeLen = (int)((c+1)/6.0);
		br.mark(10);
		int b0 = getInt(br);
		br.reset();
		
		int bi = -1;
		I_n = new double[] {b0*(1-r)/(1+r), b0*(1+r)/(1-r)};
		I_w = new double[] {2*b0*(1-r)/(1+r), 2*b0*(1+r)/(1-r)};
		
		
		for (int i = 0;i<c;i++){
			bi = getInt(br);
			if(bi == -1)throw new BadCode("Invalid Input Length");
			if ((i+1) % 6 == 0){//This is a spacing character, consume curr
				consumeCurrent(bi);
			}
			else{//This is not a spacing character
				if (inInterval(I_n,bi)){//this is a narrow bar
					nmax = max(nmax, bi);
					nmin = min(nmin, bi);
					curr += "0";
				}
				else if (inInterval(I_w,bi)){
					wmax = max(wmax, bi);
					wmin = min(wmin, bi);
					curr += "1";
				}
				else throw new BadCode("Invalid bar width encountered");
			
			}
		}
		
		//still need to consume last current... this is because there is no trailing space
		consumeCurrent(b0);
		
		if (!((nmax/(1+r) <= nmin/(1-r)) && 
			(wmax/(1+r) <= wmin/(1-r)) && 
			(wmax/(1+r) <= 2*nmin/(1-r)) && 
			(2*nmax/(1+r) <= wmin/(1-r)))){
			System.out.println("nmin:" + Integer.toString(nmin) + " nmax:" + Integer.toString(nmax));
			System.out.println("wmin:" + Integer.toString(wmin) + " wmax:" + Integer.toString(wmax));
			
			throw new BadCode("Error Tolerance violated");
		}
		
		if (reversed) code = new StringBuffer(code).reverse().toString();
		
		//verify start/stop
		if (!(code.charAt(0) == 'S' && code.charAt(codeLen-1)=='S'))
			throw new BadCode("Invalid Start/Stop Symbol");
		
		char checkC = code.charAt(codeLen-3);
		char checkK = code.charAt(codeLen-2);
		
		if (checkC != getC(code.substring(1,codeLen-3)))
			throw new BadCode("Bad C value");
		
		System.out.println(getK(code.substring(1,codeLen-2)));
		if (checkK != getK(code.substring(1,codeLen-2)))
			throw new BadCode("Bad K value");
		
		System.out.println(code);
		System.out.println(code.substring(1,codeLen-3));
		
	}
}
