package com.example.android.drivegallery;

import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import static android.R.attr.data;
import static android.R.id.message;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * Request code for opener
     */
    private static final int REQUEST_CODE_OPENER = 2;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    private ListView mResultsListView;
    private ResultsAdapter mResultsAdapter;

    private DriveFolder workFolder;
    private DriveFolder pickedFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mResultsListView = (ListView) findViewById(R.id.list_content);
        mResultsAdapter = new ResultsAdapter(this);
        mResultsListView.setAdapter(mResultsAdapter);
    }

    /**
     * Called when activity gets visible. A connection to Drive services need to
     * be initiated as soon as the activity is visible. Registers
     * {@code ConnectionCallbacks} and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Called when activity gets invisible. Connection to Drive service needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection failed: " + connectionResult.toString());
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                Log.e(TAG, "Exception while starting resolution activity", e);
            }
        } else {
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
        }
    }

    public void createFolder(View view) {
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("New folder").build();
        Drive.DriveApi.getRootFolder(getGoogleApiClient()).createFolder(
                getGoogleApiClient(), changeSet).setResultCallback(callback);
    }

    public void createFolderInFolder(View view) {
        if (workFolder != null) {
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle("New folder in a folder").build();
            workFolder.createFolder(getGoogleApiClient(), changeSet).setResultCallback(callback);
        }
    }

    final ResultCallback<DriveFolder.DriveFolderResult> callback = new ResultCallback<DriveFolder.DriveFolderResult>() {
        @Override
        public void onResult(DriveFolder.DriveFolderResult result) {
            if (!result.getStatus().isSuccess()) {
                showMessage("Error while trying to create the folder");
                return;
            }

            if (workFolder == null) workFolder = result.getDriveFolder();

            showMessage("Created a folder: " + result.getDriveFolder().getDriveId());
        }
    };

    public void pickFolder(View view) {
        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[] { DriveFolder.MIME_TYPE })
                .build(getGoogleApiClient());
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }

    public void displayFiles(View view) {
        if (pickedFolder != null) {
            /*MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle("New folder in a folder 2").build();
            pickedFolder.createFolder(
                    getGoogleApiClient(), changeSet).setResultCallback(callback);*/

            pickedFolder.listChildren(getGoogleApiClient()).setResultCallback(childrenRetrievedCallback);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_CODE_OPENER:
                if (resultCode == RESULT_OK) {
                    DriveId driveId = data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    showMessage("Selected folder's ID: " + driveId);
                    pickedFolder = driveId.asDriveFolder();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    ResultCallback<DriveApi.MetadataBufferResult> childrenRetrievedCallback = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Problem while retrieving files");
                        return;
                    }
                    mResultsAdapter.clear();
                    mResultsAdapter.append(result.getMetadataBuffer());
                    showMessage("Successfully listed files.");
                }
            };

    /**
     * Shows a toast message.
     */
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Getter for the {@code mGoogleApiClient}.
     */
    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }
}
