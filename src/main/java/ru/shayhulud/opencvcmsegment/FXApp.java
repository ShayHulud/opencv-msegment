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
import javafx.scene.control.CheckBox;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import ru.shayhulud.opencvcmsegment.common.util.CollectionUtil;
import ru.shayhulud.opencvcmsegment.common.util.MathUtil;
import ru.shayhulud.opencvcmsegment.model.ImageInfo;
import ru.shayhulud.opencvcmsegment.model.Result;
import ru.shayhulud.opencvcmsegment.model.dic.AlgorithmOptions;
import ru.shayhulud.opencvcmsegment.service.CorrelationTestService;
import ru.shayhulud.opencvcmsegment.service.PictureService;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Класс функционала приложения в графическом интерфейсе.
 */
@Slf4j
public class FXApp extends Application {

	/**
	 * Ширина бокса кадра.
	 */
	private static Double FRAME_WIDTH = 720D;
	/**
	 * Высота бокса кадра.
	 */
	private static Double FRAME_HEIGHT = 720D;

	/**
	 * Объект информации об изображении, для алгоритмов.
	 */
	public ImageInfo toAlgorythmsImage = null;
	/**
	 * Объект информации об изображении, идеал, сегментированный человеком.
	 */
	public ImageInfo humanResultImage = null;
	/**
	 * Объект информации об изображении, для клонирования в изображение для алгоритмов.
	 */
	public ImageInfo processedImage = null;
	/**
	 * Холст для ручных маркеров.
	 */
	public WritableImage handMarkers;

	/**
	 * Сет опций.
	 */
	public final Set<AlgorithmOptions> preProcessOptions = new HashSet<>();

	public Map<String, ImageInfo> srcImages = null;
	public Map<String, ImageInfo> humanResultsImages = null;

	@Override
	public void start(Stage stage) {

		final CorrelationTestService testService = new CorrelationTestService();
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
		Button iiSelectButton = new Button("Browse");
		iiSelectButton.getStyleClass().addAll("select-control");
		Separator vInputSeparator_1 = new Separator(Orientation.VERTICAL);
		Label humanResultLabel = new Label("Select human result");
		Button humanResultButton = new Button("Browse");
		humanResultButton.getStyleClass().addAll("select-control");

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
		Button notConnectedButton = new Button("not connected");
		notConnectedButton.getStyleClass().addAll("select-control");
		Separator vAlgSeparator_3 = new Separator(Orientation.VERTICAL);
		Button testButton = new Button("test");
		testButton.getStyleClass().addAll("select-control");

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
		Separator vParamSeparator_1 = new Separator(Orientation.VERTICAL);
		Label filterMaskSizeParamLabel = new Label("Mask size:");
		TextField medianFilterMaskSizeInput = new TextField();
		medianFilterMaskSizeInput.setPromptText("3");
		medianFilterMaskSizeInput.getStyleClass().addAll("select-control");

		//Separator
		Separator iiSelectSeparator_4 = new Separator();
		iiSelectSeparator_4.setOrientation(Orientation.HORIZONTAL);
		iiSelectSeparator_4.getStyleClass().addAll("separator");

		//PREPROC
		HBox preprocBox = new HBox();
		//TODO: Слайдер для переключения типов фильтров.
		Label preprocMedianLabel = new Label("Median blur:");
		CheckBox preprocMedianCB = new CheckBox();
		Separator vPreprocSeparator_1 = new Separator(Orientation.VERTICAL);
		Label preprocBilaterialLabel = new Label("Bilaterial blur:");
		CheckBox preprocBilaterialCB = new CheckBox();
		Separator vPreprocSeparator_2 = new Separator(Orientation.VERTICAL);
		Label preprocDiaplLabel = new Label("Diap Selection:");
		CheckBox preprocDiapCB = new CheckBox();
		Separator vPreprocSeparator_3 = new Separator(Orientation.VERTICAL);
		Label yaprMultiOtsuLabel = new Label("YAPR MultiOtsu:");
		CheckBox yaprMultiOtsuCB = new CheckBox();
		Separator vPreprocSeparator_4 = new Separator(Orientation.VERTICAL);
		Label coloredResultsLabel = new Label("Colored segments:");
		CheckBox coloredResultsCB = new CheckBox();
		Separator vPreprocSeparator_5 = new Separator(Orientation.VERTICAL);

		//Separator
		Separator iiSelectSeparator_5 = new Separator();
		iiSelectSeparator_5.setOrientation(Orientation.HORIZONTAL);
		iiSelectSeparator_5.getStyleClass().addAll("separator");

		HBox correlTestBox = new HBox();
		DirectoryChooser directoryChooser = new DirectoryChooser();
		Label srcDirLabel = new Label("Select src directory");
		Button srcDirButton = new Button("Browse");
		srcDirButton.getStyleClass().addAll("select-control");
		Separator vCorrelTestSeparator_1 = new Separator(Orientation.VERTICAL);
		Label humanResultDirLabel = new Label("Select human results directory");
		Button humanResultDirButton = new Button("Browse");
		humanResultDirButton.getStyleClass().addAll("select-control");
		Separator vCorrelTestSeparator_2 = new Separator(Orientation.VERTICAL);
		Button massTestButton = new Button(" mass test");
		massTestButton.getStyleClass().addAll("select-control");


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
			paramsBox,
			iiSelectSeparator_4,
			preprocBox,
			iiSelectSeparator_5,
			correlTestBox
		);
		imageSelectBox.getChildren().addAll(
			iiSelectLabel,
			iiSelectButton,
			vInputSeparator_1,
			humanResultLabel,
			humanResultButton
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
			notConnectedButton,
			vAlgSeparator_3,
			testButton
		);
		paramsBox.getChildren().addAll(
			depthParamLabel,
			depthCountInput,
			vParamSeparator_1,
			filterMaskSizeParamLabel,
			medianFilterMaskSizeInput
		);
		preprocBox.getChildren().addAll(
			preprocMedianLabel,
			preprocMedianCB,
			vPreprocSeparator_1,
			preprocBilaterialLabel,
			preprocBilaterialCB,
			vPreprocSeparator_2,
			preprocDiaplLabel,
			preprocDiapCB,
			vPreprocSeparator_3,
			yaprMultiOtsuLabel,
			yaprMultiOtsuCB,
			vPreprocSeparator_4,
			coloredResultsLabel,
			coloredResultsCB,
			vPreprocSeparator_5
		);
		correlTestBox.getChildren().addAll(
			srcDirLabel,
			srcDirButton,
			vCorrelTestSeparator_1,
			humanResultDirLabel,
			humanResultDirButton,
			vCorrelTestSeparator_2,
			massTestButton
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
							FXApp.this.processedImage = pictureService.readPicture(file);
							FXApp.this.toAlgorythmsImage = FXApp.this.processedImage.clone();
							inputImageView.setImage(pictureService.mat2Image(FXApp.this.processedImage.getMat()));

							resetHandMarkers(markersDrawingImageView, inputImageView);
							log.info("new writableImage size is {}x{}", FXApp.this.handMarkers.getWidth(), FXApp.this.handMarkers.getHeight());
						}
					} catch (IOException ex) {
						log.error("error while open image", ex);
						return;
					}
				}
			}
		);

		humanResultButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					File file = fileChooser.showOpenDialog(stage);
					try {
						if (file != null) {
							FXApp.this.humanResultImage = pictureService.readPicture(file);
						}
					} catch (IOException ex) {
						log.error("error while open image", ex);
						return;
					}
				}
			}
		);

		testButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (FXApp.this.processedImage != null && FXApp.this.toAlgorythmsImage != null && FXApp.this.humanResultImage != null) {
						FXApp.this.toAlgorythmsImage = FXApp.this.processedImage.clone();
						FXApp.this.toAlgorythmsImage = testService.test(
							FXApp.this.toAlgorythmsImage,
							FXApp.this.humanResultImage
						);
						oiDropDownList.setItems(FXCollections.observableArrayList(
							FXApp.this.toAlgorythmsImage.getResults().stream()
								.map(Result::getStepName)
								.collect(Collectors.toList())
						));
						oiDropDownList.setValue(CollectionUtil.getLastOf(oiDropDownList.getItems()));
					}
				}
			}
		);

		srcDirButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					File fileDir = directoryChooser.showDialog(stage);
					try {
						if (fileDir != null && fileDir.listFiles() != null) {
							List<File> images = Arrays.asList(fileDir.listFiles());

							FXApp.this.srcImages = new HashMap<>();

							for (File file : images) {
								ImageInfo ii = pictureService.readPicture(file);
								FXApp.this.srcImages.put(ii.getImageFileName(), ii);
							}
						}
					} catch (IOException ex) {
						log.error("error while open directory", ex);
						return;
					}
				}
			}
		);

		humanResultDirButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					File fileDir = directoryChooser.showDialog(stage);
					try {
						if (fileDir != null && fileDir.listFiles() != null) {
							List<File> images = Arrays.asList(fileDir.listFiles());

							FXApp.this.humanResultsImages = new HashMap<>();

							for (File file : images) {
								ImageInfo ii = pictureService.readPicture(file);
								FXApp.this.humanResultsImages.put(ii.getImageFileName(), ii);
							}
						}
					} catch (IOException ex) {
						log.error("error while open directory", ex);
						return;
					}
				}
			}
		);

		massTestButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (FXApp.this.srcImages != null && FXApp.this.humanResultsImages != null
						&& !FXApp.this.srcImages.isEmpty() && !FXApp.this.humanResultsImages.isEmpty()) {

						Map<ImageInfo, ImageInfo> forTests = new HashMap<>();

						FXApp.this.srcImages.keySet().forEach(s -> forTests.put(
							FXApp.this.srcImages.get(s),
							FXApp.this.humanResultsImages.get(s)
						));

						testService.massTest(forTests);
					}
				}
			}
		);

		//COLOR WATERSHED
		colorWshedButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (FXApp.this.processedImage != null && FXApp.this.toAlgorythmsImage != null) {
						FXApp.this.toAlgorythmsImage = FXApp.this.processedImage.clone();
						FXApp.this.toAlgorythmsImage = pictureService.colorAutoMarkerWatershed(
							FXApp.this.toAlgorythmsImage,
							FXApp.this.preProcessOptions
						);
						oiDropDownList.setItems(FXCollections.observableArrayList(
							FXApp.this.toAlgorythmsImage.getResults().stream()
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
					if (FXApp.this.processedImage != null && FXApp.this.toAlgorythmsImage != null && FXApp.this.handMarkers != null) {
						Mat handMarkersMat = pictureService.image2Mat(FXApp.this.handMarkers);
						handMarkersMat = pictureService.bwMat(handMarkersMat);
						FXApp.this.toAlgorythmsImage = FXApp.this.processedImage.clone();
						FXApp.this.toAlgorythmsImage = pictureService.handMarkerWatershed(FXApp.this.toAlgorythmsImage, handMarkersMat);
						oiDropDownList.setItems(FXCollections.observableArrayList(
							FXApp.this.toAlgorythmsImage.getResults().stream()
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
					if (FXApp.this.processedImage != null && FXApp.this.toAlgorythmsImage != null && FXApp.this.handMarkers != null) {
						FXApp.this.toAlgorythmsImage = FXApp.this.processedImage.clone();
						FXApp.this.toAlgorythmsImage = pictureService.shapeAutoMarkerWatershed(
							FXApp.this.toAlgorythmsImage,
							FXApp.this.preProcessOptions
						);
						oiDropDownList.setItems(FXCollections.observableArrayList(
							FXApp.this.toAlgorythmsImage.getResults().stream()
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
					if (FXApp.this.processedImage != null && FXApp.this.toAlgorythmsImage != null) {
						FXApp.this.toAlgorythmsImage = FXApp.this.processedImage.clone();
						FXApp.this.toAlgorythmsImage = pictureService.notConnectedMarkers(
							FXApp.this.toAlgorythmsImage,
							Integer.parseInt(depthCountInput.getText()),
							Integer.parseInt(medianFilterMaskSizeInput.getText()),
							FXApp.this.preProcessOptions
						);
						oiDropDownList.setItems(FXCollections.observableArrayList(
							FXApp.this.toAlgorythmsImage.getResults().stream()
								.map(Result::getStepName)
								.collect(Collectors.toList())
						));
						oiDropDownList.setValue(CollectionUtil.getLastOf(oiDropDownList.getItems()));
					}
				}
			}
		);

		preprocDiapCB.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (preprocDiapCB.isSelected()) {
						FXApp.this.preProcessOptions.add(AlgorithmOptions.GISTO_DIAP);
					} else {
						FXApp.this.preProcessOptions.remove(AlgorithmOptions.GISTO_DIAP);
					}
				}
			}
		);

		preprocMedianCB.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (preprocMedianCB.isSelected()) {
						FXApp.this.preProcessOptions.add(AlgorithmOptions.MEDIAN_BLUR);
					} else {
						FXApp.this.preProcessOptions.remove(AlgorithmOptions.MEDIAN_BLUR);
					}
				}
			}
		);

		preprocBilaterialCB.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (preprocBilaterialCB.isSelected()) {
						FXApp.this.preProcessOptions.add(AlgorithmOptions.BILATERIAL);
					} else {
						FXApp.this.preProcessOptions.remove(AlgorithmOptions.BILATERIAL);
					}
				}
			}
		);

		yaprMultiOtsuCB.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (yaprMultiOtsuCB.isSelected()) {
						FXApp.this.preProcessOptions.add(AlgorithmOptions.MULTI_OTSU);
					} else {
						FXApp.this.preProcessOptions.remove(AlgorithmOptions.MULTI_OTSU);
					}
				}
			}
		);

		coloredResultsCB.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (coloredResultsCB.isSelected()) {
						FXApp.this.preProcessOptions.add(AlgorithmOptions.COLORED);
					} else {
						FXApp.this.preProcessOptions.remove(AlgorithmOptions.COLORED);
					}
				}
			}
		);


		saveResultsButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (FXApp.this.processedImage != null && FXApp.this.toAlgorythmsImage != null
						&& !FXApp.this.toAlgorythmsImage.getResults().isEmpty()) {
						pictureService.saveResultsToFS(FXApp.this.toAlgorythmsImage);
					}
				}
			}
		);

		//RESET MARKERS
		resetMarkersButton.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (FXApp.this.processedImage != null) {
						resetHandMarkers(markersDrawingImageView, inputImageView);
						log.info("reset writableImage [{}x{}]", FXApp.this.handMarkers.getWidth(), FXApp.this.handMarkers.getHeight());
					}
				}
			});

		//ONCHANGE RESULTS DROPDOWN LIST
		ChangeListener<String> changeListener = new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (newValue != null && !newValue.isEmpty()) {
					FXApp.this.toAlgorythmsImage.getResults().stream()
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
					MathUtil.normalize(x, FRAME_WIDTH, FXApp.this.handMarkers.getWidth()).intValue(),
					MathUtil.normalize(y, FRAME_HEIGHT, FXApp.this.handMarkers.getHeight()).intValue(),
					2,
					FXApp.this.handMarkers,
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
					MathUtil.normalize(x, FRAME_WIDTH, FXApp.this.handMarkers.getWidth()).intValue(),
					MathUtil.normalize(y, FRAME_HEIGHT, FXApp.this.handMarkers.getHeight()).intValue(),
					2,
					FXApp.this.handMarkers,
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
				if (newIntValue > 255) {
					newIntValue = 255;
					newValue = Integer.toString(newIntValue);
				}
				depthCountInput.setText(newValue);
			}
		});

		medianFilterMaskSizeInput.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue,
			                    String newValue) {
				if (!newValue.matches("\\d*")) {
					newValue = newValue.replaceAll("[^\\d]", "");
				}
				if (newValue.equals("")) {
					medianFilterMaskSizeInput.setText(newValue);
					return;
				}
				int newIntValue = Integer.parseInt(newValue);
				if (newIntValue < 1) {
					newIntValue = 1;
					newValue = Integer.toString(newIntValue);
				}
				if (newIntValue > 11) {
					newIntValue = 11;
					newValue = Integer.toString(newIntValue);
				}
				medianFilterMaskSizeInput.setText(newValue);
			}
		});

		//----------------------//
		//---SET START CONTENT--//
		//----------------------//

		configureFileChooser(fileChooser);
		configureDirectoryChooser(directoryChooser);
		depthCountInput.setText(Integer.toString(4));
		medianFilterMaskSizeInput.setText(Integer.toString(3));

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

	private void configureDirectoryChooser(final DirectoryChooser directoryChooser) {
		directoryChooser.setTitle("View Pictures");
		directoryChooser.setInitialDirectory(
			new File(System.getProperty("user.home"))
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

		FXApp.this.handMarkers = new WritableImage(
			(int) sourceContainer.getImage().getWidth(),
			(int) sourceContainer.getImage().getHeight()
		);
		handMarkersContainer.setImage(FXApp.this.handMarkers);
	}
}
