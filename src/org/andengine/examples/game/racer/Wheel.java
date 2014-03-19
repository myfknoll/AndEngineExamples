package org.andengine.examples.game.racer;

import org.andengine.entity.shape.IAreaShape;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.ui.activity.BaseGameActivity;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;

public class Wheel {

    protected enum WheelPosition{
        FRONT_LEFT,
        FRONT_RIGHT,
        BACK_LEFT,
        BACK_RIGHTs
    }

    private final PhysicsWorld mPhysicsWorld;
    private final BaseGameActivity context;

    private Joint joint;

    private IAreaShape mWheelShape;
    private Body mWheelBody;

    private final Vehicle vehicle;

    protected final WheelPosition position;

    protected Wheel(final BaseGameActivity context, final PhysicsWorld physicsWorld, final Vehicle vehicle,final WheelPosition position) {
        this.mPhysicsWorld = physicsWorld;
        this.context = context;
        this.vehicle = vehicle;
        this.position = position;

        create();
    }

    protected void create(){
        IAreaShape shape = vehicle.getShape();

        switch (position) {
        case FRONT_RIGHT:
            mWheelShape = vehicle.makeColoredRectangle(shape.getX()+shape.getWidth()-Vehicle.WHEEL_WIDTH, shape.getY(), 0, 1, 0);
            break;
        case FRONT_LEFT:
            mWheelShape = vehicle.makeColoredRectangle(shape.getX(), shape.getY(), 0, 0, 1);
            break;
        case BACK_LEFT:
            mWheelShape = vehicle.makeColoredRectangle(shape.getX(), shape.getY()+shape.getHeight()-Vehicle.WHEEL_HEIGHT, 1, 1, 0);
            break;
        case BACK_RIGHTs:
            mWheelShape = vehicle.makeColoredRectangle(shape.getX()+shape.getWidth()-Vehicle.WHEEL_WIDTH, shape.getY()+shape.getHeight()-Vehicle.WHEEL_HEIGHT, 1, 1, 0);
            break;
        default:
            throw new IllegalArgumentException("Wheel position invalid");
        }
        vehicle.mScene.attachChild(mWheelShape);


        final FixtureDef wheelFixtureDef = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);
        wheelFixtureDef.isSensor = false;

        this.mWheelBody = PhysicsFactory.createBoxBody(this.mPhysicsWorld, this.mWheelShape, BodyType.DynamicBody, wheelFixtureDef);

        mWheelBody.setLinearDamping(1);
        mWheelBody.setAngularDamping(1);

        this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(this.mWheelShape, this.mWheelBody, true, true));

        createJoint();
    }

    private void createJoint(){
        Body body = vehicle.getBody();

        // Tody: get the correct center, if its rotated/scaled, etc
        float xCar = vehicle.getShape().getX() + vehicle.getShape().getWidth()/2.0f;
        float xWheel = mWheelShape.getX() + mWheelShape.getWidth()/2.0f;

        float yCar = vehicle.getShape().getY() + vehicle.getShape().getHeight()/2.0f;
        float yWheel = mWheelShape.getY() + mWheelShape.getHeight()/2.0f;

        RevoluteJointDef revoluteJointDefLeft = new RevoluteJointDef();
        revoluteJointDefLeft.initialize(body, mWheelBody, mWheelBody.getWorldCenter());
        configureJoint(revoluteJointDefLeft);

        // Spitze minus Schaft
        revoluteJointDefLeft.localAnchorA.set((xWheel-xCar)/PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT,(yWheel-yCar)/PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT);

        joint = this.mPhysicsWorld.createJoint(revoluteJointDefLeft);
    }

    protected void configureJoint(final RevoluteJointDef revoluteJointDef) {
        revoluteJointDef.enableMotor = false;

        revoluteJointDef.enableLimit = true;
        revoluteJointDef.lowerAngle = 0;
        revoluteJointDef.upperAngle = 0;

        revoluteJointDef.collideConnected = false;
        revoluteJointDef.localAnchorB.set(0, 0);
    }

    protected Joint getJoint() {
        return joint;
    }

    protected Body getBody() {
        return mWheelBody;
    }
}
