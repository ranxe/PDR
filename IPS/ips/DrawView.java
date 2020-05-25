//package com.example.ryu_10.ips;
//
//
//
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.util.AttributeSet;
//import android.view.View;
//
//public class DrawView extends View {
//
//    float x = 0;
//    float y = 0;
//    float iniX = 100;
//    float iniY = 100;
//
//    public void setX(float x) {
//        this.x = x;
//    }
//    public void setY(float y) {
//        this.y = y;
//    }
//    public void set_iniX(float iniX) {this.iniX=iniX;}
//    public void set_iniY(float iniY) {this.iniX=iniY;}
//
//    public DrawView(Context context) {
//        super(context);
//    }
//    public DrawView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setColor(Color.RED);
//        canvas.drawCircle(iniX + x*10, iniY + y*10, 30, paint);
//
//    }
//}
//
