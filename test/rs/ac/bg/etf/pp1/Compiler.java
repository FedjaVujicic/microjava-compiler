package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import java_cup.runtime.Symbol;
import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;

public class Compiler {

	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}

	public static void main(String[] args) throws Exception {
		Logger log = Logger.getLogger(Compiler.class);
		if (args.length < 1) {
			log.error("Not enough arguments supplied! Usage: MJParser <source-file> <obj-file> ");
			return;
		}

		File sourceCode = new File(args[0]);
		if (!sourceCode.exists()) {
			log.error("Source file [" + sourceCode.getAbsolutePath() + "] not found!");
			return;
		}

		log.info("Compiling source file: " + sourceCode.getAbsolutePath());

		try (BufferedReader br = new BufferedReader(new FileReader(sourceCode))) {
			Yylex lexer = new Yylex(br);
			MJParser p = new MJParser(lexer);
			Symbol s = p.parse(); // pocetak parsiranja
			SyntaxNode prog = (SyntaxNode) (s.value);

			if (p.errorDetected) {
				log.error("Parsing failed!");
				return;
			}
			log.info("Parsing successful!");

			System.out.println("=======================SEMANTIC ANALYSIS=======================");

			Tab.init(); // Universe scope
			SemanticAnalyzer semanticCheck = new SemanticAnalyzer();
			prog.traverseBottomUp(semanticCheck);

			Tab.dump();
			System.out.println("===============================================================");

			if (!semanticCheck.passed()) {
				log.error("Semantic check failed!");
				return;
			}
			log.info("Semantic check passed!");

			log.info("Compiling finished!");
		}
	}
}
