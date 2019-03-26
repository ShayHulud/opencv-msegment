package ru.shayhulud.opencvcmsegment;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import ru.shayhulud.opencvcmsegment.model.ImageInfo;
import ru.shayhulud.opencvcmsegment.model.Result;
import ru.shayhulud.opencvcmsegment.service.PictureService;
import ru.shayhulud.opencvcmsegment.util.CollectionUtil;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
public class FXApp extends Application {

	private static Double FRAME_WIDTH = 720D;
	private static Double FRAME_HEIGHT = 720D;

	public ImageInfo processedImage = null;

	@Override
	public void start(Stage stage) {

		final PictureService pictureService = new PictureService();

		//----------------------//
		//-PREPARE START CONTENT//
		//----------------------//

		//----------------------//
		//----DEFINE LAYOUT-----//
		//----------------------//

		Group root = new Group();
		HBox mainPanes = new HBox();

		Scene scene = new Scene(root);
		scene.setFill(Color.BEIGE);

		//Load styles
		File styles = new File("src/main/resources/fx/elements.css");
		scene.getStylesheets().add("file:///" + styles.getAbsolutePath().replace("\\", "/"));

		stage.setTitle("ImageView");

		//LEFT PANE

		VBox inputBox = new VBox();

		HBox imageSelectBox = new HBox();
		//Label for select picture button
		Label iiSelectLabel = new Label("Select image");
		//Select picture button
		FileChooser fileChooser = new FileChooser();
		Button iiSelectButton = new Button("browse");
		iiSelectButton.getStyleClass().addAll("select-control");
		//Separator
		Separator iiSelectSeparator = new Separator();
		iiSelectSeparator.setOrientation(Orientation.VERTICAL);
		iiSelectSeparator.getStyleClass().addAll("separator");
		//Color wshed button
		Button colorWshedButton = new Button("color wshed");
		colorWshedButton.getStyleClass().addAll("select-control");

		HBox iivBox = new HBox();
		iivBox.getStyleClass().addAll("image-bordered");

		ImageView inputImageView = new ImageView();
		inputImageView.setPreserveRatio(true);
		inputImageView.setFitWidth(FRAME_WIDTH);
		inputImageView.setFitHeight(FRAME_HEIGHT);
		inputImageView.getStyleClass().addAll("image-bordered");

		//RIGHT PANE

		VBox outputBox = new VBox();

		HBox outputSelectBox = new HBox();
		Label oiSelectLabel = new Label("Select step:");
		ChoiceBox<String> oiDropDownList = new ChoiceBox<>();
		oiDropDownList.getStyleClass().addAll("select-control");

		ChangeListener<String> changeListener = new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (newValue != null && !newValue.isEmpty()) {
					//TODO: Сделать переключение результатов
				}
			}
		};
		oiDropDownList.getSelectionModel().selectedItemProperty().addListener(changeListener);


		HBox oivBox = new HBox();
		oivBox.getStyleClass().addAll("image-bordered");

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

		inputBox.getChildren().addAll(imageSelectBox, iivBox);
		imageSelectBox.getChildren().addAll(
			iiSelectLabel,
			iiSelectButton,
			iiSelectSeparator,
			colorWshedButton
		);
		iivBox.getChildren().addAll(inputImageView);

		outputBox.getChildren().addAll(outputSelectBox, oivBox);
		outputSelectBox.getChildren().addAll(oiSelectLabel, oiDropDownList);
		oivBox.getChildren().addAll(outputImageView);

		//----------------------//
		//-----SET HANDLERS-----//
		//----------------------//

		iiSelectButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					configureFileChooser(fileChooser);
					File file = fileChooser.showOpenDialog(stage);
					try {
						if (file != null) {
							processedImage = pictureService.readPicture(file);
							inputImageView.setImage(pictureService.mat2Image(processedImage.getMat()));
						}
					} catch (IOException ex) {
						log.error("error while open image", ex);
						return;
					}
				}
			}
		);

		colorWshedButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (processedImage != null) {
						processedImage = pictureService.colorAutoMarkerWatershed(processedImage);
						oiDropDownList.setItems(FXCollections.observableArrayList(
							processedImage.getResults().stream()
								.map(Result::getStepName)
								.collect(Collectors.toList())
						));
						oiDropDownList.setValue(CollectionUtil.getLastOf(oiDropDownList.getItems()));
						outputImageView.setImage(
							pictureService.mat2Image(
								CollectionUtil.getLastOf(processedImage.getResults()).getMat()
							)
						);
					}
				}
			}
		);

		//----------------------//
		//---SET START CONTENT--//
		//----------------------//

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

	private static void configureFileChooser(
		final FileChooser fileChooser) {
		fileChooser.setTitle("View Pictures");
		fileChooser.setInitialDirectory(
			new File(System.getProperty("user.home"))
		);
		fileChooser.getExtensionFilters().addAll(
			new FileChooser.ExtensionFilter("All Images", "*.*"),
			new FileChooser.ExtensionFilter("JPG", "*.jpg"),
			new FileChooser.ExtensionFilter("PNG", "*.png")
		);
	}
}
