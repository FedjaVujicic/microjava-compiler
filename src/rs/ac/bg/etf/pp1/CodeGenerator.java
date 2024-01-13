package rs.ac.bg.etf.pp1;

import java.util.Stack;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.*;
import rs.etf.pp1.symboltable.concepts.*;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPc;

	Stack<MulOper> nextMulOp = new Stack<MulOper>();
	Stack<AddOper> nextAddOp = new Stack<AddOper>();

	enum MulOper {
		MUL, DIV, REM
	};

	enum AddOper {
		ADD, SUB
	};

	public int getMainPc() {
		return mainPc;
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
		if (designatorIndex.obj.getKind() == Obj.Elem) {
			String varName = designatorIndex.obj.getName();
			Obj arrObj = designatorIndex.getDesignator().obj;
			Code.load(arrObj);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			return;
		}
		Code.load(designatorIndex.obj);
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
		int offset = funcCallNoArg.getDesignator().obj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}
}
