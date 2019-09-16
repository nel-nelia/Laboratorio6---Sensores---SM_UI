package com.example.upt.acelerometroapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    private VistaSimulacion mSimulationView;
    private SensorManager mSensorManager;
    private PowerManager mPowerManager;
    private WindowManager mWindowManager; //para controlar las vistas
    private Display mDisplay; //para controlar las vistas
    private PowerManager.WakeLock mWakeLock;


    public class VistaSimulacion extends FrameLayout implements SensorEventListener{


        //diametro de los sensores en metros
        private static final float sBallDiameter = 0.004f;
        private static final float sBallDiameter2 = sBallDiameter * sBallDiameter;

        //alto y ancho
        private final int mDstWidth;//
        private final int mDstHeight;//
        private Sensor mAccelerometer;
        private long mLastT;

        //atributos para desplazamiento
        private float mXDpi;
        private float mYDpi;

        private float mMetersToPixelsX;
        private float mMetersToPixelsY;
        private float mXOrigin;
        private float mYOrigin;
        private float mSensorX;
        private float mSensorY;
        private float mHorizontalBound;
        private float mVerticalBound;

        private final SistemaPartes mSistemaPartes;

        //-----------------------------------------------------------------------------------------

        //Sistema de partes

        class SistemaPartes{
            static final int NUM_PARTICLES = 5;
            private Parte mBalls[] = new Parte[NUM_PARTICLES];

            SistemaPartes(){

                /*
                * Las partes inicialmente no
                * tienen ni velocidad
                * ni aceleración
                */

                for (int i = 0 ; i < mBalls.length; i++){
                    mBalls[i] = new Parte(getContext());
                    mBalls[i].setBackgroundResource(R.drawable.pig);
                    mBalls[i].setLayerType(LAYER_TYPE_HARDWARE, null);
                    addView(mBalls[i],new ViewGroup.LayoutParams(mDstWidth,mDstHeight));
                }
            }

            /*
             * Actualiza la posición de cada Parte en el
             * sistema
             */

            private void updatePositions(float sx, float sy, long timestamp){

                final long t = timestamp; //el valor de un entero alrgo en este momento, año, mes,dia, hora y fecha en tipo long, de acuerdo al tiempo

                if (mLastT != 0){
                    final float dT = (float) (t - mLastT) / 1000.f; //(1.0f / 1000000000.0f)
                    final int count = mBalls.length;
                    for (int i = 0; i < count;i++){
                        Parte ball = mBalls[i];
                        ball.computePhysics(sx,sy,dT);
                    }
                }
                mLastT = t;
            }

            /*
             * Ejecuta una iteración. Primero actualiza la
             * posición de las partes y resuelve las
             * restricciones y las colisiones
             */

            public void update (float sx, float sy, long now){
                // actualiza las posiciones
                updatePositions(sx, sy, now);

                // fijamos un límite de iteraciones

                final int NUM_MAX_ITERATIONS = 10;
                /*
                * Resuelve las colisiones, cada parte se prueba
                * contra cada otra parte por colisión. Si una
                * colisión es detectada la parte se mueve alrededor
                * de un campo virtual
                */

                boolean more = true;
                final int count = mBalls.length;
                for (int k = 0; k < NUM_MAX_ITERATIONS && more; k++)
                {
                    more = false;
                    for (int i = 0; i < count; i++) {

                        Parte curr = mBalls[i];
                        for (int j = i + 1; j < count; j++) {

                            Parte ball = mBalls[j];
                            float dx = ball.mPosX - curr.mPosX;
                            float dy = ball.mPosY - curr.mPosY;
                            float dd = dx * dx + dy * dy;
                            // Revisar las colisiones
                            if (dd <= sBallDiameter2) { //es necesario comprar el diametro de las dos partes, para que el diametro de una parte no sea menor que la otra parte.

                                /*
                                 * Agregamos un poco de entropía
                                 * para generar la alteración
                                 */
                                dx += ((float) Math.random() - 0.5f) * 0.0001f;
                                dy += ((float) Math.random() - 0.5f) * 0.0001f;
                                dd = dx * dx + dy * dy;

                                final float d = (float) Math.sqrt(dd);
                                final float c = (0.5f * (sBallDiameter - d)) / d;
                                final float effectX = dx * c;
                                final float effectY = dy * c;
                                curr.mPosX -= effectX; //actualiza el valor de x y y ,porque hubo colision
                                curr.mPosY -= effectY;
                                ball.mPosX += effectX;
                                ball.mPosY += effectY;
                                more = true;
                            }
                        }
                        curr.resolveCollisionWithBounds();
                    }
                }
            }
            public int getParteCount() {
                return mBalls.length;
            }
            public float getPosX(int i) {
                return mBalls[i].mPosX;
            }
            public float getPosY(int i) {
                return mBalls[i].mPosY;
            }
        }



        //-----------------------------------------------------------------------------------------

        public class Parte extends View{

            private float mPosX;
            private float mPosY;
            private float mVelX;
            private float mVelY;

            public Parte(Context context){
                super(context);
            }

            public Parte(Context context,AttributeSet attributeSet){
                super(context,attributeSet);
            }

            public Parte(Context context,AttributeSet attributeSet,int defStyleAttr){
                super(context,attributeSet,defStyleAttr);
            }

            //se incluye estos metodos porque es parte de la clase parte
            public void computePhysics(float sx, float sy, float dT){

                final float ax = -sx/5;
                final float ay = -sy/5;

                mPosX += mVelX * dT + ax * dT * dT /2;
                mPosY += mVelY * dT + ax * dT * dT /2;

                mVelX += ax * dT;
                mVelY += ay * dT;
            }

            //-----------------------------------------------------------------------------------------hoy

            public void resolveCollisionWithBounds(){
                final float xmax = mHorizontalBound;
                final float ymax = mVerticalBound;
                final float x = mPosX;
                final float y = mPosY;

                if(x >xmax){
                    mPosX = xmax;
                    mVelX = 0;
                }
                else if (x < -xmax){
                    mPosX = -xmax;
                    mVelX = 0;
                }
                if (y > ymax){
                    mPosY = ymax;
                    mVelY = 0;
                }
                else if (y < -ymax){
                    mPosY = -ymax;
                    mVelY = 0;
                }
            }
        }


        //-----------------------------------------------------------------------------------------


        public VistaSimulacion(@NonNull Context context){
            super(context); // se ejecutar el metodo equivalente a la clase padre

            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mXDpi = metrics.xdpi;
            mYDpi = metrics.ydpi;
            mMetersToPixelsX = mXDpi / 0.0254f;
            mMetersToPixelsY = mYDpi / 0.0254f;

            // rescale the ball so it’s about 0.5 cm on screen

            mDstWidth = (int) (sBallDiameter * mMetersToPixelsX + 0.5f);
            mDstHeight = (int) (sBallDiameter * mMetersToPixelsY + 0.5f);

            mSistemaPartes = new SistemaPartes();

            BitmapFactory.Options opts = new BitmapFactory.Options(); //ayuda a establecer las opciones que se han determinado
            opts.inDither = true;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
        }

        // para iniciar y terminar la captura del sensor

        public void startSimulation(){
            mSensorManager.registerListener(this,mAccelerometer,SensorManager.SENSOR_DELAY_GAME);
        }

        public void stopSimulation(){
            mSensorManager.unregisterListener(this);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh){ //para el cambio de tamaño de la pantalla
            mXOrigin = (w - mDstWidth) * 0.5f;
            mYOrigin = (h - mDstHeight) * 0.5f;
            mHorizontalBound = ((w / mMetersToPixelsX - sBallDiameter) * 0.5f);
            mVerticalBound = ((h / mMetersToPixelsY - sBallDiameter) * 0.5f);
        }


        @Override
        public void onSensorChanged(SensorEvent sensorEvent) { //se implementará para capturar la rotación y enviarla para la actualización del movimiento de las partes.
            if (sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER) { //comprueba si el sensor es el mismo que se esta utilizando
                return;
            }

            /*
            * Almacena los datos del acelerómetro, el timestamp
            como el tiempo
            * actual. El último se necesita para poder calcular el
            tiempo
            * actual de renderizado.
            */

            switch (mDisplay.getRotation()){
                case Surface.ROTATION_0:
                    mSensorX = sensorEvent.values[0];
                    mSensorY = sensorEvent.values[1];
                    break;

                case Surface.ROTATION_90:
                    mSensorX = -sensorEvent.values[1];
                    mSensorY = sensorEvent.values[0];
                    break;

                case Surface.ROTATION_180:
                    mSensorX = -sensorEvent.values[0];
                    mSensorY = -sensorEvent.values[1];
                    break;

                case Surface.ROTATION_270:
                    mSensorX = sensorEvent.values[1];
                    mSensorY = -sensorEvent.values[0];
                    break;
            }
        }

        //El siguiente método premitirá redibujar los objetos basados en las posiciones

        @Override
        protected void onDraw(Canvas canvas){ //es un metodo de la clase View que permite dibujar
            /*
             * Procesa la nueva posición del objeto,
             * basada en el acelerómetro y el tiempo
             */
            final SistemaPartes sistemaPartes = mSistemaPartes;
            final long now = System.currentTimeMillis();
            final float sx = mSensorX;
            final float sy = mSensorY;

            sistemaPartes.update(sx, sy, now);

            final float xc = mXOrigin;
            final float yc = mYOrigin;
            final float xs = mMetersToPixelsX;
            final float ys = mMetersToPixelsY;
            final int count = sistemaPartes.getParteCount();

            for(int i = 0; i < count; i++){
                /*
                * Transformamos el canvas para que coincida con el
                sistema
                * de coordenadas, de esta forma se pueda graficar
                * el sistema de coordenadas.
                */
                final float x = xc + sistemaPartes.getPosX(i) * xs;
                final float y = yc - sistemaPartes.getPosY(i) * ys;
                sistemaPartes.mBalls[i].setTranslationX(x);
                sistemaPartes.mBalls[i].setTranslationY(y);
            }

            // y dibuja los cambios realizados
            invalidate();
        }



        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Instanciamos SensorManager

        setContentView(R.layout.activity_main);

        // Instanciamos SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Instanciamos PowerManager
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

        // Instanciamos WindowManager
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mDisplay = mWindowManager.getDefaultDisplay();
        // Crear un WakeLock
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());

        // Instanciar la VistaSimulacion para remplazar la actividad
        // instantiate our simulation view and set it as the activity’s content
        mSimulationView = new VistaSimulacion(this);
        mSimulationView.setBackgroundResource(R.drawable.space);
        setContentView(mSimulationView);
    }

    @Override
    protected  void onResume(){
        super.onResume();
        /*
        * Cuando la actividad es activada, se configura para
        que no
        * se apague la pantalla por inactividad de botones.
        */
        mWakeLock.acquire();

        // Iniciar la simulación
        mSimulationView.startSimulation();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Detenemos la simulación
        mSimulationView.stopSimulation();

        // y liberamos la pantalla
        mWakeLock.release();
    }
}
