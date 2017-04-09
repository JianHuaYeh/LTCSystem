package edu.saic.ltc.core;

public class StochasticIndexCalculator {
	private double[] data;
	
	public StochasticIndexCalculator(double[] his) {
		this.data = his;
	}
	
	public double[] getStochasticSeries() {
		return getStochasticSeries(LTCConstant.STOCHASTIC_WINDOW, LTCConstant.STOCHASTIC_SMOOTHING);
	}
	
	public double[] getStochasticSeries(int window, double smoothing) {
		// skip 2 data window first
		int skip=2*window;
		int len=this.data.length-skip;
		double[] emas = new double[len];
		double[] kd_ks = new double[len];
		double[] kd_ds = new double[len];
		for (int i=skip; i<this.data.length; i++) {
			double[] his = new double[window];
			for (int j=0; j<window; j++) his[j]=this.data[i-window+j];
			double ema = heatIndexSegmentEMA(his);
			emas[i-skip] = ema;
			double hi = heatIndexSegmentMax(his);
			double lo = heatIndexSegmentMin(his);
			double rsv = RSV(ema, hi, lo);
			double kd_k=(i-skip==0)?50:stochastic_k(rsv, kd_ks[i-skip-1], smoothing);
			kd_ks[i-skip] = kd_k;
			double kd_d=(i-skip==0)?50:stochastic_d(kd_k, kd_ds[i-skip-1], smoothing);
			kd_ds[i-skip] = kd_d;
		}
		return kd_ds;
	}
	
	public double heatIndexSegmentEMA(double[] his) {
		double sum=0.0;
		for (double d: his) sum+=d;
		return sum/his.length;
	}
	
	public double heatIndexSegmentMax(double[] his) {
		double max = Double.MIN_VALUE;
		for (double d: his) if (d>max) max=d;
		return max;
	}

	public double heatIndexSegmentMin(double[] his) {
		double min = Double.MAX_VALUE;
		for (double d: his) if (d<min) min=d;
		return min;
	}
	
	public double RSV(double ema, double hi, double lo) {
		double epsilon=0.000000001;
		return (ema-lo+epsilon)/(hi-lo+epsilon);
	}
	
	public double stochastic_k(double rsv, double prev_k) {
		return stochastic_k(rsv, prev_k, LTCConstant.STOCHASTIC_SMOOTHING);
	}
	
	public double stochastic_k(double rsv, double prev_k, double smoothing) {
		// default to rsv*1/3+prev_k*2/3 if smoothing=3
		//return rsv/smoothing+prev_k*(smoothing-1)/smoothing;
		return rsv*smoothing+prev_k*(1-smoothing);
	}
	
	public double stochastic_d(double k, double prev_d) {
		return stochastic_d(k, prev_d, LTCConstant.STOCHASTIC_SMOOTHING);
	}
	
	public double stochastic_d(double k, double prev_d, double smoothing) {
		// default to k*1/3+prev_d*2/3 if smoothing=3
		//return k/smoothing+prev_d*(smoothing-1)/smoothing;
		return k*smoothing+prev_d*(1-smoothing);
	}
}
