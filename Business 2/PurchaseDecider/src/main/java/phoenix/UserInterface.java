package phoenix;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.text.*;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;

public class UserInterface extends Application {
    private Stage mainStage;
    private Scene mainMenu;
    private Button backBtn;
    
    @Override
    public void start(Stage stage) {
	backBtn = new Button("Back");
	backBtn.setOnAction(e -> {
		mainStage.setScene(mainMenu);
		mainStage.sizeToScene();
		mainStage.show();
	    });

	Label title = new Label("Phoenix Systems");
	title.setFont(new Font(50));
	title.setTextFill(Color.INDIGO);

	Button runBtn = new Button("Recommend Lots");

	Button openExcelBtn = new Button("Open Excel Data");

	Button alterBalanceBtn = new Button("Manipulate Bank Account Balance");
	//	alterBalanceBtn.setOnAction(e -> alterBalanceScene());
	
	VBox menuVbox = new VBox(20, title, runBtn, openExcelBtn, alterBalanceBtn);
	menuVbox.setStyle("-fx-backgroundcolor: linear-gradient(to bottom right, Teal, Cyan); "
			  + "-fx-padding: 50px;");
	menuVbox.setAlignment(Pos.CENTER);
	
	mainMenu = new Scene(menuVbox);
	mainStage = new Stage();
	mainStage.setScene(mainMenu);
	mainStage.sizeToScene();
	mainStage.show();
    }//start(Stage)

    /*
    /**
     * Sets mainStage to Scene pertaining to manipulating bank balance
     *
     * @return the scene
    public Scene alterBalanceScene() {
	
    }//alterBalanceScene()
    */

    public static void main(String[] args) {
	Application.launch(args);
    }//main(String[])
}
