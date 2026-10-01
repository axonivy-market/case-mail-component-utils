package com.axonivy.connector.casemailcomponent.demo;

import java.io.Serializable;
import java.util.List;

import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.connector.casemailcomponent.demo.model.CaseModel;

import ch.ivyteam.ivy.environment.Ivy;

@Named
@ViewScoped
public class CaseBean implements Serializable {
	private static final String CASE_REFERENCE_REGEX_VAR = "mailstoreConnector.caseReferenceRegex";

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
