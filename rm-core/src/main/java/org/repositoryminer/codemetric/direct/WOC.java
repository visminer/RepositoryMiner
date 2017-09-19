package org.repositoryminer.codemetric.direct;

import java.math.BigDecimal;
import java.util.List;

import org.repositoryminer.ast.AST;
import org.repositoryminer.ast.AbstractField;
import org.repositoryminer.ast.AbstractMethod;
import org.repositoryminer.ast.AbstractType;

@DirectMetricProperties(id = MetricId.WOC)
public class WOC implements IDirectCodeMetric {

	private static final MetricId ID = MetricId.WOC;

	@Override
	public void calculate(AST ast) {
		for (AbstractType type : ast.getTypes()) {
			type.getMetrics().put(ID, calculate(type.getMethods(), type.getFields()));
		}
	}

	public float calculate(List<AbstractMethod> methods, List<AbstractField> fields) {
		int publicMembers = 0;
		int functionalMembers = 0;

		for (AbstractField field : fields) {
			if (field.getModifiers().contains("public")) {
				publicMembers++;
			}
		}

		for (AbstractMethod method : methods) {
			if (method.getModifiers().contains("public")) {
				publicMembers++;
				if (!method.isAccessor()) {
					functionalMembers++;
				}
			}
		}

		float result = publicMembers == 0 ? 0 : functionalMembers * 1.0f / publicMembers;
		return new BigDecimal(result).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
	}

}