package com.example.android.mygarden;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.utils.PlantUtils;

/**
 * Created by su on 2018/8/23.
 */
// (2): Create a plant watering service that extends IntentService and supports the
//  action ACTION_WATER_PLANTS which updates last_watered timestamp for all plants still alive
public class PlantWateringService extends IntentService {
    public static final String ACTION_WATER_PLANTS = "com.example.android.mygarden.action.water_plants";
    public static final String ACTION_UPDATE_PLANT_WIDGETS = "com.example.android.mygarden.action.update_plant_widgets";


    public PlantWateringService() {
        super("PlantWateringService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_WATER_PLANTS.equals(action)) {
                handleActionWaterPlants();
            }else if(ACTION_UPDATE_PLANT_WIDGETS.equals(action)){
                handleActionUpdatePlantWidgets();
            }
        }

    }

    public static void startActionWaterPlants(Context context) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_WATER_PLANTS);
        context.startService(intent);
    }

    public static void startActionUpdatePlantWidget(Context context){
        Intent intent = new Intent(context,PlantWateringService.class);
        intent.setAction(ACTION_UPDATE_PLANT_WIDGETS);
        context.startService(intent);

    }

    private void handleActionUpdatePlantWidgets(){
        //query to get the plant that is most in need for water(last watered)
        Uri PLANTS_URI = PlantContract.BASE_CONTENT_URI.buildUpon().appendPath(PlantContract.PATH_PLANTS).build();
        Cursor cursor = getContentResolver().query(PLANTS_URI,
                null, null, null, PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME);
        //extract the plant details
        int imgRes = R.drawable.grass;//Default image in case our garden is empty
        if(cursor != null && cursor.getCount() >0){
            cursor.moveToFirst();
            int createTimeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_CREATION_TIME);
            int waterTimeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME);
            int plantTypeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_PLANT_TYPE);
            long timeNow = System.currentTimeMillis();
            long waterAt = cursor.getLong(waterTimeIndex);
            long createAt = cursor.getLong(createTimeIndex);
            int plantType = cursor.getInt(plantTypeIndex);
            cursor.close();
            imgRes = PlantUtils.getPlantImageRes(this,timeNow - createAt,timeNow - waterAt,plantType);
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,PlantWidgetProvider.class));
        //Now update all widgets
        PlantWidgetProvider.updatePlantWidgets(this,appWidgetManager,imgRes,appWidgetIds);
    }
    private void handleActionWaterPlants() {
        Uri PLANTS_URI = PlantContract.BASE_CONTENT_URI.buildUpon().appendPath(PlantContract.PATH_PLANTS).build();
        ContentValues contentValues = new ContentValues();
        long timeNow = System.currentTimeMillis();
        contentValues.put(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME, timeNow);
        //update only plants that are still alive
        getContentResolver().update(
                PLANTS_URI,
                contentValues,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME + ">?",
                new String[]{String.valueOf(timeNow - PlantUtils.MAX_AGE_WITHOUT_WATER)}
        );

    }
}
