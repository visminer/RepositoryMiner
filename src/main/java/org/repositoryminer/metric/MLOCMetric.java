package org.repositoryminer.metric;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.repositoryminer.ast.AST;
import org.repositoryminer.ast.AbstractTypeDeclaration;
import org.repositoryminer.ast.MethodDeclaration;

public class MLOCMetric extends MethodBasedMetricTemplate {
	
	private List<Document> methodsDoc;
	private Pattern pattern;

	public MLOCMetric(){
		pattern = Pattern.compile("(\r\n)|(\r)|(\n)");
	}

	@Override
	public void calculate(AbstractTypeDeclaration type, List<MethodDeclaration> methods, AST ast, Document document) {
		
		methodsDoc = new ArrayList<Document>();
		
		for(MethodDeclaration method : methods){
			int mloc = calculate(method, ast);
			methodsDoc.add(new Document("method", method.getName()).append("value", new Integer(mloc)));
		}
		
		document.append("name", new String("MLOC")).append("methods", methodsDoc);
	}
	
	
	public int calculate(MethodDeclaration method, AST ast){
		String sourceCode = ast.getSourceCode();
				
		String methodSourceCode = sourceCode.substring(method.getStartPositionInSourceCode(), method.getEndPositionInSourceCode());

		if(methodSourceCode == null || methodSourceCode.length() == 0)
			return 0;

		//TODO ignore blank lines and comments
		Matcher m = pattern.matcher(methodSourceCode);

		//starts from 1 because the matcher doesn't count the last line (only line breaks)
		int i = 1;

		while(m.find())
			i++;

		return i;
		
	}

}
