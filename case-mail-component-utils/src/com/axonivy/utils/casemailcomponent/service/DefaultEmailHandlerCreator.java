package com.axonivy.utils.casemailcomponent.service;

public class DefaultEmailHandlerCreator extends AbstractEmailHandlerCreator {

	@Override
	public AbstractEmailHandler getEmailHandler(String storeName) {
		return new DefaultEmailHandler(storeName);
	}

}
