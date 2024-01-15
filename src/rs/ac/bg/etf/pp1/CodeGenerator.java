package rs.ac.bg.etf.pp1;

import java.util.LinkedList;
import java.util.Stack;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPc;

	Stack<MulOper> nextMulOp = new Stack<MulOper>();
	Stack<AddOper> nextAddOp = new Stack<AddOper>();
	Stack<RelOper> relOpStack = new Stack<RelOper>();

	LinkedList<RelOper> relOpBuffer = new LinkedList<RelOper>();

	// Stack of end-of-if addresses (One for every nested if) that need a fixup
	// to jump over the else body
	Stack<Integer> stmtEndAddrStack = new Stack<Integer>();

	// Stack of lists (One for every nested if) that contain every address that
	// needs a fixup to jump to either then or else
	Stack<LinkedList<Integer>> elseAddrStack = new Stack<LinkedList<Integer>>();
	Stack<LinkedList<Integer>> thenAddrStack = new Stack<LinkedList<Integer>>();

	// List of addresses that need a fixup to jump to the next OR condition
	LinkedList<Integer> orAddrList = new LinkedList<Integer>();

	// List of variables that need to be changed by multiple assignment
	LinkedList<Obj> mulAssignVars = new LinkedList<Obj>();
	Obj mulAssignArr;

	enum MulOper {
		MUL, DIV, REM
	};

	enum AddOper {
		ADD, SUB
	};

	enum RelOper {
		EQ, NE, LT, LE, GT, GE
	}

	public int getMainPc() {
		return mainPc;
	}

	public int getRelopCode(RelOper relOper) {
		switch (relOper) {
		case EQ:
			return Code.eq;
		case NE:
			return Code.ne;
		case LT:
			return Code.lt;
		case LE:
			return Code.le;
		case GT:
			return Code.gt;
		case GE:
			return Code.ge;
		default:
			return -1;
		}
	}

	public void visit(MethodTypeName methodTypeName) {
		if (methodTypeName.getMethName().equalsIgnoreCase("main")) {
			mainPc = Code.pc;
		}
		methodTypeName.obj.setAdr(Code.pc);

		SyntaxNode methodNode = methodTypeName.getParent();
		VarCounter varCounter = new VarCounter();
		FormParCounter formParCounter = new FormParCounter();
		methodNode.traverseTopDown(varCounter);
		methodNode.traverseTopDown(formParCounter);

		Code.put(Code.enter);
		Code.put(formParCounter.getCount());
		Code.put(varCounter.getCount());
	}

	public void visit(MethodDecl methodDecl) {
		Struct retType = methodDecl.getMethodTypeName().getRetType().struct;
		if (retType == SymTab.noType) {
			Code.put(Code.exit);
			Code.put(Code.return_);
		}
		Code.put(Code.trap);
		Code.put(1);
	}

	public void visit(StmtReturnExpr stmtReturnExpr) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	public void visit(StmtRead stmtRead) {
		Obj designatorObj = stmtRead.getDesignator().obj;
		if (designatorObj.getType() == SymTab.charType) {
			Code.put(Code.bread);
			Code.store(stmtRead.getDesignator().obj);
		} else if (designatorObj.getType() == SymTab.boolType) {
			Code.put(Code.read);
			Code.put(Code.dup);
			Code.loadConst(0);
			Code.put(Code.jcc + Code.lt);
			Code.put2(8);
			Code.put(Code.dup);
			Code.loadConst(1);
			Code.put(Code.jcc + Code.le);
			Code.put2(5);
			Code.put(Code.trap);
			Code.put(1);
			Code.store(stmtRead.getDesignator().obj);
		} else {
			Code.put(Code.read);
			Code.store(stmtRead.getDesignator().obj);
		}
	}

	public void visit(StmtPrintExpr stmtPrintExpr) {
		if (stmtPrintExpr.getExpr().struct == SymTab.charType) {
			Code.loadConst(2);
			Code.put(Code.bprint);
			return;
		}
		Code.loadConst(5);
		Code.put(Code.print);
	}

	public void visit(StmtPrintExprNum stmtPrintExprNum) {
		Code.loadConst(stmtPrintExprNum.getWidth());
		Code.put(Code.print);
	}

	public void visit(ExprAddTerm exprAddTerm) {
		AddOper op = nextAddOp.pop();
		switch (op) {
		case ADD:
			Code.put(Code.add);
			break;
		case SUB:
			Code.put(Code.sub);
			break;
		}
	}

	public void visit(ExprMinusTerm exprMinusTerm) {
		Code.put(Code.neg);
	}

	public void visit(OpAdd opAdd) {
		nextAddOp.push(AddOper.ADD);
	}

	public void visit(OpSub opSub) {
		nextAddOp.push(AddOper.SUB);
	}

	public void visit(TermMulFactor termMulFactor) {
		MulOper op = nextMulOp.pop();
		switch (op) {
		case MUL:
			Code.put(Code.mul);
			break;
		case DIV:
			Code.put(Code.div);
			break;
		case REM:
			Code.put(Code.rem);
			break;
		}
	}

	public void visit(OpMul opMul) {
		nextMulOp.push(MulOper.MUL);
	}

	public void visit(OpDiv opDiv) {
		nextMulOp.push(MulOper.DIV);
	}

	public void visit(OpMod opMod) {
		nextMulOp.push(MulOper.REM);
	}

	public void visit(FactorDesignator factorDesignator) {
		Code.load(factorDesignator.getDesignator().obj);
	}

	public void visit(FactorDesignatorFunc factorDesignatorFunc) {
		int offset = factorDesignatorFunc.getFuncName().getDesignator().obj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}

	public void visit(FactorDesignatorFuncPars factorDesignatorFuncPars) {
		int offset = factorDesignatorFuncPars.getFuncName().getDesignator().obj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}

	public void visit(FactorNum factorNum) {
		Code.load(new Obj(Obj.Con, "$", factorNum.struct, factorNum.getNumVal(), 0));
	}

	public void visit(FactorChar factorChar) {
		Code.load(new Obj(Obj.Con, "$", factorChar.struct, factorChar.getCharVal(), 0));
	}

	public void visit(FactorBool factorBool) {
		boolean value = factorBool.getBoolVal();
		int constVal = value ? 1 : 0;
		Code.load(new Obj(Obj.Con, "$", factorBool.struct, constVal, 0));
	}

	public void visit(FactorNewExpr factorNewExpr) {
		Struct arrType = factorNewExpr.struct.getElemType();
		if (arrType == SymTab.charType) {
			Code.put(Code.newarray);
			Code.put(0);
		} else {
			Code.put(Code.newarray);
			Code.put(1);
		}

	}

	public void visit(DesignatorIndex designatorIndex) {
		// -> expr : ..., addr, index
		Obj arrObj = designatorIndex.getDesignator().obj;
		Code.load(arrObj);
		Code.put(Code.dup_x1);
		Code.put(Code.pop);
	}

	public void visit(Assignment assignment) {
		Code.store(assignment.getDesignator().obj);
	}

	public void visit(Increment increment) {
		Code.load(increment.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(increment.getDesignator().obj);
	}

	public void visit(Decrement decrement) {
		Code.load(decrement.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(decrement.getDesignator().obj);
	}

	public void visit(FuncCallNoArg funcCallNoArg) {
		int offset = funcCallNoArg.getFuncName().getDesignator().obj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}

	public void visit(FuncCallArg funcCallArg) {
		int offset = funcCallArg.getFuncName().getDesignator().obj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}

	public void visit(OpEqual opEqual) {
		relOpStack.push(RelOper.EQ);
		relOpBuffer.add(RelOper.EQ);
	}

	public void visit(OpNotEqual opNotEqual) {
		relOpStack.push(RelOper.NE);
		relOpBuffer.add(RelOper.NE);
	}

	public void visit(OpLesser opLesser) {
		relOpStack.push(RelOper.LT);
		relOpBuffer.add(RelOper.LT);
	}

	public void visit(OpLesserEqual opLesserEqual) {
		relOpStack.push(RelOper.LE);
		relOpBuffer.add(RelOper.LE);
	}

	public void visit(OpGreater opGreater) {
		relOpStack.push(RelOper.GT);
		relOpBuffer.add(RelOper.GT);
	}

	public void visit(OpGreaterEqual opGreaterEqual) {
		relOpStack.push(RelOper.GE);
		relOpBuffer.add(RelOper.GE);
	}

	public void visit(StmtIfElse stmtIfElse) {
		// End of if else found. Fixup jump over else address
		Code.fixup(stmtEndAddrStack.pop() + 1);
	}

	public void visit(IfWord ifWord) {
		elseAddrStack.push(new LinkedList<Integer>());
		thenAddrStack.push(new LinkedList<Integer>());
	}

	public void visit(CondFactRelExpr condFactRelExpr) {
		// Generates
		// if (!cndF1) elseAddr
		// if (!cndF2) elseAddr
		// if (!cndF3) elseAddr
		// ...
		// for current CondTerm
		RelOper relOp = relOpStack.pop();
		elseAddrStack.peek().add(Code.pc);
		Code.putFalseJump(getRelopCode(relOp), 0);
	}

	public void visit(IfCnd ifCnd) {
		// Start of then found. Fixup addresses waiting for then
		relOpBuffer.clear();
		for (int addr : thenAddrStack.peek()) {
			Code.fixup(addr + 1);
		}
		thenAddrStack.pop();
	}

	public void visit(Or_ or_) {
		// Changes previous CondTerm from
		// jmp (!cndF1) elseAddr
		// jmp (!cndF2) elseAddr
		// jmp (!cndF3) elseAddr
		// ...
		// to
		// jmp (!cndF1) nextCondTermAddr
		// jmp (!cndF2) nextCondTermAddr
		// jmp (cndF3) thenAddr
		// whenever a || token is found
		while (elseAddrStack.peek().size() > 0) {
			Code.pc = elseAddrStack.peek().removeFirst();
			RelOper op = relOpBuffer.removeFirst();

			if (elseAddrStack.peek().size() > 0) {
				orAddrList.add(Code.pc);
				Code.putFalseJump(getRelopCode(op), 0);
			} else {
				thenAddrStack.peek().add(Code.pc);
				Code.putFalseJump(Code.inverse[getRelopCode(op)], 0);
			}
		}

		for (int addr : orAddrList) {
			Code.fixup(addr + 1);
		}
		orAddrList.clear();
	}

	public void visit(IfBody ifBody) {
		// Start of else found
		// In case of if-else create a jump over then body
		// Fixup addresses waiting for else
		if (ifBody.getParent().getClass() == StmtIfElse.class) {
			stmtEndAddrStack.push(Code.pc);
			Code.putJump(0);
		}
		for (int addr : elseAddrStack.peek()) {
			Code.fixup(addr + 1);
		}
		elseAddrStack.pop();
	}

	public void visit(MultipleAssignment multipleAssignment) {
		Obj arrRObj = multipleAssignment.getDesignator().obj;

		// Generates code for assignment to variables
		for (int i = 0; i < mulAssignVars.size(); ++i) {
			Obj curVar = mulAssignVars.get(i);
			if (curVar == SymTab.noObj) {
				continue;
			}
			if (curVar.getKind() == Obj.Elem) {
				continue;
			}
			// Loads the array addr onto the expr stack
			Code.load(arrRObj);
			// Loads the array index onto the expr stack
			Code.loadConst(i);
			Code.put(Code.aload);
			Code.store(curVar);
		}

		// Generates code for assignment to array elements
		// When visiting designator[], the expr stack is already filled with addr, ind,
		// addr, ind... The mulAssign vars then need to be traversed backwards so that
		// they can pop their respective addr and ind
		for (int i = mulAssignVars.size() - 1; i >= 0; --i) {
			Obj curVar = mulAssignVars.get(i);
			if (curVar == SymTab.noObj) {
				continue;
			}
			if (curVar.getKind() != Obj.Elem) {
				continue;
			}
			// Loads the arrayR addr onto the expr stack
			Code.load(arrRObj);
			// Loads the arrayR index onto the expr stack
			Code.loadConst(i);
			Code.put(Code.aload);
			Code.store(curVar);
		}

		mulAssignVars.clear();
	}

	public void visit(DesignatorListNoElem designatorListNoElem) {
		mulAssignVars.add(SymTab.noObj);
	}

	public void visit(DesignatorListElem designatorListElem) {
		Obj designatorObj = designatorListElem.getDesignator().obj;
		mulAssignVars.add(designatorObj);
	}

	public void visit(MulAssignArray mulAssignArray) {
	}
}
