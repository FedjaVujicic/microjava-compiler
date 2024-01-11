package rs.ac.bg.etf.pp1;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
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

	public boolean passed() {
		return !errorDetected;
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

		if (obj.getType() == SymTab.intType) {
			msg.append("int, ");
		}
		if (obj.getType() == SymTab.charType) {
			msg.append("char, ");
		}
		if (obj.getType() == SymTab.boolType) {
			msg.append("bool, ");
		}
		if (obj.getType() == SymTab.arrayIntType) {
			msg.append("Arr of int, ");
		}
		if (obj.getType() == SymTab.arrayCharType) {
			msg.append("Arr of char, ");
		}
		if (obj.getType() == SymTab.arrayBoolType) {
			msg.append("Arr of bool, ");
		}
		msg.append(obj.getAdr()).append(", ");
		msg.append(obj.getLevel());
		return msg.toString();
	}

	// Program
	public void visit(Program program) {
		nVars = SymTab.currentScope().getnVars();
		Obj progObj = program.getProgName().obj;
		SymTab.chainLocalSymbols(progObj);
		SymTab.closeScope();

		if (!mainExists) {
			report_error("Error. No main function defined", null);
		}
	}

	public void visit(ProgName progName) {
		progName.obj = SymTab.insert(Obj.Prog, progName.getProgName(), SymTab.noType);
		SymTab.openScope();
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
			if (varDecl.getType().struct == SymTab.noType) {
				curVars.clear();
				return;
			}
			if (SymTab.find(curVar.name) != SymTab.noObj) {
				report_error("Error. Symbol " + curVar.name + " redefinition", varDecl);
				continue;
			}
			Struct type = varDecl.getType().struct;
			if (curVar.isArray) {
				if (type == SymTab.intType) {
					SymTab.insert(Obj.Var, curVar.name, SymTab.arrayIntType);
				} else if (type == SymTab.charType) {
					SymTab.insert(Obj.Var, curVar.name, SymTab.arrayCharType);
				} else if (type == SymTab.boolType) {
					SymTab.insert(Obj.Var, curVar.name, SymTab.arrayBoolType);
				} else {
					report_error("BIG ERROR. Undefined variable type", varDecl);
				}
			} else {
				SymTab.insert(Obj.Var, curVar.name, type);
			}
		}
		curVars.clear();
	}

	public void visit(TypeNoRef typeNoRef) {
		Obj typeNode = SymTab.find(typeNoRef.getName());
		if (typeNode == SymTab.noObj) {
			report_error("Error. Type " + typeNoRef.getName() + " not found", typeNoRef);
			typeNoRef.struct = SymTab.noType;
			return;
		}
		if (typeNode.getKind() != Obj.Type) {
			report_error("Error. Type " + typeNoRef.getName() + " not found", typeNoRef);
			typeNoRef.struct = SymTab.noType;
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
			if (constDecl.getType().struct == SymTab.noType) {
				curConsts.clear();
				return;
			}
			if (SymTab.find(curConst.name) != SymTab.noObj) {
				report_error("Error. Symbol " + curConst.name + " redefinition", constDecl);
				continue;
			}
			if (curConst.type != constDecl.getType().struct) {
				report_error("Error. Assigned type doesn't match the declared type", constDecl);
			}
			Obj constObj = SymTab.insert(Obj.Con, curConst.name, curConst.type);
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
		curConstType = SymTab.intType;
	}

	public void visit(ConstValueChar constValueChar) {
		curConstValue = constValueChar.getCharVal();
		curConstType = SymTab.charType;
	}

	public void visit(ConstValueBool constValueBool) {
		boolean value = constValueBool.getBoolVal();
		curConstValue = value ? 1 : 0;
		curConstType = SymTab.boolType;
	}

	// MethDecl
	public void visit(MethodDecl methodDecl) {
		if (methodRedefinition) {
			methodRedefinition = false;
			return;
		}

		SymTab.chainLocalSymbols(methodDecl.getMethodTypeName().obj);
		SymTab.closeScope();

		if (!curMethod.getName().endsWith("main")) {
			curMethod = SymTab.noObj;
			return;
		}
		if (curNamespace != "") {
			report_error("Error. Main function defined inside a namespace", methodDecl);
			curMethod = SymTab.noObj;
			return;
		}

		mainExists = true;
		if (curMethod.getType() != SymTab.noType) {
			report_error("Error. Main function must have a void type", methodDecl);
		}
		if (curMethod.getLevel() > 0) {
			report_error("Error. Main function can't contain parameters", methodDecl);
		}
		curMethod = SymTab.noObj;
	}

	public void visit(MethodTypeName methodTypeName) {
		String methName = methodTypeName.getMethName();
		if (curNamespace != "") {
			methName = curNamespace + "::" + methName;
		}
		if (SymTab.find(methName) != SymTab.noObj) {
			methodRedefinition = true;
			report_error("Error. Function " + methName + " redefinition", methodTypeName);
			return;
		}
		Struct methType = methodTypeName.getRetType().struct;

		methodTypeName.obj = SymTab.insert(Obj.Meth, methName, methType);
		curMethod = methodTypeName.obj;
		SymTab.openScope();
	}

	public void visit(ReturnType returnType) {
		returnType.struct = returnType.getType().struct;
	}

	public void visit(ReturnVoid returnVoid) {
		returnVoid.struct = SymTab.noType;
	}

	public void visit(FormPar formPar) {
		if (methodRedefinition) {
			return;
		}

		for (VarInfo curVar : curVars) {
			if (formPar.getType().struct == SymTab.noType) {
				curVars.clear();
				return;
			}
			if (SymTab.currentScope().findSymbol(curVar.name) != null) {
				report_error("Error. Symbol " + curVar.name + " redefinition", formPar);
				continue;
			}
			Struct type = formPar.getType().struct;
			if (curVar.isArray) {
				if (type == SymTab.intType) {
					SymTab.insert(Obj.Var, curVar.name, SymTab.arrayIntType);
				} else if (type == SymTab.charType) {
					SymTab.insert(Obj.Var, curVar.name, SymTab.arrayCharType);
				} else if (type == SymTab.boolType) {
					SymTab.insert(Obj.Var, curVar.name, SymTab.arrayBoolType);
				} else {
					report_error("BIG ERROR. Undefined variable type", formPar);
				}
			} else {
				SymTab.insert(Obj.Var, curVar.name, type);
			}
			curMethod.setLevel(curMethod.getLevel() + 1);
		}
		curVars.clear();
	}

	// Var and Con detection, Context rules
	public void visit(DesignatorIdent designatorIdent) {
		String name = designatorIdent.getName();
		Obj obj = SymTab.find(name);
		if (obj == SymTab.noObj) {
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
		Obj obj = SymTab.find(name);
		if (obj == SymTab.noObj) {
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
		if (varType != SymTab.arrayIntType && varType != SymTab.arrayCharType && varType != SymTab.arrayBoolType) {
			report_error("Error. " + varName + " is not an array", designatorIndex);
			designatorIndex.obj = SymTab.noObj;
			return;
		}
		Struct type = SymTab.noType;
		if (varType == SymTab.arrayIntType) {
			type = SymTab.intType;
		}
		if (varType == SymTab.arrayCharType) {
			type = SymTab.charType;
		}
		if (varType == SymTab.arrayBoolType) {
			type = SymTab.boolType;
		}
		designatorIndex.obj = new Obj(Obj.Elem, varName, type);

		Struct exprType = designatorIndex.getExpr().struct;
		if (exprType != SymTab.intType) {
			report_error("Error. Array index must be of type int", designatorIndex);
		}
	}

	public void visit(FactorExpr factorExpr) {
		factorExpr.struct = factorExpr.getExpr().struct;
	}

	public void visit(FactorNewExpr factorNewExpr) {
		Struct arrayType = factorNewExpr.getType().struct;
		Struct exprType = factorNewExpr.getExpr().struct;

		if (exprType != SymTab.intType) {
			report_error("Error. Argument of new must be type int", factorNewExpr);
		}

		if (arrayType == SymTab.intType) {
			factorNewExpr.struct = SymTab.arrayIntType;
		}

		if (arrayType == SymTab.charType) {
			factorNewExpr.struct = SymTab.arrayCharType;
		}

		if (arrayType == SymTab.boolType) {
			factorNewExpr.struct = SymTab.arrayBoolType;
		}
	}

	public void visit(FactorBool factorBool) {
		factorBool.struct = SymTab.boolType;
	}

	public void visit(FactorChar factorChar) {
		factorChar.struct = SymTab.charType;
	}

	public void visit(FactorNum factorNum) {
		factorNum.struct = SymTab.intType;
	}

	public void visit(FactorDesignatorFuncPars factorDesignatorFuncPars) {
		Obj obj = factorDesignatorFuncPars.getDesignator().obj;
		String name = factorDesignatorFuncPars.getDesignator().obj.getName();
		if (obj.getKind() != Obj.Meth) {
			report_error("Error. " + name + "is not a function", factorDesignatorFuncPars);
			factorDesignatorFuncPars.struct = SymTab.noType;
			actParsTypes.clear();
			return;
		}
		if (actParsTypes.size() != obj.getLevel()) {
			report_error("Error. Invalid function argument count", factorDesignatorFuncPars);
			factorDesignatorFuncPars.struct = SymTab.noType;
			actParsTypes.clear();
			return;
		}

		ArrayList<Obj> formPars = new ArrayList<Obj>(obj.getLocalSymbols());
		for (int i = 0; i < actParsTypes.size(); ++i) {
			Struct actType = actParsTypes.get(i);
			Struct formType = formPars.get(i).getType();
			if (actType != formType && (!(name == "len" && actParsTypes.get(i) == SymTab.arrayIntType
					|| actParsTypes.get(i) == SymTab.arrayCharType || actParsTypes.get(i) == SymTab.arrayBoolType))) {
				report_error("Error. Type mismatch in function call", factorDesignatorFuncPars);
				factorDesignatorFuncPars.struct = SymTab.noType;
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
			factorDesignatorFunc.struct = SymTab.noType;
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

		if (!(termType == SymTab.intType && factorType == SymTab.intType)) {
			report_error("Error. Factors must be of type int", termMulFactor);
			return;
		}

		termMulFactor.struct = SymTab.intType;
	}

	public void visit(ExprTerm exprTerm) {
		exprTerm.struct = exprTerm.getTerm().struct;
	}

	public void visit(ExprMinusTerm exprMinusTerm) {
		Struct termType = exprMinusTerm.getTerm().struct;

		if (termType != SymTab.intType) {
			report_error("Error. Term must be of type int", exprMinusTerm);
		}

		exprMinusTerm.struct = SymTab.intType;
	}

	public void visit(ExprAddTerm exprAddTerm) {
		Struct exprType = exprAddTerm.getExpr().struct;
		Struct termType = exprAddTerm.getTerm().struct;

		if (!(termType == SymTab.intType && exprType == SymTab.intType)) {
			report_error("Error. Terms must be of type int", exprAddTerm);
			return;
		}

		exprAddTerm.struct = SymTab.intType;
	}

	public void visit(CondFactExpr condFactExpr) {
		condFactExpr.struct = condFactExpr.getExpr().struct;
	}

	public void visit(CondFactRelExpr condFactRelExpr) {
		Struct exprType = condFactRelExpr.getExpr().struct;
		Struct expr1Type = condFactRelExpr.getExpr1().struct;

		if (exprType != expr1Type) {
			report_info("Error. Condition types do not match", condFactRelExpr);
			condFactRelExpr.struct = SymTab.noType;
			return;
		}

		condFactRelExpr.struct = SymTab.boolType;
	}

	public void visit(CondTermFact condTermFact) {
		Struct factType = condTermFact.getCondFact().struct;
		if (factType != SymTab.boolType) {
			condTermFact.struct = SymTab.noType;
			return;
		}
		condTermFact.struct = factType;
	}

	public void visit(CondTermAndFact condTermAndFact) {
		Struct factType = condTermAndFact.getCondFact().struct;
		if (factType != SymTab.boolType) {
			condTermAndFact.struct = SymTab.noType;
			return;
		}
		condTermAndFact.struct = factType;
	}

	public void visit(ConditionTerm conditionTerm) {
		Struct termType = conditionTerm.getCondTerm().struct;
		if (termType != SymTab.boolType) {
			conditionTerm.struct = SymTab.noType;
			return;
		}
		conditionTerm.struct = termType;
	}

	public void visit(ConditionOrTerm conditionOrTerm) {
		Struct termType = conditionOrTerm.getCondTerm().struct;
		if (termType != SymTab.boolType) {
			conditionOrTerm.struct = SymTab.noType;
			return;
		}
		conditionOrTerm.struct = termType;
	}

	public void visit(ActParsOneExpr actParsOneExpr) {
		Struct type = actParsOneExpr.getExpr().struct;
		actParsTypes.add(type);
	}

	public void visit(ActParsMulExpr actParsMulExpr) {
		Struct type = actParsMulExpr.getExpr().struct;
		actParsTypes.add(type);
	}

	public void visit(FuncCallNoArg funcCallNoArg) {
		String funcName = funcCallNoArg.getDesignator().obj.getName();
		Obj funcObj = funcCallNoArg.getDesignator().obj;

		if (funcObj.getKind() != Obj.Meth) {
			actParsTypes.clear();
			return;
		}

		if (actParsTypes.size() != funcObj.getLevel()) {
			report_error("Error. Invalid function argument count", funcCallNoArg);
			actParsTypes.clear();
			return;
		}
		actParsTypes.clear();
	}

	public void visit(FuncCallArg funcCallArg) {
		String funcName = funcCallArg.getDesignator().obj.getName();
		Obj funcObj = funcCallArg.getDesignator().obj;

		if (funcObj.getKind() != Obj.Meth) {
			actParsTypes.clear();
			return;
		}

		if (actParsTypes.size() != funcObj.getLevel()) {
			report_error("Error. Invalid function argument count", funcCallArg);
			actParsTypes.clear();
			return;
		}

		ArrayList<Obj> formPars = new ArrayList<Obj>(funcObj.getLocalSymbols());

		for (int i = 0; i < actParsTypes.size(); ++i) {
			Struct actType = actParsTypes.get(i);
			Struct formType = formPars.get(i).getType();
			if (actType != formType && (!(funcName == "len" && actParsTypes.get(i) == SymTab.arrayIntType
					|| actParsTypes.get(i) == SymTab.arrayCharType || actParsTypes.get(i) == SymTab.arrayBoolType))) {
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

	public void visit(Increment increment) {
		Obj designatorObj = increment.getDesignator().obj;

		if (designatorObj.getKind() != Obj.Var && designatorObj.getKind() != Obj.Elem) {
			report_error("Error. Invalid increment operand " + designatorObj.getName(), increment);
			return;
		}
		if (designatorObj.getType() != SymTab.intType) {
			report_error("Error. " + designatorObj.getName() + " is not a number", increment);
			return;
		}
	}

	public void visit(Decrement decrement) {
		Obj designatorObj = decrement.getDesignator().obj;

		if (designatorObj.getKind() != Obj.Var && designatorObj.getKind() != Obj.Elem) {
			report_error("Error. Invalid decrement operand " + designatorObj.getName(), decrement);
			return;
		}
		if (designatorObj.getType() != SymTab.intType) {
			report_error("Error. " + designatorObj.getName() + " is not a number", decrement);
			return;
		}
	}

	public void visit(StmtRead stmtRead) {
		Obj designatorObj = stmtRead.getDesignator().obj;

		if (designatorObj.getKind() != Obj.Var && designatorObj.getKind() != Obj.Elem) {
			report_error("Error. Cannot read " + designatorObj.getName(), stmtRead);
			return;
		}
		if (designatorObj.getType() != SymTab.intType && designatorObj.getType() != SymTab.charType
				&& designatorObj.getType() != SymTab.boolType) {
			report_error("Error. " + designatorObj.getName() + " must be int, char or bool type", stmtRead);
			return;
		}
	}

	public void visit(StmtPrintExpr stmtPrintExpr) {
		Struct exprType = stmtPrintExpr.getExpr().struct;

		if (exprType != SymTab.intType && exprType != SymTab.charType && exprType != SymTab.boolType) {
			report_error("Error. Print expression must be of int, char or bool type", stmtPrintExpr);
		}
	}

	public void visit(StmtPrintExprNum stmtPrintExprNum) {
		Struct exprType = stmtPrintExprNum.getExpr().struct;

		if (exprType != SymTab.intType && exprType != SymTab.charType && exprType != SymTab.boolType) {
			report_error("Error. Print expression must be of int, char or bool type", stmtPrintExprNum);
		}
	}

	public void visit(StmtReturn stmtReturn) {
		if (curMethod == SymTab.noObj || curMethod == null) {
			report_error("Error. Return statement found outside a function", stmtReturn);
			return;
		}
		if (curMethod.getType() != SymTab.noType) {
			report_error("Error. Function must return a value", stmtReturn);
			return;
		}
	}

	public void visit(StmtReturnExpr stmtReturnExpr) {
		if (curMethod == SymTab.noObj || curMethod == null) {
			report_error("Error. Return statement found outside a function", stmtReturnExpr);
			return;
		}

		Struct returnType = stmtReturnExpr.getExpr().struct;

		if (curMethod.getType() != returnType) {
			report_error("Error. Return type mismatch", stmtReturnExpr);
			return;
		}
	}

	public void visit(StmtIf stmtIf) {
		Struct cndType = stmtIf.getCondition().struct;

		if (cndType != SymTab.boolType) {
			report_error("Error. Condition must be of type bool", stmtIf);
			return;
		}
	}

	public void visit(StmtIfElse stmtIfElse) {
		Struct cndType = stmtIfElse.getCondition().struct;

		if (cndType != SymTab.boolType) {
			report_error("Error. Condition must be of type bool", stmtIfElse);
			return;
		}
	}

	public void visit(StmtBreak stmtBreak) {
		// Works because for loops are not supported
		report_error("Error. Break statement found outside a for loop", stmtBreak);
	}

	public void visit(StmtContinue stmtContinue) {
		// Works because for loops are not supported
		report_error("Error. Continue statement found outside a for loop", stmtContinue);
	}
}
