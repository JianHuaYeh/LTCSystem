package edu.saic.ltc.core;

public class HeatIndexCalculator {
	private static double c1 = -42.379;
	private static double c2 = 2.04901523;
	private static double c3 = 10.14333127;
	private static double c4 = -0.22475541;
	private static double c5 = -0.00683783;
	private static double c6 = -0.05481717;
	private static double c7 = 0.00122874;
	private static double c8 = 0.00085282;
	private static double c9 = 0.00085282;
	
	public static double tempC2F(double C) { return C*9.0/5.0+32.0; }
	public static double tempF2C(double F) { return (F-32)*5.0/9.0; }
	public static double heatIndexFromC(double C, double H) { return heatIndex(tempC2F(C), H); }
	public static double heatIndexFromF(double F, double H) { return heatIndex(F, H); }

	// heat index in F
	public static double heatIndex(double F, double H) {
		//=$N$1+$N$2*I2+$N$3*C2+$N$4*I2*C2+$N$5*I2*I2+$N$6*C2*C2+$N$7*I2*I2*C2+$N$8*I2*C2*C2+$N$9*I2*I2*C2*C2
		return c1+c2*F+c3*H+c4*F*H+c5*F*F+c6*H*H+c7*F*F*H+c8*F*H*H+c9*F*F*H*H;
	}
}
