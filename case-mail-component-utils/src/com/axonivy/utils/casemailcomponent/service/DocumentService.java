package com.axonivy.utils.casemailcomponent.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.Strings;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import com.aspose.cells.ImageOrPrintOptions;
import com.aspose.cells.SheetRender;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.aspose.pdf.Document;
import com.aspose.pdf.Image;
import com.aspose.pdf.Page;
import com.aspose.pdf.devices.PngDevice;
import com.aspose.slides.ISlide;
import com.aspose.slides.Presentation;
import com.aspose.words.ImageSaveOptions;
import com.aspose.words.PageSet;
import com.aspose.words.SaveFormat;

import ch.ivyteam.ivy.addons.docfactory.aspose.AsposeProduct;
import ch.ivyteam.ivy.addons.docfactory.aspose.LicenseLoader;
import ch.ivyteam.ivy.environment.Ivy;

/**
 * Generic service for working with documents
 *
 * @author ny.huynh
 *
 */
public class DocumentService {

	public static final String[] ASPOSE_IMAGE_FILE_TYPES = { "BMP", "EMF", "JPG", "JPEG", "PNG", "SVG" };
	public static final String[] ASPOSE_IMAGES_WITHOUT_PREVIEW = { "TIFF", "TIF" };
	public static final String[] ASPOSE_EXCEL_FILE_TYPES = { "XLS", "XLSX" };
	public static final String[] ASPOSE_WORD_FILE_TYPES = { "DOC", "DOCX" };
	public static final String[] ASPOSE_POWERPOINT_FILE_TYPES = { "PPT", "PPTX" };
	public static final String[] ASPOSE_PDF_FILE_TYPES = { "PDF" };

	public static final String[] ASPOSE_MS_DOCUMENT_TYPES = Stream.of(new String[][] 
			{ 
				ASPOSE_EXCEL_FILE_TYPES, 
				ASPOSE_WORD_FILE_TYPES, 
				ASPOSE_POWERPOINT_FILE_TYPES, 
			}).flatMap(Stream::of)
			.toArray(String[]::new);

	/**
	 * Loads license for Aspose library
	 */
	static {
		try {
			LicenseLoader.loadLicenseforProduct(AsposeProduct.PDF);
			LicenseLoader.loadLicenseforProduct(AsposeProduct.CELLS);
			LicenseLoader.loadLicenseforProduct(AsposeProduct.WORDS);
			LicenseLoader.loadLicenseforProduct(AsposeProduct.SLIDES);
		} catch (final Exception e) {
			Ivy.log().error("Error loading licences", e);
		}
	}

	/**
	 * Converts Word document to PDF using Aspose library and writes it to the
	 * provided {@link OutputStream}
	 * 
	 * @param docStream       {@link InputStream} of the document to be converted
	 * @param pdfOutputStream {@link OutputStream} where the PDF will be written
	 * @throws Exception when conversion failed
	 */
	public static void convertWordToPdf(InputStream docInputStream, OutputStream pdfOutpuStream) throws Exception {
		final com.aspose.words.Document doc = new com.aspose.words.Document(docInputStream);
		doc.save(pdfOutpuStream, com.aspose.words.SaveFormat.PDF);
	}

	/**
	 * Converts Word document to PDF using Aspose library and returns it as a byte
	 * array
	 * 
	 * @param docInputData data of the document to be converted
	 * @return byte array of the converted PDF
	 * @throws Exception when conversion failed
	 */
	public static byte[] convertWordToPdf(byte[] docInputData) throws Exception {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		convertWordToPdf(new ByteArrayInputStream(docInputData), os);
		return os.toByteArray();
	}

	/**
	 * Converts Excel document to PDF using Aspose library
	 * 
	 * @param docStream       {@link InputStream} of the document to be converted
	 * @param pdfOutputStream {@link OutputStream} where the PDF will be written
	 * @throws Exception when conversion failed
	 */
	public static void convertExcelToPdf(InputStream docStream, OutputStream pdfOutpuStream) throws Exception {
		final Workbook wb = new Workbook(docStream);
		wb.save(pdfOutpuStream, com.aspose.cells.SaveFormat.PDF);
	}

	/**
	 * Converts Excel document to PDF using Aspose library and returns it as a byte
	 * array
	 * 
	 * @param docInputData data of the document to be converted
	 * @return byte array of the converted PDF
	 * @throws Exception when conversion failed
	 */
	public static byte[] convertExcelToPdf(byte[] docInputData) throws Exception {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		convertExcelToPdf(new ByteArrayInputStream(docInputData), os);
		return os.toByteArray();
	}

	/**
	 * Converts PowerPoint document to PDF using Aspose library
	 * 
	 * @param docStream       {@link InputStream} of the document to be converted
	 * @param pdfOutputStream {@link OutputStream} where the PDF will be written
	 * @throws Exception when conversion failed
	 */
	public static void convertPowerPointToPdf(InputStream docStream, OutputStream pdfOutpuStream) throws Exception {
		final Presentation doc = new Presentation(docStream);
		doc.save(pdfOutpuStream, com.aspose.slides.SaveFormat.Pdf);
	}

	/**
	 * Converts PowerPoint document to PDF using Aspose library and returns it as a
	 * byte array
	 * 
	 * @param docInputData data of the document to be converted
	 * @return byte array of the converted PDF
	 * @throws Exception when conversion failed
	 */
	public static byte[] convertPowerPointToPdf(byte[] docInputData) throws Exception {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		convertPowerPointToPdf(new ByteArrayInputStream(docInputData), os);
		return os.toByteArray();
	}

	/**
	 * Converts document to PDF using Aspose library and returns it as a byte array.
	 * File extension needs to be passed. Allowed types are Excel, Word and
	 * PowerPoint types
	 * 
	 * @param docInputData data of the document to be converted
	 * @param extension    file extension of the document
	 * @return byte array of the converted PDF or null if invalid extension
	 * @throws Exception when conversion failed
	 */
	public static byte[] convertDocumentToPdf(byte[] docInputData, String extension) throws Exception {
		byte[] result = null;
		if (Strings.CI.equalsAny(extension, ASPOSE_EXCEL_FILE_TYPES)) {
			result = DocumentService.convertExcelToPdf(docInputData);
		} else if (Strings.CI.equalsAny(extension, ASPOSE_WORD_FILE_TYPES)) {
			result = DocumentService.convertWordToPdf(docInputData);
		} else if (Strings.CI.equalsAny(extension, ASPOSE_POWERPOINT_FILE_TYPES)) {
			result = DocumentService.convertPowerPointToPdf(docInputData);
		}
		return result;
	}

	/**
	 * Converts pages from provided PDF PNGs
	 * 
	 * @param pdfData data of PDF file
	 * @return list of PNGS
	 * @throws Exception if there is an error during the conversion
	 */
	public static List<ByteArrayOutputStream> convertPdfToPngs(byte[] pdfData) throws Exception {
		final List<ByteArrayOutputStream> result = new ArrayList<>();
		try (final Document pdfDocument = new Document(pdfData);) {
			for (final Page page : pdfDocument.getPages()) {
				try (ByteArrayOutputStream pageOs = new ByteArrayOutputStream()) {
					final PngDevice renderer = new PngDevice();
					renderer.process(page, pageOs);
					result.add(pageOs);
				} catch (final IOException e) {
					throw e;
				}
			}
		} catch (final Exception e) {
			throw e;
		}
		return result;
	}

	/**
	 * Converts provided images to a PDF file. Each image is a separate page
	 * 
	 * @param images to be converted to PDF
	 * @return data of the PDF file
	 * @throws Exception if there is an error during the conversion
	 */
	public static byte[] imagesToPdf(List<byte[]> images) throws Exception {
		try (final Document pdf = new Document(); final ByteArrayOutputStream pdfOs = new ByteArrayOutputStream()) {
			for (final byte[] imageData : images) {
				// create new page
				final Page page = pdf.getPages().add();

				final double pageMargin = 0;
				page.getPageInfo().getMargin().setBottom(pageMargin);
				page.getPageInfo().getMargin().setTop(pageMargin);
				page.getPageInfo().getMargin().setLeft(pageMargin);
				page.getPageInfo().getMargin().setRight(pageMargin);

				try (final ByteArrayInputStream imgStream = new ByteArrayInputStream(imageData);
						final ByteArrayInputStream pdfImgStream = new ByteArrayInputStream(imageData)) {
					// get image width and height
					final BufferedImage img = ImageIO.read(imgStream);
					final double imageWidth = img.getWidth();
					final double imageHeight = img.getHeight();

					final double pageWidth = page.getPageInfo().getWidth();
					// calculating the scale factor for the image's width to fit the page
					final double imageScaleWidth = pageWidth / imageWidth;

					final Image image = new Image();
					// setting fixed width for the image to match the page width
					image.setFixWidth(pageWidth);
					// scaling the image height to maintain aspect ratio
					image.setFixHeight(imageHeight * imageScaleWidth);

					// adjusting the height of the PDF page to accommodate the scaled image
					page.getPageInfo().setHeight(image.getFixHeight());

					// add image to the page and set data stream
					page.getParagraphs().add(image);
					image.setImageStream(pdfImgStream);

				} catch (final IOException e) {
					Ivy.log().error("Error loading image to determine width and height", e);
					throw e;
				}
			}
			pdf.save(pdfOs);
			return pdfOs.toByteArray();
		} catch (final Exception e) {
			throw e;
		}
	}

	/**
	 * Converts pages from provided PDF PNGs
	 * 
	 * @param pdfData data of PDF file
	 * @return list of PNGS
	 * @throws Exception if there is an error during the conversion
	 */
	public static List<ByteArrayOutputStream> convertPDFToPNGsApache(byte[] pdfData) {
		final List<ByteArrayOutputStream> result = new ArrayList<>();

		try (PDDocument document = Loader.loadPDF(pdfData)) {
			final PDFRenderer pdfRenderer = new PDFRenderer(document);
			for (int page = 0; page < document.getNumberOfPages(); ++page) {
				final BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIOUtil.writeImage(bim, "png", baos, 300);
				result.add(baos);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Converts pages from provided WORD PNGs
	 * 
	 * @param docData data of Word file
	 * @return list of PNGS
	 * @throws Exception if there is an error during the conversion
	 */
	public static List<ByteArrayOutputStream> convertWordToPngs(byte[] docData) throws Exception {
		final List<ByteArrayOutputStream> result = new ArrayList<>();

		try (InputStream inputStream = new ByteArrayInputStream(docData)) {
			final com.aspose.words.Document docDocument = new com.aspose.words.Document(inputStream);

			final ImageSaveOptions options = new ImageSaveOptions(SaveFormat.PNG);

			for (int i = 0; i < docDocument.getPageCount(); i++) {
				try (ByteArrayOutputStream pageOs = new ByteArrayOutputStream()) {

					options.setPageSet(new PageSet(i));
					docDocument.save(pageOs, options);
					result.add(pageOs);
				} catch (final IOException e) {
					throw e;
				}
			}
		}

		return result;
	}

	/**
	 * Converts pages from provided EXCEL PNGs
	 * 
	 * @param excelData data of Excel file
	 * @return list of PNGS
	 * @throws Exception if there is an error during the conversion
	 */
	public static List<ByteArrayOutputStream> convertExcelToPngs(byte[] excelData) throws Exception {
		final List<ByteArrayOutputStream> result = new ArrayList<>();

		try (InputStream inputStream = new ByteArrayInputStream(excelData)) {
			final Workbook workbook = new Workbook(inputStream);

			final ImageOrPrintOptions options = new ImageOrPrintOptions();
			options.setImageType(com.aspose.cells.ImageType.PNG);

			for (int i = 0; i < workbook.getWorksheets().getCount(); i++) {
				final Worksheet sheet = workbook.getWorksheets().get(i);
				final SheetRender sr = new SheetRender(sheet, options);

				for (int j = 0; j < sr.getPageCount(); j++) {
					try (ByteArrayOutputStream pageOs = new ByteArrayOutputStream()) {
						sr.toImage(j, pageOs);
						result.add(pageOs);
					} catch (final IOException e) {
						throw e;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Converts pages from provided POWERPOINT PNGs
	 * 
	 * @param pptData data of POWERPOINT file
	 * @return list of PNGS
	 * @throws Exception if there is an error during the conversion
	 */
	public static List<ByteArrayOutputStream> convertPowerPointToPngs(byte[] pptData) throws Exception {
		final List<ByteArrayOutputStream> result = new ArrayList<>();

		try (InputStream inputStream = new ByteArrayInputStream(pptData)) {
			final Presentation presentation = new Presentation(inputStream);

			for (int i = 0; i < presentation.getSlides().size(); i++) {
				final ISlide slide = presentation.getSlides().get_Item(i);

				try (ByteArrayOutputStream slideOs = new ByteArrayOutputStream()) {
					final BufferedImage image = slide.getThumbnail(1f, 1f);
					ImageIO.write(image, "png", slideOs);
					result.add(slideOs);
				} catch (final IOException e) {
					throw e;
				}
			}
		}

		return result;
	}
}
