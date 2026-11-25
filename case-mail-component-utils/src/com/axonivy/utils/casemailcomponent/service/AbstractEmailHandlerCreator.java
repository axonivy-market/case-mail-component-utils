package com.axonivy.utils.casemailcomponent.service;

public abstract class AbstractEmailHandlerCreator {

	/**
	 * Retrieve Mail
	 *
	 */
	public void retrieveEmails(String storeName) {
		final AbstractEmailHandler emailHandler = getEmailHandler(storeName);

		emailHandler.handleMail();
	}

	protected AbstractEmailHandler getEmailHandler(String storeName) {
		return new DefaultEmailHandler(storeName);
	}
}
