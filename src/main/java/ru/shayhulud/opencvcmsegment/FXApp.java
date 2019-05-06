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
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import ru.shayhulud.opencvcmsegment.common.util.CollectionUtil;
import ru.shayhulud.opencvcmsegment.common.util.MathUtil;
import ru.shayhulud.opencvcmsegment.model.ImageInfo;
import ru.shayhulud.opencvcmsegment.model.Result;
import ru.shayhulud.opencvcmsegment.service.PictureService;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
public class FXApp extends Application {

	private static Double FRAME_WIDTH = 720D;
	private static Double FRAME_HEIGHT = 720D;

	public ImageInfo toAlgorythmsImage = null;
	public ImageInfo processedImage = null;
	public WritableImage handMarkers;

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
		VBox mainPanes = new VBox();

		Scene scene = new Scene(root);
		scene.setFill(Color.BEIGE);

		//Load styles
		File styles = new File("src/main/resources/fx/elements.css");
		scene.getStylesheets().add("file:///" + styles.getAbsolutePath().replace("\\", "/"));

		stage.setTitle("ImageView");

		//MENU
		HBox menuBox = new HBox();

		//MENU-LEFT
		//TODO: сделать кнопки с предобработкой (медианный фильтр там или усиление резкозти)
		VBox inputMenuBox = new VBox();
		inputMenuBox.setMinWidth(FRAME_WIDTH);

		HBox imageSelectBox = new HBox();
		//Label for select picture button
		Label iiSelectLabel = new Label("Select image");
		//Select picture button
		FileChooser fileChooser = new FileChooser();
		Button iiSelectButton = new Button("browse");
		iiSelectButton.getStyleClass().addAll("select-control");

		//Separator
		Separator iiSelectSeparator_1 = new Separator();
		iiSelectSeparator_1.setOrientation(Orientation.HORIZONTAL);
		iiSelectSeparator_1.getStyleClass().addAll("separator");

		//MANAGE
		//TODO: Сделать настраиваемый радиус ручного маркера
		HBox manageBox = new HBox();
		Button resetMarkersButton = new Button("reset markers");
		resetMarkersButton.getStyleClass().addAll("select-control");

		//Separator
		Separator iiSelectSeparator_2 = new Separator();
		iiSelectSeparator_2.setOrientation(Orientation.HORIZONTAL);
		iiSelectSeparator_2.getStyleClass().addAll("separator");

		//ALGORYTHMS
		HBox algorythmsSelectBox = new HBox();
		Button colorWshedButton = new Button("color wshed");
		colorWshedButton.getStyleClass().addAll("select-control");
		Button handWshedButton = new Button("hand wshed");
		handWshedButton.getStyleClass().addAll("select-control");
		Separator vAlgSeparator_1 = new Separator(Orientation.VERTICAL);
		Button shapeWshedButton = new Button("shape wshed");
		shapeWshedButton.getStyleClass().addAll("select-control");
		Separator vAlgSeparator_2 = new Separator(Orientation.VERTICAL);
		Button brightDepthButton = new Button("bright dpth");
		brightDepthButton.getStyleClass().addAll("select-control");
		Separator vAlgSeparator_3 = new Separator(Orientation.VERTICAL);
		Button notConnectedButton = new Button("not connected");
		notConnectedButton.getStyleClass().addAll("select-control");

		//Separator
		Separator iiSelectSeparator_3 = new Separator();
		iiSelectSeparator_3.setOrientation(Orientation.HORIZONTAL);
		iiSelectSeparator_3.getStyleClass().addAll("separator");

		//PARAMS
		HBox paramsBox = new HBox();
		Label depthParamLabel = new Label("Depth:");
		TextField depthCountInput = new TextField();
		depthCountInput.setPromptText("1");
		depthCountInput.getStyleClass().addAll("select-control");

		//MENU-RIGHT
		VBox outputMenuBox = new VBox();
		outputMenuBox.setMinWidth(FRAME_WIDTH);

		//TODO: Кнопки перелистывания результатов
		HBox outputSelectBox = new HBox();
		Label oiSelectLabel = new Label("Select step:");
		ChoiceBox<String> oiDropDownList = new ChoiceBox<>();
		oiDropDownList.getStyleClass().addAll("select-control");
		Separator vResultsSeparator_1 = new Separator(Orientation.VERTICAL);
		Button saveResultsButton = new Button("Save");
		saveResultsButton.getStyleClass().addAll("select-control");

		//IMAGES
		HBox imagesBox = new HBox();

		//LEFT-IMAGES
		HBox inputImagesBox = new HBox();
		inputImagesBox.getStyleClass().addAll("image-bordered");
		Pane iifp = new Pane();
		//Show input image
		ImageView inputImageView = new ImageView();
		inputImageView.setPreserveRatio(true);
		inputImageView.setFitWidth(FRAME_WIDTH);
		inputImageView.setFitHeight(FRAME_HEIGHT);
		//Transparent iview for drawing
		ImageView markersDrawingImageView = new ImageView();
		markersDrawingImageView.setPreserveRatio(true);
		markersDrawingImageView.setFitWidth(FRAME_WIDTH);
		markersDrawingImageView.setFitHeight(FRAME_HEIGHT);

		//RIGHT-IMAGES
		HBox outputImagesBox = new HBox();
		outputImagesBox.getStyleClass().addAll("image-bordered");

		ImageView outputImageView = new ImageView();
		outputImageView.setPreserveRatio(true);
		outputImageView.setFitWidth(FRAME_WIDTH);
		outputImageView.setFitHeight(FRAME_HEIGHT);

		//----------------------//
		//------SET LAYOUT------//
		//----------------------//

		root.getChildren().add(mainPanes);
		mainPanes.getChildren().addAll(
			menuBox,
			imagesBox
		);
		//MENU
		menuBox.getChildren().addAll(
			inputMenuBox,
			outputMenuBox
		);
		//INPUT MENU
		inputMenuBox.getChildren().addAll(
			imageSelectBox,
			iiSelectSeparator_1,
			manageBox,
			iiSelectSeparator_2,
			algorythmsSelectBox,
			iiSelectSeparator_3,
			paramsBox
		);
		imageSelectBox.getChildren().addAll(
			iiSelectLabel,
			iiSelectButton
		);
		manageBox.getChildren().addAll(
			resetMarkersButton
		);
		algorythmsSelectBox.getChildren().addAll(
			colorWshedButton,
			handWshedButton,
			vAlgSeparator_1,
			shapeWshedButton,
			vAlgSeparator_2,
			brightDepthButton,
			vAlgSeparator_3,
			notConnectedButton
		);
		paramsBox.getChildren().addAll(
			depthParamLabel,
			depthCountInput
		);
		//OUTPUT MENU
		outputMenuBox.getChildren().addAll(outputSelectBox);
		outputSelectBox.getChildren().addAll(
			oiSelectLabel,
			oiDropDownList,
			vResultsSeparator_1,
			saveResultsButton
		);

		//IMAGES
		imagesBox.getChildren().addAll(
			inputImagesBox,
			outputImagesBox
		);
		//INPUT IMAGES
		inputImagesBox.getChildren().addAll(iifp);
		iifp.getChildren().addAll(inputImageView, markersDrawingImageView);
		//OUTPUT IMAGES
		outputImagesBox.getChildren().addAll(outputImageView);

		//----------------------//
		//-----SET HANDLERS-----//
		//----------------------//

		//OPEN IMAGE
		iiSelectButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					File file = fileChooser.showOpenDialog(stage);
					try {
						if (file != null) {
							processedImage = pictureService.readPicture(file);
							toAlgorythmsImage = processedImage.clone();
							inputImageView.setImage(pictureService.mat2Image(processedImage.getMat()));

							resetHandMarkers(markersDrawingImageView, inputImageView);
							log.info("new writableImage size is {}x{}", handMarkers.getWidth(), handMarkers.getHeight());
						}
					} catch (IOException ex) {
						log.error("error while open image", ex);
						return;
					}
				}
			}
		);

		//COLOR WATERSHED
		colorWshedButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (processedImage != null && toAlgorythmsImage != null) {
						toAlgorythmsImage = processedImage.clone();
						toAlgorythmsImage = pictureService.colorAutoMarkerWatershed(toAlgorythmsImage);
						oiDropDownList.setItems(FXCollections.observableArrayList(
							toAlgorythmsImage.getResults().stream()
								.map(Result::getStepName)
								.collect(Collectors.toList())
						));
						oiDropDownList.setValue(CollectionUtil.getLastOf(oiDropDownList.getItems()));
					}
				}
			}
		);

		//HAND WATERSHED
		handWshedButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (processedImage != null && toAlgorythmsImage != null && handMarkers != null) {
						Mat handMarkersMat = pictureService.image2Mat(handMarkers);
						handMarkersMat = pictureService.bwMat(handMarkersMat);
						toAlgorythmsImage = processedImage.clone();
						toAlgorythmsImage = pictureService.handMarkerWatershed(toAlgorythmsImage, handMarkersMat);
						oiDropDownList.setItems(FXCollections.observableArrayList(
							toAlgorythmsImage.getResults().stream()
								.map(Result::getStepName)
								.collect(Collectors.toList())
						));
						oiDropDownList.setValue(CollectionUtil.getLastOf(oiDropDownList.getItems()));
					}
				}
			}
		);

		shapeWshedButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (processedImage != null && toAlgorythmsImage != null && handMarkers != null) {
						toAlgorythmsImage = processedImage.clone();
						toAlgorythmsImage = pictureService.shapeAutoMarkerWatershed(toAlgorythmsImage);
						oiDropDownList.setItems(FXCollections.observableArrayList(
							toAlgorythmsImage.getResults().stream()
								.map(Result::getStepName)
								.collect(Collectors.toList())
						));
						oiDropDownList.setValue(CollectionUtil.getLastOf(oiDropDownList.getItems()));
					}
				}
			}
		);

		brightDepthButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (processedImage != null && toAlgorythmsImage != null && handMarkers != null) {
						toAlgorythmsImage = processedImage.clone();
						toAlgorythmsImage = pictureService.brightDepth(
							toAlgorythmsImage,
							Integer.parseInt(depthCountInput.getText())
						);
						oiDropDownList.setItems(FXCollections.observableArrayList(
							toAlgorythmsImage.getResults().stream()
								.map(Result::getStepName)
								.collect(Collectors.toList())
						));
						oiDropDownList.setValue(CollectionUtil.getLastOf(oiDropDownList.getItems()));
					}
				}
			}
		);

		notConnectedButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (processedImage != null && toAlgorythmsImage != null && handMarkers != null) {
						toAlgorythmsImage = processedImage.clone();
						toAlgorythmsImage = pictureService.notConnectedMarkers(
							toAlgorythmsImage,
							Integer.parseInt(depthCountInput.getText())
						);
						oiDropDownList.setItems(FXCollections.observableArrayList(
							toAlgorythmsImage.getResults().stream()
								.map(Result::getStepName)
								.collect(Collectors.toList())
						));
						oiDropDownList.setValue(CollectionUtil.getLastOf(oiDropDownList.getItems()));
					}
				}
			}
		);

		saveResultsButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (processedImage != null && toAlgorythmsImage != null && handMarkers != null
						&& !toAlgorythmsImage.getResults().isEmpty()) {
						pictureService.saveResultsToFS(toAlgorythmsImage);
					}
				}
			}
		);

		//RESET MARKERS
		resetMarkersButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (processedImage != null) {
						resetHandMarkers(markersDrawingImageView, inputImageView);
						log.info("reset writableImage [{}x{}]", handMarkers.getWidth(), handMarkers.getHeight());
					}
				}
			});

		//ONCHANGE RESULTS DROPDOWN LIST
		ChangeListener<String> changeListener = new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (newValue != null && !newValue.isEmpty()) {
					toAlgorythmsImage.getResults().stream()
						.filter(_result -> _result.getStepName().equals(newValue))
						.findFirst()
						.ifPresent(_result -> {
							outputImageView.setImage(pictureService.result2Image(_result));
							log.info("select result {} named {}", _result.getStep(), _result.getStepName());
						});
				}
			}
		};
		oiDropDownList.getSelectionModel().selectedItemProperty().addListener(changeListener);

		inputImageView.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent me) {
				double x = me.getX();
				double y = me.getY();
				if (x < 0 || x > FRAME_WIDTH || y < 0 || y > FRAME_HEIGHT) {
					return;
				}
				log.debug("clicked on {}x{}", x, y);
				//TODO: Сделать настраиваемый радиус ручного маркера
				drawMarkerWithRadius(
					MathUtil.normalize(x, FRAME_WIDTH, handMarkers.getWidth()).intValue(),
					MathUtil.normalize(y, FRAME_HEIGHT, handMarkers.getHeight()).intValue(),
					2,
					handMarkers,
					new Color(1, 1, 1, 1)
				);
				me.consume();
			}
		});
		inputImageView.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent me) {
				double x = me.getX();
				double y = me.getY();
				if (x < 0 || x > FRAME_WIDTH || y < 0 || y > FRAME_HEIGHT) {
					return;
				}
				//TODO: Сделать настраиваемый радиус ручного маркера
				drawMarkerWithRadius(
					MathUtil.normalize(x, FRAME_WIDTH, handMarkers.getWidth()).intValue(),
					MathUtil.normalize(y, FRAME_HEIGHT, handMarkers.getHeight()).intValue(),
					2,
					handMarkers,
					new Color(1, 1, 1, 1)
				);
			}
		});

		// force the field to be numeric only
		depthCountInput.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue,
			                    String newValue) {
				if (!newValue.matches("\\d*")) {
					newValue = newValue.replaceAll("[^\\d]", "");
				}
				if (newValue.equals("")) {
					depthCountInput.setText(newValue);
					return;
				}
				int newIntValue = Integer.parseInt(newValue);
				if (newIntValue < 1) {
					newIntValue = 1;
					newValue = Integer.toString(newIntValue);
				}
				depthCountInput.setText(newValue);
			}
		});

		//----------------------//
		//---SET START CONTENT--//
		//----------------------//

		configureFileChooser(fileChooser);
		depthCountInput.setText(Integer.toString(4));

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

	private void configureFileChooser(final FileChooser fileChooser) {
		fileChooser.setTitle("View Pictures");
		fileChooser.setInitialDirectory(
			new File(System.getProperty("user.home"))
		);
		fileChooser.getExtensionFilters().clear();
		fileChooser.getExtensionFilters().addAll(
			new FileChooser.ExtensionFilter("JPG", "*.jpg"),
			new FileChooser.ExtensionFilter("PNG", "*.png")
		);
	}

	private void drawMarkerWithRadius(int x, int y, int r, WritableImage image, Color color) {
		PixelWriter pw = image.getPixelWriter();
		for (int i = x - r; i <= x + r; i++) {
			if (i >= 0) {
				for (int j = y - r; j <= y + r; j++) {
					if (j >= 0) {
						pw.setColor(i, j, color);
					}
				}
			}
		}
	}

	private void resetHandMarkers(ImageView handMarkersContainer, ImageView sourceContainer) {
		handMarkersContainer.setFitWidth(sourceContainer.fitWidthProperty().doubleValue());
		handMarkersContainer.setFitHeight(sourceContainer.fitHeightProperty().doubleValue());

		handMarkers = new WritableImage(
			(int) sourceContainer.getImage().getWidth(),
			(int) sourceContainer.getImage().getHeight()
		);
		handMarkersContainer.setImage(handMarkers);
	}
}
