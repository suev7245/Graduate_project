package com.example.sm_pc.trial1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

public class Box extends View {
    float x1 = 0;
    float y1 = 0;
    float x2 = 0;
    float y2 = 0;

    void getLocation(float tmp_x1, float tmp_y1, float tmp_x2, float tmp_y2){
        x1 = tmp_x1;
        y1 = tmp_y1;
        x2 = tmp_x2;
        y2 = tmp_y2;
    }

    private Paint paint = new Paint();
    Box(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) { // Override the onDraw() Method
        super.onDraw(canvas);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.rgb(72, 146, 98));
        //paint.setColor(Color.RED);
        paint.setStrokeWidth(15);

        //draw guide box
        canvas.drawRect(x1, y1, x2, y2, paint);


        Log.d("Rect-xmin: ", String.valueOf(x1));
        Log.d("Rect-ymin: ", String.valueOf(y1));
        Log.d("Rect-xmax: ", String.valueOf(x2));
        Log.d("Rect-ymax: ", String.valueOf(y2));

        /*
        //center
        int x0 = canvas.getWidth()/2;
        int y0 = canvas.getHeight()/2;
        int dx = canvas.getHeight()/3;
        int dy = canvas.getHeight()/3;
        //draw guide box
        canvas.drawRect(x0-dx, y0-dy, x0+dx, y0+dy, paint);
        */
    }
}
