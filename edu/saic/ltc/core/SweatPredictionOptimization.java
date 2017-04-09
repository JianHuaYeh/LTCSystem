package edu.saic.ltc.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

public class SweatPredictionOptimization {
	private static int ALPHA_GRANULARITY=100000;
	private ArrayList<DHT22Data> data;
	private double[] heatIndexSeries;
	private long time2SweatWarning;

	public static void main(String[] args) {
		String fname="test.cfg";
		SweatPredictionOptimization opt = new SweatPredictionOptimization(fname);
		opt.go();
	}
	
	public SweatPredictionOptimization(String str) {
        try {
        	// load log file first
        	// C:/Users/bubble/Documents/MyDesktop/expr/LTC/test1.csv,17:12,76
        	BufferedReader br = new BufferedReader(new FileReader(str));
        	String line=br.readLine();
        	br.close();
        	StringTokenizer st = new StringTokenizer(line, ",");
        	String fname=st.nextToken();
        	System.out.println("Loading temperature/humidity data log history: "+fname);
        	DHT22Reader reader = new DHT22Reader(fname);
        	ArrayList<DHT22Data> recs = reader.getData();
        	System.out.println("History record length: "+recs.size());
        	String startTimeStr = st.nextToken();
        	double min2Sweat = Double.parseDouble(st.nextToken());
        	System.out.println("Time to sweat of subject in minutes: "+min2Sweat);
        	Object[] objs = transform2RelativeData(recs, min2Sweat);
        	this.data = (ArrayList<DHT22Data>)objs[0];
        	this.time2SweatWarning = (Long)objs[1]; // 1 min before sweat
        	System.out.println("Time to sweat warning in minutes: "+(this.time2SweatWarning/1000.0/60.0));
        	// calculate heat index
        	this.heatIndexSeries = calculateHeatIndexSeries(this.data);
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
    
    public long cost(int[] sol) {
    	StochasticIndexCalculator sic = new StochasticIndexCalculator(this.heatIndexSeries);
    	int window = sol[0];
    	double smoothing = ((double)sol[1])/ALPHA_GRANULARITY;
    	int threshold = sol[2];
    	double[] kds = sic.getStochasticSeries(window, smoothing);
    	int pos=0;
    	while ((pos<kds.length) && (kds[pos]>threshold)) pos++;
    	long cost=Long.MAX_VALUE;
    	if (pos < kds.length) {
    		cost = this.time2SweatWarning - this.data.get(pos).getTimestamp();
    		if (cost < 0) cost=Long.MAX_VALUE;
    	}
        return cost;
    }
    
    private int[] copy(int[] sol) { return sol.clone(); }

    public int[] mutate(int[] r) {
        int step = 1;
        int[] vec = copy(r);
        int pos = (int)(Math.random()*vec.length);
        // solution: int[3] for { n, alpha, threshold } ranges {9~60, 99~1, 30~1 }
        if (Math.random()<0.5) {
        	vec[pos]--;
        	switch (pos) {
        		case 0: // n
        			if (vec[0]<9) vec[0]+=2; break;
        		case 1: // alpha
        			if (vec[1]<1) vec[1]+=2; break;
        		default: // threshold
        			if (vec[pos]<1) vec[pos]+=2;
        	}
        }
        else {
        	vec[pos]++;
        	switch (pos) {
        		case 0: // n
        			if (vec[0]>60) vec[0]-=2; break;
        		case 1: // alpha
        			if (vec[1]>=ALPHA_GRANULARITY) vec[1]-=2; break;
        		default: // threshold
        			if (vec[pos]>30) vec[pos]-=2;
        	}
        }
        return vec;
    }

    public int[] crossover(int[] r1, int[] r2) {
        int[] vec = copy(r1);
        int pos = (int)(Math.random()*vec.length);
        for (int i=pos; i<vec.length; i++) {
            vec[i] = r2[i];
        }
        return vec;
    }
    
    public ArrayList<int[]> geneticOptimize() {
    	return this.geneticOptimize(1000, 0.2, 3000, 0.2);
    }

    public ArrayList<int[]> geneticOptimize(int popsize, double elite, int maxiter, double mutprob) {
    	// default values
        //int popsize = 50;
        //double elite = 0.2;
        //int maxiter = 50;
        //double mutprob = 0.2;
    	
    	// solution: int[3] for { n, alpha, threshold } ranges {9~60, 99~1, 30~1 }

        ArrayList<int[]> pop = new ArrayList<int[]>();
        for (int i=0; i<popsize; i++) {
            int[] sol = new int[3];
            sol[0] = (int)(Math.random()*(60-9)+9);
            double rand=0.0;
            while ((rand=Math.random()) == 0.0);
            sol[1] = (int)(rand*ALPHA_GRANULARITY);
            sol[2] = (int)(Math.random()*(30-1)+1);
            pop.add(sol);
        }
        
        //System.out.println("Initial population:");
        //printSolutions(pop);

        int topelite=(int)(elite*popsize);

        int count=0;
        while (count++ < maxiter) {
        	if (count%100==0) System.out.println("Iteration "+count+"/"+maxiter);
            ASObject[] array = new ASObject[pop.size()];
            for (int i=0; i<pop.size(); i++) {
                int[] r = (int[])pop.get(i);
                long cost = this.cost(r);
                array[i] = new ASObject(r[0], r[1], r[2], cost);
            }
            Arrays.sort(array);
            ArrayList<int[]> pop2 = new ArrayList<int[]>();
            for (int i=0; i<topelite; i++) {
                //pop2.add(pop.get(array[i].label));
            	pop2.add(array[i].getSolution());
            }
            pop = pop2;
            
            if (count == maxiter) break;

            while (pop.size() < popsize) {
                if (Math.random() < mutprob) {
                    int c = (int)(Math.random()*topelite);
                    pop.add(mutate((int[])pop.get(c)));
                }
                else {
                    int c1 = (int)(Math.random()*topelite);
                    int c2 = (int)(Math.random()*topelite);
                    pop.add(crossover((int[])pop.get(c1), (int[])pop.get(c2)));
                }
            }
            //System.out.println("Current best cost = "+
            //        ((ASObject)array[0]).score+" in generation "+count);
        }

        return pop;
    }

    public void randomRestartGeneticOptimization() {
        long best = Long.MAX_VALUE;
        ArrayList<int[]> bestsols = null;
        int iteration = 0;
        int bestiter = 0;
        while (true) {
        	ArrayList<int[]> sols = this.geneticOptimize();
            long cost = this.cost(sols.get(0));
            iteration++;
            //System.out.println("Lowest cost = "+cost);
            if (cost < best) {
                best = cost;
                bestsols = sols;
                bestiter = iteration;
                System.out.println("Iteration "+iteration+", best cost = "+best+", sol="+
                        		getSolutionString(sols.get(0)));
            }
            else if (cost==best) {
            	if (betterSolution(bestsols.get(0), sols.get(0))) {
            		best = cost;
                    bestsols = sols;
                    bestiter = iteration;
                    System.out.println("Iteration "+iteration+", best cost = "+best+", sol="+
                            		getSolutionString(sols.get(0)));
            	}
            }
            //System.out.println("Iteration "+iteration+", best cost = "+best+", sol="+
            //		getSolutionString(sols.get(0)));
            if (iteration-bestiter >= 100) {
                System.out.println("Maybe stablized, stop at iteration "+
                        iteration);
                System.out.println("Current best solution:");
                System.out.println(getSolutionString(bestsols.get(0)));
                break;
            }
        }
        // bestsols...
        //printSolutions(bestsols);
    }
    
    public String getSolutionString(int[] sol) {
    	return "{"+sol[0]+","+((double)sol[1])/ALPHA_GRANULARITY+","+sol[2]+"}, cost="+cost(sol);
    }
    
    public void printSolutions(ArrayList<int[]> sols) {
    	System.out.println("Solution list:");
    	for (int[] sol: sols) System.out.println(getSolutionString(sol));
    }

    public void go() {
    	ArrayList<int[]> sols = this.geneticOptimize(1000, 0.2, 3000, 0.2);
        long cost = this.cost(sols.get(0));
        System.out.println("Best cost = "+cost+", sol="+getSolutionString(sols.get(0)));
        //this.randomRestartGeneticOptimization();
    }
       
	public class ASObject implements Comparable<ASObject> {
        public int window;
        public int smoothing;
        public int threshold;
        public long score;
        
        public ASObject(int[] sol, long s) {
        	this(sol[0], sol[1], sol[2], s);
        }

        public ASObject(int wn, int sm, int th, long s) {
            this.window = wn;
            this.smoothing = sm;
            this.threshold = th;
            this.score = s;
        }
        
        public int[] getSolution() {
        	int[] sol = new int[3];
        	sol[0] = this.window;
        	sol[1] = this.smoothing;
        	sol[2] = this.threshold;
        	return sol;
        }

        public int compareTo(ASObject other) {
            if (this.score < other.score) return -1;
            else if (this.score > other.score) return 1;
            else { // scores are equal
        		if (this.window < other.window) return -1;
        		else if (this.window > other.window) return 1;
            	else {
                	if (this.threshold > other.threshold) return -1;
                	else if (this.threshold < other.threshold) return 1;
            		else {
            			if (this.smoothing > other.smoothing) return -1;
            			else if (this.smoothing < other.smoothing) return 1;
            		}
            	}
            }
        	return 0;
        }
    }
	
	// modified synchronously with public int compareTo(ASObject other)
    public boolean betterSolution(int[] orig, int[] newsol) {
   		if (orig[0] > newsol[0]) return true;
   		else if (orig[0] < newsol[0]) return false;
       	else {
           	if (orig[2] < newsol[2]) return true;
           	else if (orig[2] > newsol[2]) return false;
       		else {
       			if (orig[1] < newsol[1]) return true;
       			else if (orig[1] > newsol[1]) return false;
       		}
       	}
    	return false;
    }
 
}
