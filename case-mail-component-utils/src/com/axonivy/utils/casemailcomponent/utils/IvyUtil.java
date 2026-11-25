package com.axonivy.utils.casemailcomponent.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IvyUtil {

	/**
	 * Converts parameter to (List<Object>) to be passed as second parameter to
	 * method Ivy.cms().co(cmsUri, parameters);
	 *
	 * @param pars
	 * @return
	 */
	public static List<Object> getCmsPars(String... pars) {
		final List<Object> result = new ArrayList<>();
		if (pars != null) {
			result.addAll(Arrays.asList(pars));
		}
		return result;
	}
}
