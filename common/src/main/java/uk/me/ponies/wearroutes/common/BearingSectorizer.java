package uk.me.ponies.wearroutes.common;

import android.util.Log;

import uk.me.ponies.wearroutes.common.logging.DebugEnabled;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * Created by rummy on 13/05/2016.
 * takes an orientation and effectively rounds it into a compass sector
 * also applied some hysterises to avoid jiggling on sector edges
 */
public class BearingSectorizer {

    private static final String TAG = "BearingSectorizer";

    private boolean mHasLastKnownSector = false;
    private float mLastKnownSectorDegrees;
    private float mSectorDegrees = FIVE_DEGREES;
    private int mNumberOfSectors = 72;
    private float mHalfSectorDegrees = mSectorDegrees / 2f;
    private float mSectorHysteresisInDegrees = mSectorDegrees / 16f;

    /* N, W, S, E */
    public static final int QUADRANTS = 4;
    /**
     * n, nw, w etc
     */
    public static final int OCTANTS = 8;
    /**
     * n, nnw, nw, wnw, w etc
     */
    public static int HEXADECANTS = 16;
    public static int FIVE_DEGREES = 360 / 5;
    public static int TEN_DEGREES = 360 / 10;

    public BearingSectorizer() {
        this(FIVE_DEGREES);
    }

    public BearingSectorizer(int pSectors) {
        setNumberOfSectors(pSectors);
    }

    public float getNumberOfSectors() {
        return mNumberOfSectors;
    }

    public void setNumberOfSectors(int newNumberOfSectors) {
        if (newNumberOfSectors <= 0) {
            throw new IllegalArgumentException("newNumberOfSectors must be positive, it was " + newNumberOfSectors);
        }

        mNumberOfSectors = newNumberOfSectors;
        mSectorDegrees = 360f / newNumberOfSectors;
        mHalfSectorDegrees = mSectorDegrees / 2f;
        mSectorHysteresisInDegrees = mSectorDegrees / 16f;
    }


    public float convertToSectorDegrees(float orientation) {
        float input = orientation;

        //  Log.d("SectoredOrientation","Got " + orientation + " last was " + mLastKnownSectorDegrees);
        // First see if it is still in the same sector (+/- 1/16th of the sector)
        // adding 360 onto both numbers to avoid issues with comparing 359 against 0
        if (mHasLastKnownSector) {

            float delta = mLastKnownSectorDegrees - orientation;
            if (delta < 0) {
                delta += 360.0f;
            }
            if (delta > 180.0f) {
                delta = Math.abs(delta - 360.0f);
            }
            if (delta < (mHalfSectorDegrees + mSectorHysteresisInDegrees)) {
                if (tagEnabled(TAG)) Log.d(TAG, "returning unchanged: " + mLastKnownSectorDegrees + "(delta:" + delta + " is less than " + (mHalfSectorDegrees + mSectorHysteresisInDegrees));
                return mLastKnownSectorDegrees;
            }
        }
        // otherwise it looks like it has changed sector
        // compute sector


        orientation += 360; // add on 360 to avoid issues with 359 -> 0

        // do "truncation"
        orientation = ((int) (0.5f + (orientation / mSectorDegrees))) * mSectorDegrees;
        while (orientation >= 360) { // convert back to range 0..<360 // probably a Math. function?
            orientation -= 360;
        }

        if (orientation != input) {
            if (tagEnabled(TAG)) Log.d(TAG, "clamped to :" + orientation + " from:" + input);
        }
        if (mLastKnownSectorDegrees != orientation) {
            if (tagEnabled(TAG)) Log.d(TAG, "returning changed: " + orientation);
        } else {
            // Log.d("SectoredOrientation", "returning unChanged: " + orientation);
        }
        mLastKnownSectorDegrees = orientation;
        return orientation;

    }

    private void resetHasLastKnownOrientation() {
        mHasLastKnownSector = false;
    }

    public static void main(String[] args) {
        BearingSectorizer testee = new BearingSectorizer(QUADRANTS);
        for (int f = 0; f < 360; f++) {
            testee.resetHasLastKnownOrientation();
            System.out.println(f + "=" + testee.convertToSectorDegrees(f));
        }
    }
}
