package com.axonivy.utils.casemailcomponent.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.mail.Address;
import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeUtility;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.mailstore.MailStoreService;
import com.axonivy.utils.mailstore.MailStoreService.MessageIterator;
import com.axonivy.utils.casemailcomponent.Constants;
import com.axonivy.utils.casemailcomponent.businessData.Attachment;
import com.axonivy.utils.casemailcomponent.businessData.Mail;
import com.axonivy.utils.casemailcomponent.enums.BpmErrorCode;
import com.axonivy.utils.casemailcomponent.enums.MailStatus;
import com.axonivy.utils.casemailcomponent.enums.NotificationTaskType;
import com.axonivy.utils.casemailcomponent.model.CustomTaskDefinition;
import com.axonivy.utils.casemailcomponent.utils.DateUtil;
import com.axonivy.utils.casemailcomponent.utils.EmailContentUtil;
import com.axonivy.utils.casemailcomponent.utils.IvyUtil;
import com.axonivy.utils.casemailcomponent.utils.MessageComparator;
import com.axonivy.utils.mailstore.MessageService;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.scripting.objects.DateTime;
import ch.ivyteam.ivy.workflow.signal.IBpmSignalService;

public abstract class AbstractEmailHandler {
	private static final String INBOX = "INBOX";
	private static final String SUBJECT_MATCHES_VAR = "mailstoreUtils.subjectMatches";
	private static final String RECEIVED_MAIL_TASK_ROLE_VAR = "mailstoreUtils.retrieveMailTaskRole";
	private static final String CASE_REFERENCE_REGEX_VAR = "mailstoreUtils.caseReferenceRegex";
	private static final String MAIL_STORE_VAR = "mailstoreUtils";
	private static final String PROCESSED_FOLDER = Ivy.var().get("mailstoreUtils.processedFolderName");
	private static final String ERROR_FOLDER = Ivy.var().get("mailstoreUtils.errorFolderName");
	private static final String HANDLE_UNCLEAR_EMAILS_SIGNAL_CODE = "email:handleUnclear";

	protected Mail mail;
	protected Message message;
	private boolean isHandleMessageFail;
	private boolean isIncompleteMail;
	private boolean isUnclearMail;
	protected List<Attachment> files;
	private Attachment originalMailFile;
	private String storeName;

	private final String mailBoxServer = Ivy.var().get(String.format("%s.%s.%s", MAIL_STORE_VAR, storeName, "host"));

	public AbstractEmailHandler(String storeName) {
		this.setStoreName(storeName);
	}

	/**
	 * Handle mail message from IMAP
	 *
	 * @return
	 */
	public void handleMail() {
		boolean storeConnected = false;

		try (MessageIterator iterator = getMessageIterator(PROCESSED_FOLDER)) {
			storeConnected = true;

			while (iterator.hasNext()) {
				message = iterator.next();

				this.isIncompleteMail = false;
				extractMailFromMessage();
				extractAttachmentsFromMessage();
				createOriginalMailFile();
				processMail();
				if (isHandleMessageFail || isIncompleteMail || isUnclearMail) {
					setFlag();
					continue;
				}
				Ivy.log().debug("Mail with subject {0} was moved to {1}", getMessage().getSubject(), PROCESSED_FOLDER);
				moveEmailAfterProcessing(iterator);
			}
		} catch (final MessagingException | IOException ex) {
			setFlag();
			Ivy.log().error(BpmErrorCode.RECEIVE_MAIL_READING_MAIL_MESSAGE_ERROR.getCmsMessage(mailBoxServer, storeName,
					this.mail.getSubject(), this.mail.getSender()), ex);
		} catch (final Exception ex) {
			setFlag();
			Ivy.log().error(BpmErrorCode.RECEIVE_MAIL_UTILS_ERROR.getCmsMessage(mailBoxServer, ex.getMessage()), ex);
		} finally {
			if (storeConnected) {
				moveMailError();
			}
		}
	}

	protected void extractMailFromMessage() throws MessagingException, IOException {
		final Instant mailReceivedDT = DateUtil.convertDateToInstant(message.getReceivedDate());
		final String mailReceivedDTString = DateUtil.formatInstantToMediumStyleWithTime(mailReceivedDT,
				Ivy.session().getFormattingLocale());
		Ivy.log().info(formatWithEmailReadableMetadata("Extracting mail from javax.mail.message", mailBoxServer,
				message.getSubject(), mailReceivedDTString));
		final Mail newMail = new Mail();
		final Address[] froms = message.getFrom();
		newMail.setSender(froms == null ? null : ((InternetAddress) froms[0]).getAddress());
		newMail.setRecipient(getRecipientByType(message, Message.RecipientType.TO));
		newMail.setSubject(message.getSubject());
		newMail.setBody(EmailContentUtil.getAllTextBySubtype(message, Constants.TEXT_HTML, StringUtils.EMPTY, false));
		if (StringUtils.isBlank(newMail.getBody())) {
			newMail.setBody(MessageService.getAllPlainTexts(message, Constants.COMMA, false));
		}
		newMail.setRecipientCC(getRecipientByType(message, Message.RecipientType.CC));
		newMail.setRecipientBCC(getRecipientByType(message, Message.RecipientType.BCC));
		newMail.setReceivedDateTime(new DateTime(message.getReceivedDate()));
		this.mail = newMail;
	}

	/**
	 * Enriches given info message with email and mailbox traceable meta
	 * information. Formatted string is mentioned to be printed in engine log.
	 * <br />
	 * <br />
	 * Uses {@link BpmErrorCode#RECEIVE_MAIL_PROCESSING_MESSAGE}
	 *
	 * @param info
	 * @param mailBoxName
	 * @param mailSubject
	 * @param mailReceivedDTString
	 * @return
	 */
	private String formatWithEmailReadableMetadata(String info, String mailBoxName, String mailSubject,
			String mailReceivedDTString) {
		return com.axonivy.utils.casemailcomponent.enums.BpmErrorCode.RECEIVE_MAIL_PROCESSING_MESSAGE.getCmsMessage(info,
				mailBoxName, mailSubject, mailReceivedDTString);
	}

	private String getRecipientByType(Message message, Message.RecipientType recipientType)
			throws MessagingException, IOException {
		final Optional<Address[]> recipientOptional = Optional.ofNullable(message.getRecipients(recipientType));
		final List<String> recipientString = recipientOptional.map(Arrays::stream).orElse(Arrays.stream(new Address[0]))
				.map(a -> ((InternetAddress) a).getAddress()).collect(Collectors.toList());

		return StringUtils.join(recipientString, Constants.COMMA);
	}

	protected void extractAttachmentsFromMessage() {
		final List<Part> parts = MessageService.getAllParts(message, false, null);

		files = new ArrayList<>();

		for (final Part part : parts) {
			try {
				Ivy.log().debug("Part Disposition: {0}", part.getDisposition());
				if (!(Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())
						|| Part.INLINE.equalsIgnoreCase(part.getDisposition()))) {
					continue;
				}
				final String fileName = part.getFileName() == null ? "" : MimeUtility.decodeText(part.getFileName());
				if (StringUtils.isNotBlank(fileName)) {
					final String contentType = part.getContentType();
					if (contentType == null || part.getContent() == null) {
						this.isIncompleteMail = true;
						Ivy.log().error(createIncompleteErrorLog("Attachment has no content-type or no content",
								fileName, contentType, null));
						continue;
					}

					final Attachment file = new Attachment();
					file.setName(fileName);
					file.setContent(IOUtils.toByteArray(part.getInputStream()));
					if (Part.INLINE.equalsIgnoreCase(part.getDisposition())) {
						file.setInlineAttachment(true);
						file.setContentId(StringUtils.replaceChars(((MimeBodyPart) part).getContentID(), "<>", null));
					} else {
						file.setInlineAttachment(false);
					}

					final String fileExtension = getExtension(part);
					file.setDefaultExtension(fileExtension);
					files.add(file);
				}
			} catch (final Exception ex) {
				this.isIncompleteMail = true;
				Ivy.log().error(createIncompleteErrorLog(ex.getMessage(), null, null, ex));
			}
		}
	}
	
	private String getExtension(Part part) {
		try {
			// From filename if available
			final String fileName = part.getFileName() == null ? "" : MimeUtility.decodeText(part.getFileName());
			if (StringUtils.isNotBlank(fileName) && fileName.contains(".")) {
				return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
			}

			// From Content-Type
			final String contentType = part.getContentType();
			if (contentType != null) {
				final ContentType ct = new ContentType(contentType);
				final String baseType = ct.getBaseType();

				switch (baseType.toLowerCase()) {
				case "image/jpeg":
					return "jpg";
				case "image/pjpeg":
					return "jpg";
				case "image/png":
					return "png";
				case "image/gif":
					return "gif";
				case "image/bmp":
					return "bmp";
				case "image/tiff":
					return "tif";
				case "application/pdf":
					return "pdf";
				case "text/plain":
					return "txt";
				case "text/html":
					return "html";
				}

				// If Content-Type has "name="
				final String paramName = ct.getParameter("name");
				if (StringUtils.isNotBlank(paramName) && paramName.contains(".")) {
					return paramName.substring(paramName.lastIndexOf('.') + 1).toLowerCase();
				}
			}
		} catch (final Exception e) {
			Ivy.log().warn("getExtension error: {0}", e.getMessage());
		}

		return "";
	}

	private String createIncompleteErrorLog(String message, String attachmentFileName, String attachmentContentType,
			Exception errorCause) {
		return BpmErrorCode.RECEIVE_MAIL_INCOMPLETE_MAIL_ERROR.getCmsMessage(message, this.mail.getSubject(),
				this.mail.getSender(), attachmentFileName, attachmentContentType, ERROR_FOLDER,
				errorCause == null ? new Exception() : errorCause);
	}

	private void createOriginalMailFile() throws MessagingException, IOException {
		final String dotEmlExt = Constants.DOT + Constants.EML_EXTENTION;
		final int filenameWithoutDotExtLength = Constants.DEFAULT_SMALL_STRING_LENGTH - dotEmlExt.length();
		final StringBuilder filename = new StringBuilder()
				.append(StringUtils.substring(message.getSubject(), 0, filenameWithoutDotExtLength)).append(dotEmlExt);

		originalMailFile = new Attachment();
		originalMailFile.setName(filename.toString());
		originalMailFile.setContent(MailStoreService.saveMessage(message).readAllBytes());
		originalMailFile.setInlineAttachment(false);
		originalMailFile.setDefaultExtension(Constants.EML_EXTENTION);
		files.add(originalMailFile);
	}

	/**
	 * Mechanism to create relation of the email with case. Decision is based on
	 * subject of the message, searching for Reference pattern<br />
	 * Result is assigning the email to an existing case even with the status
	 * change, assigning to a new case etc
	 *
	 */
	protected void processMail() {
		final String caseReference = getReferenceCaseId(mail.getSubject());
		final Instant mailReceivedDT = DateUtil.convertDateToInstant(mail.getReceivedDateTime().toJavaDate());
		final String receivedDateTimeString = DateUtil.formatInstantToMediumStyleWithTime(mailReceivedDT,
				Ivy.session().getFormattingLocale());
		final List<Object> parameters = IvyUtil.getCmsPars(mail.getSender(), mail.getSubject(), receivedDateTimeString,
				caseReference);
		if (StringUtils.isNotBlank(caseReference)) {
			mail.setCaseId(caseReference);
			createNotificationTask(NotificationTaskType.RETRIEVE_EMAIL_REFERENCE_FOUND, parameters,
					Ivy.var().get(RECEIVED_MAIL_TASK_ROLE_VAR));
			processValidReference();
		} else {
			createNotificationTask(NotificationTaskType.RETRIEVE_EMAIL_NO_REFERENCE_FOUND, parameters,
					Ivy.var().get(RECEIVED_MAIL_TASK_ROLE_VAR));
			processInValidReference();
		}
	};

	protected String getReferenceCaseId(String subject) {
		final Pattern pattern = Pattern.compile(Ivy.var().get(CASE_REFERENCE_REGEX_VAR), Pattern.CASE_INSENSITIVE);
		final Matcher matcher = pattern.matcher(subject);

		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	};

	/**
	 * Creates a ivy task to notify user to handle received email. Task is directly
	 * started with {@link IBpmSignalService} in process
	 * /case-mail-component-utils/processes/RetrieveMails
	 *
	 * @see {@link NotificationTaskType} and its CMS entries with task details
	 *      '/RetrieveMail/NotificationTaskType'
	 *
	 *
	 * @param type
	 * @param parameters
	 * @param assigneeRoleName
	 */
	protected void createNotificationTask(NotificationTaskType type, List<Object> parameters, String assigneeRoleName) {
		final CustomTaskDefinition td = new CustomTaskDefinition();
		// name
		final String taskName = Ivy.cms().co("/RetrieveMail/NotificationTaskType/" + type.toString() + "/name",
				parameters);
		td.setName(taskName);

		// assignee role
		if (Ivy.security().roles().find(assigneeRoleName) != null) {
			td.setActivatorRole(assigneeRoleName);
		} else {
			final String roleNotFoundMessage = BpmErrorCode.IVY_ROLE_NOT_FOUND.getCmsMessage(assigneeRoleName);
			Ivy.log().warn(roleNotFoundMessage);
			return;
		}

		// category
		final String category = Ivy.cms().co("/RetrieveMail/NotificationTaskType/" + type.toString() + "/category");
		td.setCategory(category);

		// description
		final String description = Ivy.cms()
				.co("/RetrieveMail/NotificationTaskType/" + type.toString() + "/description", parameters);
		td.setDescription(description);

		// create the ivy task eventually
		Ivy.wf().signals().create().data(td.toJson()).send(HANDLE_UNCLEAR_EMAILS_SIGNAL_CODE);
	}

	protected void processValidReference() {
		Ivy.log().info("Reference of Email is valid");
		if (isIncompleteMail) {
			mail.setStatus(MailStatus.ASSIGN_INCMPLT);
		} else {
			mail.setStatus(MailStatus.ASSIGNED);
		}
		MailService.saveMail(mail, files);
		setUnclearMail(false);
		this.mail = null;
		this.files = null;
	}

	protected void processInValidReference() {
		Ivy.log().info("Reference of Email is invalid");
		mail.setStatus(MailStatus.SKIPPED);
		MailService.saveMail(mail, files);
		setUnclearMail(true);
	}

	private void setFlag() {
		try {
			if (this.message != null) {
				this.message.setFlag(Flags.Flag.FLAGGED, true);
			}
		} catch (final Exception e) {
			Ivy.log().error(BpmErrorCode.RECEIVE_MAIL_UTILS_ERROR.getCmsMessage(mailBoxServer, e.getMessage()), e);
		}
	}

	private void unFlag() {
		try {
			this.message.setFlag(Flags.Flag.FLAGGED, false);
		} catch (final Exception e) {
			Ivy.log().error(BpmErrorCode.RECEIVE_MAIL_UTILS_ERROR.getCmsMessage(mailBoxServer, e.getMessage()), e);
		}
	}

	private void moveEmailAfterProcessing(MessageIterator iterator) {
		iterator.handledMessage(true);
	}

	private void moveMailError() {
		try {
			final MessageIterator iterator = getMessageIterator(ERROR_FOLDER);
			while (iterator.hasNext()) {
				this.message = iterator.next();
				if (this.message.isSet(Flags.Flag.FLAGGED)) {
					unFlag();
					moveEmailAfterProcessing(iterator);
					Ivy.log().debug("Mail with subject {0} was moved to {1}", message.getSubject(), ERROR_FOLDER);
				}
			}
		} catch (final Exception ex) {
			Ivy.log().error(BpmErrorCode.RECEIVE_MAIL_UTILS_ERROR.getCmsMessage(mailBoxServer, ex.getMessage()), ex);
		}
	}

	protected MessageIterator getMessageIterator(String dstFolderName) {
		final String subjectMatches = Ivy.var().get(SUBJECT_MATCHES_VAR);

		// basic auth
		return MailStoreService.messageIterator(storeName, INBOX, dstFolderName, true,
				MailStoreService.subjectMatches(subjectMatches), new MessageComparator());
	}

	public String getMailBoxName() {
		return mailBoxServer;
	}

	public Mail getMail() {
		return mail;
	}

	public void setMail(Mail mail) {
		this.mail = mail;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public boolean isHandleMessageFail() {
		return isHandleMessageFail;
	}

	public void setHandleMessageFail(boolean isHandleMessageFail) {
		this.isHandleMessageFail = isHandleMessageFail;
	}

	public boolean isIncompleteMail() {
		return isIncompleteMail;
	}

	public void setIncompleteMail(boolean isIncompleteMail) {
		this.isIncompleteMail = isIncompleteMail;
	}

	public boolean isUnclearMail() {
		return isUnclearMail;
	}

	public void setUnclearMail(boolean isUnclearMail) {
		this.isUnclearMail = isUnclearMail;
	}

	public List<Attachment> getFiles() {
		return files;
	}

	public void setFiles(List<Attachment> files) {
		this.files = files;
	}

	public Attachment getOriginalMailFile() {
		return originalMailFile;
	}

	public void setOriginalMailFile(Attachment originalMailFile) {
		this.originalMailFile = originalMailFile;
	}

	public String getStoreName() {
		return storeName;
	}

	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}

}
