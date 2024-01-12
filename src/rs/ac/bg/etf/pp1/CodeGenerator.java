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
		if (methodTypeName.getMethName() == "main") {
			mainPc = Code.pc;
		}

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

	public void visit(StmtPrintExpr stmtPrintExpr) {
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

	public void visit(FactorNum factorNum) {
		Code.load(new Obj(Obj.Con, "$", factorNum.struct, factorNum.getNumVal(), 0));
	}

}
