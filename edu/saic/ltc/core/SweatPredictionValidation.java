package edu.saic.ltc.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

public class SweatPredictionValidation {
	private ArrayList<ExprSetting> settings;

	public static void main(String[] args) {
		String fname="test.cfg";
		SweatPredictionValidation spv = new SweatPredictionValidation(fname);
		spv.go(); // n, alpha, threshold
	}
	
	public SweatPredictionValidation(String str) {
		this.settings = new ArrayList<ExprSetting>();
        try {
        	// load log file first
        	// C:/Users/bubble/Documents/MyDesktop/expr/LTC/test1.csv,17:12,76
        	BufferedReader br = new BufferedReader(new FileReader(str));
        	String line=br.readLine(); // skip the first line
        	while ((line=br.readLine()) != null) {
        		if (line.startsWith("=====")) break;
        		StringTokenizer st = new StringTokenizer(line, ",");
        		String fname = st.nextToken().trim();
        		String startTimestamp = st.nextToken().trim();
        		double min2Sweat = Double.parseDouble(st.nextToken().trim());
        		int n = Integer.parseInt(st.nextToken().trim());
        		double alpha = Double.parseDouble(st.nextToken().trim());
        		double threshold = Double.parseDouble(st.nextToken().trim());
        		ExprSetting setting = new ExprSetting(fname, startTimestamp, min2Sweat, n, alpha, threshold);
        		this.settings.add(setting);
        	}
        	br.close();
        } catch (Exception e) {
        	e.printStackTrace(System.err);
        	System.exit(-1);
        }
    }
	
	public double[] calculateHeatIndexSeries(ArrayList<DHT22Data> recs) {
		double[] result = new double[recs.size()];
		for (int i=0; i<result.length; i++) {
			DHT22Data rec = recs.get(i);
			result[i] = HeatIndexCalculator.heatIndexFromC(rec.getTemperature(), rec.getHumidity());
		}
		return result;
	}
	
	public Object[] transform2RelativeData(ArrayList<DHT22Data> recs, double min2Sweat) {
		int DROP_LEN = LTCConstant.STOCHASTIC_WINDOW*2;
		ArrayList<DHT22Data> result = new ArrayList<DHT22Data>();
		long startTime = recs.get(0).getTimestamp();
		long startTimeRelative = recs.get(DROP_LEN).getTimestamp();
		for (int i=DROP_LEN; i<recs.size(); i++) {
			DHT22Data rec = recs.get(i);
			DHT22Data newRec = new DHT22Data(rec.getTimestamp()-startTimeRelative, 
					rec.getTemperature(), rec.getHumidity());
			result.add(newRec);
		}
		long time2Sweat = (long)(min2Sweat*60*1000)-(startTimeRelative-startTime)-60000L;
		//long time2Sweat = (long)(min2Sweat*60*1000)-(startTimeRelative-startTime);
		Object[] ret = new Object[]{result, time2Sweat};
		return ret;
	}
	
	public long doTest(String fname, double min2Sweat, int n, double alpha, double threshold) {
		DHT22Reader reader = new DHT22Reader(fname);
    	ArrayList<DHT22Data> recs = reader.getData();
    	Object[] objs = transform2RelativeData(recs, min2Sweat);
    	ArrayList<DHT22Data> data = (ArrayList<DHT22Data>)objs[0];
    	long time2SweatWarning = (Long)objs[1]; // 1 min before sweat
    	// calculate heat index
    	double[] heatIndexSeries = calculateHeatIndexSeries(data);
    	StochasticIndexCalculator sic = new StochasticIndexCalculator(heatIndexSeries);
    	double[] kds = sic.getStochasticSeries(n, alpha);
    	int pos=0;
    	while ((pos<kds.length) && (kds[pos]>threshold)) pos++;
    	if (pos < kds.length) {
    		long diff = time2SweatWarning-data.get(pos).getTimestamp();
    		return diff;
    	}
    	return -99999L;
	}
	
	public void go() {
		int succ=0, fail=0;
		for (int i=0; i<this.settings.size(); i++) {
			for (int j=0; j<this.settings.size(); j++) {
				//if (i==j) continue;
				// params from i
				String fname = this.settings.get(i).getFname();
				double min2Sweat = this.settings.get(i).getMin2Sweat();
				//long warning = this.settings.get(i).getTime2SweatWarning();
				// params from j
				int n = this.settings.get(j).getN();
				double alpha = this.settings.get(j).getAlpha();
				double threshold = this.settings.get(j).getThreshold();
				long diff = doTest(fname, min2Sweat, n, alpha, threshold);
				if (diff >= 0) { succ++; System.out.print("SUCC"); }
				else { fail++; System.out.print("FAIL"); }
				System.out.println(" for series "+i+"("+fname+") on param "+j+" diff value="+diff);
			}
		}
		System.out.println("Overall successful prediction rate: "+(100.0*succ/(succ+fail))+"%");
	}
	
	private class ExprSetting {
		private String fname;
		private String startTimestamp;
		private double min2Sweat;
		private int n;
		private double alpha;
		private double threshold;
		
		public ExprSetting(String s1, String s2, double d1, int i1, double d2, double d3) {
			this.fname = s1;
			this.startTimestamp = s2;
			this.min2Sweat = d1;
			this.n = i1;
			this.alpha = d2;
			this.threshold = d3;
		}
		
		public String getFname() { return this.fname; }
		public String getStartTimestamp() { return this.startTimestamp; }
		public double getMin2Sweat() { return min2Sweat; }
		public int getN() { return this.n; }
		public double getAlpha() { return this.alpha; }
		public double getThreshold() { return this.threshold; }
	}
}
