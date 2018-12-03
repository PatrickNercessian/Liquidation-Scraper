package phoenix;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.*;
import org.apache.poi.xssf.usermodel.*;

//EITHER CREATE NEW CONSTRUCTOR FOR BID ITEMS OR COMBINE THEM BY DETERMINING MAXIMUM
//VIABLE PRICE (if flat cost, only purchase if lotCost is lower than maximum)
public class Lot extends ArrayList<Item> implements Comparable<Lot>{

    private double lotCost;
    private File manifest;
    private String id;

    /**
     * Creates a Lot object
     *
     * @param lot an ArrayList of Items which make up the Lot
     * @param lotCost the cost of the lot
     */
    public Lot(double lotCost, File manifest) {
	this.manifest = manifest;
	this.lotCost = lotCost;
	try {
	    this.id = new XSSFWorkbook(manifest).getSheetAt(0).getRow(1).getCell(0).getStringCellValue();
	} catch (Exception ex) {
	    this.id = "ID NOT FOUND";
	    ex.printStackTrace();
	}
    }//Purchaser(double, double, double)



    
    public int compareTo(Lot l) {
	double lotWeighted = calculateLotWeight();
	double lWeighted = l.calculateLotWeight();
	
	if (lotWeighted > lWeighted) return 1;
	else if (lotWeighted < lWeighted) return -1;
	else return 0;
    }//compareTo(Lot)

    /**
     * Calculates the sum of estimated prices for the lot
     *
     * @return the sum
     */
    public double calculateEstimatedPriceSum() {
	double sum = 0;
	for (Item item : this) {
	    double price = item.getEstimatedPrice() * item.getQty();
	    if (price > 0) {
		sum += price;
	    }
	}//for
	return Math.round(sum * 100.0) / 100.0;
    }//calculateEstimatedPriceSum()

    /**
     * Calculates the sum of weighted prices for the lot
     *
     * @return the sum
     */    
    public double calculateWeightedSum() {
	double sum = 0;
	for (Item item : this) {
	    double weightedValue = item.getWeightedValue() * item.getQty();
	    if (weightedValue > 0)
		sum += weightedValue;
	}//for
	return Math.round(sum * 100.0) / 100.0;
    }//calculateWeightedSum()
 
    /**
     * Calculates the percent of items found on either Amazon or eBay
     *
     * @return the percentage (in decimal form)
     */   
    public int numFound() {
	int num = 0;
	for (Item item : this)
	    if (item.getEstimatedPrice() > 0) num++;
	return num;
    }//calculatePercentFound()

    public double calculateEstimatedProfitPct() {
	return Math.round((((calculateEstimatedPriceSum()) / lotCost * 100) - 100) * 100.0) / 100.0;
    }

    public double calculateLotWeight() {
	return calculateWeightedSum() / lotCost * 100;
    }

    public double getLotCost() {
	return lotCost;
    }

    public void populateFromManifest(ArrayList<String[]> proxies, ThreadGroup scrapers, ArrayList<Thread> itemThreads) {
	try {
	    int x = 0;
	    XSSFSheet sheet = new XSSFWorkbook(manifest).getSheet("Manifest by SKU");
	    int upcCol = ExcelHelper.findColumnIndex(sheet, "UPC", 0);
	    int qtyCol = ExcelHelper.findColumnIndex(sheet, "Quantity", 0);
	    for (int i = 1; i <= sheet.getLastRowNum(); i++){
		XSSFCell upcCell = sheet.getRow(i).getCell(upcCol);
		if (upcCell != null && !upcCell.getStringCellValue().equals("")) {//if not empty
		    XSSFCell qtyCell = sheet.getRow(i).getCell(qtyCol);
		    if (qtyCell != null && qtyCell.getNumericCellValue() != 0) { //if not empty
			while (scrapers.activeCount() >= 50) Thread.sleep(1000); //if 50 threads are active, wait until one finishes
			Thread t = new Thread(scrapers, () -> {
				try {
				    this.add(Scraper.scrapeAmazon((int)qtyCell.getNumericCellValue(), upcCell.getStringCellValue(), proxies));
				} catch (Exception ex) { //so that lots do not stop populating if an error occurs
				    this.add(new Item());
				    ex.printStackTrace();
				    System.out.println(upcCell.getStringCellValue());
				}
			    });
			itemThreads.add(t);
			t.setDaemon(true);
			t.start();
		    }//if
		}//if
	    }//for
	    Thread wait = new Thread(() -> {
		    System.out.println("butthole mcnbuttface");
		    for (int i = 0; i < itemThreads.size(); i++) {
			try {
			    itemThreads.get(i).join();
			} catch (Exception ex) {
			    ex.printStackTrace();
			}
		    }
		    System.out.println("weiner mcweinerface");		    
		    writeToFile();
		});
	    wait.setDaemon(true);
	    wait.start();
		
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	
    }

    public void writeToFile() {
	Date date = new Date();
	SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
	File dir = new File("src/main/resources/Analyzed Lots " + sdf.format(date));
	File file = new File("src/main/resources/Analyzed Lots " + sdf.format(date)
			     + "/Lot " + (Math.round(calculateLotWeight() * 100.0))
			     + ".txt");
	if (!dir.exists())
	    dir.mkdirs();

	FileWriter fw = null;
	BufferedWriter bw = null;
	try {
	    file.createNewFile();
	    
	    fw = new FileWriter(file, true);
	    bw = new BufferedWriter(fw);
	    bw.append(this + "\n\n\n\n\n-----------ITEMS-----------");
	    for (Item item : this)
		bw.append(item.toString());
	} catch (Exception ex) {
	    ex.printStackTrace();
	} finally {
	    try {
		if (bw != null) bw.close();
		if (fw != null) fw.close();
	    } catch (IOException ioe) {
		ioe.printStackTrace();
	    }//try-catch
	}//try-catch-finally
    }

    public String toString() {
	int numFound = numFound();
	String str = "\n\n\n----- Lot " + id + " -----";
	str += "Link: https://www.bulq.com/detail/" + id;
	str += "\nLot Weight: " + (Math.round(calculateLotWeight() * 100.0) / 100.0) + "%";
	str += "\nLot Cost: $" + this.lotCost;
	str += "\nEstimated Worth: $" + calculateEstimatedPriceSum();
	str += "\nEstimated Profit: " + calculateEstimatedProfitPct() + "%";
	str += "\nWeighted Worth: $" + calculateWeightedSum();
	str += "\n" + numFound + " out of " + size() + " items found (" + (Math.round(((double)numFound / size()) * 10000.0) / 100.0) + "%).";
	return str;
    }

    public static void main(String[] args) {
	int[] saleSpeedCount = new int[6]; //0: no_sell, 1: very_slow, etc.
	File[] manifests = new File ("src/main/resources/tempExcel").listFiles();
	ArrayList<Lot> lots = new ArrayList<>();
	ArrayList<String[]> proxies = new ArrayList<>();
	ThreadGroup scrapers = new ThreadGroup("Scrapers");

	try {
	    Scraper.scrapeProxies(proxies);
	    while (proxies.size() < 5) Thread.sleep(1000);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}//try-catch

	double avgWeight = 0;
	for (File manifest : manifests) {
	    String name = manifest.getName();
	    double cost = Double.parseDouble(name.substring(name.indexOf("$") + 1, name.indexOf(".")).replaceAll(",", ""));
	    Lot lot = new Lot(cost, manifest);
	    if (proxies == null) break;
	    ArrayList<Thread> itemThreads = new ArrayList<>();
	    lot.populateFromManifest(proxies, scrapers, itemThreads);
	    lots.add(lot);
	    
	    double avgWeightInLot = 0;
	    int numItemsInLot = lot.size();
	    for (Item i : lot) {
		/*
		switch (i.getSaleSpeed()) {
		case NO_SELL: saleSpeedCount[0]++;break;
		case VERY_SLOW: saleSpeedCount[1]++;break;
		case SLOW: saleSpeedCount[2]++;break;
		case MEDIUM: saleSpeedCount[3]++;break;
		case FAST: saleSpeedCount[4]++;break;
		case VERY_FAST: saleSpeedCount[5]++;break;
		*/
		if (i.weight != Double.NaN)
		    avgWeightInLot += i.weight;
		else
		    numItemsInLot--;
		//		}//switch
	    }
	    avgWeightInLot = avgWeightInLot / numItemsInLot;
	    avgWeight += avgWeightInLot;
	}//for
	avgWeight = avgWeight / manifests.length;
	

	try {
	    while (scrapers.activeCount() != 0) Thread.sleep(1000);
	} catch (InterruptedException ie) {
	    ie.printStackTrace();
	}

	for (Lot lot : lots)
	    System.out.println(lot);

	System.out.println("\n\n\n\n\n\n\n**********Recommended Lots:**********");
	Purchaser p = new Purchaser(lots);
	Lot[] goodLots = p.filterAndSort();
	for (Lot lot : goodLots) {
	    System.out.println(lot);
	    for (Item item : lot)
		System.out.println(item);
	}

	/*
	for (int i : saleSpeedCount)
	    System.out.println(i);
	*/
	System.out.println("Average Weight:" + avgWeight);

    }
}//Lot
