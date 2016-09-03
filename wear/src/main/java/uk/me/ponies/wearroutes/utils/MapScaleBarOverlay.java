package uk.me.ponies.wearroutes.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.lang.ref.WeakReference;

/**
 * Attempt to add a scale bar
 * Created by rummy on 22/07/2016.
 */
public class MapScaleBarOverlay extends TextView {
    private static final String STR_M = "m";
    private static final String STR_KM = "km";

    //Constants and config
    private static float scaleBarProportion = 0.5f;
    private final float MARGIN_LEFT_DP = 4;
    private final float END_CAP_SIZE_DP = 8;
    private final float MARGIN_TOP_DP = 6;
    private final float MARGIN_BOTTOM_DP = 2;
    private final float TEXT_SIZE_DP = 26;
    private final float GAP_SCALE_LINE_TO_TEXT_DP = 1;
    private final int BACKGROUND_RECT_ALPHA = 160; // 80 is faint, but not always strong enough, 128 is close
    private boolean mDrawHalfTick = false; // draw the half way tick on the scale bar
    // private int mBarStyle = 0; /// 0 =  ├──┤ , 1 = └──┘, 2 = ┌─┐
    private boolean mDrawQuarterTick = false;  // draw quarter ticks on the scale bar
    private boolean mHalfEndCaps = true; // only draw half of end caps, produces a kind of ┌─┐ shape


    //instantiation
    private Context context;
    private int width, height, pi;
    private float marginLeftPx, marginTopPx, marginBottomPx, endCapSizePx;
    private float gapScaleLineToTextPx;


    private Paint paintLine, paintText, paintRectangle, paintSolidRectangle;
    private Paint paintLineTicks;
    private Paint paintLineCaps;
    private float ds;
    private WeakReference<GoogleMap> mapRef;
    private int textHeight; // the height of the rendered text, used in measure

    private Location l0;
    private Location l1;


    public MapScaleBarOverlay(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public MapScaleBarOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    public MapScaleBarOverlay(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        this.context = context;

        ds = this.context.getApplicationContext().getResources().getDisplayMetrics().density;
        width = this.context.getApplicationContext().getResources().getDisplayMetrics().widthPixels;
        height = this.context.getApplicationContext().getResources().getDisplayMetrics().heightPixels;

        paintText = new TextPaint();
        paintText.setARGB(180, 0, 0, 0);
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(TEXT_SIZE_DP * ds); // ds is display density
        Rect bounds = new Rect();
        paintText.getTextBounds("1", 0, 1, bounds); // text_x_size is pixels wide of the text
        textHeight = bounds.height();

        paintRectangle = new Paint();
        paintRectangle.setARGB(BACKGROUND_RECT_ALPHA, 255, 255, 255);
        paintRectangle.setAntiAlias(true);

        paintSolidRectangle = new Paint();
        paintSolidRectangle.setARGB(255, 255, 255, 255);
        paintSolidRectangle.setAntiAlias(false);

        paintLine = new Paint();
        paintLine.setARGB(180, 0, 0, 0);
        paintLine.setAntiAlias(true);
        paintLine.setStrokeWidth(ds);

        paintLineCaps = new Paint();
        paintLineCaps.setARGB(180, 0, 0, 0);
        paintLineCaps.setAntiAlias(true);
        paintLine.setStrokeWidth(ds);

        paintLineTicks = new Paint();
        paintLineTicks.setARGB(180, 0, 0, 255);
        paintLineTicks.setAntiAlias(true);
        paintLine.setStrokeWidth(ds);


        l0 = new Location("none");
        l1 = new Location("none");


        pi = 0; //WAS pi = (int) (height - distanceFromBottom *ds);

        marginLeftPx = MARGIN_LEFT_DP * ds;
        endCapSizePx = END_CAP_SIZE_DP * ds;
        marginTopPx = MARGIN_TOP_DP * ds;
        marginBottomPx = MARGIN_BOTTOM_DP * ds;
        gapScaleLineToTextPx = GAP_SCALE_LINE_TO_TEXT_DP * ds;

        // force the view size
        //this.setWidth((int) (width * scaleBarProportion + marginLeftPx));
        //this.setHeight(height);
        //this.setLeft(0);
        //this.setBottom(height);


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        // setMeasuredDimension(100, 50);
        int desiredWidth = (int) (width * scaleBarProportion + marginLeftPx) + paddingLeft + paddingRight;
        int desiredHeight = (int) (textHeight + paddingTop + paddingBottom + marginTopPx + marginBottomPx + gapScaleLineToTextPx);

        setMeasuredDimension(desiredWidth, desiredHeight);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!"".equals(getText())) {
            setText("");
        }
        super.onDraw(canvas);


        if (mapRef != null && mapRef.get() != null) {
            draw(canvas, mapRef.get(), false);
        }

    }

    private void draw(Canvas canvas, GoogleMap mapview, boolean shadow) {
        // super.draw(canvas, mapview, shadow);
        if (true
            //&& 1 < mapview.getZoomLevel()
                ) {

            //Calculate scale bar size and units
            // g0,g1 are coords of left/right at the MIDDLE vertically of the screen
            LatLng g0 = mapview.getProjection().fromScreenLocation(new Point(0, height / 2));
            LatLng g1 = mapview.getProjection().fromScreenLocation(new Point(width, height / 2));
            l0.setLatitude(g0.latitude);
            l0.setLongitude(g0.longitude);
            l1.setLatitude(g1.latitude);
            l1.setLongitude(g1.longitude);
            // mapWidthMeters is the distance in meters of the displayed view in the middle vertically of the screen
            float mapWidthMeters = l0.distanceTo(l1);

            // I think scaleBarProportion is supposed to be how much horizontally the scale bar occupies
            // so scaleBarMaxLengthMeters is the width in meters of scaleBarProportion of the screen
            float scaleBarMaxLengthMeters = mapWidthMeters * scaleBarProportion;
            // multiply scaleBarMaxLengthMeters by a unit conversion factor if needed
            float scaleBarMaxLengthUnits;
            String unit;
            if (scaleBarMaxLengthMeters > 1000) {
                unit = STR_KM;
                scaleBarMaxLengthUnits = scaleBarMaxLengthMeters / 1000;
            } else {
                unit = STR_M;
                scaleBarMaxLengthUnits = scaleBarMaxLengthMeters;
            }
            // scaleBarMaxLengthUnits is now turned into miles or km/meters as required

            int i = 1;
            do {
                i *= 10;
            } while (i <= scaleBarMaxLengthUnits);
            i /= 10;
            float dcd02 = (int) (scaleBarMaxLengthUnits / i) * i; // dcd02 is power of 10 units ish though it can be e.g. 60m
            float barSize = dcd02 * width / mapWidthMeters * scaleBarMaxLengthMeters / scaleBarMaxLengthUnits; // barSize unknown

            @SuppressLint("DefaultLocale")
            String text = String.format("%.0f %s", dcd02, unit);

            float textWidthPx = paintText.measureText(text); // textWidthPx is pixels wide of the text
            float x_size = barSize + textWidthPx / 2 + 2 * marginLeftPx;

            //Draw semi opaque rectangle
            canvas.drawRect(0, pi, x_size, pi + marginTopPx + paintText.getFontSpacing() + marginBottomPx, paintRectangle);


            if (mHalfEndCaps) {
                //Draw solid white rectangle where the bar is
                canvas.drawRect(marginLeftPx - 1, pi, marginLeftPx + barSize + 1, pi + 1 + endCapSizePx / 2 + 1, paintSolidRectangle);
                //Draw line
                canvas.drawLine(marginLeftPx, pi + 1 + endCapSizePx / 2, marginLeftPx + barSize, pi + 1 + endCapSizePx / 2, paintLine);
                //Draw line end caps
                canvas.drawLine(marginLeftPx, pi + 1, marginLeftPx, pi + 1 + endCapSizePx / 2, paintLineCaps);
                canvas.drawLine(marginLeftPx + barSize, pi + 1, marginLeftPx + barSize, pi + 1 + endCapSizePx / 2, paintLineCaps);
            } else {
                //Draw line
                canvas.drawLine(marginLeftPx, pi + marginTopPx, marginLeftPx + barSize, pi + marginTopPx, paintLine);
                //Draw line end caps
                canvas.drawLine(marginLeftPx, pi + marginTopPx - endCapSizePx / 2, marginLeftPx, pi + marginTopPx + endCapSizePx / 2, paintLineCaps);
                canvas.drawLine(marginLeftPx + barSize, pi + marginTopPx - endCapSizePx / 2, marginLeftPx + barSize, pi + marginTopPx + endCapSizePx / 2, paintLineCaps);
            }


            //Draw Tick line middle
            if (mDrawHalfTick) {
                canvas.drawLine(marginLeftPx + barSize / 2, pi + marginTopPx - endCapSizePx / 3, marginLeftPx + barSize / 2, pi + marginTopPx + endCapSizePx / 3, paintLineTicks);
            }
            //Draw line quarters
            if (mDrawQuarterTick) {
                canvas.drawLine(marginLeftPx + barSize / 4, pi + marginTopPx - endCapSizePx / 4, marginLeftPx + barSize / 4, pi + marginTopPx + endCapSizePx / 4, paintLineTicks);
                canvas.drawLine(marginLeftPx + 3 * barSize / 4, pi + marginTopPx - endCapSizePx / 4, marginLeftPx + 3 * barSize / 4, pi + marginTopPx + endCapSizePx / 4, paintLineTicks);
            }
            //Draw text
            // was                 canvas.drawText(text, marginLeftPx +barSize, pi+marginTopPx+paintText.getFontSpacing(), paintText);
            // which had the text START in the middle of the scale bar and so it could extend beyond the scale bar
            canvas.drawText(text, marginLeftPx, pi + marginTopPx + textHeight + gapScaleLineToTextPx/* + paintText.getFontSpacing() */, paintText);
        }
    }

    public void setMap(GoogleMap map) {
        // only change if actually required
        if (mapRef == null || mapRef.get() != map) {
            mapRef = new WeakReference<GoogleMap>(map);
        }
    }

    /**
     * call this when the map has loaded and we'll redraw oursleves
     */
    public void mapLoaded() {
        super.setVisibility(VISIBLE);
        this.invalidate(); // force a redraw

    }

    /**
     * call this when the map starts to zoom and we'll clear oursleves
     */
    public void mapZooming() {
        super.setVisibility(INVISIBLE);
    }
}
