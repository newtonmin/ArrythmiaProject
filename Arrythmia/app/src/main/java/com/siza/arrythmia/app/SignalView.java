package com.siza.arrythmia.app;

/**
 * Created by Siza on 6/2/2014.
 */
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SignalView extends SurfaceView implements SurfaceHolder.Callback{

    // plot area size
    public static int width = 400;
    public static int height = 300;

    private static int[] ch1_data;
    private static int[] ch2_data;
    private static int ch1_pos; //HEIGHT/2;
    private static int ch2_pos; //HEIGHT/2;

    private SignalPlotThread plot_thread;

    private Paint ch1_color = new Paint();
    private Paint ch2_color = new Paint();
    private Paint grid_paint = new Paint();
    private Paint cross_paint = new Paint();
    private Paint outline_paint = new Paint();

    public SignalView(Context context, AttributeSet attrs){
        super(context, attrs);

        getHolder().addCallback(this);

        // initial values
        ch1_data = new int[width];
        ch2_data = new int[width];
        ch1_pos = height/2;
        ch2_pos = height/2;

        for(int x=0; x<width; x++){
            ch1_data[x] = ch1_pos;
            ch2_data[x] = ch2_pos;
        }

        plot_thread = new SignalPlotThread(getHolder(), this);

        ch1_color.setColor(Color.YELLOW);
        ch2_color.setColor(Color.RED);
        grid_paint.setColor(Color.rgb(100, 100, 100));
        cross_paint.setColor(Color.rgb(70, 100, 70));
        outline_paint.setColor(Color.GREEN);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        //Get the SurfaceView layout parameters
        android.view.ViewGroup.LayoutParams lp = this.getLayoutParams();

        lp.width = w; // store the width
        lp.height = h; // store the height

        //Commit the layout parameters
        this.setLayoutParams(lp);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
        plot_thread.setRunning(true);
        plot_thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        boolean retry = true;
        plot_thread.setRunning(false);
        while (retry){
            try{
                plot_thread.join();
                retry = false;
            }catch(InterruptedException e){

            }
        }
    }

    @Override
    public void onDraw(Canvas canvas){
        PlotPoints(canvas);
    }

    public void set_data(int[] data1, int[] data2 ){

        plot_thread.setRunning(false);

        for(int x=0; x<width; x++){
            // channel 1
            if(x<(data1.length)){
                ch1_data[x] = height-data1[x]+1;
            }else{
                ch1_data[x] = ch1_pos;
            }
            // channel 2
            if(x<(data1.length)){
                ch2_data[x] = height-data2[x]+1;
            }else{
                ch2_data[x] = ch2_pos;
            }
        }
        plot_thread.setRunning(true);
    }

    public void PlotPoints(Canvas canvas){
        // clear screen
        canvas.drawColor(Color.rgb(20, 20, 20));

        // draw vertical grids
        for(int vertical = 1; vertical<10; vertical++){
            canvas.drawLine(
                    vertical*(width/10)+1, 1,
                    vertical*(width/10)+1, height+1,
                    grid_paint);
        }
        // draw horizontal grids
        for(int horizontal = 1; horizontal<10; horizontal++){
            canvas.drawLine(
                    1, horizontal*(height/10)+1,
                    width+1, horizontal*(height/10)+1,
                    grid_paint);
        }
        // draw outline
        canvas.drawLine(0, 0, (width+1), 0, outline_paint);	// top
        canvas.drawLine((width+1), 0, (width+1), (height+1), outline_paint); //right
        canvas.drawLine(0, (height+1), (width+1), (height+1), outline_paint); // bottom
        canvas.drawLine(0, 0, 0, (height+1), outline_paint); //left

        // plot data
        for(int x=0; x<(width-1); x++){
            canvas.drawLine(x+1, ch2_data[x], x+2, ch2_data[x+1], ch2_color);
            canvas.drawLine(x+1, ch1_data[x], x+2, ch1_data[x+1], ch1_color);
        }
    }
}