package chan.eddie.wifipositioning;

import java.util.Arrays;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class OrientationSensor implements SensorEventListener {
	// callback of OnSensorChangeListener
	private OnSensorChangeListener mCallback;
	
	private Context c;
	private SensorManager mSensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	
	protected float[] mGravity = new float[3], mGeomagnetic = new float[3];
	protected float R[] = new float[9];
	protected float I[] = new float[9];
	protected float orientation[] = new float[3];
	
    // Listener must implement this interface
    public interface OnSensorChangeListener {
    	public void OnSensorChange(float degree, float gradient);
    }
    
	public OrientationSensor(Context context) {
		c = context;
		mSensorManager = (SensorManager)c.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}
	
	public void setOnSensorChangeListener(OnSensorChangeListener listener) {
		mCallback = listener;
	}
	
	public void startSensor() {
		mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
	}
	
	public void stopSensor() {
		mSensorManager.unregisterListener(this);
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	public void onSensorChanged(SensorEvent event) {
		Arrays.fill(R, 0);
		Arrays.fill(I, 0);
		Arrays.fill(orientation, 0);

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			for (int i=0; i<3; i++)
				mGravity[i] = event.values[i];
		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			for (int i=0; i<3; i++)
				mGeomagnetic[i] = event.values[i];
		}

		if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {
			SensorManager.getOrientation(R, orientation);
			float gradient = orientation[0];
			float degree = (float)Math.toDegrees(gradient);

			if (mCallback != null)
				mCallback.OnSensorChange(degree, gradient);
		}
	}
}
