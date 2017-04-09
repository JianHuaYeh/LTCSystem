package edu.saic.ltc.expr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import edu.saic.ltc.core.DHT22Data;
import edu.saic.ltc.core.DHT22Reader;

public class Experiment1 {
	private String startTime;
	private long period;
	private ArrayList<DHT22Data> data;
	
	public static void main(String[] args) throws Exception {
		String logname = "C:/Users/bubble/Documents/MyDesktop/expr/LTC/test1.log";
		Experiment1 expr1 = new Experiment1(logname);
		expr1.go();
	}
	
	public Experiment1(String str) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(str));
		String line="";
		if ((line=br.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, ",");
			String dataname = st.nextToken();
			this.startTime = st.nextToken();
			String s3 = st.nextToken();
			this.period = Long.parseLong(s3)*60*1000;
			// load data
			DHT22Reader reader = new DHT22Reader(dataname);
			this.data = reader.getData();
		}
		br.close();
	}
	
	public void go() {
		
	}

}
