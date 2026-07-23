package com.axonivy.utils.casemailcomponent.bean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.axonivy.utils.casemailcomponent.businessData.Attachment;
import com.axonivy.utils.casemailcomponent.enums.BpmErrorCode;
import com.axonivy.utils.casemailcomponent.service.DocumentService;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.htmldialog.IHtmlDialogContext;
import ch.ivyteam.ivy.scripting.objects.Binary;

/**
 * Managed bean for DocumentViewer.xhtml component. Files are "cached" as
 * temporary {@link ch.ivyteam.ivy.scripting.objects.File}s
 * 
 * @author ny.huynh
 *
 */
@ManagedBean
@ViewScoped
public class DocumentViewerBean {
	private static final String[] ALLOWED_TYPES = Stream.of(new String[][] 
			{ 
				DocumentService.ASPOSE_IMAGE_FILE_TYPES,
				DocumentService.ASPOSE_EXCEL_FILE_TYPES, 
				DocumentService.ASPOSE_WORD_FILE_TYPES,
				DocumentService.ASPOSE_POWERPOINT_FILE_TYPES, 
				DocumentService.ASPOSE_PDF_FILE_TYPES 
			}).flatMap(Stream::of)
			.toArray(String[]::new);;
			
	private static final String[] ALLOWED_IMAGE_FILE_TYPES = DocumentService.ASPOSE_IMAGE_FILE_TYPES;

	private static final String DOT = ".";
	private static final String PDF_EXTENSION = "pdf";

	private String currentFileId;

	private ch.ivyteam.ivy.scripting.objects.File currentFile;
	private boolean isFileImage;
	private boolean previewNotAvailable;

	/**
	 * If provided file is of the allowed type it initializes the file to be shown.
	 * Document files are converted to PDF before initialized to be show. All files
	 * are "cached" as temporary {@link ch.ivyteam.ivy.scripting.objects.File}s to
	 * the session to avoid repeated conversion of the same file.
	 * 
	 * @param file file to be show
	 */
	public void showFile(Attachment file) {
		if (file != null && file.getDefaultExtension() != null
				&& Strings.CI.equalsAny(file.getDefaultExtension(), ALLOWED_TYPES)) {
			String sessionDirectory = "";

			final byte[] fileContent = file.getContent();
			if (fileContent == null) {
				this.currentFile = null;
				return;
			}

			final String extension = file.getDefaultExtension();
			this.isFileImage = Strings.CI.equalsAny(extension, ALLOWED_IMAGE_FILE_TYPES);
			this.currentFile = handleFile(file, sessionDirectory);
			if (this.currentFile != null) {
				this.currentFileId = file.getId();
			}
			setPreviewNotAvailable(false);
		} else {
			this.currentFile = null;
			if (file != null) {
				this.currentFileId = file.getId();
				setPreviewNotAvailable(true);
			} else {
				setPreviewNotAvailable(false);
			}
		}
	}

	private ch.ivyteam.ivy.scripting.objects.File handleFile(Attachment file, String sessionDirectory) {

		final String extension = file.getDefaultExtension();

		// check if conversion to PDF is needed
		final boolean needsConversion = Strings.CI.equalsAny(extension,
				DocumentService.ASPOSE_MS_DOCUMENT_TYPES);

		// change extension to "pdf" if conversion is needed
		final String fileName = needsConversion ? changeExtensionToPdf(file.getName()) : file.getName();

		final String filePath = StringUtils.isNotBlank(sessionDirectory) ? sessionDirectory + "/" + fileName : fileName;

		ch.ivyteam.ivy.scripting.objects.File ivyFile;
		try {
			ivyFile = new ch.ivyteam.ivy.scripting.objects.File(filePath, true);
		} catch (final IOException e) {
			Ivy.log().error(BpmErrorCode.DOC_VIEWER_FAILED_TO_LOAD_FILE.getCmsMessage(), e);
			return null;
		}

		final Instant fileModifiedInstant = Instant.now();
		// truncate to milliseconds because file from DB has higher precision than
		// File.lastModified()
		final FileTime fileModified = FileTime.from(fileModifiedInstant.truncatedTo(ChronoUnit.MILLIS));

		if (ivyFile.exists() && ivyFile.lastModified() != null) {
			final Instant chachedModifiedInstant = Instant.ofEpochMilli(ivyFile.getJavaFile().lastModified());
			final FileTime cachedModified = FileTime.from(chachedModifiedInstant.truncatedTo(ChronoUnit.MILLIS));

			// cache file if was modified
			if (fileModified.compareTo(cachedModified) != 0) {
				ivyFile = cacheFile(ivyFile, file.getName(), file.getContent(), extension, needsConversion,
						fileModified);
			}
		} else {
			ivyFile = cacheFile(ivyFile, file.getName(), file.getContent(), extension, needsConversion, fileModified);
		}

		return ivyFile;
	}

	private ch.ivyteam.ivy.scripting.objects.File cacheFile(ch.ivyteam.ivy.scripting.objects.File ivyFile,
			String origFileName, byte[] content, String extension, boolean needsConversion, FileTime lastModified) {
		// convert document to pdf if needed
		final byte[] fileContent = needsConversion ? convertFileToPdf(content, origFileName, extension) : content;
		try {
			ivyFile.createNewFile();
			ivyFile.writeBinary(new Binary(fileContent));
			Files.setLastModifiedTime(ivyFile.getJavaFile().toPath(), lastModified);
			return ivyFile;
		} catch (final IOException e) {
			Ivy.log().error(BpmErrorCode.DOC_VIEWER_FAILED_TO_CACHE_FILE.getCmsMessage(), e);
			return null;
		}
	}

	private byte[] convertFileToPdf(byte[] content, String fileName, String extension) {
		byte[] result = null;
		try {
			result = DocumentService.convertDocumentToPdf(content, extension);
		} catch (final Exception e) {
			Ivy.log().error(BpmErrorCode.DOC_VIEWER_FAILED_TO_CONVERT_TO_PDF.getCmsMessage(), e);
		}
		return result;
	}

	private String changeExtensionToPdf(String fileName) {
		return FilenameUtils.removeExtension(fileName) + DOT + PDF_EXTENSION;
	}

	/**
	 * Used for the p:galleria component which expects a collection
	 * 
	 * @return current file to render put to a list
	 */
	public List<ch.ivyteam.ivy.scripting.objects.File> getCurrentFileList() {
		final List<ch.ivyteam.ivy.scripting.objects.File> list = new ArrayList<>();
		list.add(currentFile);
		return list;
	}

	/**
	 * @return if file to be show is not null and is an image
	 */
	public boolean canRenderImage() {
		final boolean result = this.currentFile != null && this.isFileImage;
		return result;
	}

	/**
	 * @return if file to be shown is not null and is a document
	 */
	public boolean canRenderDocument() {
		final boolean result = this.currentFile != null && !this.isFileImage;
		return result;
	}

	/**
	 * @return URL of current file to be show
	 * @see {@link IHtmlDialogContext#fileLink(ch.ivyteam.ivy.scripting.objects.File)}
	 */
	public String getCurrentFileRef() {
		return Ivy.html().fileLink(currentFile).get();
	}

	/**
	 * Gets the currently viewed ivy file.
	 *
	 * @return currently viewed ivy file
	 */
	public ch.ivyteam.ivy.scripting.objects.File getCurrentFile() {
		return currentFile;
	}

	/**
	 * @return the currentFileId
	 */
	public String getCurrentFileId() {
		return currentFileId;
	}

	/**
	 * @return the previewNotAvailable
	 */
	public boolean isPreviewNotAvailable() {
		return previewNotAvailable;
	}

	/**
	 * @param previewNotAvailable the previewNotAvailable to set
	 */
	public void setPreviewNotAvailable(boolean previewNotAvailable) {
		this.previewNotAvailable = previewNotAvailable;
	}
}
