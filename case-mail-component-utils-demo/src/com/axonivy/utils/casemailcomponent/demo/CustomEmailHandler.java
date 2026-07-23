package com.axonivy.utils.casemailcomponent.demo;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;

import com.axonivy.utils.casemailcomponent.demo.model.CaseModel;
import com.axonivy.utils.casemailcomponent.service.AbstractEmailHandler;

import ch.ivyteam.ivy.environment.Ivy;

public class CustomEmailHandler extends AbstractEmailHandler {
	private static final String CASE_REFERENCE_REGEX_VAR = "mailstoreUtils.caseReferenceRegex";

	public CustomEmailHandler(String storeName) {
		super(storeName);
	}

	@Override
	protected String getReferenceCaseId(String subject) {
		// use this to use the default handler
		// return super.getReferenceCaseId(subject);
		
		final Pattern pattern = Pattern.compile(Ivy.var().get(CASE_REFERENCE_REGEX_VAR), Pattern.CASE_INSENSITIVE);
		final Matcher matcher = pattern.matcher(subject);

		if (matcher.find() && matcher.group(1) != null) {
			List<CaseModel> cases = Ivy.repo().search(CaseModel.class).textField("code")
					.containsPhrase(matcher.group(1)).execute().getAll();
			return CollectionUtils.isNotEmpty(cases) ? cases.getFirst().getId() : null;
		}
		return null;
	}

}
