package org.andengine.examples.game.racer;

/**
 * Interface for car controlling
 * @author Knoll
 *
 */
public interface IVehicleControl {

    void steerLeft();

    void steerRight();

    void steerNone();

    void pedalAccelerate();

    void pedalBreak();

    void pedalNone();

}
