package com.axonivy.utils.casemailcomponent.model;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.casemailcomponent.businessData.Mail;

import ch.ivyteam.ivy.business.data.store.search.Filter;
import ch.ivyteam.ivy.business.data.store.search.Query;

public class MailLazyDataModel extends AbstractBusinessDataLazyDataModel<Mail> {

	private static final long serialVersionUID = -501730717404259085L;
	private String caseId = "";

	public MailLazyDataModel() {
		super(Mail.class);
	}

	public MailLazyDataModel(String caseId) {
		super(Mail.class);
		this.caseId = caseId;
	}

	@Override
	protected Filter<Mail> filter(Query<Mail> query) {
		if (StringUtils.isBlank(caseId)) {
			return null;
		}
		return query.textField("caseId").containsPhrase(caseId);
	}

	public String getCaseId() {
		return caseId;
	}

	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

}
