package ro.stancalau.pong.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import ro.stancalau.pong.engine.Engine;
import ro.stancalau.pong.engine.Metrics;

import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;

class PlayGroundPresentation extends Presentation implements Observer {

    public PlayGroundPresentation(ScreensConfig config) {
        super(config);
    }

    private Engine engine;

    @FXML
    StackPane root;
    @FXML
    private
    Pane pane;
    @FXML
    private
    Label fpsLabel;
    @FXML
    private
    Label fpsLabelMin;
    @FXML
    private Label fpsLabelMax;
    @FXML
    private
    Button startButton;

    private final DecimalFormat df = new DecimalFormat("##.#");

    @FXML
    public void onPressStart(ActionEvent event) {
        if (engine.isRunning()) {
            stop();
        } else {
            start();
        }
    }

    @FXML
    void initialize() {
        engine = new Engine(pane);
        final Observer metricsObserver = (o, arg) -> {
            Metrics metrics = (Metrics) o;
            fpsLabel.setText(df.format(metrics.getFps()));
            fpsLabelMin.setText(df.format(metrics.getMinFps()));
            fpsLabelMax.setText(df.format(metrics.getMaxFps()));
        };
        engine.addMetricsObserver(metricsObserver);
        engine.addObserver(this);
    }

    private void start() {
        new Thread(engine).start();
    }

    private void stop() {
        engine.stop();
    }

    @Override
    public void update(Observable o, Object arg) {
        Platform.runLater(() -> {
            if (engine.isRunning())
                startButton.setText("stop");
            else
                startButton.setText("start");
        });
    }
}
