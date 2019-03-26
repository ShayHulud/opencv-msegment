package ru.shayhulud.opencvcmsegment;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import ru.shayhulud.opencvcmsegment.fx.Styles;
import ru.shayhulud.opencvcmsegment.model.ImageInfo;
import ru.shayhulud.opencvcmsegment.service.PictureService;

import java.util.List;

@Slf4j
public class FXApp extends Application {

	private static Double FRAME_WIDTH = 720D;
	private static Double FRAME_HEIGHT = 720D;

	@Override
	public void start(Stage stage) {

		//----------------------//
		//----DEFINE LAYOUT-----//
		//----------------------//

		Group root = new Group();
		HBox mainPanes = new HBox();

		Scene scene = new Scene(root);
		scene.setFill(Color.BLACK);
		stage.setTitle("ImageView");
		stage.setWidth(FRAME_WIDTH * 2);
		stage.setHeight(FRAME_HEIGHT);

		VBox inputBox = new VBox();
		inputBox.setStyle(Styles.BORDER_STYLE);

		VBox outputBox = new VBox();
		outputBox.setStyle(Styles.BORDER_STYLE);

		ImageView inputImageView = new ImageView();
		inputImageView.setPreserveRatio(true);
		inputImageView.setFitWidth(FRAME_WIDTH);
		inputImageView.setFitHeight(FRAME_HEIGHT);

		ImageView outputImageView = new ImageView();
		outputImageView.setPreserveRatio(true);
		outputImageView.setFitWidth(FRAME_WIDTH);
		outputImageView.setFitHeight(FRAME_HEIGHT);

		//----------------------//
		//------SET LAYOUT------//
		//----------------------//

		root.getChildren().add(mainPanes);
		mainPanes.getChildren().add(inputBox);
		mainPanes.getChildren().add(outputBox);
		inputBox.getChildren().add(inputImageView);
		outputBox.getChildren().add(outputImageView);


		//----------------------//
		//---SET START CONTENT--//
		//----------------------//

		List<String> params = getParameters().getRaw();
		PictureService pictureService = new PictureService();
		ImageInfo ii = null;
		ii = pictureService.colorAutoMarkerWatershed(params.get(0), params.get(1), params.get(2));
		if (ii == null) {
			return;
		}

		Image inputImage = pictureService.mat2Image(ii.getMat());
		inputImageView.setImage(inputImage);

		//TODO сделать выпадающий список.
		Image outputImage = pictureService.mat2Image(ii.getResults().get(ii.getResults().size() - 1).getMat());
		outputImageView.setImage(outputImage);

		//----------------------//
		//---------SHOW---------//
		//----------------------//

		stage.setScene(scene);
		stage.sizeToScene();
		stage.show();
	}

	public static void main(String[] args) {
		OpenCV.loadLocally();
		Application.launch(args);
	}
}
