package org.repositoryminer.metric;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.repositoryminer.ast.AST;
import org.repositoryminer.ast.AbstractTypeDeclaration;
import org.repositoryminer.ast.MethodDeclaration;
import org.repositoryminer.ast.NodeType;
import org.repositoryminer.ast.Statement;

public class LVARMetric extends MethodBasedMetricTemplate {

	private List<Document> methodsDoc;
	
	@Override
	public void calculate(AbstractTypeDeclaration type, List<MethodDeclaration> methods, AST ast, Document document) {
		
		methodsDoc = new ArrayList<Document>();
		
		for(MethodDeclaration method : methods){
			int lvar = calculate(method);
			methodsDoc.add(new Document("method", method.getName()).append("value", new Integer(lvar)));
		}
		
		document.append("name", new String("LVAR")).append("methods", methodsDoc);
	}
	
	public int calculate(MethodDeclaration method){
		List<String> lvar = new ArrayList<String>();

		for(Statement statement : method.getStatements()){
			if(NodeType.VARIABLE.equals(statement.getNodeType()) && !lvar.contains(statement.getExpression()))
				lvar.add(statement.getExpression());
		}
		
		return lvar.size();
	}

}
