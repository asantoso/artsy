package com.neusou.vivacious;

public class LifecycleFlags {
	
	public int[] flags = new int[10];
	
	public static final int LC_CREATE = 0;	
	public static final int LC_RESUME = 1;
	public static final int POSTRESUME = 2;
	public static final int START = 3;
	public static final int RESTART = 4;
	public static final int STOP = 5;
	public static final int DESTROY = 6;
	public static final int POSTCREATE = 7;
	public static final int PAUSE = 8;
	
	public static final int ACTIVITYRESULT = 9;
	
	public void clearAll(){
		for(int i : flags){
			flags[i] = 0;
		}
	}
	
	public void clearAndSet(int i){
		clearAll();
		flags[i] = 0;
	}
	
	public void clear(int i){
		flags[i] = 0;
	}
	
	public void set(int i){
		flags[i] = 1;
	}
	
	public boolean is(int i){
		return flags[i]==1;
	}
	
	
	
}