package org.andengine.examples.game.racer;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.shape.IAreaShape;
import org.andengine.examples.game.racer.Wheel.WheelPosition;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.ui.activity.BaseGameActivity;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;

public class Vehicle implements IVehicleControl, IUpdateHandler {

    protected static final int WHEEL_HEIGHT = 20;
    protected static final int WHEEL_WIDTH = 10;

    public static final float DEGTORAD = 0.0174532925199432957f;
    public static final float RADTODEG = 57.295779513082320876f;

    public static final int ACCELERATE = 1;
    public static final int ACCELERATE_NONE = 0;
    public static final int BREAK = -1;

    public static final int STEER_RIGHT = 1;
    public static final int STEER_NONE = 0;
    public static final int STEER_LEFT = -1;

    float m_currentTraction = 1;
    // Char caracteristics
    float m_maxForwardSpeed;
    float m_maxBackwardSpeed;
    float m_maxDriveForce;
    float m_maxLateralImpulse;

    private Wheel rightFront;
    private Wheel leftFront;
    private Wheel rightBack;
    private Wheel leftBack;

    private final boolean powered = true;

    protected Body mCarBody;
    protected IAreaShape mCarShape;

    private int steer = STEER_NONE;
    private int accelerate = ACCELERATE_NONE;

    BaseGameActivity context;
    PhysicsWorld mPhysicsWorld;
    Scene mScene;

    public Vehicle(final BaseGameActivity context, final Scene scene, final PhysicsWorld physicWorld) {
        this.context = context;
        this.mPhysicsWorld = physicWorld;
        this.mScene = scene;
    }

    public void create() {


        mCarShape = this.makeColoredRectangle(10, 160, 1, 1, 1,30,50);
        mScene.attachChild(mCarShape);

        final float pDensity = 1.0f;
        final float pElasticity = 0.5f;
        final float pFriction = 0.5f;

        final FixtureDef carFixtureDef = PhysicsFactory.createFixtureDef(pDensity, pElasticity, pFriction);
        this.mCarBody = PhysicsFactory.createBoxBody(this.mPhysicsWorld, this.mCarShape, BodyType.DynamicBody, carFixtureDef);
        mCarBody.setLinearDamping(5);
        mCarBody.setAngularDamping(20);

        MassData data = mCarBody.getMassData();
        data.mass = 3.9f;
        mCarBody.setMassData(data);

        System.out.println("Mass:"+mCarBody.getMass()+" density:"+pDensity+" volume:"+mCarShape.getWidth()*mCarShape.getHeight());

        this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(this.mCarShape, this.mCarBody, true, true));

        rightFront = new Wheel(context,mPhysicsWorld,this,WheelPosition.FRONT_RIGHT);
        leftFront = new Wheel(context,mPhysicsWorld,this,WheelPosition.FRONT_LEFT);
        rightBack = new Wheel(context,mPhysicsWorld,this,WheelPosition.BACK_RIGHTs);
        leftBack = new Wheel(context,mPhysicsWorld,this,WheelPosition.BACK_LEFT);
    }

    protected void setCharacteristics(final float maxForwardSpeed, final float maxBackwardSpeed,
            final float backTireMaxDriveForce, final float backTireMaxLateralImpulse) {
        this.m_maxForwardSpeed = maxForwardSpeed;
        this.m_maxBackwardSpeed = maxBackwardSpeed;
        this.m_maxDriveForce = backTireMaxDriveForce;
        this.m_maxLateralImpulse = backTireMaxLateralImpulse;
    }

    @Override
    public void steerLeft() {
        steer = STEER_LEFT;
    }

    @Override
    public void steerRight() {
        steer = STEER_RIGHT;
    }

    @Override
    public void steerNone() {
        steer = STEER_NONE;
    }

    @Override
    public void pedalAccelerate() {
        accelerate = ACCELERATE;
    }

    @Override
    public void pedalBreak() {
        accelerate = BREAK;
    }

    @Override
    public void pedalNone() {
        accelerate = ACCELERATE_NONE;
    }

    @Override
    public void onUpdate(final float pSecondsElapsed) {
        //Driving
        updateDrive();

        //control steering
        float lockAngle = 35f * DEGTORAD;
        float turnSpeedPerSec = 160f * DEGTORAD;//from lock to lock in 0.5 sec
        float turnPerTimeStep = turnSpeedPerSec / 60.0f;

        float desiredAngle = 0f;
        switch (steer) {
        case STEER_LEFT:
            desiredAngle = -lockAngle;
            break;
        case STEER_RIGHT:
            desiredAngle = lockAngle;
            break;
        default:
            ;// nothing
        }

        RevoluteJoint leftJoint = (RevoluteJoint) leftFront.getJoint();
        RevoluteJoint rightJoint = (RevoluteJoint) rightFront.getJoint();

        float angleNow = leftJoint.getJointAngle();
        float angleToTurn = desiredAngle - angleNow;
        angleToTurn = b2Clamp(angleToTurn, -turnPerTimeStep, turnPerTimeStep);
        float newAngle = angleNow + angleToTurn;

        leftJoint.setLimits(newAngle, newAngle);
        rightJoint.setLimits(newAngle, newAngle);
    }

    protected void updateDrive(){

        // find desired speed
        float desiredSpeed = 0;
        switch (accelerate) {
        case ACCELERATE:
            desiredSpeed = m_maxForwardSpeed;
            break;
        case BREAK:
            desiredSpeed = m_maxBackwardSpeed;
            break;
        default:
            return;// do nothing
        }

        //find current speed in forward direction
        Vector2 localPoint = Vector2Pool.obtain(new Vector2(0, 1));
        Vector2 currentForwardNormal = Vector2Pool.obtain(leftFront.getBody().getWorldVector(localPoint));
        Vector2Pool.recycle(localPoint);
        Vector2 forwardVelocity = getForwardVelocity(leftFront.getBody());
        float currentSpeed = b2Dot(forwardVelocity, currentForwardNormal);
        Vector2Pool.recycle(forwardVelocity);

        // apply necessary force
        //apply necessary force
        float force = 0;
        if ( desiredSpeed > currentSpeed ) {
            force = m_maxDriveForce;
        } else if ( desiredSpeed < currentSpeed ) {
            force = -m_maxDriveForce;
        } else {
            Vector2Pool.recycle(currentForwardNormal);
            return;
        }

        Vector2 forceVector = currentForwardNormal.mul(m_currentTraction * force*-1) ;

        leftFront.getBody().applyForce(forceVector, leftFront.getBody().getWorldCenter());
        rightFront.getBody().applyForce(forceVector, rightFront.getBody().getWorldCenter());
        Vector2Pool.recycle(currentForwardNormal);
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    protected float b2Clamp(final float a, final float low, final float high) {
        return Math.max(low, Math.min(a, high));
    }

    protected float b2Dot(final Vector2 a, final Vector2 b) {
        return a.x * b.x + a.y * b.y;
    }

    protected Vector2 getForwardVelocity(final Body body) {
        Vector2 localPoint = Vector2Pool.obtain(new Vector2(0, 1));
        Vector2 currentForwardNormal = Vector2Pool.obtain(body.getWorldVector(localPoint));
        Vector2Pool.recycle(localPoint);
        return currentForwardNormal.mul(b2Dot(currentForwardNormal, body.getLinearVelocity()));
    }

    protected Vector2 getLateralVelocity(final Body body) {
        Vector2 localPoint = Vector2Pool.obtain(new Vector2(1, 0));
        Vector2 currentForwardNormal = Vector2Pool.obtain(body.getWorldVector(localPoint));
        Vector2Pool.recycle(localPoint);
        return currentForwardNormal.mul(b2Dot(currentForwardNormal, body.getLinearVelocity()));
    }

    protected Body getBody() {
        return mCarBody;
    }

    protected IAreaShape makeColoredRectangle(final float pX, final float pY,
            final float pRed, final float pGreen, final float pBlue) {
        return makeColoredRectangle(pX, pY, pRed, pGreen, pBlue, WHEEL_WIDTH, WHEEL_HEIGHT);
    }

    protected IAreaShape makeColoredRectangle(final float pX, final float pY,
            final float pRed, final float pGreen, final float pBlue,
            final int width, final int height) {
        final Rectangle coloredRect = new Rectangle(pX, pY, width, height,context.getVertexBufferObjectManager());
        coloredRect.setColor(pRed, pGreen, pBlue);
        return coloredRect;
    }

    public IAreaShape getShape() {
        return mCarShape;
    }
}
