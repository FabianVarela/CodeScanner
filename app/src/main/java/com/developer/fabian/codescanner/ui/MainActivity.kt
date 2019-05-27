package com.developer.fabian.codescanner.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import com.developer.fabian.codescanner.R
import com.developer.fabian.codescanner.client.CodeScannerApiClient
import com.developer.fabian.codescanner.entity.Category
import com.developer.fabian.codescanner.entity.Location
import com.developer.fabian.codescanner.entity.TerminalScanner
import com.github.fafaldo.fabtoolbar.widget.FABToolbarLayout
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener, ZXingScannerView.ResultHandler {

    companion object {
        private const val TERMINAL_VALUE = "XYZ_DEMO"
        private const val FORMAT_DATE = "ddMMyyyy"
        private const val PERMISSION_CAM = 123
    }

    private lateinit var layout: CoordinatorLayout
    private lateinit var txtLocation: EditText
    private lateinit var txtCategory: EditText
    private lateinit var imvCamera: ImageView
    private lateinit var fabToolbar: FABToolbarLayout
    private lateinit var fabMenu: FloatingActionButton
    private var scannerView: ZXingScannerView? = null

    private lateinit var imbFocus: ImageButton
    private lateinit var imbScan: ImageButton
    private lateinit var imbFlash: ImageButton

    private lateinit var scan: View
    private lateinit var flash: View
    private lateinit var focus: View
    private lateinit var listCode: View

    private var isFlash: Boolean = false
    private var isFocus: Boolean = false
    private var isCamera: Boolean = false

    private lateinit var listCodes: ArrayList<String>

    private val isConnected: Boolean
        get() {
            val connMgr = getSystemService(Activity.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connMgr.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        fabMenu = findViewById(R.id.fabMenu)

        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle(R.string.app_name)

        layout = findViewById(R.id.cordinatorContainter)
        txtLocation = findViewById(R.id.txtLocation)
        txtCategory = findViewById(R.id.txtCategory)
        imvCamera = findViewById(R.id.imvCamera)
        fabToolbar = findViewById(R.id.fabToolbar)

        focus = findViewById(R.id.imbFocus)
        scan = findViewById(R.id.imbScan)
        flash = findViewById(R.id.imbFlash)
        listCode = findViewById(R.id.imbListCodes)

        imbFocus = findViewById(R.id.imbFocus)
        imbScan = findViewById(R.id.imbScan)
        imbFlash = findViewById(R.id.imbFlash)

        setListener()

        listCodes = ArrayList()

        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_CAM)
        }

        enableScanTools(false)
    }

    override fun onPause() {
        super.onPause()

        if (scannerView != null && scannerView!!.isActivated && isCamera)
            scannerView!!.stopCamera()
    }

    override fun onResume() {
        super.onResume()

        if (scannerView != null && !scannerView!!.isActivated && isCamera)
            scannerView!!.startCamera()

        if (!isConnected)
            Snackbar.make(layout, R.string.messageConnection, Snackbar.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_CAM -> if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, R.string.permissionMessage, Toast.LENGTH_SHORT).show()
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.fabMenu -> fabToolbar.show()
            R.id.imbFocus -> if (isFocus)
                setFocus(false)
            else
                setFocus(true)
            R.id.imbScan -> if (isCamera)
                destroyScanner()
            else
                setupScanner()
            R.id.imbFlash -> if (isFlash)
                setFlash(false)
            else
                setFlash(true)
            R.id.imbListCodes -> showDialogCodes()
        }

        fabToolbar.hide()
    }

    override fun handleResult(result: Result) {
        val resultScan = result.text

        when {
            txtLocation.isFocusable -> {
                txtLocation.setText(resultScan)

                txtLocation.isFocusable = false
                txtCategory.isFocusable = true
            }
            txtCategory.isFocusable -> {
                txtCategory.setText(resultScan)

                txtLocation.isFocusable = false
                txtCategory.isFocusable = false
            }
            else -> addCodeToList(resultScan)
        }

        destroyScanner()
        isCamera = false
    }

    private fun setListener() {
        fabMenu.setOnClickListener(this)
        focus.setOnClickListener(this)
        scan.setOnClickListener(this)
        flash.setOnClickListener(this)
        listCode.setOnClickListener(this)
    }

    private fun enableScanTools(enabled: Boolean) {
        focus.isEnabled = enabled
        flash.isEnabled = enabled
    }

    private fun setFocus(isFocus: Boolean) {
        if (isFocus) {
            Toast.makeText(this, R.string.messageFocusOn, Toast.LENGTH_SHORT).show()
            imbFocus.setImageResource(R.drawable.ic_visibility_on)
        } else {
            Toast.makeText(this, R.string.messageFocusOff, Toast.LENGTH_SHORT).show()
            imbFocus.setImageResource(R.drawable.ic_visibility_off)
        }

        scannerView!!.isFocusable = isFocus
        this.isFocus = isFocus
    }

    private fun setFlash(isFlash: Boolean) {
        if (isFlash) {
            Toast.makeText(this, R.string.messageFlashOn, Toast.LENGTH_SHORT).show()
            imbFlash.setImageResource(R.drawable.ic_flash_on)
        } else {
            Toast.makeText(this, R.string.messageFlashOff, Toast.LENGTH_SHORT).show()
            imbFlash.setImageResource(R.drawable.ic_flash_off)
        }

        scannerView!!.flash = isFlash
        this.isFlash = isFlash
    }

    private fun setupScanner() {
        scannerView = findViewById(R.id.scanner)
        scannerView!!.setResultHandler(this)

        scannerView!!.visibility = View.VISIBLE
        imvCamera.visibility = View.GONE

        scannerView!!.startCamera()

        Toast.makeText(this@MainActivity, R.string.messageCameraOn, Toast.LENGTH_SHORT).show()
        imbScan.setImageResource(R.drawable.ic_camera_on)
        isCamera = true

        enableScanTools(true)
    }

    private fun destroyScanner() {
        setFocus(false)
        setFlash(false)
        scannerView!!.stopCamera()

        scannerView!!.visibility = View.INVISIBLE
        imvCamera.visibility = View.VISIBLE

        scannerView = null

        Toast.makeText(this@MainActivity, R.string.messageCameraOff, Toast.LENGTH_SHORT).show()
        imbScan.setImageResource(R.drawable.ic_camera_off)
        isCamera = false

        enableScanTools(false)
    }

    private fun addCodeToList(resultScan: String) {
        var existsCode = false

        for (item in listCodes) {
            if (item == resultScan) {
                existsCode = true
                break
            }
        }

        if (!existsCode) {
            listCodes.add(resultScan)
            Toast.makeText(this, R.string.messageAddListCode, Toast.LENGTH_SHORT).show()
        } else
            Toast.makeText(this, R.string.messageCodeAlreadyScan, Toast.LENGTH_SHORT).show()
    }

    private fun showDialogCodes() {
        val builder = AlertDialog.Builder(this)

        if (listCodes.size > 0) {
            val listSequence = listCodes.toTypedArray<CharSequence>()

            builder.setTitle(R.string.dialogTitleCodes)
                    .setItems(listSequence, null)
                    .setPositiveButton(R.string.textButtonSaveDialog) { _, _ -> saveCodeScannedToData() }
        } else {
            builder.setTitle(R.string.dialogTitleCodes)
                    .setMessage(R.string.emptyMessageCodes)
                    .setPositiveButton(R.string.textButtonOKDialog, null)
        }

        builder.show()
    }

    private fun saveCodeScannedToData() {
        if (isConnected) {
            val category = Category(txtCategory.text.toString(), listCodes)
            val location = Location(txtLocation.text.toString(), category)
            val terminalScanner = TerminalScanner(TERMINAL_VALUE, "", location)

            val codeScannerApiClient = CodeScannerApiClient(this, layout)
            codeScannerApiClient.execute(terminalScanner)
        } else
            Snackbar.make(layout, R.string.messageConnection, Snackbar.LENGTH_LONG).show()
    }
}
