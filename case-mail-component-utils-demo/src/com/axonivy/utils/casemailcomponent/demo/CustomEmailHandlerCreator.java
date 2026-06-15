package com.axonivy.utils.casemailcomponent.demo;

import com.axonivy.utils.casemailcomponent.service.AbstractEmailHandler;
import com.axonivy.utils.casemailcomponent.service.AbstractEmailHandlerCreator;

public class CustomEmailHandlerCreator extends AbstractEmailHandlerCreator {

	@Override
	protected AbstractEmailHandler getEmailHandler(String storeName) {
		return new CustomEmailHandler(storeName);
	}

}
