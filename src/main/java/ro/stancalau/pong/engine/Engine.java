package ro.stancalau.pong.engine;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Pane;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.stancalau.pong.config.Constants;
import ro.stancalau.pong.model.*;

import java.util.Observable;
import java.util.Observer;

public class Engine extends Observable implements Runnable {

    private static final Logger logger = LogManager.getLogger(Engine.class);
    private static final double SECOND_IN_MILIS = 1000d;
    private static final String GRADIENT_STRING = String.format("radial-gradient(center 50%% 50%%, radius 100%%, %s, %s)", Constants.BALL_COLOR_1, Constants.BALL_COLOR_2);

    private Circle ball;
    private Rectangle pad;
    private int framerate = Constants.FPS;
    private double speed = Constants.SPEED;
    private boolean running = false;

    private BallState ballState;
    private MouseState mouseState;
    private PadState padState;
    private PlayGroundState playGroundState;

    private Metrics metrics;

    public Engine(Pane pane) {
        playGroundState = new PlayGroundState(Constants.PLAY_GROUND_WIDTH, Constants.PLAY_GROUND_HEIGHT);
        ballState = new BallState(playGroundState, Constants.BALL_RADIUS);
        padState = new PadState(Constants.PAD_WIDTH, Constants.PAD_HEIGHT);
        mouseState = new MouseState();
        metrics = new Metrics();

        createBall(pane, ballState);
        createPad(pane, padState);

        bindPaneSizeProperties(pane);
    }

    private void bindPaneSizeProperties(final Pane pane) {
        ChangeListener<Number> listener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> arg0,
                                Number arg1, Number arg2) {
                updateSizes(pane);
            }
        };

        pane.widthProperty().addListener(listener);
        pane.heightProperty().addListener(listener);

        updateSizes(pane);
    }

    private void updateSizes(Pane pane) {
        playGroundState.setWidth(pane.getWidth());
        playGroundState.setHeight(pane.getHeight());
        pad.setY(playGroundState.getHeight() - pad.getHeight());
    }

    private void createBall(Pane pane, BallState state) {
        ball = new Circle();
        ball.setRadius(state.getRadius());

        RadialGradient g = RadialGradient.valueOf(GRADIENT_STRING);
        ball.setFill(g);

        pane.getChildren().add(ball);
        moveBall();
    }

    private void createPad(Pane pane, PadState state) {
        pad = new Rectangle(state.getWidth(), state.getHeight());
        pad.setX(0);
        pad.setY(pane.getHeight() - state.getHeight());

        RadialGradient g = RadialGradient.valueOf(GRADIENT_STRING);
        pad.setFill(g);

        pane.getChildren().add(pad);
    }

    @Override
    public void run() {
        setRunning(true);
        long lastUpdateTime = System.currentTimeMillis();

        while (isRunning()) {
            double vector = getVector(lastUpdateTime);

            try {
                ballState.updatePositions(padState, vector);
                updateMetrics(lastUpdateTime);
                lastUpdateTime = System.currentTimeMillis();
                Thread.sleep((long) (SECOND_IN_MILIS / (double) framerate));
            } catch (IllegalBallPositionException e) {
                stop();
                ballState.reset();
            } catch (InterruptedException e) {
                logger.error("Could not sleep... maybe it's insomnia.", e);
            } finally {
                moveBall();
                movePad();
            }
        }
    }

    private void updateMetrics(final long lastUpdateTime) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                final double fps = 1 / ((double) System.currentTimeMillis() - lastUpdateTime) * SECOND_IN_MILIS;
                metrics.setFps(Math.min(framerate, fps)); // minimum because the first cycle tends to give off huge unrealistic values
            }
        });
    }

    private void moveBall() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ball.setCenterX(ballState.getXPosition());
                ball.setCenterY(ballState.getYPosition());
            }
        });
    }

    private void movePad() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                padState.updatePosition(mouseState.getCurrentDeltaX(), playGroundState.getWidth());
                pad.setX(padState.getX());
            }
        });
    }

    private double getVector(long lastUpdateTime) {
        long sinceLastPositionUpdate = System.currentTimeMillis() - lastUpdateTime;
        double hypotenuse = speed / SECOND_IN_MILIS * sinceLastPositionUpdate;
        double cathetus = hypotenuse / Math.sqrt(Math.pow(ballState.getDeltaX(), 2) + Math.pow(ballState.getDeltaY(), 2));
        return cathetus;
    }

    private void setRunning(boolean running) {
        if (running == this.running) return;
        this.running = running;
        setChanged();
        notifyObservers();
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        setRunning(false);
    }

    public void addMetricsObserver(Observer observer) {
        metrics.addObserver(observer);
    }
}
