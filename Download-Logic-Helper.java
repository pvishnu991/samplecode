
#######Export Action###################################################

@Name("Export Email")
public class ExportEmail extends WebHawkPage {
    public ExportEmail(WebDriver driver) {
        super(driver);
    }

    private String docDownloadPath;
    private String fileName;
    private final Logger log = LoggerFactory.getLogger(ExportEmail.class);

    private CSVUtilities utils = new CSVUtilities();

    @FindBy(xpath = "//select[@name='partnerCode']")
    public WebElementFacade partnerCode;

    @FindBy(xpath = "//input[@value='Export to CSV']")
    public WebElementFacade exportSubmitButton;

    @FindBy(xpath = "//table/tbody/tr")
    public List<WebElementFacade> exportTableRows;

    @Action("Export Action")
    public void exportAction() throws IOException, CsvException {
        String alliance = System.getProperty("ALLIANCE");

        partnerCode.waitUntilVisible().selectByVisibleText(alliance);
        int rows = exportTableRows.size();
        if (rows > 0) {

            log.info("Rows displayed to perform export validation");
            exportSubmitButton.waitUntilEnabled().click();
            waitABit(TEN_SECONDS);
            docDownloadPath = System.getProperty("user.dir");
            log.info("Default download path ==>" + docDownloadPath);
            File getLatestFile = DownloadHelper.getLatestFilefromDir(docDownloadPath, DOC_TYPE_CSV);
            fileName = getLatestFile.getName();
            log.info("File name ===>" + fileName);
            utils.readCSVFiles(fileName);

        } else {
            Assert.fail("No rows displayed to perform export validation");
        }
    }
}

################Download Helper##############################################
package com.fd.product.mp.helpers;

import java.io.File;

public final class DownloadHelper {

    private DownloadHelper() {

    }

    public static File getLatestFilefromDir(String dirPath, String docType) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return null;
        }

        File lastModifiedFile = files[0];
        for (int i = 1; i < files.length; i++) {
            if (files[i].getName().contains(docType) && (lastModifiedFile.lastModified() < files[i].lastModified())) {
                lastModifiedFile = files[i];
            }
        }
        return lastModifiedFile;
    }

    public static boolean fileDownloadedCheck(String downloadPath, String fileName) {
        boolean flag = false;
        File dir = new File(downloadPath);
        File[] dirContents = dir.listFiles();

        for (int i = 0; i < dirContents.length; i++) {
            if (dirContents[i].getName().contains(fileName)) {
                flag = true;
                return flag;
            }

        }
        return flag;
    }
}
######################################################################################
########CSV Utilities########################################

package com.fd.product.mp.helpers;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class CSVUtilities {

    private final Logger log = LoggerFactory.getLogger(CSVUtilities.class);

    public CSVUtilities() {

    }

    public void readCSVFiles(String fileName) throws IOException, CsvException {

        CSVReader reader = new CSVReader(new FileReader(fileName));

        if (reader.readNext() != null) {

            List allContent = reader.readAll();
            log.info("Total row count for the file " + fileName + " ===> " + allContent.size());
            log.info("CSV file not empty !!");
        } else {
            Assert.fail("CSV file" + " " + fileName + " is empty");
        }
    }

}
########CSV Utilities########################################

###############Excel Utilities########################################
package com.fd.product.mp.helpers;


import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;

public class ExcelUtilities {

    private final Logger log = LoggerFactory.getLogger(ExcelUtilities.class);

    public ExcelUtilities() {

    }

    public void readExcel(String fileName, String docType) throws IOException {
        File file = new File(System.getProperty("user.dir") + File.separator + fileName);
        FileInputStream inputStream = new FileInputStream(file);
        Workbook excelWorkbook = null;
        if (docType.equalsIgnoreCase(".xls")) {
            excelWorkbook = new HSSFWorkbook(inputStream);

        } else if (docType.equalsIgnoreCase(".xlsx")) {
            excelWorkbook = new XSSFWorkbook(inputStream);
        }

        Sheet excelSheet = excelWorkbook.getSheetAt(0);
        int rowCount = excelSheet.getLastRowNum() - excelSheet.getFirstRowNum();

        log.info("Total rows in the sheet  ===> " + rowCount);
        if (rowCount > 0) {
            log.info("Excel not empty");
        } else {
            Assert.fail("Excel content is empty!!");
        }
    }
}
###############Excel Utilities########################################


################XML Utilities############################################################

package com.fd.product.mp.helpers;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class XMLUtilities {

    private final Logger log = LoggerFactory.getLogger(ExcelUtilities.class);

    public XMLUtilities() {

    }

    public void readXML(String fileName, String page) throws ParserConfigurationException, IOException, SAXException {

        File file = new File(System.getProperty("user.dir") + File.separator + fileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();

        log.info("Root Element :" + doc.getDocumentElement().getNodeName());
        String pageTag = getXMLtagNameMPmapping(page);
        NodeList nodeTags = doc.getElementsByTagName(pageTag);
        log.info("nodeTags - " + nodeTags.getLength());

        int nodeTagsCount = nodeTags.getLength();
        log.info("Total nodes/elements in the XML  ===> " + nodeTagsCount);
        if (nodeTagsCount > 0) {
            log.info("XML not empty");
        } else {
            Assert.fail("XML content is empty!!");
        }
    }

    public String getXMLtagNameMPmapping(String page) {

        String tag = null;

        switch (page) {

            case "funding":
                tag = "FundingList";
                break;

            case "transactions":
                tag = "Transaction";
                break;

            case "gsmTopUp":
                tag = "GsmTopUpOutPut";
                break;

            case "batches":
                tag = "Batch";
                break;

            case "invoices":
                tag = "InvoiceList";
                break;

            case "authorisations":
                tag = "Authorisation";
                break;

            case "preauthorisations":
                tag = "Pre-Authorization";
                break;

            case "authorisationsLog":
                tag = "authorisationConsolidates";
                break;

            default:
                Assert.fail("page " + page + " Not Found");
        }

        return tag;
    }
}
################XML Utilities############################################################


###############PDF Utilities############################################################

package com.fd.product.mp.helpers;

import com.fd.taf.core.pdf.PdfToTextConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class PdfUtils {
    private final Logger log = LoggerFactory.getLogger(PdfUtils.class);

    public PdfUtils() {

    }

    public void readPdfFile(String downloadPath, String fileName) throws IOException {

        String pdfText = new PdfToTextConverter().convert(new File(downloadPath + File.separator + fileName));
        log.info("Pdf content is===>" + pdfText);
        if (!(pdfText.isEmpty())) {
            log.info("Downloaded pdf file " + fileName + "validated!!");
        } else {
            log.info("Downloaded pdf file " + fileName + "is empty!!");
        }
    }
}
###############PDF Utilities############################################################




