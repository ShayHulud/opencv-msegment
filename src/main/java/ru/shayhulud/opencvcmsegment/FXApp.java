package ru.shayhulud.opencvcmsegment;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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

	private static Double BORDER = 10D;
	private static Double FRAME_WIDTH = 720D;
	private static Double FRAME_HEIGHT = 720D;

	@Override
	public void start(Stage stage) {

		List<String> params = getParameters().getRaw();
		PictureService pictureService = new PictureService();
		ImageInfo ii = null;
		ii = pictureService.colorAutoMarkerWatershed(params.get(0), params.get(1), params.get(2));
		if (ii == null) {
			return;
		}

		// load the image
		Image inputImage = pictureService.mat2Image(ii.getMat());

		// simple displays ImageView the image as is
		ImageView inputImageView = new ImageView();
		inputImageView.setImage(inputImage);
		inputImageView.setCache(true);
		inputImageView.setPreserveRatio(true);
		inputImageView.setFitWidth(FRAME_WIDTH);
		inputImageView.setFitHeight(FRAME_HEIGHT);

		Image outputImage = pictureService.mat2Image(ii.getResults().get(ii.getResults().size() - 1).getMat());

		ImageView outputImageView = new ImageView();
		outputImageView.setImage(outputImage);
		outputImageView.setPreserveRatio(true);
		outputImageView.setFitWidth(FRAME_WIDTH);
		outputImageView.setFitHeight(FRAME_HEIGHT);

		Group root = new Group();

		Scene scene = new Scene(root);
		scene.setFill(Color.BLACK);

		HBox inputBox = new HBox();
		inputBox.getChildren().add(inputImageView);
		inputBox.setStyle(Styles.BORDER_STYLE);
		root.getChildren().add(inputBox);

		HBox outputBox = new HBox();
		outputBox.getChildren().add(outputImageView);
		outputBox.setStyle(Styles.BORDER_STYLE);
		outputBox.setLayoutX(FRAME_WIDTH + BORDER);
		root.getChildren().add(outputBox);

		stage.setTitle("ImageView");
		stage.setWidth(FRAME_WIDTH * 2);
		stage.setHeight(FRAME_HEIGHT);
		stage.setScene(scene);
		stage.sizeToScene();
		stage.show();
	}

	public static void main(String[] args) {
		OpenCV.loadLocally();
		Application.launch(args);
	}
}
