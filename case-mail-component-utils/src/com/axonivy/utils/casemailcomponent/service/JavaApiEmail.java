package com.axonivy.utils.casemailcomponent.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.casemailcomponent.Constants;
import com.axonivy.utils.casemailcomponent.businessData.Mail;
import com.axonivy.utils.casemailcomponent.enums.BpmErrorCode;
import com.axonivy.utils.casemailcomponent.utils.FileUtils;

import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.io.DataStream;
import ch.ivyteam.ivy.io.DataStreamProvider;
import ch.ivyteam.ivy.mail.MailAttachment;
import ch.ivyteam.ivy.mail.MailClient;
import ch.ivyteam.ivy.mail.MailMessage;

/**
 * Class accepts object of type {@link Mail}, creates {@link MailMessage} from
 * it using the ivy email client and sends it.<br />
 * Important feature is that it transforms base64 inline attachments to cid
 * reference attachments.<br />
 * Any problem is wrapped to {@link BpmError} and thrown to be distributed to
 * calling ivy process, e.g. SendMailAsync.p.json.
 *
 * @see BpmError
 */
public class JavaApiEmail {
	private final static String MAIL_CID_INLINE_IMAGES_MODE = "mailstoreUtils.cidInlineImagesMode";

	private MailMessage.Builder ivyMessageBuilder;

	private JavaApiEmail() {
		super();
	}

	/**
	 * Instantiation of the object using {@link Mail} object. <br />
	 * Creates {@link MailMessage.Builder} and initializes the basic email
	 * properties: <br />
	 * subject, recipient, cc, bcc, and sender<br />
	 * Created object is stored to property {@link #ivyMessageBuilder}.
	 *
	 * @param mail
	 * @return
	 */
	public static JavaApiEmail getInstance(Mail mail) {
		final JavaApiEmail jaMail = new JavaApiEmail();
		try {
			// mandatory email properties
			final MailMessage.Builder mBuilder = MailMessage.create().subject(mail.getSubject())
					.to(MailService.extractEmails(mail.getRecipient())).from(mail.getSender());

			// optional cc
			if (StringUtils.isNotBlank(mail.getRecipientCC())) {
				mBuilder.toMailMessage().cc().addAll(MailService.extractEmails(mail.getRecipientCC()));
			}

			jaMail.setIvyMessageBuilder(mBuilder);
		} catch (final Exception e) {
			BpmErrorService.get().throwBpmErrorSimplified(BpmErrorCode.ERROR_JAVA_API_MAIL_NOT_SENT, e);
		}
		return jaMail;
	}

	/**
	 * @return the local {@link #ivyMessageBuilder} initialized in
	 *         {@link #getInstance(Mail)}
	 */
	public MailMessage.Builder getIvyMessageBuilder() {
		return ivyMessageBuilder;
	}

	/**
	 * @param ivyMessageBuilder the ivyMessageBuilder to set
	 */
	public void setIvyMessageBuilder(MailMessage.Builder ivyMessageBuilder) {
		this.ivyMessageBuilder = ivyMessageBuilder;
	}

	/**
	 * @return true if variable {@link #MAIL_CID_INLINE_IMAGES_MODE} is set to true,
	 *         false otherwise
	 */
	public static boolean useJavaApiEmail() {
		return Boolean.parseBoolean(Ivy.var().get(MAIL_CID_INLINE_IMAGES_MODE));
	}

	/**
	 * Internal helper class that contains a processed html and the extracted
	 * attachments that are referenced from the html using cid.
	 */
	private static class HtmlAndImages {
		public final String html;
		public final List<MailAttachment> imageParts;
		public HtmlAndImages(String html, List<MailAttachment> imageParts) {
			this.html = html;
			this.imageParts = imageParts;
		}
	}

	/**
	 * Crucial method which transforms inline base64 attachments of passed html body
	 * to proper cid referenced attachments.<br />
	 * Result is stored to {@link #ivyMessageBuilder}.
	 *
	 * @uses extractImagesAndReplaceWithCid(String originalHtml)
	 * @param mailHtmlBody
	 * @throws BpmError
	 */
	public void prepareBody(String mailHtmlBody) throws BpmError {
		try {
			// extract images and turn into inline attachments
			final HtmlAndImages result = extractImagesAndReplaceWithCid(mailHtmlBody);

			// prepare builder that contains the inline attachments and the updated html
			// with proper <img src="cid:imageid"> tags
			getIvyMessageBuilder().htmlContent(result.html);
			getIvyMessageBuilder().attachments(result.imageParts);
		} catch (final Exception e) {
			BpmErrorService.get().throwBpmErrorSimplified(BpmErrorCode.ERROR_JAVA_API_MAIL_NOT_SENT, e);
		}
	}

	/**
	 * Extracts the base64 images from the provided html and transforms them into
	 * inline attachments.
	 *
	 * @param originalHtml
	 * @return wrapper that contains the inline attachments and the updated html
	 * @throws Exception
	 */
	private HtmlAndImages extractImagesAndReplaceWithCid(String originalHtml) throws Exception {
		final List<MailAttachment> imageParts = new ArrayList<>();
		final StringBuilder htmlBuilder = new StringBuilder(originalHtml);
		// pattern to find base64 images
		final Pattern pattern = Pattern.compile("data:image/(\\w+);base64,([A-Za-z0-9+/=]+)");
		final Matcher matcher = pattern.matcher(originalHtml);

		int imageIndex = 1;
		int offset = 0;

		// find and process all base64 images
		while (matcher.find()) {
			final String imageType = matcher.group(1);
			final String base64Data = matcher.group(2);
			final String cid = "image" + imageIndex;

			// Decode image
			final byte[] imageBytes = Base64.getDecoder().decode(base64Data);

			DataStreamProvider ds = () -> new DataStream() {
				public String name() { return "inline-" + cid; }
				public String contentType() { return "image/" + imageType; }
				public InputStream openStream() { return new ByteArrayInputStream(imageBytes); }
			};
			
			final MailAttachment imagePart = MailAttachment.create().dataStream(ds).fileName("inline" + cid).inline(cid)
					.toMailAttachment();
			imageParts.add(imagePart);

			// Replace base64 in hmtl with CID reference
			final int start = matcher.start() + offset;
			final int end = matcher.end() + offset;
			final String replacement = Constants.CID + cid;
			htmlBuilder.replace(start, end, replacement);
			offset += replacement.length() - (end - start);
			imageIndex++;
		}
		return new HtmlAndImages(htmlBuilder.toString(), imageParts);
	}

	/**
	 * Transforms all passed {@link Attachment} objects to email attachments. <br />
	 * Result is stored to {@link #ivyMessageBuilder}.
	 *
	 * @param files
	 * @throws BpmError
	 */
	public void prepareAttachments(List<com.axonivy.utils.casemailcomponent.businessData.Attachment> files)
			throws BpmError {
		try {
			MailAttachment properMailAtt;
			DataStreamProvider ds;

			for (final com.axonivy.utils.casemailcomponent.businessData.Attachment file : files) {
				String contentType = FileUtils.getContentType(file.getName());
				ds = () -> new DataStream() {
					public String name() { return file.getName(); }
					public String contentType() { return contentType; }
					public InputStream openStream() { return new ByteArrayInputStream(file.getContent()); }
				};
				properMailAtt = MailAttachment.create().dataStream(ds).fileName(file.getName())
						.inline(file.getContentId()).toMailAttachment();

				getIvyMessageBuilder().attachments(properMailAtt);
			}
		} catch (final Exception e) {
			BpmErrorService.get().throwBpmErrorSimplified(BpmErrorCode.ERROR_JAVA_API_MAIL_NOT_SENT, e);
		}
	}

	/**
	 * Sends the email in {@link #ivyMessageBuilder}
	 *
	 * @throws BpmError
	 */
	public void sendEmail() throws BpmError {
		try {
			MailClient.create().send(getIvyMessageBuilder().toMailMessage());
		} catch (final Exception e) {
			BpmErrorService.get().throwBpmErrorSimplified(BpmErrorCode.ERROR_JAVA_API_MAIL_NOT_SENT, e);
		}
	}
}
