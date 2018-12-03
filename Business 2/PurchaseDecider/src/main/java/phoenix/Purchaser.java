package phoenix;

import java.util.ArrayList;
import java.util.Arrays;

public class Purchaser {
    private Lot[] arr;

    public Purchaser(ArrayList<Lot> lots) {
	this.arr = new Lot[lots.size()];
	this.arr = lots.toArray(this.arr);
    }

    /**
     * Filters out any lots with less than 10% profit margin, and sorts by Weighted Value
     */
    public Lot[] filterAndSort() {
	arr = Arrays.stream(arr).filter(lot -> {
		if (lot.calculateEstimatedProfitPct() < 10) return false;
		else return true;
	    }).sorted().toArray(Lot[]::new);
	return arr;
    }//filterAndSort()
    
    /**
     * Determines whether the lot is viable to be purchased
     *
     * @return true or false
     */
    public void purchase() {
	double capital = Bank.getBalance();
	for (Lot lot : arr) {
	    double lotCost = lot.getLotCost();
	    if (capital - lotCost < 0) //if out of money, stop buying
		break;

	    //code for buying
	}//for
    }//shouldBuy()

}
