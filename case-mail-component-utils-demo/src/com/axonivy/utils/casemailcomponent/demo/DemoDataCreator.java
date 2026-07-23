package com.axonivy.utils.casemailcomponent.demo;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.axonivy.utils.casemailcomponent.demo.model.CaseModel;

import ch.ivyteam.ivy.environment.Ivy;

public class DemoDataCreator {

	public static List<CaseModel> createDemoData() {
		List<CaseModel> caseList = Ivy.repo().search(CaseModel.class).execute().getAll();
		if (CollectionUtils.isEmpty(caseList)) {
			caseList = new ArrayList<>();
			caseList.add(createDemoCase("CASE_01", "Case 1", "Test Case 1", "C-2025-01"));
			caseList.add(createDemoCase("CASE_02", "Case 2", "Test Case 2", "C-2025-02"));
			caseList.add(createDemoCase("CASE_03", "Case 3", "Test Case 3", "C-2025-03"));
			caseList.add(createDemoCase("CASE_04", "Case 4", "Test Case 4", "C-2025-04"));
			caseList.add(createDemoCase("CASE_05", "Case 5", "Test Case 5", "C-2025-05"));
		}
		return caseList;
	}

	private static CaseModel createDemoCase(String caseId, String name, String desc, String code) {
		CaseModel caseModel = new CaseModel();
		caseModel.setId(caseId);
		caseModel.setName(name);
		caseModel.setDescription(desc);
		caseModel.setCode(code);
		Ivy.repo().save(caseModel);
		return caseModel;
	}
}
