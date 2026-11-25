package com.axonivy.utils.casemailcomponent.enums;

public enum ResponseAction {
	REPLY, 
	RESEND, 
	FORWARD;

	public static boolean isReplyForward(ResponseAction mailActionType) {
		return FORWARD.equals(mailActionType) || REPLY.equals(mailActionType);
	}

	public static boolean isReplyResendForward(ResponseAction mailActionType) {
		return FORWARD.equals(mailActionType) || REPLY.equals(mailActionType) || RESEND.equals(mailActionType);
	}
}
