package com.axonivy.utils.casemailcomponent.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.casemailcomponent.enums.BpmErrorCode;

import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.bpm.error.BpmPublicErrorBuilder;

/**
 * Service to create and throw BpmErrors based on given parameters
 *
 * @author ny.huynh
 */
public class BpmErrorService {
	private static final BpmErrorService INSTANCE = new BpmErrorService();

	private BpmErrorService() {
	} // locked constructor

	/**
	 * gets the singleton instance of this Service
	 *
	 * @return
	 */
	public static BpmErrorService get() {
		return INSTANCE;
	}

	/**
	 * throws BpmError based on bpmErrorCode and message
	 *
	 * @param bpmErrorCode
	 * @param message      message to show
	 */
	public void throwBpmError(BpmErrorCode bpmErrorCode, String message) {
		throwBpmError(bpmErrorCode, message, null);
	}

	/**
	 * throws BpmError based on bpmErrorCode and cause
	 *
	 * @param bpmErrorCode
	 * @param cause        Exception to show
	 */
	public void throwBpmError(BpmErrorCode bpmErrorCode, Exception cause) {
		throwBpmError(bpmErrorCode, null, cause);
	}

	/**
	 * throws BpmError based on bpmErrorCode, message, cause and parameters for
	 * message
	 *
	 * @param bpmErrorCode
	 * @param message      message to show
	 * @param cause        Exception to show
	 * @param params       attributes for Message param, must be in correct order
	 */
	public void throwBpmError(BpmErrorCode bpmErrorCode, String message, Exception cause, Object... params) {
		getBpmErrorBuilder(bpmErrorCode, message, cause, params).throwError();
	}

	/**
	 * Calls {@link #throwBpmError(BpmErrorCode, String, Exception, Object...)} using bpmErrorCode.getCmsMessage() for the second parameter
	 *
	 * @param bpmErrorCode
	 * @param cause        Exception to show
	 * @param params       attributes for Message param, must be in correct order
	 */
	public void throwBpmErrorSimplified(BpmErrorCode bpmErrorCode, Exception cause, Object... params) {
		getBpmErrorBuilder(bpmErrorCode, bpmErrorCode.getCmsMessage(), cause, params).throwError();
	}

	/**
	 * Calls {@link #throwBpmErrorSimplified(BpmErrorCode, Exception, Object...)} passing no additional info parameters
	 *
	 * @param bpmErrorCode
	 * @param cause        Exception to show
	 */
	public void throwBpmErrorSimplified(BpmErrorCode bpmErrorCode, Exception cause) {
		getBpmErrorBuilder(bpmErrorCode, bpmErrorCode.getCmsMessage(), cause, new Object[0]).throwError();
	}

	/**
	 * creates BpmError based on bpmErrorCode and message
	 *
	 * @param bpmErrorCode
	 * @param message      message to show
	 */
	public BpmError buildBpmError(BpmErrorCode bpmErrorCode, String message) {
		return buildBpmError(bpmErrorCode, message, null);
	}

	/**
	 * creates BpmError based on bpmErrorCode and cause
	 *
	 * @param bpmErrorCode
	 * @param cause        Exception to show
	 */
	public BpmError buildBpmError(BpmErrorCode bpmErrorCode, Exception cause) {
		return buildBpmError(bpmErrorCode, null, cause);
	}

	/**
	 * creates BpmError based on bpmErrorCode, message, cause and parameters for
	 * message
	 *
	 * @param bpmErrorCode
	 * @param message      message to show
	 * @param cause        Exception to show
	 * @param params       attributes for Message param, must be in correct order
	 */
	public BpmError buildBpmError(BpmErrorCode bpmErrorCode, String message, Exception cause, Object... params) {
		return getBpmErrorBuilder(bpmErrorCode, message, cause, params).build();
	}

	/**
	 * builds BpmPublicErrorBuilder based on given params
	 *
	 * @param bpmErrorCode
	 * @param message      message to show
	 * @param cause        Exception to show
	 * @param params       attributes for Message param, must be in correct order
	 */
	private BpmPublicErrorBuilder getBpmErrorBuilder(BpmErrorCode bpmErrorCode, String message, Exception cause,
			Object[] params) {
		final BpmPublicErrorBuilder builder = new BpmPublicErrorBuilder();
		if (Objects.nonNull(bpmErrorCode)) {
			builder.withErrorCode(bpmErrorCode.getCode());
		}
		if (StringUtils.isNotEmpty(message)) {
			builder.withMessage(message);
		}
		if (Objects.nonNull(cause)) {
			builder.withCause(cause);
		}
		if (Objects.nonNull(params)) {
			final Map<String, Object> paramsMap = createParamsMap(params);
			builder.withAttributes(paramsMap);
		}
		return builder;
	}

	private Map<String, Object> createParamsMap(Object[] params) {
		final Map<String, Object> paramsMap = new HashMap<>();
		for (int i = 0; i < params.length; i++) {
			if (params[i] == null) {
				paramsMap.put(String.valueOf(i), "");
			} else {
				paramsMap.put(String.valueOf(i), params[i].toString());
			}
		}
		return paramsMap;
	}
}

