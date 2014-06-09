package com.siza.arrythmia.app;

/**
 * Created by Siza on 6/2/2014.
 */
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

public class SignalPlotThread extends Thread {
    private SurfaceHolder holder;
    private SignalView plot_area;
    private boolean _run = false;

    public SignalPlotThread(SurfaceHolder surfaceHolder, SignalView view){
        holder = surfaceHolder;
        plot_area = view;
    }
    public void setRunning(boolean run){
        _run = run;
    }

    @Override
    public void run(){
        Canvas c;
        while(_run){
            c = null;
            try{
                c = holder.lockCanvas(null);
                //Log.d("SignalPlotThread", "lockCanvas");
                synchronized (holder) {
                    plot_area.PlotPoints(c);
                }
            }catch(Exception exp){
                Log.d("SignalPlotThread", exp.toString());
            }finally{
                if(c!=null){
                    holder.unlockCanvasAndPost(c);
                }
            }
        }
    }
}
