package app.organicmaps;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

/**
 * Singleton class to hold map-related callbacks from TutActivity
 */
public class MapCallbackHolder {
    private static MapCallbackHolder sInstance;

    @Nullable
    private Function0<Unit> mOnMapReady;
    @Nullable
    private Function0<Unit> mOnMapClose;
    @Nullable
    private String mOrderId;
    @Nullable
    private Function1<? super String, Unit> mOnStartOrderClicked;
    @Nullable
    private Function1<? super String, Unit> mOnCancelOrderClicked;
    @Nullable
    private Function1<? super String, Unit> mOnCompleteOrderClicked;
    @Nullable
    private Function0<Unit> mOnShowRouteClicked;
    @Nullable
    private Function2<? super Double, ? super Double, Unit> mOnMapClick;

    private MapCallbackHolder() {
    }

    @NonNull
    public static MapCallbackHolder getInstance() {
        if (sInstance == null) {
            sInstance = new MapCallbackHolder();
        }
        return sInstance;
    }

    public void setCallbacks(
            @Nullable Function0<Unit> onMapReady,
            @Nullable Function0<Unit> onMapClose,
            @Nullable String orderId,
            @Nullable Function1<? super String, Unit> onStartOrderClicked,
            @Nullable Function1<? super String, Unit> onCancelOrderClicked,
            @Nullable Function1<? super String, Unit> onCompleteOrderClicked,
            @Nullable Function0<Unit> onShowRouteClicked,
            @Nullable Function2<? super Double, ? super Double, Unit> onMapClick) {
        mOnMapReady = onMapReady;
        mOnMapClose = onMapClose;
        mOrderId = orderId;
        mOnStartOrderClicked = onStartOrderClicked;
        mOnCancelOrderClicked = onCancelOrderClicked;
        mOnCompleteOrderClicked = onCompleteOrderClicked;
        mOnShowRouteClicked = onShowRouteClicked;
        mOnMapClick = onMapClick;
    }

    public void onMapReady() {
        if (mOnMapReady != null) {
            mOnMapReady.invoke();
        }
    }

    public void onMapClose() {
        if (mOnMapClose != null) {
            mOnMapClose.invoke();
        }
    }

    public void onStartOrderClicked() {
        if (mOnStartOrderClicked != null && mOrderId != null) {
            mOnStartOrderClicked.invoke(mOrderId);
        }
    }

    public void onCancelOrderClicked() {
        if (mOnCancelOrderClicked != null && mOrderId != null) {
            mOnCancelOrderClicked.invoke(mOrderId);
        }
    }

    public void onCompleteOrderClicked() {
        if (mOnCompleteOrderClicked != null && mOrderId != null) {
            mOnCompleteOrderClicked.invoke(mOrderId);
        }
    }

    public void onShowRouteClicked() {
        if (mOnShowRouteClicked != null) {
            mOnShowRouteClicked.invoke();
        }
    }

    public void onMapClick(double lat, double lng) {
        if (mOnMapClick != null) {
            mOnMapClick.invoke(lat, lng);
        }
    }

    @Nullable
    public String getOrderId() {
        return mOrderId;
    }

    public void clear() {
        mOnMapReady = null;
        mOnMapClose = null;
        mOrderId = null;
        mOnStartOrderClicked = null;
        mOnCancelOrderClicked = null;
        mOnCompleteOrderClicked = null;
        mOnShowRouteClicked = null;
        mOnMapClick = null;
    }
}