package edu.saic.ltc.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class DHT22Reader {
	
	private ArrayList<DHT22Data> data;
	
	public ArrayList<DHT22Data> getData() { return this.data; }
	
	public DHT22Reader(String s) {
		this.data = loadData(s);
	}
	
	public ArrayList<DHT22Data> loadData(String s) {
		ArrayList<DHT22Data> result = new ArrayList<DHT22Data>(); 
		try {
			BufferedReader br = new BufferedReader(new FileReader(s));
			String line="";
			// skip one line of header
			br.readLine();
			while ((line=br.readLine()) != null) {
				DHT22Data rec = new DHT22Data(line, LTCConstant.FORMAT_DHT22);
				result.add(rec);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return result;
	}

}
