package com.axonivy.utils.casemailcomponent.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.casemailcomponent.businessData.Mail;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.ExecutionResult;
import ch.ivyteam.ivy.bpm.engine.client.History;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.Ivy;

@IvyProcessTest
public class SendMailProcessTest {

	private static final BpmProcess SEND_MAIL_PROCESS = BpmProcess.path("SendMail");
	private static final BpmProcess SEND_MAIL_ASYNC_PROCESS = BpmProcess.path("SendMailAsync");
	
	@AfterAll
	public static void cleanup() {
		Ivy.repo().deleteById("TEST");
	}

	@Test
	public void callProcess_SendMailError(BpmClient bpmClient) {
		ExecutionResult result = bpmClient.start().process(SEND_MAIL_ASYNC_PROCESS).execute();

		History history = result.history();
		assertThat(history.elementNames()).contains("SendMailException");
		assertThat(result.workflow().activeTask().name(Ivy.cms().co("/Tasks/name"))).isPresent();
	}

	@Test
	public void callProcess_SendMailSucess(BpmClient bpmClient) {
		Mail mail = new Mail();
		mail.setId("TEST");
		mail.setSender("test_sender@gmail.com");
		mail.setRecipient("test_recipient@gmail.com");
		mail.setSubject("test subject");
		mail.setBody("test body");

		ExecutionResult result = bpmClient.start().subProcess(SEND_MAIL_PROCESS).execute(mail);
		History history = result.history();
		assertThat(history.elementNames()).containsExactly("sendMail(Mail)", "call send mail async", "end");
	}

}
