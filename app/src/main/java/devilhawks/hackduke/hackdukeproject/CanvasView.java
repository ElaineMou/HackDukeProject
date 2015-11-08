package devilhawks.hackduke.hackdukeproject;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.jar.Attributes;

public class CanvasView extends View {
    private Dataset dataset;
    public int COLUMN_HEIGHT = 180;
    public int SPACE_FROM_TOP = 20;
    public int width;
    public int height;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mPaint;
    private Paint linePaint;
    private Rect rect;

    public CanvasView(Context context) {
        super(context);

        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(4f);
    }

    public CanvasView(Context context, AttributeSet attrs){
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(4f);

        linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1f);
        rect = new Rect();
    }

    public void setDataset(Dataset set){
        this.dataset = set;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        mBitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(this.dataset!=null){
            int leftPadding = 15;
            int width = getWidth() - 2*leftPadding;

            Resources res = getResources();
            mPaint.setColor(res.getColor(R.color.column1));
            float ratio = dataset.blinkRate/ShowDataActivity.maxBlinkRate;
            rect.top = (int) ((1 - ratio)*COLUMN_HEIGHT) + SPACE_FROM_TOP;
            rect.bottom = COLUMN_HEIGHT + SPACE_FROM_TOP;
            rect.left = leftPadding;
            rect.right = width/9 + leftPadding;
            canvas.drawRect(rect, mPaint);

            mPaint.setColor(res.getColor(R.color.column2));
            ratio = dataset.turnRate/ShowDataActivity.maxTurnRate;
            rect.top = (int) ((1 - ratio)*COLUMN_HEIGHT) + SPACE_FROM_TOP;
            rect.bottom = COLUMN_HEIGHT + SPACE_FROM_TOP;
            rect.left = 2*width/9 + leftPadding;
            rect.right = width/3 + leftPadding;
            canvas.drawRect(rect, mPaint);

            mPaint.setColor(res.getColor(R.color.column3));
            ratio = dataset.tiltRate/ShowDataActivity.maxTiltRate;
            rect.top = (int) ((1 - ratio)*COLUMN_HEIGHT) + SPACE_FROM_TOP;
            rect.bottom = COLUMN_HEIGHT + SPACE_FROM_TOP;
            rect.left = 4*width/9 + leftPadding;
            rect.right = 5*width/9 + leftPadding;
            canvas.drawRect(rect, mPaint);

            mPaint.setColor(res.getColor(R.color.column4));
            ratio = dataset.pauseRate/ShowDataActivity.maxPauseRate;
            rect.top = (int) ((1 - ratio)*COLUMN_HEIGHT) + SPACE_FROM_TOP;
            rect.bottom = COLUMN_HEIGHT + SPACE_FROM_TOP;
            rect.left = 2*width/3 + leftPadding;
            rect.right = 7*width/9 + leftPadding;
            canvas.drawRect(rect, mPaint);

            mPaint.setColor(res.getColor(R.color.column5));
            ratio = dataset.pauseAvg/ShowDataActivity.maxPauseAvg;
            rect.top = (int) ((1 - ratio)*COLUMN_HEIGHT) + SPACE_FROM_TOP;
            rect.bottom = COLUMN_HEIGHT + SPACE_FROM_TOP;
            rect.left = 8*width/9 + leftPadding;
            rect.right = width + leftPadding;
            canvas.drawRect(rect, mPaint);

            canvas.drawLine(leftPadding, COLUMN_HEIGHT + SPACE_FROM_TOP, leftPadding + width, COLUMN_HEIGHT + SPACE_FROM_TOP, linePaint);

            Paint textPaint = new Paint();
            textPaint.setTextSize(26f);
            canvas.drawText("Blinks", leftPadding, COLUMN_HEIGHT + SPACE_FROM_TOP + 20, textPaint);
            canvas.drawText("Turns", leftPadding + 2*width/9, COLUMN_HEIGHT + SPACE_FROM_TOP + 20, textPaint);
            canvas.drawText("Tilts", leftPadding + 4*width/9, COLUMN_HEIGHT + SPACE_FROM_TOP + 20, textPaint);
            canvas.drawText("Pauses", leftPadding + 2*width/3, COLUMN_HEIGHT + SPACE_FROM_TOP + 20, textPaint);
            canvas.drawText("Pause Time", leftPadding + 8*width/9, COLUMN_HEIGHT + SPACE_FROM_TOP + 20, textPaint);
        }
    }

    public void clearCanvas(){
        invalidate();
    }
}