package com.developer.fabian.codescanner.ui;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.developer.fabian.codescanner.R;
import com.developer.fabian.codescanner.client.CodeScannerApiClient;
import com.developer.fabian.codescanner.entity.Category;
import com.developer.fabian.codescanner.entity.Location;
import com.developer.fabian.codescanner.entity.TerminalScanner;
import com.github.fafaldo.fabtoolbar.widget.FABToolbarLayout;
import com.google.zxing.Result;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ZXingScannerView.ResultHandler {

    //TODO: Éstos datos son de la versión demo.
    private static final String TERMINAL_VALUE = "XYZ_DEMO";

    private static final String FORMAT_DATE = "ddMMyyyy";
    private static final int PERMISSION_CAM = 123;

    private CoordinatorLayout layout;
    private EditText txtLocation;
    private EditText txtCategory;
    private ImageView imvCamera;
    private FABToolbarLayout fabToolbar;
    private FloatingActionButton fabMenu;
    private ZXingScannerView scannerView;

    private ImageButton imbFocus;
    private ImageButton imbScan;
    private ImageButton imbFlash;

    private View scan;
    private View flash;
    private View focus;
    private View listCode;

    private boolean isFlash;
    private boolean isFocus;
    private boolean isCamera;

    private ArrayList<String> listCodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        fabMenu = findViewById(R.id.fabMenu);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        layout = findViewById(R.id.cordinatorContainter);
        txtLocation = findViewById(R.id.txtLocation);
        txtCategory = findViewById(R.id.txtCategory);
        imvCamera = findViewById(R.id.imvCamera);
        fabToolbar = findViewById(R.id.fabToolbar);

        focus = findViewById(R.id.imbFocus);
        scan = findViewById(R.id.imbScan);
        flash = findViewById(R.id.imbFlash);
        listCode = findViewById(R.id.imbListCodes);

        imbFocus = findViewById(R.id.imbFocus);
        imbScan = findViewById(R.id.imbScan);
        imbFlash = findViewById(R.id.imbFlash);

        setListener();

        listCodes = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CAM);
            }
        }

        enableScanTools(false);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (scannerView != null && (scannerView.isActivated() && isCamera))
            scannerView.stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (scannerView != null && (!scannerView.isActivated() && isCamera))
            scannerView.startCamera();

        if (!isConnected())
            Snackbar.make(layout, R.string.messageConnection, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CAM:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, R.string.permissionMessage, Toast.LENGTH_SHORT).show();

                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fabMenu:
                fabToolbar.show();
                break;
            case R.id.imbFocus:
                if (isFocus)
                    setFocus(false);
                else
                    setFocus(true);
                break;
            case R.id.imbScan:
                if (isCamera)
                    destroyScanner();
                else
                    setupScanner();
                break;
            case R.id.imbFlash:
                if (isFlash)
                    setFlash(false);
                else
                    setFlash(true);
                break;
            case R.id.imbListCodes:
                showDialogCodes();
                break;
        }

        fabToolbar.hide();
    }

    @Override
    public void handleResult(Result result) {
        String resultScan = result.getText();

        if (txtLocation.isFocusable()) {
            txtLocation.setText(resultScan);

            txtLocation.setFocusable(false);
            txtCategory.setFocusable(true);
        } else if (txtCategory.isFocusable()) {
            txtCategory.setText(resultScan);

            txtLocation.setFocusable(false);
            txtCategory.setFocusable(false);
        } else {
            addCodeToList(resultScan);
        }

        destroyScanner();
        isCamera = false;
    }

    private boolean isConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void setListener() {
        fabMenu.setOnClickListener(this);
        focus.setOnClickListener(this);
        scan.setOnClickListener(this);
        flash.setOnClickListener(this);
        listCode.setOnClickListener(this);
    }

    private void enableScanTools(boolean enabled) {
        focus.setEnabled(enabled);
        flash.setEnabled(enabled);
    }

    private void setFocus(boolean isFocus) {
        if (isFocus) {
            Toast.makeText(this, R.string.messageFocusOn, Toast.LENGTH_SHORT).show();
            imbFocus.setImageResource(R.drawable.ic_visibility_on);
        } else {
            Toast.makeText(this, R.string.messageFocusOff, Toast.LENGTH_SHORT).show();
            imbFocus.setImageResource(R.drawable.ic_visibility_off);
        }

        scannerView.setFocusable(isFocus);
        this.isFocus = isFocus;
    }

    private void setFlash(boolean isFlash) {
        if (isFlash) {
            Toast.makeText(this, R.string.messageFlashOn, Toast.LENGTH_SHORT).show();
            imbFlash.setImageResource(R.drawable.ic_flash_on);
        } else {
            Toast.makeText(this, R.string.messageFlashOff, Toast.LENGTH_SHORT).show();
            imbFlash.setImageResource(R.drawable.ic_flash_off);
        }

        scannerView.setFlash(isFlash);
        this.isFlash = isFlash;
    }

    private void setupScanner() {
        scannerView = findViewById(R.id.scanner);
        scannerView.setResultHandler(this);

        scannerView.setVisibility(View.VISIBLE);
        imvCamera.setVisibility(View.GONE);

        scannerView.startCamera();

        Toast.makeText(MainActivity.this, R.string.messageCameraOn, Toast.LENGTH_SHORT).show();
        imbScan.setImageResource(R.drawable.ic_camera_on);
        isCamera = true;

        enableScanTools(true);
    }

    private void destroyScanner() {
        setFocus(false);
        setFlash(false);
        scannerView.stopCamera();

        scannerView.setVisibility(View.INVISIBLE);
        imvCamera.setVisibility(View.VISIBLE);

        scannerView = null;

        Toast.makeText(MainActivity.this, R.string.messageCameraOff, Toast.LENGTH_SHORT).show();
        imbScan.setImageResource(R.drawable.ic_camera_off);
        isCamera = false;

        enableScanTools(false);
    }

    private void addCodeToList(String resultScan) {
        boolean existsCode = false;

        for (String item : listCodes) {
            if (item.equals(resultScan)) {
                existsCode = true;
                break;
            }
        }

        if (!existsCode) {
            listCodes.add(resultScan);
            Toast.makeText(this, R.string.messageAddListCode, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.messageCodeAlreadyScan, Toast.LENGTH_SHORT).show();
        }
    }

    private void showDialogCodes() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (listCodes.size() > 0) {
            final CharSequence[] listSequence = listCodes.toArray(new CharSequence[listCodes.size()]);

            builder.setTitle(R.string.dialogTitleCodes)
                    .setItems(listSequence, null)
                    .setPositiveButton(R.string.textButtonSaveDialog, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            saveCodeScannedToData();
                        }
                    });
        } else {
            builder.setTitle(R.string.dialogTitleCodes)
                    .setMessage(R.string.emptyMessageCodes)
                    .setPositiveButton(R.string.textButtonOKDialog, null);
        }

        builder.show();
    }

    private void saveCodeScannedToData() {
        if (isConnected()) {
            String[] codes = new String[listCodes.size()];
            codes = listCodes.toArray(codes);

            Category category = new Category();
            category.setId(txtCategory.getText().toString());
            category.setCodes(codes);

            Location location = new Location();
            location.setId(txtLocation.getText().toString());
            location.setCategory(category);

            TerminalScanner terminalScanner = new TerminalScanner();
            terminalScanner.setTerminal(TERMINAL_VALUE);
            terminalScanner.setDate("");
            terminalScanner.setLocations(location);

            CodeScannerApiClient codeScannerApiClient = new CodeScannerApiClient(this, layout);
            codeScannerApiClient.execute(terminalScanner);
        } else {
            Snackbar.make(layout, R.string.messageConnection, Snackbar.LENGTH_LONG).show();
        }
    }
}
