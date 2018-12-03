package phoenix;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.text.*;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;

import java.util.ArrayList;

import java.io.*;

public class Launcher extends Application {

    private Stage mainStage;
    private Label title;

    public void start(Stage stage) {
	mainStage = stage;

	VBox mainVBox = new VBox(20);
	mainVBox.setStyle("-fx-background-color: linear-gradient(to bottom right, SpringGreen,"
			  + " White); -fx-padding: 40px;");
	mainVBox.setAlignment(Pos.CENTER);

	title = makeTitle("King Stock System");

	Button runBtn = new Button("Run");
	runBtn.setOnAction(e -> wholesaleScene());

	mainVBox.getChildren().addAll(title, runBtn);
	
	mainStage.setScene(new Scene(mainVBox));
	mainStage.sizeToScene();
	mainStage.show();
    }//start(Stage)

    public void wholesaleScene()  {
	Button scrapeBtn = new Button("Scrape");
	Button evaluateBtn = new Button("Evaluate Scraped Manifests");
	ProgressBar pb = new ProgressBar();
	//	pb.progressProperty().bind(Scraper.progress);

	evaluateBtn.disarm();

	ComboBox<String> siteCB = new ComboBox<>();
	siteCB.getItems().addAll("Bulq", "BlueLots");
	siteCB.getSelectionModel().selectFirst();
	title = makeTitle(siteCB.getValue());
	siteCB.setOnAction(e -> {
		title.setText(siteCB.getValue());
		mainStage.sizeToScene();
	    });
	
	scrapeBtn.setOnAction(e -> {
		try {
		    ArrayList<String[]> proxies = new ArrayList<>();
		    Scraper.scrapeProxies(proxies);
		    while (proxies.size() == 0) Thread.sleep(1000);
		    if (siteCB.getValue().equalsIgnoreCase("bulq"))
			Scraper.scrapeBulq(proxies);
		} catch (Exception ex) {
		    ex.printStackTrace();
		}//try-catch
		evaluateBtn.arm();
	    });


	evaluateBtn.setOnAction(e -> {
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
	
		for (File manifest : manifests) {
		    String name = manifest.getName();
		    double cost = Double.parseDouble(name.substring(name.indexOf("$") + 1, name.indexOf(".")).replaceAll(",", ""));
		    Lot lot = new Lot(cost, manifest);
		    if (proxies == null) break;
		    ArrayList<Thread> itemThreads = new ArrayList<>();
		    lot.populateFromManifest(proxies, scrapers, itemThreads);
		    lots.add(lot);
		}//for

		try {
		    while (scrapers.activeCount() != 0) Thread.sleep(1000);
		} catch (InterruptedException ie) {
		    ie.printStackTrace();
		}//try-catch
	    });
	
	VBox vbox = new VBox(20, title, siteCB, scrapeBtn, evaluateBtn, pb);
	vbox.setStyle("-fx-background-color: linear-gradient(to bottom right, SpringGreen,"
			  + " White); -fx-padding: 40px;");
	vbox.setAlignment(Pos.CENTER);	
	mainStage.setScene(new Scene(vbox));
    }//bulqScene()

    private Label makeTitle(String str) {
	Label label = new Label(str);
	label.setFont(new Font(50));
	label.setTextFill(Color.INDIGO);
	return label;
    }

    public static void main(String[] args) {
	try {
	    Application.launch(args);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }
}
