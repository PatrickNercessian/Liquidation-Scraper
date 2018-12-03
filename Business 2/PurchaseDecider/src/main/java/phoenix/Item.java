package phoenix;

import java.util.ArrayList;

public class Item {

    public enum SaleSpeed {NOT_FOUND, NO_SELL, VERY_SLOW, SLOW, MEDIUM, FAST, VERY_FAST}

    private String upc;
    private int qty;
    private double estimatedPrice;
    private double weightedValue;
    private SaleSpeed saleSpeed;
    protected double weight; //temporary variable to see spread of weight

    /**
     * Creates an Item object if it cannot be found on Amazon or eBay
     */
    public Item() {
	this.estimatedPrice = -1;
	this.weightedValue = -1;
	this.saleSpeed = SaleSpeed.NOT_FOUND;
    }

    /**
     * Creates an Item object for Amazon
     *
     * @param estimatedPrice the estimate for the price we can sell the item
     * @param rating the rating out of 5 on Amazon
     * @param numRatings the number of ratings on Amazon
     */
    public Item(String upc, int qty, double estimatedPrice, double rating, int numRatings) {
	double ratingValue;

	this.upc = upc;
	this.qty = qty;
	this.estimatedPrice = estimatedPrice;
	
	ratingValue = rating * numRatings;

	/*	
	if (ratingValue <= 5)
	    saleSpeed = SaleSpeed.NO_SELL;
	else if (ratingValue > 5 && ratingValue <= 35)
	    saleSpeed = SaleSpeed.VERY_SLOW;
	else if (ratingValue > 35 && ratingValue <= 115)
	    saleSpeed = SaleSpeed.SLOW;
	else if (ratingValue > 115 && ratingValue <= 700)
	    saleSpeed = SaleSpeed.MEDIUM;
	else if (ratingValue > 700 && ratingValue <= 2000)
	    saleSpeed = SaleSpeed.FAST;
	else
	    saleSpeed = SaleSpeed.VERY_FAST;

	weightedValue = calculateWeightedValue(saleSpeed, estimatedPrice);
	*/
	weightedValue = calculateWeightedValue(ratingValue, estimatedPrice);
    }
    /*
    /**
     * Creates an Item object for eBay
     *
     * @param estimatedPrice the estimate for the price we can sell the item
     * @param numSold the number of items sold on eBay in the last month
     
    public Item(int qty, double estimatedPrice, int numSold) {
	this.qty = qty;
	this.estimatedPrice = estimatedPrice;

	/*
	if (numSold == 0)
	    saleSpeed = SaleSpeed.NO_SELL;
	else if (numSold > 0 && numSold <= 2)
	    saleSpeed = SaleSpeed.VERY_SLOW;
	else if (numSold > 2 && numSold <= 6)
	    saleSpeed = SaleSpeed.SLOW;
	else if (numSold > 6 && numSold <= 15)
	    saleSpeed = SaleSpeed.MEDIUM;
	else if (numSold > 15 && numSold <= 25)
	    saleSpeed = SaleSpeed.FAST;
	else
	    saleSpeed = SaleSpeed.VERY_FAST;

	weightedValue = calculateWeightedValue(saleSpeed, estimatedPrice);	
	
	weightedValue = calculateWeightedValue(ratingValue, estimatedPrice);
    }
    */

    private double calculateWeightedValue(double ratingValue, double estimatedPrice) {
	if (ratingValue <= 10)
	    this.weight = 0;
	else
	    this.weight = Math.log(ratingValue) / Math.log(100);
	
	/*	
	switch (saleSpeed) {
	    case NO_SELL: weight = 0;break;
	    case VERY_SLOW: weight = 0.5;break;
	    case SLOW: weight = 0.75;break;
	    case MEDIUM: weight = 1;break;
	    case FAST: weight = 1.25;break;
	    case VERY_FAST: weight = 1.5;break;
	    default: weight = 0;
	}//switch
	*/
	return estimatedPrice * weight;
    }

    public String getUpc() {
	return upc;
    }
    public int getQty() {
	return qty;
    }
    public double getEstimatedPrice() {
	return estimatedPrice;
    }
    public double getWeightedValue() {
	return weightedValue;
    }
    public SaleSpeed getSaleSpeed() {
	return saleSpeed;
    }

    public String toString() {
	String str = "\n\nUPC: " + upc + "\nLink:";
	if (upc != null) str += "https://www.amazon.com/s/ref=nb_sb_noss?url=search-alias%3Daps&field-keywords=" + upc + "&sort=price-asc-rank";
	else str += "NOT FOUND";
	str += "\nQuantity: " + qty + "\nEstimated Price: " + estimatedPrice + "\nWeighted Value: " + weightedValue + "\nSale Speed: " + saleSpeed;
	return str;
    }

    public static void main(String[] args) {
	try {
	    ArrayList<String[]> proxies = new ArrayList<>();
	    Scraper.scrapeProxies(proxies);
	    while (proxies.size() < 5) Thread.sleep(1000);
	    System.out.println(Scraper.scrapeAmazon(1, "305210469990", proxies));
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }
}//Item
