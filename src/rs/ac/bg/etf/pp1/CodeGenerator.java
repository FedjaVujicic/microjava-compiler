package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.*;
import rs.etf.pp1.symboltable.concepts.*;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPc;

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

	public void visit(FactorNum factorNum) {
		Code.load(new Obj(Obj.Con, "$", factorNum.struct, factorNum.getNumVal(), 0));
	}
}
