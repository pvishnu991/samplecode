package com.fd.product.mp.pages.components;

import com.fd.product.mp.pages.interfaces.SortableColumns;
import com.fd.product.mp.helpers.Column;
import com.fd.taf.core.WebHawkContext;
import com.fd.taf.core.page.WebHawkPage;
import com.fd.taf.reflection.ReflectionHelper;
import com.fd.taf.ui.annotations.Action;
import com.fd.taf.ui.annotations.Name;
import com.fd.taf.ui.annotations.SubPage;
import net.serenitybdd.core.annotations.findby.FindBy;
import net.serenitybdd.core.pages.WebElementFacade;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.fd.product.mp.config.Constants.*;

public class TableWithEntitiesComponent extends WebHawkPage {

    private final Logger log = LoggerFactory.getLogger(TableWithEntitiesComponent.class);
    private String totalRecordsMessage;
    private int pageCount;

    //common elements for pagination
    @FindBy(css = "[data-at*='-table-row']")
    @Name("TABLE_ROWS")
    public List<WebElementFacade> tableRows;

    //1st Loaded Table Element
    @FindBy(xpath = "(//table[not(contains(@class,'tg-loading'))]//app-table-grid-row)[1]")
    @Name("TABLE_LOADED_AMOUNT")
    public WebElementFacade tblLoadedFirstRow;

    @FindBy(css = "thead:not([style*='display: none']) > tr > th:not([class*='tg-cell--hide-by-picker']) > button > span[data-tour-id*='data-grid-header-']")
    @Name("TABLE_COLUMN_HEADERS")
    public List<WebElementFacade> tblColumnHeaders;

    @FindBy(xpath = "//app-table-grid//div[contains(@class,'tg-container')]")
    @Name("TABLE_COLUMN_HEADERS")
    public WebElementFacade tblScroll;

    @FindBy(css = "[data-at='void-button']")
    @Name("Void Button")
    public WebElementFacade voidButton;

    @FindBy(xpath = "//h3[contains(text(),'Authorised offline')]/parent::div//app-simple-dictionary-data-type")
    @Name("TABLE_AUTHORISED_OFFLINE")
    public List<WebElementFacade> authorisedOffline;

    private String fieldHighlightDesc = "arguments[0].setAttribute('style', 'background: yellow; border: 2px solid red;');";
    private String fieldHighlightAsc = "arguments[0].setAttribute('style', 'background: #90EE90; border: 2px solid red;');";


    @SubPage
    private CommonComponent headerComponent;

    public TableWithEntitiesComponent(WebDriver driver) {
        super(driver);
        this.pageCount = 1;
        this.totalRecordsMessage = "";
    }


    @Action("Void Option Verification")
    /* Void button should be displayed for approved transactions alone. Hence validating that the button void is enabled for approved status
     * Also the date has to current date. Email validation cannot be at this stage*/
    public void voidOptionVerification() {

        if (tableRows.size() > 0) {
            log.info("Web table available to validate further!!");
            tblLoadedFirstRow.waitUntilVisible();
            for (int count = 0; count <= tableRows.size() - 1; count++) {

                WebElementFacade status = find(By.cssSelector(String.format("[data-at*='%s-table-row']>td[data-at='status']>app-status-data-type", count)));
                getJavascriptExecutorFacade().executeScript("arguments[0].scrollIntoView();", status.getElement());
                if (status.getText().equalsIgnoreCase("Approved")) {
                    tableRows.get(count).click();
                    getJavascriptExecutorFacade().executeScript(fieldHighlightDesc, status.getElement());
                    if (voidButton.waitUntilVisible().isEnabled()) {
                        log.info("Void button enabled for Approved status!!");
                    } else {
                        Assert.fail("Void button not enabled for Approved Status!! Error");
                    }
                }
            }
        } else {
            Assert.fail("No table displayed to validate: Number of rows displayed is: " + tableRows.size());
        }

    }

    @Action("Pagination Action")
    public void customActionTest() {

        log.debug("Inside paginationAction");
        waitForElementToBeVisibleOrNot(tblLoadedFirstRow, TEN_SECONDS);

        final Paginator paginator = ReflectionHelper.getInstanceOfPage(getDriver(), Paginator.class);

        if (tableRows.size() > 0) {

            //Scroll to Table view element
            getJavascriptExecutorFacade().executeScript("arguments[0].scrollIntoView();", tblScroll.getElement());

            log.info("Records Returned : " + tableRows.size());
            log.info("NEXT ENABLED  : " + paginator.getPageNumber("next").isEnabled());
            log.info("Page Count : : " + pageCount);

            if (paginator.getPageNumber("next").isEnabled() && pageCount <= MAX_NUM_PAGES_TO_ITERATE) {

                pageCount++;
                Assertions.assertThat(tableRows.size()).isEqualTo(MAXIMUM_NUMBERS_OF_ROWS);
                paginator.getPageNumber(Integer.toString(pageCount)).waitUntilEnabled().click();
                customActionTest();
            } else {
                Assertions.assertThat(tableRows.size()).isLessThanOrEqualTo(MAXIMUM_NUMBERS_OF_ROWS);
                log.debug("(this.pageCount - 1) * 20 " + (pageCount - 1) * MAXIMUM_NUMBERS_OF_ROWS);
//                totalRecordsMessage = pageCount <= MAX_NUM_PAGES_TO_ITERATE
//                        ? "Total Number of Records are = "
//                        + ((pageCount - 1) * MAXIMUM_NUMBERS_OF_ROWS + tableRows.size()) + " and Total Number of Pages are = " + (pageCount)
//                        : "Total Number of Pages are 5+ and Total Number of Records are 100+";
                log.info(totalRecordsMessage);

                if (pageCount <= MAX_NUM_PAGES_TO_ITERATE) {
                    totalRecordsMessage = "Total Number of Records are = " + ((pageCount - 1) * MAXIMUM_NUMBERS_OF_ROWS + tableRows.size()) + " and Total Number of Pages are = " + (pageCount);
                } else {
                    totalRecordsMessage = "Total Number of Pages are 5+ and Total Number of Records are 100+";
                }
            }
        } else {
            log.info("No Records Returned for Pagination logic");
        }

    }

    @Action("Sorting Action")
    public void sortingAction() {

        final WebHawkPage page = WebHawkContext.getInstance().getCurrentPage();
        final SortableColumns sortableColumnsPage = page.asInterface(SortableColumns.class);
        final List<Column> columsToSort = sortableColumnsPage.getListOfSortColumns();

        log.debug("Inside sortingAction");
        waitForElementToBeVisibleOrNot(tblLoadedFirstRow, TEN_SECONDS);

        log.debug("Outside loop ColumnHeaders size : " + tblColumnHeaders.size());
        log.info("");

        if (tableRows.size() > 0) {

            //Scroll to Table view element
            getJavascriptExecutorFacade().executeScript("arguments[0].scrollIntoView();", tblScroll.getElement());

            for (int column = 0; column < tblColumnHeaders.size(); column++) {

                log.debug("Inside loop ColumnHeaders size : " + tblColumnHeaders.size());
                log.info("");
                String columnText = tblColumnHeaders.get(column).getText().trim();
                log.info("Column text : " + columnText);

                //Only Sort Columns that have Names => Statement and Invoices Column Header 7 = empty, hence is not sortable
                if (columnText != null && !columnText.isEmpty()) {

                    //Check whether HTML column is one of the columsToSort; by comparing HTML (data-tour-id) with Column.getName() for match
                    int finalColumn = column;
                    Optional<Column> foundColumn = columsToSort.stream().filter(x -> tblColumnHeaders.get(finalColumn).getAttribute("data-tour-id").contains(x.getName())).findFirst();
                    log.info(tblColumnHeaders.get(column).getText() + " foundColumn is Found : " + foundColumn.isPresent());

                    //Click Column to sort in Ascending Order
                    tblColumnHeaders.get(column).click();
                    getJavascriptExecutorFacade().executeScript(fieldHighlightAsc, tblColumnHeaders.get(column).getElement());
                    tblLoadedFirstRow.waitUntilVisible();

                    //Scroll Right Across Column Table Scroll
                    getJavascriptExecutorFacade().executeScript("arguments[0].scrollLeft += 60", tblScroll.getElement());

                    //HTML column is one of the columsToSort
                    foundColumn.ifPresent(col -> {
                                //Perform Sorting in Ascending order on column
                                verifyColumnsSortedAscending(getfirstFiveRows(col.getName()), col);
                            }
                    );

                    //Click Column to sort in Descending Order
                    tblColumnHeaders.get(column).click();
                    getJavascriptExecutorFacade().executeScript(fieldHighlightDesc, tblColumnHeaders.get(column).getElement());
                    tblLoadedFirstRow.waitUntilVisible();

                    //HTML column is one of the columsToSort
                    foundColumn.ifPresent(col -> {
                                //Perform Sorting in Ascending order on column
                                verifyColumnsSortedDescending(getfirstFiveRows(col.getName()), col);
                            }
                    );
                }

            }
        } else {
            log.info("No Records Returned for Sorting logic");
        }
    }


    @Action("Offline Approval Action")
    public void offlineApproval() {

        waitForElementToBeVisibleOrNot(tblLoadedFirstRow, TEN_SECONDS);

        if (tableRows.size() > 0) {

            for (int row = 0; row < tableRows.size(); row++) {

                tableRows.get(row).click();
                getJavascriptExecutorFacade().executeScript("arguments[0].scrollIntoView(true);", authorisedOffline.get(row).getElement());
                String offlineApproval = authorisedOffline.get(row).waitUntilVisible().getText().trim();
                log.info("Offline Approval Text - Row : " + row + " = " + offlineApproval);
                Assertions.assertThat(Arrays.asList("Yes", "No").contains(offlineApproval));
            }
        } else {
            log.info("No Records Returned for Sorting logic");
        }
    }


    public void waitForElementToBeVisibleOrNot(WebElementFacade element, int waitTimeInMillis) {
        try {
            //element.withTimeoutOf(waitTimeInMillis, ChronoUnit.SECONDS).waitUntilVisible();
            element.waitUntilVisible();
        } catch (Exception e) {
            log.info("Waited for element to be visible for " + element);
        }
    }

    public List<String> getfirstFiveRows(String sortBy) {

        List<WebElementFacade> rows = findAll(By.cssSelector(String.format("td[data-at='%s']", sortBy)));
        List<String> firstFiveRows = new ArrayList<>();

        for (WebElementFacade elementFacade : rows) {
            firstFiveRows.add(elementFacade.getText().trim());
        }

        firstFiveRows = new ArrayList<>(firstFiveRows.subList(0, NUMBER_OF_ROWS));

        return firstFiveRows;
    }

    public List<Float> convertToFloatArray(List<String> fiveRowsString) {
        return fiveRowsString
                .stream()
                .map(x -> Float.parseFloat(x.replaceAll("[^\\d.]", "")))
                .collect(Collectors.toList());
    }

    public List<Date> convertToDateArray(List<String> fiveRowsString) {

        List<Date> dateRows = new ArrayList<>();
        for (String s : fiveRowsString) {
            try {
                dateRows.add(new SimpleDateFormat(DATE_FORMAT).parse(s));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return dateRows;
    }

    public void verifyColumnsSortedAscending(List<String> fiveRows, Column column) {

        switch (column.getType()) {

            case "string":
                //Verify 1st five rows sorted in Ascending Order
                List<String> fiveRowsSorted = new ArrayList<>(fiveRows);
                Collections.sort(fiveRowsSorted);
                log.info("Ascending - fiveRowsSorted : ");
                log.info(String.valueOf(fiveRowsSorted));
                log.info("Are equal : " + fiveRows.equals(fiveRowsSorted));
                Assertions.assertThat(fiveRows.equals(fiveRowsSorted));
                break;

            case "number":
                //Verify 1st five rows sorted in Ascending Order
                List<Float> fiveRowsFloat = new ArrayList<>(convertToFloatArray(fiveRows));
                log.info("Ascending - fiveRowsFloat (Converted to Float) : ");
                log.info(String.valueOf(fiveRowsFloat));
                List<Float> fiveRowsFloatSorted = new ArrayList<>(fiveRowsFloat);
                Collections.sort(fiveRowsFloatSorted);
                log.info("Ascending - fiveRowsFloatSorted : ");
                log.info(String.valueOf(fiveRowsFloatSorted));
                log.info("Are equal : " + fiveRowsFloat.equals(fiveRowsFloatSorted));
                Assertions.assertThat(fiveRowsFloat.equals(fiveRowsFloatSorted));
                break;

            case "date":
                //Verify 1st five rows sorted in Ascending Order
                List<Date> fiveRowsDate = new ArrayList<>(convertToDateArray(fiveRows));
                log.info("Ascending - fiveRowsDate (Converted to Date): ");
                log.info(String.valueOf(fiveRowsDate));
                List<Date> fiveRowsDateSorted = new ArrayList<>(fiveRowsDate);
                Collections.sort(fiveRowsDateSorted);
                log.info("Ascending - fiveRowsDateSorted : ");
                log.info(String.valueOf(fiveRowsDateSorted));
                log.info("Are equal : " + fiveRowsDate.equals(fiveRowsDateSorted));
                Assertions.assertThat(fiveRowsDate.equals(fiveRowsDateSorted));
                break;

            default:
                Assertions.fail("Sort By " + column.getType() + " Not Found");
        }
    }


    public void verifyColumnsSortedDescending(List<String> fiveRows, Column column) {

        switch (column.getType()) {

            case "string":
                //Verify 1st five rows sorted in Descending Order
                List<String> fiveRowsSorted = new ArrayList<>(fiveRows);
                fiveRowsSorted = new ArrayList<>(fiveRows);
                Collections.sort(fiveRowsSorted, Collections.reverseOrder());
                log.info("Descending - fiveRowsSorted : ");
                log.info(String.valueOf(fiveRowsSorted));
                log.info("Are equal : " + fiveRows.equals(fiveRowsSorted));
                Assertions.assertThat(fiveRows.equals(fiveRowsSorted));
                break;
            case "number":
                //Verify 1st five rows sorted in Descending Order
                List<Float> fiveRowsFloat = new ArrayList<>(convertToFloatArray(fiveRows));
                log.info("Descending - fiveRowsFloat (Converted to Float) : ");
                log.info(String.valueOf(fiveRowsFloat));
                List<Float> fiveRowsFloatSorted = new ArrayList<>(fiveRowsFloat);
                Collections.sort(fiveRowsFloatSorted, Collections.reverseOrder());
                log.info("Descending - fiveRowsFloatSorted : ");
                log.info(String.valueOf(fiveRowsFloatSorted));
                log.info("Are equal : " + fiveRowsFloat.equals(fiveRowsFloatSorted));
                Assertions.assertThat(fiveRowsFloat.equals(fiveRowsFloatSorted));
                break;

            case "date":
                //Verify 1st five rows sorted in Descending Order
                List<Date> fiveRowsDate = new ArrayList<>(convertToDateArray(fiveRows));
                log.info("Descending - fiveRowsDate (Converted to Date) : ");
                log.info(String.valueOf(fiveRowsDate));
                List<Date> fiveRowsDateSorted = new ArrayList<>(fiveRowsDate);
                fiveRowsDateSorted = new ArrayList<>(fiveRowsDate);
                Collections.sort(fiveRowsDateSorted, Collections.reverseOrder());
                log.info("Descending - fiveRowsDateSorted : ");
                log.info(String.valueOf(fiveRowsDateSorted));
                log.info("Are equal : " + fiveRowsDate.equals(fiveRowsDateSorted));
                Assertions.assertThat(fiveRowsDate.equals(fiveRowsDateSorted));
                break;

            default:
                Assertions.fail("Sort By " + column.getType() + " Not Found");
        }

    }
}
