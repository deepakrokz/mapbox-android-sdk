package com.mapbox.mapboxsdk.offline;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.exceptions.OfflineDatabaseException;
import com.mapbox.mapboxsdk.geometry.CoordinateRegion;
import com.mapbox.mapboxsdk.geometry.CoordinateSpan;
import com.mapbox.mapboxsdk.geometry.LatLng;
import java.util.Date;

public class OfflineMapDatabase implements MapboxConstants {

    private static final String TAG = "OfflineMapDatabase";

    private Context context;

    private String uniqueID;
    private String mapID;
    private boolean includesMetadata;
    private boolean includesMarkers;
    private RasterImageQuality imageQuality;
    private CoordinateRegion mapRegion;
    private Integer minimumZ;
    private Integer maximumZ;
    private String path;
    private boolean invalid;
    private boolean initializedProperly = false;

    /**
     * Default Constructor
     * @param context Context of Android app
     */
    public OfflineMapDatabase(Context context) {
        super();
        this.context = context;
    }

    public boolean initializeDatabase() {

        String uniqueID = sqliteMetadataForName("uniqueID");
        String mapID = sqliteMetadataForName("mapID");
        String includesMetadata = sqliteMetadataForName("includesMetadata");
        String includesMarkers = sqliteMetadataForName("includesMarkers");
        String imageQuality = sqliteMetadataForName("imageQuality");
        String region_latitude = sqliteMetadataForName("region_latitude");
        String region_longitude = sqliteMetadataForName("region_longitude");
        String region_latitude_delta = sqliteMetadataForName("region_latitude_delta");
        String region_longitude_delta = sqliteMetadataForName("region_longitude_delta");
        String minimumZ = sqliteMetadataForName("minimumZ");
        String maximumZ = sqliteMetadataForName("maximumZ");

        if (TextUtils.isEmpty(uniqueID)) {
            uniqueID = String.format("%s-%s-%s-%s-%s-%s-%s-%d", mapID, region_latitude, region_longitude, region_latitude_delta, region_longitude_delta, minimumZ, maximumZ, new Date().getTime() / 1000l);
        }

        if (!TextUtils.isEmpty(mapID) && !TextUtils.isEmpty(includesMetadata) && !TextUtils.isEmpty(includesMarkers) && !TextUtils.isEmpty(imageQuality)
                && !TextUtils.isEmpty(region_latitude) && !TextUtils.isEmpty(region_longitude) && !TextUtils.isEmpty(region_latitude_delta) && !TextUtils.isEmpty(region_longitude_delta)
                && !TextUtils.isEmpty(minimumZ) && !TextUtils.isEmpty(maximumZ)
                ) {
            // Reaching this point means that the specified database file at path pointed to an sqlite file which had
            // all the required values in its metadata table. That means the file passed the test for being a valid
            // offline map database.
            //
            this.uniqueID = uniqueID;
            this.mapID = mapID;
            this.includesMetadata = Boolean.getBoolean(includesMetadata);
            this.includesMarkers = Boolean.getBoolean(includesMarkers);

            this.imageQuality = RasterImageQuality.getEnumForValue(Integer.getInteger(imageQuality));

            LatLng center = new LatLng(Double.parseDouble(region_latitude), Double.parseDouble(region_longitude));
            CoordinateSpan span = new CoordinateSpan(Double.parseDouble(region_latitude_delta), Double.parseDouble(region_longitude_delta));
            this.mapRegion = new CoordinateRegion(center, span);

            this.minimumZ = Integer.getInteger(minimumZ);
            this.maximumZ = Integer.getInteger(maximumZ);

            this.initializedProperly = true;
        }
        else
        {
            // Reaching this point means the file at path isn't a valid offline map database, so we can't use it.
            Log.w(TAG, "Invalid offline map database.  Can't be used.");
        }
        return initializedProperly;
    }

    public byte[] dataForURL(String url) throws OfflineDatabaseException
    {
        byte[] data = sqliteDataForURL(url);
        if (data == null || data.length == 0)
        {
            String reason = String.format("The offline database has no data for %s", url);
            throw new OfflineDatabaseException(reason);
        }
        return data;
    }

    public void invalidate() {
        this.invalid = false;
    }

    public String sqliteMetadataForName (String name)
    {
        String query = "SELECT " + OfflineDatabaseHandler.FIELD_METADATA_VALUE + " FROM " + OfflineDatabaseHandler.TABLE_METADATA +  " WHERE " + OfflineDatabaseHandler.FIELD_METADATA_NAME + "='" + name + "';";
        SQLiteDatabase db = OfflineDatabaseHandler.getInstance(context).getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        return cursor.getString(cursor.getColumnIndex(OfflineDatabaseHandler.FIELD_METADATA_VALUE));
    }

    public byte[] sqliteDataForURL(String url)
    {
        SQLiteDatabase db = OfflineDatabaseHandler.getInstance(context).getReadableDatabase();
        String query = "SELECT " + OfflineDatabaseHandler.FIELD_DATA_VALUE + " FROM " + OfflineDatabaseHandler.TABLE_DATA +  " WHERE " + OfflineDatabaseHandler.FIELD_DATA_ID + "= (SELECT " + OfflineDatabaseHandler.FIELD_RESOURCES_ID + " from " + OfflineDatabaseHandler.TABLE_RESOURCES + " where " + OfflineDatabaseHandler.FIELD_RESOURCES_URL + " = '" + url + "');";
        Cursor cursor = db.rawQuery(query, null);
        return cursor.getBlob(cursor.getColumnIndex(OfflineDatabaseHandler.FIELD_DATA_VALUE));
    }
}
