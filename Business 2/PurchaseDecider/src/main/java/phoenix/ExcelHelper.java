package phoenix;
import org.apache.poi.xssf.usermodel.*;

public class ExcelHelper {

    /**
     * finds desired column based on 'str' and 'row'
     *
     * @param sheet the XSSFSheet to be searched
     * @param str the String to be found
     * @param row the row to be searched
     * @return the column index
     */
    public static int findColumnIndex(XSSFSheet sheet, String str, int row) {
	int lastCol = sheet.getRow(row).getLastCellNum();
	for (int col = 0; col <= lastCol; col++) {
	    //	    if (sheet.getRow(row).getCell(col) == null)
	    //		sheet.getRow(row).createCell(col);
	    try {
		if (sheet.getRow(row).getCell(col).getStringCellValue().trim().equalsIgnoreCase(str.trim()))
		    return col;
	    } catch (NullPointerException npe) { }
	}//for
	return -1; //if not found
    }//findColumnIndex(XSSFSheet, String, int)

    /**
     * finds desired row based on 'str' and 'col'
     *
     * @param sheet the XSSFSheet to be searched
     * @param str the string to be found
     * @param col the column to be searched
     * @return the row index
     */
    public static int findRowIndex(XSSFSheet sheet, String str, int col) {
	for (int row = 0; row <= sheet.getLastRowNum(); row++) {
	    if (sheet.getRow(row) == null)
		sheet.createRow(row);
	    if (sheet.getRow(row).getCell(col) == null)
		sheet.getRow(row).createCell(col);
	    XSSFCell cell = sheet.getRow(row).getCell(col);

	    try {
		if (cell.getRichStringCellValue().getString().trim().equalsIgnoreCase(str))
		    return row;
	    } catch (Exception ex) {}
		
	}//for
	return -1; //if not found
    }//findColumnIndex(XSSFSheet, String, int)    
}
