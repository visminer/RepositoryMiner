package br.edu.ufba.softvis.visminer.metric;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import br.edu.ufba.softvis.visminer.annotations.MetricAnnotation;
import br.edu.ufba.softvis.visminer.ast.AST;
import br.edu.ufba.softvis.visminer.ast.Document;
import br.edu.ufba.softvis.visminer.constant.MetricType;
import br.edu.ufba.softvis.visminer.constant.MetricUid;
import br.edu.ufba.softvis.visminer.model.bean.Commit;
import br.edu.ufba.softvis.visminer.model.bean.File;
import br.edu.ufba.softvis.visminer.persistence.MetricPersistance;

@MetricAnnotation(
		name = "Number of Enums",
		description = "Number of Enums is a software metric used to measure the size of a computer program"+
				" by counting the number of enums",
		acronym = "NOE",
		type = MetricType.COMPLEX,
		uid = MetricUid.NOE
	)
public class NOEMetric implements IMetric{

	@Override
	public void calculate(Map<File, AST> filesMap, List<Commit> commits, MetricPersistance persistence) {
		
		Map<Integer, Integer> packageCls = new HashMap<Integer, Integer>();
		
		for(Entry<File, AST> entry : filesMap.entrySet()){
			
			AST ast = entry.getValue();
			
			if(ast == null){
				continue;
			}
			
			Document doc = ast.getDocument();
			int id, num = 0;

			if(doc.getEnumsDeclarations() != null){
				num = doc.getEnumsDeclarations().size();
			}
			
			if(doc.getPackageDeclaration() != null){
				id = doc.getPackageDeclaration().getId();
			}else{
				id = ast.getProject().getId();
			}
			
			if(packageCls.containsKey(id)){
				packageCls.put(id, packageCls.get(id) + num);
			}else{
				packageCls.put(id, num);
			}
			
		}
		
		for(Entry<Integer, Integer> entry : packageCls.entrySet()){
			persistence.postMetricValue(entry.getKey(), String.valueOf(entry.getValue()));
		}
		
	}

}
