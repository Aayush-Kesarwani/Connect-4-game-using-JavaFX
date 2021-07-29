package com.internshala.connect4;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Controller implements Initializable {

	private static final int Columns = 7;
	private static final int Rows = 6;
	private static final int Circle_Diameter = 80;
	private static final String discColor1 = "#24303E";
	private static final String discColor2 = "#4CAA88";

	private static String Player_One = "Player One";
	private static String Player_Two = "Player Two";

	private static boolean isPlayerOneTurn = true;

	private Disc[][] insertedDiscsArray = new Disc[Rows][Columns]; // For structural changes: For developers

	@FXML
	public GridPane rootGridPane;

	@FXML
	public Pane insertedDiscsPane;

	@FXML
	public Label playerNameLabel;

	@FXML
	public TextField playerOneTextField;

	@FXML
	public TextField playerTwoTextField;

	@FXML
	public Button setNamesButton;

	private boolean isAllowedToInsert = true; //To avoid same colour disc being added

	public void createPlayground(){
		Shape rectangleWithHoles = createGameStructuralGrid();
		rootGridPane.add(rectangleWithHoles, 0, 1);

		List<Rectangle> rectangleList = createClickableColumns();

		for(Rectangle rectangle: rectangleList) {
			rootGridPane.add(rectangle, 0, 1);

		setNamesButton.setOnAction(event -> {
			Player_One = playerOneTextField.getText();
			Player_Two = playerTwoTextField.getText();
		});
		}
	}
	private Shape createGameStructuralGrid(){
		Shape rectangleWithHoles = new Rectangle((Columns + 1) * Circle_Diameter, (Rows + 1) * Circle_Diameter);

		for (int row = 0; row < Rows; row++){
			for (int col = 0; col < Columns; col++){
				Circle circle = new Circle();
				circle.setRadius(Circle_Diameter / 2);
				circle.setCenterX(Circle_Diameter / 2);
				circle.setCenterY(Circle_Diameter / 2);
				circle.setSmooth(true);

				circle.setTranslateX(col * (Circle_Diameter + 5) + Circle_Diameter / 4);
				circle.setTranslateY(row * (Circle_Diameter + 5) + Circle_Diameter / 4);
				rectangleWithHoles = Shape.subtract(rectangleWithHoles, circle);
			}
		}
		rectangleWithHoles.setFill(Color.WHITE);
		return rectangleWithHoles;
	}

	private List<Rectangle> createClickableColumns(){

		List<Rectangle> rectangleList = new ArrayList();

		for (int column = 0; column < Columns; column++){
			Rectangle rectangle = new Rectangle(Circle_Diameter, (Rows + 1) * Circle_Diameter);
			rectangle.setFill(Color.TRANSPARENT);
			rectangle.setTranslateX(column * (Circle_Diameter + 5) + Circle_Diameter / 4);

			rectangle.setOnMouseEntered(event -> rectangle.setFill(Color.valueOf("#8e8e8e26")));
			rectangle.setOnMouseExited(event -> rectangle.setFill(Color.TRANSPARENT));

			final int col = column;
			rectangle.setOnMouseClicked(event -> {
				if (isAllowedToInsert){
					isAllowedToInsert = false; // When the disc is being dropped, no more disc will be inserted
					insertDisc(new Disc(isPlayerOneTurn), col);
				}
			});

			rectangleList.add(rectangle);
		}

		return rectangleList;
	}

	private void insertDisc(Disc disc, int column){

		int row = Rows - 1;
		while(row >= 0) {
			if(getDiscIfPresent(row, column) == null)
				break;
			row--;
		}

		if (row < 0) //If it is full, we can't insert anymore disc.
			return;

		insertedDiscsArray[row][column] = disc; //For structural changes: For developers
		insertedDiscsPane.getChildren().add(disc);

		disc.setTranslateX(column * (Circle_Diameter + 5) + Circle_Diameter / 4);

		int currentRow = row;
		TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.5), disc);
		translateTransition.setToY(row * (Circle_Diameter + 5) + Circle_Diameter / 4);
		translateTransition.setOnFinished(event -> {
			isAllowedToInsert = true; //Finally, when disc is dropped, allow the next player to insert disc
			if(gameEnded(currentRow, column)){
				gameOver();
				return;
			}

			isPlayerOneTurn = !isPlayerOneTurn;
			playerNameLabel.setText(isPlayerOneTurn? Player_One : Player_Two);
		});

		translateTransition.play();

	}

	private boolean gameEnded(int row, int column){

		//Vertical points.
		List<Point2D> verticalPoints = IntStream.rangeClosed(row - 3, row + 3) //Range of row values = 0,1,2,3,4,5.
												.mapToObj(r -> new Point2D(r, column)) //Return values of Point2D objects. Example: 0,3  1,3  2,3  etc --> Point2D x,y
												.collect(Collectors.toList()); //

		//Horizontal points.
		List<Point2D> horizontalPoints = IntStream.rangeClosed(column - 3, column + 3)
												  .mapToObj(col -> new Point2D(row, col))
												  .collect(Collectors.toList());

		Point2D startPoint1 = new Point2D(row -3, column + 3);
		List<Point2D> diagonalPoints = IntStream.rangeClosed(0, 6)
												.mapToObj(i -> startPoint1.add(i, -i))
												.collect(Collectors.toList());

		Point2D startPoint2 = new Point2D(row - 3, column - 3);
		List<Point2D> diagonal2Points = IntStream.rangeClosed(0, 6)
											    .mapToObj(i -> startPoint2.add(i, i))
												.collect(Collectors.toList());


		boolean isEnded = checkCombination(verticalPoints) || checkCombination(horizontalPoints)
						  || checkCombination(diagonalPoints) || checkCombination(diagonal2Points);

		return isEnded;
	}

	private boolean checkCombination(List<Point2D> points) {
		int chain =0;

		for (Point2D point: points){

			int rowIndexForArray = (int) point.getX();
			int columnIndexForArray = (int) point.getY();

			Disc disc = getDiscIfPresent(rowIndexForArray, columnIndexForArray);

			if (disc != null && disc.isPlayerOneMove == isPlayerOneTurn){ //If the last inserted disc belongs to the current player

				chain++;
				if(chain == 4){
					return true;
				}
			} else{
				chain = 0;
			}
		}

		return false;
	}

	private Disc getDiscIfPresent(int row, int column){ //To prevent ArrayIndexOutOfBoundException
		if(row >= Rows || row < 0 || column >= Columns || column < 0)
			return null;

		return insertedDiscsArray[row][column];
	}

	private void gameOver(){
		String winner = isPlayerOneTurn ? Player_One : Player_Two;
		System.out.println("Winner is " + winner);

		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Connect Four");
		alert.setHeaderText("The winner is " + winner);
		alert.setContentText("Wan to play again? ");

		ButtonType yesButton = new ButtonType("Yes");
		ButtonType noButton = new ButtonType("No, Exit");

		alert.getButtonTypes().setAll(yesButton, noButton);

		Platform.runLater(() -> {

			Optional<ButtonType> buttonClicked =  alert.showAndWait();
			if (buttonClicked.isPresent() && buttonClicked.get() == yesButton){
				//.. User has chosen yes so reset the game
				resetGame();
			}
			else{
				//.. User chose no, so exit the game
				Platform.exit();
				System.exit(0);
			}
		});
	}

	public void resetGame() {

		insertedDiscsPane.getChildren().clear();//Removes all discs from the Pane

		for(int row = 0; row < insertedDiscsArray.length; row++){
			for (int col = 0; col < insertedDiscsArray[row].length; col++){
				insertedDiscsArray[row][col] = null;
			}
		}

		isPlayerOneTurn = true; //Let player one start the game
		playerNameLabel.setText(Player_One);

		createPlayground();//Prepare a fresh playground
	}

	private static class Disc extends Circle{
		private final boolean isPlayerOneMove;

		public Disc(boolean isPlayerOneMove){

			this.isPlayerOneMove = isPlayerOneMove;
			setRadius(Circle_Diameter / 2);
			setFill(isPlayerOneMove? Color.valueOf(discColor1) : Color.valueOf(discColor2));
			setCenterX(Circle_Diameter / 2);
			setCenterY(Circle_Diameter / 2);
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

	}
}
