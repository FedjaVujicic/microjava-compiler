package rs.ac.bg.etf.pp1;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticAnalyzer extends VisitorAdaptor {

	boolean errorDetected = false;
	boolean mainExists = false;
	boolean methodRedefinition = false;
	String curNamespace = "";
	int curConstValue;
	int nVars;
	Struct curConstType;
	Obj curMethod;

	ArrayList<VarInfo> curVars = new ArrayList<VarInfo>();
	ArrayList<ConstInfo> curConsts = new ArrayList<ConstInfo>();
	ArrayList<String> namespaces = new ArrayList<String>();
	ArrayList<Struct> actParsTypes = new ArrayList<Struct>();

	static final Struct boolType = Tab.insert(Obj.Type, "bool", new Struct(Struct.Bool)).getType();
	static final Struct arrayIntType = Tab.insert(Obj.Type, "arrayInt", new Struct(Struct.Array, Tab.intType))
			.getType();
	static final Struct arrayCharType = Tab.insert(Obj.Type, "arrayChar", new Struct(Struct.Array, Tab.charType))
			.getType();
	static final Struct arrayBoolType = Tab.insert(Obj.Type, "arrayBool", new Struct(Struct.Array, boolType)).getType();

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

	public String getSymbolDetectedMsg(Obj obj) {
		StringBuilder msg = new StringBuilder();
		msg.append("Detected ");
		switch (obj.getKind()) {
		case Obj.Con:
			msg.append("Con ");
			break;
		case Obj.Var:
			msg.append("Var ");
			break;
		case Obj.Meth:
			msg.append("Meth ");
			break;
		default:
			break;
		}
		msg.append(obj.getName()).append(": ");

		if (obj.getType() == Tab.intType) {
			msg.append("int, ");
		}
		if (obj.getType() == Tab.charType) {
			msg.append("char, ");
		}
		if (obj.getType() == boolType) {
			msg.append("bool, ");
		}
		if (obj.getType() == arrayIntType) {
			msg.append("Arr of int, ");
		}
		if (obj.getType() == arrayCharType) {
			msg.append("Arr of char, ");
		}
		if (obj.getType() == arrayBoolType) {
			msg.append("Arr of bool, ");
		}
		msg.append(obj.getAdr()).append(", ");
		msg.append(obj.getLevel());
		return msg.toString();
	}

	// Program
	public void visit(Program program) {
		nVars = Tab.currentScope().getnVars();
		Obj progObj = program.getProgName().obj;
		Tab.chainLocalSymbols(progObj);
		Tab.closeScope();

		if (!mainExists) {
			report_error("Error. No main function defined", null);
		}
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
			Struct type = varDecl.getType().struct;
			if (curVar.isArray) {
				if (type == Tab.intType) {
					Tab.insert(Obj.Var, curVar.name, arrayIntType);
				} else if (type == Tab.charType) {
					Tab.insert(Obj.Var, curVar.name, arrayCharType);
				} else if (type == boolType) {
					Tab.insert(Obj.Var, curVar.name, arrayBoolType);
				} else {
					report_error("BIG ERROR. Undefined variable type", varDecl);
				}
			} else {
				Tab.insert(Obj.Var, curVar.name, type);
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

	// MethDecl
	public void visit(MethodDecl methodDecl) {
		if (methodRedefinition) {
			methodRedefinition = false;
			return;
		}

		Tab.chainLocalSymbols(methodDecl.getMethodTypeName().obj);
		Tab.closeScope();

		if (!curMethod.getName().endsWith("main")) {
			curMethod = Tab.noObj;
			return;
		}
		if (curNamespace != "") {
			report_error("Error. Main function defined inside a namespace", methodDecl);
			curMethod = Tab.noObj;
			return;
		}

		mainExists = true;
		if (curMethod.getType() != Tab.noType) {
			report_error("Error. Main function must have a void type", methodDecl);
		}
		if (curMethod.getLevel() > 0) {
			report_error("Error. Main function can't contain parameters", methodDecl);
		}
		curMethod = Tab.noObj;
	}

	public void visit(MethodTypeName methodTypeName) {
		String methName = methodTypeName.getMethName();
		if (curNamespace != "") {
			methName = curNamespace + "::" + methName;
		}
		if (Tab.find(methName) != Tab.noObj) {
			methodRedefinition = true;
			report_error("Error. Function " + methName + " redefinition", methodTypeName);
			return;
		}
		Struct methType = methodTypeName.getRetType().struct;

		methodTypeName.obj = Tab.insert(Obj.Meth, methName, methType);
		curMethod = methodTypeName.obj;
		Tab.openScope();
	}

	public void visit(ReturnType returnType) {
		returnType.struct = returnType.getType().struct;
	}

	public void visit(ReturnVoid returnVoid) {
		returnVoid.struct = Tab.noType;
	}

	public void visit(FormPar formPar) {
		if (methodRedefinition) {
			return;
		}

		for (VarInfo curVar : curVars) {
			if (formPar.getType().struct == Tab.noType) {
				curVars.clear();
				return;
			}
			if (Tab.currentScope().findSymbol(curVar.name) != null) {
				report_error("Error. Symbol " + curVar.name + " redefinition", formPar);
				continue;
			}
			Struct type = formPar.getType().struct;
			if (curVar.isArray) {
				if (type == Tab.intType) {
					Tab.insert(Obj.Var, curVar.name, arrayIntType);
				} else if (type == Tab.charType) {
					Tab.insert(Obj.Var, curVar.name, arrayCharType);
				} else if (type == boolType) {
					Tab.insert(Obj.Var, curVar.name, arrayBoolType);
				} else {
					report_error("BIG ERROR. Undefined variable type", formPar);
				}
			} else {
				Tab.insert(Obj.Var, curVar.name, type);
			}
			curMethod.setLevel(curMethod.getLevel() + 1);
		}
		curVars.clear();
	}

	// Var and Con detection, Context rules
	public void visit(DesignatorIdent designatorIdent) {
		String name = designatorIdent.getName();
		Obj obj = Tab.find(name);
		if (obj == Tab.noObj) {
			report_error("Error. Undefined symbol " + name, designatorIdent);
			designatorIdent.obj = obj;
			return;
		}
		if (obj.getKind() != Obj.Con && obj.getKind() != Obj.Var && obj.getKind() != Obj.Meth) {
			report_error("Error. Invalid identifier " + name, designatorIdent);
			designatorIdent.obj = obj;
			return;
		}
		designatorIdent.obj = obj;
		report_info(getSymbolDetectedMsg(obj), designatorIdent);
	}

	public void visit(DesignatorIdentRef designatorIdentRef) {
		if (!namespaces.contains(designatorIdentRef.getNamespace())) {
			report_error("Error. " + designatorIdentRef.getNamespace() + " is not a namespace", designatorIdentRef);
		}
		String name = designatorIdentRef.getNamespace() + "::" + designatorIdentRef.getName();
		Obj obj = Tab.find(name);
		if (obj == Tab.noObj) {
			report_error("Error. Undefined symbol " + name, designatorIdentRef);
			designatorIdentRef.obj = obj;
			return;
		}
		if (obj.getKind() != Obj.Con && obj.getKind() != Obj.Var && obj.getKind() != Obj.Meth) {
			report_error("Error. Invalid identifier " + name, designatorIdentRef);
			designatorIdentRef.obj = obj;
			return;
		}
		designatorIdentRef.obj = obj;
		report_info(getSymbolDetectedMsg(obj), designatorIdentRef);
	}

	public void visit(DesignatorIndex designatorIndex) {
		String varName = designatorIndex.getDesignator().obj.getName();
		Struct varType = designatorIndex.getDesignator().obj.getType();
		if (varType != arrayIntType && varType != arrayCharType && varType != arrayBoolType) {
			report_error("Error. " + varName + " is not an array", designatorIndex);
			designatorIndex.obj = Tab.noObj;
			return;
		}
		Struct type = Tab.noType;
		if (varType == arrayIntType) {
			type = Tab.intType;
		}
		if (varType == arrayCharType) {
			type = Tab.charType;
		}
		if (varType == arrayBoolType) {
			type = boolType;
		}
		designatorIndex.obj = new Obj(Obj.Elem, varName, type);

		Struct exprType = designatorIndex.getExpr().struct;
		if (exprType != Tab.intType) {
			report_error("Error. Array index must be of type int", designatorIndex);
		}
	}

	public void visit(FactorExpr factorExpr) {
	}

	public void visit(FactorNewExpr factorNewExpr) {
		factorNewExpr.struct = factorNewExpr.getType().struct;
		Struct exprType = factorNewExpr.getExpr().struct;

		if (exprType != Tab.intType) {
			report_error("Error. Argument of new must be type int", factorNewExpr);
		}
	}

	public void visit(FactorBool factorBool) {
		factorBool.struct = boolType;
	}

	public void visit(FactorChar factorChar) {
		factorChar.struct = Tab.charType;
	}

	public void visit(FactorNum factorNum) {
		factorNum.struct = Tab.intType;
	}

	public void visit(FactorDesignatorFuncPars factorDesignatorFuncPars) {
		Obj obj = factorDesignatorFuncPars.getDesignator().obj;
		String name = factorDesignatorFuncPars.getDesignator().obj.getName();
		if (obj.getKind() != Obj.Meth) {
			report_error("Error. " + name + "is not a function", factorDesignatorFuncPars);
			factorDesignatorFuncPars.struct = Tab.noType;
			actParsTypes.clear();
			return;
		}
		if (actParsTypes.size() != obj.getLevel()) {
			report_error("Error. Invalid function argument count", factorDesignatorFuncPars);
			factorDesignatorFuncPars.struct = Tab.noType;
			actParsTypes.clear();
			return;
		}

		ArrayList<Obj> formPars = new ArrayList<Obj>(obj.getLocalSymbols());
		for (int i = 0; i < actParsTypes.size(); ++i) {
			if (actParsTypes.get(i) != formPars.get(i).getType()) {
				report_error("Error. Type mismatch in function call", factorDesignatorFuncPars);
				factorDesignatorFuncPars.struct = Tab.noType;
				actParsTypes.clear();
				return;
			}
		}
		actParsTypes.clear();

		Struct designatorType = factorDesignatorFuncPars.getDesignator().obj.getType();
		factorDesignatorFuncPars.struct = designatorType;
	}

	public void visit(FactorDesignatorFunc factorDesignatorFunc) {
		int kind = factorDesignatorFunc.getDesignator().obj.getKind();
		String name = factorDesignatorFunc.getDesignator().obj.getName();
		if (kind != Obj.Meth) {
			report_error("Error. " + name + "is not a function", factorDesignatorFunc);
			factorDesignatorFunc.struct = Tab.noType;
			return;
		}
		Struct designatorType = factorDesignatorFunc.getDesignator().obj.getType();
		factorDesignatorFunc.struct = designatorType;
	}

	public void visit(FactorDesignator factorDesignator) {
		Struct designatorType = factorDesignator.getDesignator().obj.getType();
		factorDesignator.struct = designatorType;
	}

	public void visit(TermFactor termFactor) {
		termFactor.struct = termFactor.getFactor().struct;
	}

	public void visit(TermMulFactor termMulFactor) {
		Struct termType = termMulFactor.getTerm().struct;
		Struct factorType = termMulFactor.getFactor().struct;

		if (!(termType == Tab.intType && factorType == Tab.intType)) {
			report_error("Error. Factors must be of type int", termMulFactor);
			return;
		}

		termMulFactor.struct = Tab.intType;
	}

	public void visit(ExprTerm exprTerm) {
		exprTerm.struct = exprTerm.getTerm().struct;
	}

	public void visit(ExprMinusTerm exprMinusTerm) {
		Struct termType = exprMinusTerm.getTerm().struct;

		if (termType != Tab.intType) {
			report_error("Error. Term must be of type int", exprMinusTerm);
		}

		exprMinusTerm.struct = Tab.intType;
	}

	public void visit(ExprAddTerm exprAddTerm) {
		Struct exprType = exprAddTerm.getExpr().struct;
		Struct termType = exprAddTerm.getTerm().struct;

		if (!(termType == Tab.intType && exprType == Tab.intType)) {
			report_error("Error. Terms must be of type int", exprAddTerm);
			return;
		}

		exprAddTerm.struct = Tab.intType;
	}

	public void visit(CondFactRelExpr condFactRelExpr) {
		Struct exprType = condFactRelExpr.getExpr().struct;
		Struct expr1Type = condFactRelExpr.getExpr1().struct;

		if (exprType != expr1Type) {
			report_info("Error. Condition types do not match", condFactRelExpr);
		}
	}

	public void visit(ActParsOneExpr actParsOneExpr) {
		Struct type = actParsOneExpr.getExpr().struct;
		actParsTypes.add(type);
	}

	public void visit(ActParsMulExpr actParsMulExpr) {
		Struct type = actParsMulExpr.getExpr().struct;
		actParsTypes.add(type);
	}

	public void visit(FuncCallArg funcCallArg) {
		String funcName = funcCallArg.getDesignator().obj.getName();
		Obj funcObj = funcCallArg.getDesignator().obj;

		if (funcObj.getKind() != Obj.Meth) {
			report_error("Error. " + funcName + "is not a function", funcCallArg);
			actParsTypes.clear();
			return;
		}

		if (actParsTypes.size() != funcObj.getLevel()) {
			report_error("Error. Invalid function argument count", funcCallArg);
		}

		ArrayList<Obj> formPars = new ArrayList<Obj>(funcObj.getLocalSymbols());

		for (int i = 0; i < actParsTypes.size(); ++i) {
			if (actParsTypes.get(i) != formPars.get(i).getType()) {
				if (actParsTypes.get(i) == arrayIntType) {
					report_info("ACTUAL GUD", null);
				}
				if (formPars.get(i).getType() == arrayIntType) {
					report_info("FORMAL GUD", null);
				}
				report_error("Error. Type mismatch in function call", funcCallArg);
			}
		}

		actParsTypes.clear();
	}

	public void visit(Assignment assignment) {
		Obj designatorObj = assignment.getDesignator().obj;
		Struct exprType = assignment.getAssignExpr().struct;
		
		if (designatorObj.getKind() != Obj.Var && designatorObj.getKind() != Obj.Elem) {
			report_error("Error. Invalid left side operand", assignment);
			return;
		}
		if (designatorObj.getType() != exprType) {
			report_error("Error. Incompatible assignment types", assignment);
		}
	}
	
	public void visit(AssignExpr0 assignExpr) {
		assignExpr.struct = assignExpr.getExpr().struct;
	}
}
