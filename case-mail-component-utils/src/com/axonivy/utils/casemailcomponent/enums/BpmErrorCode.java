package com.axonivy.utils.casemailcomponent.enums;

import java.util.Arrays;

import ch.ivyteam.ivy.environment.Ivy;

public enum BpmErrorCode {
	ERROR_MAIL_NOT_SENT("com:axonivy:utils:mail:mailNotSent","/Errors/mailNotSent"),
	DOC_VIEWER_FAILED_TO_LOAD_FILE("com:axonivy:utils:mail:documentViewer:failedToLoadFile","/Errors/failedToLoadFile"),
	DOC_VIEWER_FAILED_TO_CACHE_FILE("com:axonivy:utils:mail:documentViewer:failedToCacheFile","/Errors/failedToCacheFile"),
	DOC_VIEWER_FAILED_TO_CONVERT_TO_PDF("com:axonivy:utils:mail:documentViewer:failedToConvertToPdf","/Errors/failedToConvertToPdf"),
	RECEIVE_MAIL_PROCESSING_MESSAGE("com:axonivy:utils:mail:retrieveMail:processingMessage","/RetrieveMail/processingMessage"),
	RECEIVE_MAIL_CONTENT_ERROR("com:axonivy:utils:mail:retrieveMail:contentError","/RetrieveMail/contentError"),
	RECEIVE_MAIL_READING_MAIL_MESSAGE_ERROR("com:axonivy:utils:mail:retrieveMail:readingMailMessageError","/RetrieveMail/readingMailMessageError"),
	RECEIVE_MAIL_INCOMPLETE_MAIL_ERROR("com:axonivy:utils:mail:retrieveMail:incompleteMailError","/RetrieveMail/incompleteMailError"),
	RECEIVE_MAIL_UTILS_ERROR("com:axonivy:utils:mail:retrieveMail:utilsError","/RetrieveMail/utilsError"),
	IVY_ROLE_NOT_FOUND("com:axonivy:utils:mail:utils:ivyRoleNotFound", "/RetrieveMail/ivyRoleNotFound"),
	ERROR_JAVA_API_MAIL_NOT_SENT("at:psa:cmt:core:email:mailJavaApiNotSent", "/Errors/mailJavaApiNotSend"),
	;

	private final String code;
	private final String cmsPath;

	private BpmErrorCode(String code, String cmsPath) {
		this.cmsPath = cmsPath;
		this.code = code;
	}

	/**
	 * Return the message entry of the instance.
	 *
	 * @return
	 */
	public String getCmsMessage(Object... params) {
		return Ivy.cms().co(cmsPath, Arrays.asList(params));
	}

	public String getCmsPath() {
		return cmsPath;
	}

	public String getCode() {
		return code;
	}
}
