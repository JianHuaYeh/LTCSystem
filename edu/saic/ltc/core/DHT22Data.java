package edu.saic.ltc.core;

import java.util.StringTokenizer;

public class DHT22Data {
	private long timestamp;
	private double temperature;
	private double humidity;
	
	public DHT22Data(long l, double d1, double d2) {
		this.timestamp = l;
		this.temperature = d1;
		this.humidity = d2;
	}
	
	public DHT22Data(String s, double d1, double d2) {
		this.temperature = d1;
		this.humidity = d2;
		// 1460275083916 for DHT22
		StringTokenizer st = new StringTokenizer(s, "'.");
		st.nextToken();
		String s2 = st.nextToken();
		this.timestamp = Integer.parseInt(s2);
	}
	
	public DHT22Data(String line, int format) {
		StringTokenizer st = new StringTokenizer(line, ",");
		String s1 = st.nextToken().trim(); 
		if (format == LTCConstant.FORMAT_DHT22) { // 1460275083916
			this.timestamp = Long.parseLong(s1);
		}
		String s2 = st.nextToken().trim(); // 29.70000076
		String s3 = st.nextToken().trim(); // 95.40000153
		this.temperature = Double.parseDouble(s2);
		this.humidity = Double.parseDouble(s3);
	}

	public long getTimestamp() { return timestamp; }
	public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
	public double getTemperature() { return temperature; }
	public void setTemperature(double temperature) { this.temperature = temperature; }
	public double getHumidity() { return humidity; }
	public void setHumidity(double humidity) { this.humidity = humidity; }

}
