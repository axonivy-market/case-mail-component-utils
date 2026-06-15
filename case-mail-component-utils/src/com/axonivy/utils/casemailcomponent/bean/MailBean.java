package com.axonivy.utils.casemailcomponent.bean;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.StringEscapeUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.file.UploadedFile;

import com.axonivy.utils.casemailcomponent.Constants;
import com.axonivy.utils.casemailcomponent.businessData.Attachment;
import com.axonivy.utils.casemailcomponent.businessData.Mail;
import com.axonivy.utils.casemailcomponent.enums.MailStatus;
import com.axonivy.utils.casemailcomponent.enums.ResponseAction;
import com.axonivy.utils.casemailcomponent.model.MailLazyDataModel;
import com.axonivy.utils.casemailcomponent.service.MailService;
import com.axonivy.utils.casemailcomponent.utils.DateUtil;
import com.axonivy.utils.casemailcomponent.utils.EmailContentUtil;
import com.axonivy.utils.casemailcomponent.utils.TextUtil;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.scripting.objects.DateTime;

@ManagedBean
@ViewScoped
public class MailBean {
	private Mail mail;
	private Mail selectedMail;
	private MailLazyDataModel mailModel;
	private MailService mailService;
	private String caseId;
	private String caseRef;
	private List<Attachment> attachments;
	private String allowFileTypes = Ivy.var().get("mailstoreUtils.allowFileTypes");
	private String maxUploadSize = Ivy.var().get("mailstoreUtils.maxUploadSize");
	private List<Attachment> inlineAttachments;

	private static final Map<String, String> MIME_TYPE_ICON_MAP = new HashMap<>();

	static {
		MIME_TYPE_ICON_MAP.put(Constants.MIME_PDF, Constants.ICON_PDF);
		MIME_TYPE_ICON_MAP.put(Constants.MIME_EXCEL_LEGACY, Constants.ICON_EXCEL);
		MIME_TYPE_ICON_MAP.put(Constants.MIME_EXCEL_OPENXML, Constants.ICON_EXCEL);
		MIME_TYPE_ICON_MAP.put(Constants.MIME_WORD_LEGACY, Constants.ICON_WORD);
		MIME_TYPE_ICON_MAP.put(Constants.MIME_WORD_OPENXML, Constants.ICON_WORD);
	}

	@PostConstruct
	public void init() {
		mailService = new MailService();
	}

	public void initMail() {
		mailModel = new MailLazyDataModel(caseId);
		mail = new Mail();
		mail.setCaseId(caseId);
		if (StringUtils.isNotBlank(caseRef)) {
			mail.setSubject(caseRef);
		}
		attachments = new ArrayList<Attachment>();
		inlineAttachments = new ArrayList<Attachment>();
	}

	public void handleCloseDialog() {
		initMail();
	}

	public void prepareMail(String actionType) throws IllegalAccessException, InvocationTargetException {
		ResponseAction type = ResponseAction.valueOf(actionType);
		mail.setCaseId(caseId);
		List<Attachment> allAttachments = mailService.copyMailAttachment(selectedMail.getId());
		categorizeAttachments(allAttachments);
		switch (type) {
		case RESEND:
			setMail(mailService.setUpResendMail(selectedMail));
			break;
		case FORWARD:
			setMail(mailService.setUpForwardMail(selectedMail));
			break;
		case REPLY:
			setMail(mailService.setUpReplyMail(selectedMail));
			attachments = new ArrayList<>();
			break;
		default:
			break;
		}
		getMailBodyWithEmbeddedImages(mail);
	}

	/**
	 * Check if mail is Sent
	 *
	 * @return boolean
	 */
	public boolean isSent() {
		return selectedMail.getStatus().equals(MailStatus.SENT);
	}

	public String getConfirmMessage() {
		return mailService.setUpMessageConfirm(selectedMail, ResponseAction.RESEND.toString());
	}

	/**
	 * Get mail body with embedded images
	 */
	public String getMailBodyWithEmbeddedImages(Mail mail) {
		replaceInlineImageWithBase64(mail);
		// Detect if it's HTML or plain text
		if (!EmailContentUtil.isHtml(mail.getBody())) {
			// Escape HTML and wrap in <pre> to preserve formatting
			mail.setBody("<pre>" + StringEscapeUtils.escapeHtml4(mail.getBody()) + "</pre>");
		}

		return mail.getBody();
	}

	private void replaceInlineImageWithBase64(Mail mail) {
		if (CollectionUtils.isEmpty(inlineAttachments) || !shouldReplaceCidWithBase64(mail.getBody())) {
			return;
		}
		if (CollectionUtils.isNotEmpty(inlineAttachments)) {
			for (final Attachment file : inlineAttachments) {
				final String content = Base64.getEncoder().encodeToString(file.getContent());
				final StringBuilder base64Content = new StringBuilder().append("data:image/")
						.append(file.getDefaultExtension()).append(";base64,").append(content);
				mail.setBody(mail.getBody().replace(Constants.CID + file.getContentId(), base64Content));
			}
		}
	}

	public static boolean shouldReplaceCidWithBase64(String htmlContent) {
		if (htmlContent == null || htmlContent.isEmpty()) {
			return false;
		}

		return htmlContent.contains(Constants.CID);
	}

	/**
	 * Gets the maximum size allowed for uploading files in bytes
	 *
	 * @return
	 */
	public Integer getMaxUploadSizeInBytes() {
		return getMaxUploadSizeInMB() * 1024 * 1024;
	}

	/**
	 * Gets the maximum size allowed for uploading files in megabytes.
	 * 
	 * @return
	 */
	public Integer getMaxUploadSizeInMB() {
		if (StringUtils.isBlank(maxUploadSize)) {
			return Integer.valueOf(10);
		}
		return Integer.valueOf(maxUploadSize);
	}

	/**
	 * Gets the allowed file types.
	 *
	 * @return
	 */
	public String getAllowedFileTypes() {
		if (StringUtils.isBlank(allowFileTypes)) {
			return "";
		}
		return java.util.Arrays.stream(allowFileTypes.split(",")).map(String::trim).filter(s -> !s.isEmpty())
				.collect(java.util.stream.Collectors.joining("|"));
	}

	/**
	 * Uploads a file and attaches it to the mail
	 *
	 * @param event {@link FileUploadEvent}
	 */
	public void handleFileUpload(FileUploadEvent event) throws IOException {
		final UploadedFile uploadedFile = event.getFile();
		final Attachment attachment = new Attachment();
		attachment.setName(uploadedFile.getFileName());
		attachment.setContent(uploadedFile.getContent());
		attachment.setSize(uploadedFile.getSize());
		attachment.setContentType(uploadedFile.getContentType());
		attachment.setDefaultExtension(
				StringUtils.upperCase(StringUtils.substringAfterLast(uploadedFile.getFileName(), ".")));
		attachment.setInlineAttachment(false);
		if (CollectionUtils.isEmpty(attachments)) {
			attachments = new java.util.ArrayList<Attachment>();
		}
		attachments.add(attachment);
	}

	public void removeFile(Attachment attachment) {
		attachments.remove(attachment);
	}

	public String formatDate(DateTime dateTime) {
		return DateUtil.format(dateTime);
	}

	public static String getAttachmentIcon(String contentType) {
		return MIME_TYPE_ICON_MAP.getOrDefault(contentType, Constants.ICON_DEFAULT);
	}

	/**
	 * Show a file from attachment on DocumentViewer dialog
	 * 
	 * @param file to be for show
	 */
	public void viewDocument(Attachment file) {
		Map<String, Object> viewMap = FacesContext.getCurrentInstance().getViewRoot().getViewMap();
		DocumentViewerBean documentViewerBean = (DocumentViewerBean) viewMap.get("documentViewerBean");
		documentViewerBean.showFile(file);
	}

	public String textEllipsis(String text, int maxLength) {
		return TextUtil.ellipsis(text, maxLength);
	}

	public void handleSelectMail(SelectEvent<Mail> event) {
		selectedMail = event.getObject();
		if (selectedMail == null) {
			attachments = Collections.emptyList();
			inlineAttachments = Collections.emptyList();
			return;
		}

		List<Attachment> allAttachments = MailService.findMailAttachments(selectedMail.getId());
		categorizeAttachments(allAttachments);
	}

	private void categorizeAttachments(List<Attachment> allAttachments) {
		attachments = new ArrayList<>();
		inlineAttachments = new ArrayList<>();
		for (Attachment attachment : allAttachments) {
			boolean isInline = Boolean.TRUE.equals(attachment.getInlineAttachment());
			boolean isEml = Strings.CI.equals(attachment.getDefaultExtension(), Constants.EML_EXTENTION);

			if (isInline) {
				inlineAttachments.add(attachment);
			} else if (!isEml) {
				attachments.add(attachment);
			}
		}
	}

	public Mail getMail() {
		return mail;
	}

	public void setMail(Mail mail) {
		this.mail = mail;
	}

	public MailLazyDataModel getMailModel() {
		return mailModel;
	}

	public void setMailModel(MailLazyDataModel mailModel) {
		this.mailModel = mailModel;
	}

	public Mail getSelectedMail() {
		return selectedMail;
	}

	public void setSelectedMail(Mail selectedMail) {
		this.selectedMail = selectedMail;
	}

	public String getCaseId() {
		return caseId;
	}

	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	public String getAllowFileTypes() {
		return allowFileTypes;
	}

	public void setAllowFileTypes(String allowFileTypes) {
		this.allowFileTypes = allowFileTypes;
	}

	public String getMaxUploadSize() {
		return maxUploadSize;
	}

	public void setMaxUploadSize(String maxUploadSize) {
		this.maxUploadSize = maxUploadSize;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public String getCaseRef() {
		return caseRef;
	}

	public void setCaseRef(String caseRef) {
		this.caseRef = caseRef;
	}

}
