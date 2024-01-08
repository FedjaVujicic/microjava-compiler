package rs.ac.bg.etf.pp1;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticAnalyzer extends VisitorAdaptor {

	boolean errorDetected = false;

	ArrayList<String> curVars = new ArrayList<String>();

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

	// VarDecl
	public void visit(VarDecl varDecl) {
		for (String curVar : curVars) {
			Tab.insert(Obj.Var, curVar, varDecl.getType().struct);
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
		curVars.add(varIdent.getName());
		report_info("Declared variable " + varIdent.getName(), varIdent);
	}

	public void visit(VarIdentArr varIdentArr) {
		curVars.add(varIdentArr.getName());
		report_info("Declared variable " + varIdentArr.getName(), varIdentArr);
	}

}
