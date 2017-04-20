package org.repositoryminer.codemetric.indirect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.repositoryminer.ast.AST;
import org.repositoryminer.ast.AbstractClassDeclaration;
import org.repositoryminer.codemetric.CodeMetricId;
import org.repositoryminer.codemetric.direct.EC;

public class AC implements IIndirectCodeMetric{

	//this map holds the AC metric for each found type
	private Map<String, Map<String, Integer>> acMap = new HashMap<>();
	
	@Override
	public void calculate(AbstractClassDeclaration type, AST ast) {		
		EC ec = new EC();
		Map<String, Integer> ecMap = ec.calculate(type);
		for(Entry<String, Integer> entry : ecMap.entrySet()){
			Map<String, Integer> typeAcRelations = acMap.getOrDefault(entry.getKey(), new HashMap<>());
			typeAcRelations.put(type.getName(), entry.getValue());
			acMap.put(entry.getKey(), typeAcRelations);
		}
	}

	@Override
	public CodeMetricId getId() {
		return CodeMetricId.AC;
	}

	@Override
	public Map<String, Document> getResult() {

		Map<String, Document> result = new HashMap<>();
		for(Entry<String, Map<String, Integer>> entry : acMap.entrySet()){
			List<Document> acRelationsDoc = new ArrayList<>();
			entry.getValue().entrySet().stream().forEach( relation -> 
						acRelationsDoc.add(
								new Document("class",relation.getKey())
								.append("value", relation.getValue())));
			result.put(entry.getKey(),
							new Document("metric",this.getId().toString())
								.append("classes", acRelationsDoc)
								.append("afferentCount", entry.getValue().keySet().size()));
		}
		
		acMap.clear();
		
		return result;
	}

}
