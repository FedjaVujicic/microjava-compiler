package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SymTab extends Tab {

	public static final Struct boolType = new Struct(Struct.Bool);
	public static final Struct arrayIntType = new Struct(Struct.Array, SymTab.intType);
	public static final Struct arrayCharType = new Struct(Struct.Array, SymTab.charType);
	public static final Struct arrayBoolType = new Struct(Struct.Array, boolType);

	public static void addTypes() {
		currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
		currentScope.addToLocals(new Obj(Obj.Type, "arrayInt", arrayIntType));
		currentScope.addToLocals(new Obj(Obj.Type, "arrayChar", arrayCharType));
		currentScope.addToLocals(new Obj(Obj.Type, "arrayBool", arrayBoolType));
	}
}
