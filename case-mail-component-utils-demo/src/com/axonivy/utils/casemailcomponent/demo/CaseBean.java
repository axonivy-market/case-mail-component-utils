package com.axonivy.utils.casemailcomponent.demo;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.casemailcomponent.demo.model.CaseModel;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class CaseBean {
	private static final String CASE_REFERENCE_REGEX_VAR = "mailstoreUtils.caseReferenceRegex";

	public String buildCaseReference(String caseId) {
		String regexPattern = Ivy.var().get(CASE_REFERENCE_REGEX_VAR);
		if (caseId == null || caseId.isBlank()) {
			throw new IllegalArgumentException("caseId must not be null or empty");
		}
		if (regexPattern == null || regexPattern.isBlank()) {
			throw new IllegalArgumentException("regexPattern must not be null or empty");
		}
		List<CaseModel> cases = Ivy.repo().search(CaseModel.class).textField("id").containsPhrase(caseId).execute()
				.getAll();
		String caseCode = CollectionUtils.isNotEmpty(cases) ? cases.getFirst().getCode() : null;
		if (StringUtils.isBlank(caseCode)) {
			return "";
		}

		// Remove regex special chars for building the literal output
		// Replace capture group (.+?) with actual case info
		String refPattern = regexPattern
				.replace("\\[", "[")
				.replace("\\]", "]")
				.replace("(.+?)", caseCode.toUpperCase());

		return refPattern;
	}

}
