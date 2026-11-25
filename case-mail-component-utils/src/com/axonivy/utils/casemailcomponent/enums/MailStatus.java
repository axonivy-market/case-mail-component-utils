package com.axonivy.utils.casemailcomponent.enums;

import ch.ivyteam.ivy.environment.Ivy;

public enum MailStatus {
	OUTBOX("/Mail/Status/outbox"), 
	SENT("/Mail/Status/sent"), 
	SMTP_FAILED("/Mail/Status/failed"),
	
	// mail receive
	ASSIGNED("/Mail/Status/assigned"), 
	ASSIGN_INCMPLT("/Mail/Status/assignedIncomplete"),
	SKIPPED("/Mail/Status/skipped"), 
	REF_NOT_FOUND("/Mail/Status/refNotFound"),
	;

	private String cmsPath;

	private MailStatus(String cmsPath) {
		this.cmsPath = cmsPath;
	}

	public String getCms() {
		return Ivy.cms().co(cmsPath);
	}

	public String getCmsPath() {
		return cmsPath;
	}

	public void setCmsPath(String cmsPath) {
		this.cmsPath = cmsPath;
	}

}
