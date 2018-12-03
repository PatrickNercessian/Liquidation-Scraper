package phoenix;

import java.util.ArrayList;

public class Test {

    public static void main(String[] args) {
	Item item;
	ArrayList<String[]> proxies = new ArrayList<>();
	try {
	    Scraper.scrapeProxies(proxies);
	    while (proxies.size() < 3) Thread.sleep(1000);
	    item = Scraper.scrapeAmazon(1, "9780375823022", proxies);
	    System.out.println(item);	    
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }
}
