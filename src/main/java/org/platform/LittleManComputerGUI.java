package org.platform;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

public class LittleManComputerGUI extends Application {

    private final int[] memory = new int[100];
    private int accumulator = 0;
    private int programCounter = 0;
    private boolean halted = false;
    private boolean waitingForInput = false;

    private TextArea codeArea;
    private Label accLabel, pcLabel, irLabel, arLabel;
    private TextField inputField;
    private TextArea outputArea;
    private GridPane ramGrid;
    private List<Label> ramCells = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        HBox topPane = new HBox(10);

        codeArea = new TextArea();
        codeArea.setPrefWidth(300);
        codeArea.setPromptText("Assembly code here");

        VBox cpuPanel = new VBox(5);
        accLabel = new Label("ACC: 000");
        pcLabel = new Label("Program Counter: 000");
        irLabel = new Label("Instruction Register: 000");
        arLabel = new Label("Address Register: 000");
        cpuPanel.getChildren().addAll(accLabel, pcLabel, irLabel, arLabel);

        topPane.getChildren().addAll(codeArea, cpuPanel);

        ramGrid = new GridPane();
        ramGrid.setHgap(5);
        ramGrid.setVgap(5);
        updateRAMGrid();

        inputField = new TextField();
        inputField.setPromptText("Input");
        inputField.setDisable(true);

        outputArea = new TextArea();
        outputArea.setPromptText("Output");
        outputArea.setEditable(false);
        outputArea.setPrefHeight(60);

        HBox buttonBox = new HBox(10);
        Button assembleButton = new Button("Assemble");
        Button runButton = new Button("Run");
        Button stepButton = new Button("Step");

        assembleButton.setOnAction(e -> assembleCode());
        runButton.setOnAction(e -> runProgram());
        stepButton.setOnAction(e -> step());

        buttonBox.getChildren().addAll(assembleButton, runButton, stepButton);

        root.getChildren().addAll(topPane, ramGrid, inputField, outputArea, buttonBox);

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Little Man Computer GUI");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
    }

    private void updateRAMGrid() {
        ramGrid.getChildren().clear();
        ramCells.clear();
        for (int i = 0; i < 100; i++) {
            Label cell = new Label(String.format("%03d", memory[i]));
            cell.setStyle("-fx-border-color: black; -fx-padding: 3;");
            ramGrid.add(cell, i % 10, i / 10);
            ramCells.add(cell);
        }
        highlightCurrentInstruction();
    }

    private void highlightCurrentInstruction() {
        for (int i = 0; i < ramCells.size(); i++) {
            if (i == programCounter && !halted) {
                ramCells.get(i).setStyle("-fx-border-color: red; -fx-background-color: yellow; -fx-padding: 3;");
            } else {
                ramCells.get(i).setStyle("-fx-border-color: black; -fx-padding: 3;");
            }
        }
    }

    private void assembleCode() {
        Arrays.fill(memory, 0);
        programCounter = 0;
        halted = false;
        waitingForInput = false;
        inputField.setDisable(true);
        inputField.setStyle(null);
        inputField.setPromptText("Input");

        String[] lines = codeArea.getText().split("\\n");
        Map<String, Integer> labels = new HashMap<>();
        List<String[]> parsedLines = new ArrayList<>();

        int address = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            String[] parts = line.split("\\s+");
            if (parts.length > 1 && !isInstruction(parts[0])) {
                labels.put(parts[0], address);
                parsedLines.add(Arrays.copyOfRange(parts, 1, parts.length));
            } else {
                parsedLines.add(parts);
            }
            address++;
        }

        address = 0;
        for (String[] parts : parsedLines) {
            if (parts.length == 0) continue;
            String instr = parts[0].toUpperCase();
            int operand = 0;
            if (parts.length > 1) {
                if (labels.containsKey(parts[1])) operand = labels.get(parts[1]);
                else try { operand = Integer.parseInt(parts[1]); } catch (Exception e) { }
            }
            int code;
            switch (instr) {
                case "ADD" -> code = 100 + operand;
                case "SUB" -> code = 200 + operand;
                case "STA" -> code = 300 + operand;
                case "LDA" -> code = 500 + operand;
                case "BRA" -> code = 600 + operand;
                case "BRZ" -> code = 700 + operand;
                case "BRP" -> code = 800 + operand;
                case "INP" -> code = 901;
                case "OUT" -> code = 902;
                case "HLT" -> code = 0;
                case "DAT" -> code = operand;
                default -> {
                    showMessage("Unknown instruction: " + instr);
                    return;
                }
            }
            if (address >= 0 && address < memory.length) memory[address++] = code;
        }
        updateRAMGrid();
    }

    private boolean isInstruction(String s) {
        return Set.of("ADD", "SUB", "STA", "LDA", "BRA", "BRZ", "BRP", "INP", "OUT", "HLT", "DAT").contains(s.toUpperCase());
    }

    private void runProgram() {
        while (!halted && programCounter < 100 && !waitingForInput) {
            step();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void step() {
        if (halted || programCounter >= 100) return;

        if (waitingForInput) {
            try {
                accumulator = Integer.parseInt(inputField.getText());
                waitingForInput = false;
                inputField.setDisable(true);
                inputField.setStyle(null);
                inputField.setPromptText("Input");
                programCounter++;
            } catch (NumberFormatException e) {
                showMessage("Invalid input.");
                return;
            }
        } else {
            int instruction = memory[programCounter++];
            int opcode = instruction / 100;
            int address = instruction % 100;

            irLabel.setText("Instruction Register: " + String.format("%03d", instruction));
            arLabel.setText("Address Register: " + String.format("%02d", address));

            switch (opcode) {
                case 1 -> accumulator += memory[address];
                case 2 -> accumulator -= memory[address];
                case 3 -> memory[address] = accumulator;
                case 5 -> accumulator = memory[address];
                case 6 -> programCounter = address;
                case 7 -> { if (accumulator == 0) programCounter = address; }
                case 8 -> { if (accumulator >= 0) programCounter = address; }
                case 9 -> {
                    if (address == 1) {
                        waitingForInput = true;
                        programCounter--;
                        inputField.setDisable(false);
                        inputField.setStyle("-fx-border-color: #eeff00; -fx-border-width: 2px;");
                        inputField.setPromptText("Enter the number and press Step");
                        inputField.requestFocus();
                        return;
                    } else if (address == 2) {
                        outputArea.appendText(accumulator + "\n");
                    }
                }
                case 0 -> halted = true;
            }
        }

        accLabel.setText("ACC: " + String.format("%03d", accumulator));
        pcLabel.setText("Program Counter: " + String.format("%03d", programCounter));
        updateRAMGrid();
    }

    private void showMessage(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}