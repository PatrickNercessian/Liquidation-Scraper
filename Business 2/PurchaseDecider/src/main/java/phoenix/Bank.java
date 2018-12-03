package phoenix;

import java.util.Properties;
import java.io.*;

public class Bank {
    private static double balance;
    private static Properties prop = new Properties();

    public static void manipulate(double amount) {
	try {
	    File file = new File("balance.properties");
	    if (file.createNewFile())
		prop.setProperty("balance", "0");
	    else
		prop.load(new FileInputStream("balance.properties"));
	    
	    balance = Integer.parseInt(prop.getProperty("balance")) + amount;
	    prop.setProperty("balance", "" + balance);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}//try-catch
    }//manipulate(double)

    public static double getBalance() {
	return balance;
    }//getBalance()
}
