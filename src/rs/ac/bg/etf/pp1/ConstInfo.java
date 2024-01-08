package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.concepts.*;

public class ConstInfo {
	public String name;
	public int value;
	public Struct type;
	
	public ConstInfo(String name, int value, Struct type) {
		super();
		this.name = name;
		this.value = value;
		this.type = type;
	}
	
}
