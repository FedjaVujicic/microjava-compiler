package rs.ac.bg.etf.pp1;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticAnalyzer extends VisitorAdaptor {

	boolean errorDetected = false;
	String curNamespace = "";
	int curConstValue;
	Struct curConstType;

	ArrayList<VarInfo> curVars = new ArrayList<VarInfo>();
	ArrayList<ConstInfo> curConsts = new ArrayList<ConstInfo>();
	ArrayList<String> namespaces = new ArrayList<String>();

	static final Struct boolType = Tab.insert(Obj.Type, "bool", new Struct(Struct.Bool)).getType();

	Logger log = Logger.getLogger(getClass());

	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" (line ").append(line).append(")");
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" (line ").append(line).append(")");
		log.info(msg.toString());
	}

	// Program
	public void visit(Program program) {
		Obj progObj = program.getProgName().obj;
		Tab.chainLocalSymbols(progObj);
		Tab.closeScope();
	}

	public void visit(ProgName progName) {
		progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
		Tab.openScope();
	}

	// Namespace
	public void visit(NamespaceDecl namespace) {
		namespaces.add(curNamespace);
		curNamespace = "";
	}

	public void visit(NamespaceName namespaceName) {
		curNamespace = namespaceName.getName();
	}

	// VarDecl
	public void visit(VarDecl varDecl) {
		for (VarInfo curVar : curVars) {
			if (varDecl.getType().struct == Tab.noType) {
				curVars.clear();
				return;
			}
			if (Tab.find(curVar.name) != Tab.noObj) {
				report_error("Error. Symbol " + curVar.name + " redefinition", varDecl);
				continue;
			}
			if (curVar.isArray) {
				Tab.insert(Obj.Var, curVar.name, new Struct(Struct.Array, varDecl.getType().struct));
			} else {
				Tab.insert(Obj.Var, curVar.name, varDecl.getType().struct);
			}
		}
		curVars.clear();
	}

	public void visit(TypeNoRef typeNoRef) {
		Obj typeNode = Tab.find(typeNoRef.getName());
		if (typeNode == Tab.noObj) {
			report_error("Error. Type " + typeNoRef.getName() + " not found", typeNoRef);
			typeNoRef.struct = Tab.noType;
			return;
		}
		if (typeNode.getKind() != Obj.Type) {
			report_error("Error. Type " + typeNoRef.getName() + " not found", typeNoRef);
			typeNoRef.struct = Tab.noType;
			return;
		}
		typeNoRef.struct = typeNode.getType();
	}

	public void visit(VarIdent varIdent) {
		String varName = varIdent.getName();
		if (curNamespace != "") {
			varName = curNamespace + "::" + varName;
		}

		VarInfo curVar = new VarInfo(varName);
		curVars.add(curVar);
	}

	public void visit(VarIdentArr varIdentArr) {
		String varName = varIdentArr.getName();
		if (curNamespace != "") {
			varName = curNamespace + "::" + varName;
		}

		VarInfo curVar = new VarInfo(varName);
		curVar.isArray = true;
		curVars.add(curVar);
	}

	// ConstDecl
	public void visit(ConstDecl constDecl) {
		for (ConstInfo curConst : curConsts) {
			if (constDecl.getType().struct == Tab.noType) {
				curConsts.clear();
				return;
			}
			if (Tab.find(curConst.name) != Tab.noObj) {
				report_error("Error. Symbol " + curConst.name + " redefinition", constDecl);
				continue;
			}
			if (curConst.type != constDecl.getType().struct) {
				report_error("Error. Assigned type doesn't match the declared type", constDecl);
			}
			Obj constObj = Tab.insert(Obj.Con, curConst.name, curConst.type);
			constObj.setAdr(curConst.value);
		}
		curConsts.clear();
	}

	public void visit(ConstVar0 constVar) {
		String constName = constVar.getName();
		if (curNamespace != "") {
			constName = curNamespace + "::" + constName;
		}

		ConstInfo curConst = new ConstInfo(constName, curConstValue, curConstType);
		curConsts.add(curConst);
	}

	public void visit(ConstValueNum constValueNum) {
		curConstValue = constValueNum.getNumVal();
		curConstType = Tab.intType;
	}

	public void visit(ConstValueChar constValueChar) {
		curConstValue = constValueChar.getCharVal();
		curConstType = Tab.charType;
	}

	public void visit(ConstValueBool constValueBool) {
		boolean value = constValueBool.getBoolVal();
		curConstValue = value ? 1 : 0;
		curConstType = boolType;
	}
}
