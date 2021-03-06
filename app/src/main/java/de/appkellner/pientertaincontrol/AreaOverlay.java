package de.appkellner.pientertaincontrol;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class AreaOverlay extends View {

    private ArrayList<Point> points = null;

    private Paint circlePaint = null;

    private int width = 0;
    private int height = 0;

    private int imageWidth = 640;
    private int imageHeight = 480;

    private final MainActivity listener = null;
    private final ArrayList<Integer> cornerIndices = new ArrayList<>();

    public AreaOverlay(Context context) {
        super(context);
    }

    public AreaOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPoints(ArrayList<Point> points) {
        this.points = points;
        invalidate();
    }

    public void setImageSize(int w, int h) {
        imageWidth = w;
        imageHeight = h;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        super.onSizeChanged(w,h,oldw,oldh);
        width = w;
        height = h;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (circlePaint == null) {
            init();
        }

        if (points == null || points.size() != 4) {
            return;
        }

        for (int i=0;i<4;i++) {
            Point p1 = points.get(i);
            float x1 = (float)p1.x/(float)imageWidth*(float)width;
            float y1 = (float)p1.y/(float)imageHeight*(float)height;
            canvas.drawCircle(x1,y1, 10, circlePaint);
        }
        if (cornerIndices.isEmpty()) {
            cornerIndices.add(0);
            cornerIndices.add(1);
            cornerIndices.add(3);
            cornerIndices.add(2);
            cornerIndices.add(0);
        }
        for (int i=0;i<cornerIndices.size()-1;i++) {
            Point p1 = points.get(cornerIndices.get(i));
            float x1 = (float)p1.x/(float)imageWidth*(float)width;
            float y1 = (float)p1.y/(float)imageHeight*(float)height;
            Point p2 = points.get(cornerIndices.get(i+1));
            float x2 = (float)p2.x/(float)imageWidth*(float)width;
            float y2 = (float)p2.y/(float)imageHeight*(float)height;

            canvas.drawLine(x1,y1,x2,y2, circlePaint);

        }

    }

    private float previousX = 0.0f;
    private float previousY = 0.0f;
    private int draggedPos = 0;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (points == null || points.size() != 4) {
            return true;
        }

        float x = e.getX();
        float y = e.getY();

        PointF pTouch = new PointF(x,y);

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                float bestDistance = (float) width + (float) height;
                draggedPos = 0;
                for (int i = 0; i < 4; i++) {
                    Point p1 = points.get(i);
                    PointF pn = new PointF(
                            ((float) p1.x / (float) imageWidth) * (float) width,
                             ((float)p1.y / (float) imageHeight) * (float) height
                    );
                    if (distance(pTouch, pn) < bestDistance) {
                        bestDistance = distance(pTouch, pn);
                        draggedPos = i;
                    }
                }
            }
            break;

            case MotionEvent.ACTION_MOVE: {
                float dx = x - previousX;
                float dy = y - previousY;

                Point p1 = points.get(draggedPos);
                PointF pn = new PointF(
                        ((float) p1.x / (float) imageWidth) * (float) width,
                        ((float) p1.y / (float) imageHeight) * (float) height
                );
                pn.x += dx;
                pn.y += dy;

                p1.x = (int) ((pn.x * (float) imageWidth) / (float) width);
                p1.y = (int) ((pn.y * (float) imageHeight) / (float) height);

                p1.x = Math.max(0, p1.x);
                p1.y = Math.max(0, p1.y);

                p1.x = Math.min(imageWidth, p1.x);
                p1.y = Math.min(imageHeight, p1.y);

                break;
            }
            case MotionEvent.ACTION_UP: {

                if (listener != null) {
                    listener.setPoints(points);
                }
                // todo commit
            }
        }
        previousX = x;
        previousY = y;

        invalidate();

        return true;
    }

    private void init() {
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.RED);
        circlePaint.setStyle(Paint.Style.FILL);
    }

    private float distance(PointF p1, PointF p2) {
        return (float) Math.sqrt( ( (p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y) ) );
    }
}
