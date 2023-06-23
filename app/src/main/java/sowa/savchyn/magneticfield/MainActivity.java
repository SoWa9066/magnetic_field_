package sowa.savchyn.magneticfield;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor magnetikSensor;
    private TextView xTextView;
    private TextView yTextView;
    private TextView zTextView;
    private TextView intensityTextView; // Додано поле для відображення загальної інтенсивності

    private boolean isFlashing = false;
    private Handler handler;
    private Runnable flashingRunnable;

    private float lastX = 0;
    private float lastY = 0;
    private float lastZ = 0;

    private static final float THRESHOLD = 20.0f; // Змінено порігове значення

    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xTextView = findViewById(R.id.xTextView);
        yTextView = findViewById(R.id.yTextView);
        zTextView = findViewById(R.id.zTextView);
        intensityTextView = findViewById(R.id.intensityTextView); // Додано посилання на TextView для загальної інтенсивності

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            magnetikSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        if (magnetikSensor == null) {
            Toast.makeText(this, "Пристрій не підтримує детектор магнітного поля", Toast.LENGTH_SHORT).show();
            finish();
        }

        handler = new Handler();
        flashingRunnable = new Runnable() {
            @Override
            public void run() {
                toggleFlash();
                handler.postDelayed(this, 500); // Інтервал між блиманнями (в мілісекундах)
            }
        };

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            Toast.makeText(this, "Пристрій не підтримує вібрацію", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && magnetikSensor != null) {
            sensorManager.registerListener(this, magnetikSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        startFlashing();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        stopFlashing();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            xTextView.setText("X: " + String.format("%.2f", x) + " T");
            yTextView.setText("Y: " + String.format("%.2f", y) + " T");
            zTextView.setText("Z: " + String.format("%.2f", z) + " T");

            // Розрахунок загальної інтенсивності магнітного поля
            float intensity = calculateIntensity(x, y, z);
            intensityTextView.setText("Загальна інтенсивність: " + String.format("%.2f", intensity) + " T");

            // Перевірка на різке збільшення магнітного поля
            if (isFieldIncreasing(x, y, z)) {
                startFlashing();
                vibrateDevice();
            } else {
                stopFlashing();
            }

            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Не використовується в цьому прикладі
    }

    private boolean isFieldIncreasing(float x, float y, float z) {
        // Розраховуємо різницю між поточними значеннями і попередними
        float deltaX = Math.abs(x - lastX);
        float deltaY = Math.abs(y - lastY);
        float deltaZ = Math.abs(z - lastZ);

        // Перевіряємо, чи перевищують різниці порігове значення (20)
        if (deltaX > THRESHOLD || deltaY > THRESHOLD || deltaZ > THRESHOLD) {
            return true; // Магнітне поле різко збільшується
        } else {
            return false; // Магнітне поле не збільшується різко
        }
    }

    private float calculateIntensity(float x, float y, float z) {
        // Обчислення загальної інтенсивності магнітного поля
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    private void toggleFlash() {
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        if (isFlashing) {
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            window.setAttributes(params);
        } else {
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
            window.setAttributes(params);
        }
        isFlashing = !isFlashing;
    }

    private void startFlashing() {
        if (!isFlashing) {
            handler.post(flashingRunnable);
        }
    }

    private void stopFlashing() {
        if (isFlashing) {
            handler.removeCallbacks(flashingRunnable);
            toggleFlash();
        }
    }

    private void vibrateDevice() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(200);
            }
        }
    }
}
