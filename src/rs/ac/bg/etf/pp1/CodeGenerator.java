package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.*;

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

}
