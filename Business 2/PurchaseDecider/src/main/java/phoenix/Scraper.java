package phoenix;

import java.io.*;
import java.nio.file.Files;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.commons.io.FileUtils;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.Date;
import java.text.SimpleDateFormat;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class Scraper {
    private static File tempExcelDir = new File("src/main/resources/tempExcel");
    //    private static String bulqUrl = "https://www.bulq.com/lots/search/?category=Consumer%20Electronics&condition%5B%5D=Brand%20New";
    private static String bulqUrl = "https://www.bulq.com/lots/search/?condition%5B%5D=Brand%20New&lot_size%5B%5D=Case&min_price=0&max_price=600";
    private static final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36";

    //    public static DoubleProperty progress = new SimpleDoubleProperty(0);
    
    private static Thread gather = new Thread(); //used in secureConnect()

    private static int i; //used in scrapeBulqDocument() lambda
    
    public static void scrapeBulq(ArrayList<String[]> proxies) throws IOException{
	Random r = new Random();
	clearDirectory(tempExcelDir);
	
	//Connects to Brand New Consumer Electronics in Case Size
	//	Document doc = Jsoup.connect(bulqUrl).userAgent(userAgent).get();
	Document doc = secureConnect(bulqUrl, proxies, r);
	scrapeBulqDocument(doc, proxies);

    }//scrapeBulq()

    private static void scrapeBulqDocument(Document d, ArrayList<String[]> proxies) throws IOException{
	Elements lots = d.select("a.pallet-tile__link");
	File[] files = new File("src/main/resources/tempExcel").listFiles();
	Random r = new Random();

	int numLots = Integer.parseInt(d.selectFirst("span.results-header__count").ownText());
	System.out.println(numLots);
	System.out.println((1 / numLots));
	
	i = files.length + 1;
	for (Element lot : lots) {
	    Thread t = new Thread(() -> {
		    try {

			Document lotDoc = secureConnect(lot.attr("abs:href"), proxies, r);

			Elements x = lotDoc.select("div.pricing__total-cost");
			if (x.size() > 0) {
			    String price = x.get(0).html();
	    
			    Element manifestLink = lotDoc.select("a.pallet-manifest-link__btn").get(0);
			    String linkStr = manifestLink.attr("abs:href");
			    FileUtils.copyURLToFile(new URL(linkStr), new File("src/main/resources/tempExcel/Manifest" + i++ + " " + price + ".xlsx"));
			} else {
			    System.out.println(lot.html());		    
			    System.out.println("Price not found! Discontinued lot?");
			}//if-else

			//			Thread.sleep(1000);
		    } catch (Exception ex) {
			ex.printStackTrace();
		    }
		    
		    //		    System.out.println("setting to " + (progress.get() + (double)(1 / numLots)));
		    //		    progress.set(progress.get() + 0.2);
		});
	    t.setDaemon(true);
	    t.start();
	}//for

	//warning: hacky code that essentially recursively scrapes the following page
	Elements pages = d.select("div.pagination");
	for (Element page : pages) {
	    if (page.text().contains("Next")) {
		String currentUrl = d.location();
		String pageLink;
		if (currentUrl.contains("&page=")) {
		    int nextPage = Integer.parseInt(currentUrl.substring(currentUrl.indexOf("&page=") + 6)) + 1;
		    pageLink = bulqUrl + "&page=" + nextPage;
		} else {
		    pageLink = bulqUrl + "&page=1";
		}//if-else
		scrapeBulqDocument(secureConnect(pageLink, proxies, r), proxies);
	    }//if
	}//for	
    }//scrapeBulqDocument(Document d)


    private static void clearDirectory(File file) throws IOException{
	while(tempExcelDir.listFiles().length != 0) {
	    if (!tempExcelDir.listFiles()[0].delete())
		clearDirectory(tempExcelDir.listFiles()[0]);
	}//while
    }//clearDirectory(File)

    public static Item scrapeAmazon(int qty, String upc, ArrayList<String[]> proxies) throws IOException, InterruptedException{
	double minPrice, avgShipPrice, rating;
	int numRatings;
	Random r = new Random();
	SimpleDateFormat sdf = new SimpleDateFormat("kk:mm:ss:SSS");

	String searchUrl = "https://www.amazon.com/s/ref=nb_sb_noss?url=search-alias%3Daps&field-keywords=" + upc + "&sort=price-asc-rank";

	Document doc = secureConnect(searchUrl, proxies, r);

	
	//finding rating values
	try {

	    String ratingStr = doc.selectFirst("i.a-icon.a-icon-star").selectFirst("span.a-icon-alt").text();
	    rating = Double.parseDouble(ratingStr.split(" ")[0]);

	    numRatings = Integer.parseInt(doc.select("div.a-column.a-span5.a-span-last").select("div.a-row.a-spacing-mini").select("a.a-size-small.a-link-normal.a-text-normal").first().text().replaceAll(",",""));

	} catch (NullPointerException npe) { //if no ratings

	    rating = 0;
	    numRatings = 0;
	}//try-catch

	Element pricesElement = doc.selectFirst("a.a-size-small.a-link-normal.a-text-normal:contains(offers)");

	if (pricesElement == null) pricesElement = doc.selectFirst("a.a-link-normal.a-text-normal:contains(Other Sellers)");

	//this ensures it is the first listing that is used
	if (pricesElement != null) {
	    Elements ancestors = pricesElement.parents();
	    boolean isFirstListing = false;
	    for (Element ancestor : ancestors)
		if (ancestor.id().equals("result_0")) isFirstListing = true;
	    if (!isFirstListing) pricesElement = null;
	}//if
	if (pricesElement != null) {
	    //Creating arraylist of prices for items of New-condition
	    ArrayList<Double> newPrices = new ArrayList<>();
	    String pricesLink = pricesElement.attr("href");
	    System.out.println(sdf.format(new Date()) + pricesLink);
	    Document priceDoc = secureConnect(pricesLink, proxies, r);

	    avgShipPrice = scrapeAmazonListings(priceDoc.select("div.a-row.a-spacing-mini.olpOffer"), newPrices, "new");
	    if (newPrices.size() == 0) { //only go to further pages if New price was not found
		Elements els = priceDoc.select("ul.a-pagination");
		if (els.size() != 0) {
		    for (Element e : els.get(0).children()) {
			if (e.className().contains("a"))
			    continue;
			String pageLink = e.children().get(0).attr("abs:href");
			System.out.println(pageLink);		    
			priceDoc = secureConnect(pageLink, proxies, r);
			scrapeAmazonListings(priceDoc.select("div.a-row.a-spacing-mini.olpOffer"), newPrices, "new");
		    }//for
		}//if
	    }//if
	    if (newPrices.size() != 0) { //Selecting the minimum price if New listings exist
		minPrice = newPrices.get(0);
		for (int i = 1; i < newPrices.size(); i++)
		    if (newPrices.get(i) < minPrice) minPrice = newPrices.get(i);
	    } else { //if no new listings, get avgShipPrice from all conditions, and use lowest price as minPrice
		avgShipPrice = scrapeAmazonListings(priceDoc.select("div.a-row.a-spacing-mini.olpOffer"), newPrices);		
		try {
		    minPrice = Double.parseDouble(doc.selectFirst("span.sx-price-whole").text() + "." + doc.selectFirst("sup.sx-price-fractional").text());
		} catch (NullPointerException npe) { //if amazon doesn't have a price, pick lowest of other conditions
		    if (newPrices.size() != 0) { //Selecting the minimum price if New listings exist
			minPrice = newPrices.get(0);
			for (int i = 1; i < newPrices.size(); i++)
			    if (newPrices.get(i) < minPrice) minPrice = newPrices.get(i);
		    } else {
			//			npe.printStackTrace();
			return new Item();
		    }
		}//try-catch
	    }//if-else
	} else {
	    avgShipPrice = 5; //if free shipping and there is no listing page, no way to know ship price
	    try {
		minPrice = Double.parseDouble(doc.selectFirst("span.sx-price-whole").text() + "." + doc.selectFirst("sup.sx-price-fractional").text());
	    } catch (NullPointerException npe) { //price not found, (item was not found)
		//		npe.printStackTrace();
		return new Item();
	    }//try-catch
	}//if-else

	if (minPrice - avgShipPrice >= 0)
	    return new Item(upc, qty, minPrice - avgShipPrice, rating, numRatings);
	else
	    return new Item(upc, qty, 0, rating, numRatings);
    }//scrapeAmazon(String)

    private static double scrapeAmazonListings(Elements elements, ArrayList<Double> prices, String condition) {
	double sumShipPrices = 0;
	double count = 0;
	for (Element listing : elements) {
	    if (listing.selectFirst("span.a-size-medium.olpCondition.a-text-bold").text().equalsIgnoreCase(condition)) {
		try {
		    String price = listing.selectFirst("span.a-size-large.a-color-price.olpOfferPrice.a-text-bold").text().substring(1);
		    Element e = listing.selectFirst("span.olpShippingPrice");
		    if (e != null) {
			String shipPrice = e.text().substring(1);
			prices.add(Double.parseDouble(price) + Double.parseDouble(shipPrice));
		    } else {
			prices.add(Double.parseDouble(price));
		    }//if-else
		} catch (NullPointerException npe) { //if price is not present
		    continue;
		}//try-catch
	    }//if
	    Element e = listing.selectFirst("span.olpShippingPrice");
	    if (e != null) {
		sumShipPrices += Double.parseDouble(e.text().substring(1));
		count++;
	    }
	}//for
	return sumShipPrices / count;
    }//scrapeAmazonListings(Elements, ArrayList<Double>)

    private static double scrapeAmazonListings(Elements elements, ArrayList<Double> prices) {
	double sumShipPrices = 0;
	double count = 0;
	for (Element listing : elements) {
	    try {
		String price = listing.selectFirst("span.a-size-large.a-color-price.olpOfferPrice.a-text-bold").text().substring(1);
		Element e = listing.selectFirst("span.olpShippingPrice");
		if (e != null) {
		    String shipPrice = e.text().substring(1);
		    prices.add(Double.parseDouble(price) + Double.parseDouble(shipPrice));
		} else {
		    prices.add(Double.parseDouble(price));
		}//if-else
	    } catch (NullPointerException npe) { //if price is not present
		continue;
	    }//try-catch
	}//for
	return sumShipPrices / count;	
    }//scrapeAmazonListings(Elements, ArrayList<Double>)    


    //should stop after gathering 10 IPs
    public static void scrapeProxies(ArrayList<String[]> proxies) throws IOException {
	Document doc = Jsoup.connect("https://www.us-proxy.org").userAgent(userAgent).get();

	try {
	    for (Element oddRow : doc.select("tr:not(:contains(IP Address)):contains(elite proxy)")) {
		Element https = oddRow.selectFirst("td.hx");
		if (https != null && https.text().equals("yes")) {
		    String[] str = { oddRow.selectFirst("*:eq(0)").text(), oddRow.selectFirst("*:eq(1)").text() };
		    if (str[0].contains("no") || str[0].contains("yes")
			|| str[1].contains("no") || str[1].contains("yes")) continue; //weirdly gets whole row
		    Thread test = new Thread(() -> {
			    try {
				Document d = Jsoup.connect("https://www.amazon.com").userAgent(userAgent).proxy(str[0], Integer.parseInt(str[1])).get();
				proxies.add(str);
				System.out.println("SUCCESS: " + str[0] + " - " + str[1]);
			    } catch (Exception ex) {
				//			    ex.printStackTrace();
				System.out.println("FAILED: " + str[0] + " - " + str[1]);
			    }//try-catch
			});
		    test.setDaemon(true);
		    test.start();
		}//if
		if (proxies.size() >= 30) break;
	    }//for

	    for (String[] s : proxies) System.out.println(s[0] + " - " + s[1]);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

    private static Document secureConnect(String url, ArrayList<String[]> proxies, Random r){
	int index = r.nextInt(proxies.size());
	String[] proxy = proxies.get(index);
	try {
	    System.out.println(proxy[0] + " --- " + url);
	    return Jsoup.connect(url).proxy(proxy[0], Integer.parseInt(proxy[1])).userAgent(userAgent).get();
	} catch (Exception ex) {
	    System.out.println("IP " + proxy[0] + " with port " + proxy[1] + " failed. Removing from list.");
	    proxies.remove(proxy);
	    System.out.println("Remaining Proxies: " + proxies.size());
	    if (proxies.size() <= 20) {
		if (!gather.isAlive()) { //ensures thread is not running already
		    gather = new Thread(() -> {
			    try {
				scrapeProxies(proxies);
				//				for (String[] arr : temp)
				//				    proxies.add(arr);
			    } catch (IOException ioe) {
				ioe.printStackTrace();
			    }
			}, "proxyGatherer");
		    gather.setDaemon(true);
		    gather.start();
		}//if
	    }//if
	    return secureConnect(url, proxies, r);
	}
    }

    public static void main(String[] args) {
	try {
	    ArrayList<String[]> proxies = new ArrayList<>();
	    scrapeProxies(proxies);
	    while (proxies.size() == 0) {
		Thread.sleep(1000);
		System.out.println("slept 1 second");
	    }
	    Scraper.scrapeBulq(proxies);
	    //	    Scraper.scrapeProxies();
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	
	/*
	try {
	    Document doc = Jsoup.connect("https://www.amazon.com/s/ref=nb_sb_noss?url=search-alias%3Daps&field-keywords=071249232972&sort=price-asc-rank").userAgent(userAgent).get();
	    System.out.println(doc.outerHtml());
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	*/
    }
}//Scraper
