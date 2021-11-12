/*
 * (c) Copyright 2021. All rights reserved by Synapses S.r.l.s.
 * https://www.synapseslab.com/
 *
 * Created by Davide Agostini on 22/06/21, 12:56.
 * Last modified 22/06/21, 12:56
 */

package $appid

import android.os.Bundle
import android.transition.Fade
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synapseslab.bluegps_sdk.component.map.BlueGPSMapListener
import com.synapseslab.bluegps_sdk.data.model.map.*
import com.synapseslab.bluegps_sdk.data.model.stats.NavInfo
import com.synapseslab.bluegps_sdk.data.model.stats.NavigationStats

import $appid.databinding.ActivityMapBinding
import com.outsystems.bluegps.BlueGPS

private val TAG = "MapActivity"

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private var toolbarView: View? = null

    /*
    private var configurationMap = ConfigurationMap(
        tagid = "DEMO00000001",
        style = MapStyle(
            icons = IconStyle(
                name = "bluegps",
                align = "center",
                vAlign = "bottom",
                followZoom = true
            ),
            navigation = NavigationStyle(
                iconSource = "/api/public/resource/icons/commons/start.svg",
                iconDestination = "/api/public/resource/icons/commons/end.svg",
                velocityOptions = mutableMapOf("foot" to 4.0, "bike" to 10.0),
                navigationStep = 1.5,
                autoZoom = true,
                showVoronoy = false
            )
        ),
        show = ShowMap(all = false, me = true, room = true),
    )*/

    private var hideRoomLayer = false

    private var navigationMode = false

    private lateinit var source: Position

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportActionBar?.title = "Map View"
        tapViewClickListener()

        //Receive Map Config and sdk Config also login info

        /**
         * The BlueGPSMapView component expose an initMap method for initialize the web view
         * with the required parameters and load the start url. [ *baseURL* + api/public/resource/sdk/mobile.html]
         */
        binding.webView.initMap(
            sdkEnvironment = BlueGPS.sdkEnvironment,
            configurationMap = BlueGPS.configurationMap
        )

        setListenerOnMapView()
        setOnNavigationModeButtonListener()
        setOnGoToClickListener()
    }

    /**
     * Setup the listener for BlueGPSMapView in order to implement the code
     * to run when an event click on map occurs.
     */
    private fun setListenerOnMapView() {
        binding.webView.setBlueGPSMapListener(object : BlueGPSMapListener {
            override fun resolvePromise(data: JavascriptCallback, typeMapCallback: TypeMapCallback) {
                /**
                 * Callback that intercept the click on the map
                 *
                 * @param data the clicked point with all info.
                 * @param typeMapCallback the type of the clicked point.
                 *
                 */
                when (typeMapCallback) {
                    TypeMapCallback.INIT_SDK_END -> {
                        Log.d(TAG, TAG + " INIT_SDK_END")
                    }
                    TypeMapCallback.PARK_CONF -> {
                        val cType = object : TypeToken<PayloadResponse>() {}.type
                        val payloadResponse = Gson().fromJson<PayloadResponse>(data.payload, cType)
                        if (payloadResponse.availableDateList!!.isNotEmpty()) {
                            Log.d(TAG, TAG + payloadResponse.availableDateList)
                        }
                    }
                    TypeMapCallback.ROOM_CLICK, TypeMapCallback.MAP_CLICK, TypeMapCallback.TAG_CLICK -> {
                        val cType = object : TypeToken<Position>() {}.type
                        val payloadResponse = Gson().fromJson<Position>(data.payload, cType)
                        if (navigationMode) {
                            runOnUiThread {
                                source = payloadResponse
                                binding.tvDestination.text = "Destination: (${(payloadResponse.x.toString()).take(6)}, ${(payloadResponse.y.toString()).take(6)})"
                                showHideLayoutDestination(true)
                            }
                        } else {
                        MaterialAlertDialogBuilder(this@MapActivity)
                            .setTitle("Type: ${typeMapCallback.name}")
                            .setMessage(payloadResponse.toString())
                            .setPositiveButton("Ok") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                        }
                    }
                    TypeMapCallback.BOOKING_CLICK -> {
                        val cType = object : TypeToken<ClickedObject>() {}.type
                        val payloadResponse = Gson().fromJson<ClickedObject>(data.payload, cType)
                        MaterialAlertDialogBuilder(this@MapActivity)
                            .setTitle("Type: ${typeMapCallback.name}")
                            .setMessage(payloadResponse.toString())
                            .setPositiveButton("Ok") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                    TypeMapCallback.NAV_STATS -> {
                        val cType = object : TypeToken<NavigationStats>() {}.type
                        val payloadResponse = Gson().fromJson<NavigationStats>(data.payload, cType)
                        Log.d(TAG, " $TAG $payloadResponse ")
                        var vehicles: String? = ""
                        payloadResponse.vehicles?.let {
                            payloadResponse.vehicles!!.map { v ->
                                vehicles += "${v.name}: ${Math.round(v.remainingTimeSecond!! * 100) / 100.0}s\n"
                            }
                        }

                        runOnUiThread {
                            binding.tvRemaining.text =
                                "Remaining distance: ${Math.round(payloadResponse.remainingDistance!! * 100) / 100.0}m \n$vehicles"
                        }
                    }
                    TypeMapCallback.NAV_INFO -> {
                        val cType = object : TypeToken<NavInfo>() {}.type
                        val payloadResponse = Gson().fromJson<NavInfo>(data.payload, cType)
                        Snackbar
                            .make(
                                findViewById(android.R.id.content),
                                "${payloadResponse.message}",
                                Snackbar.LENGTH_LONG
                            )
                            .show()
                    }
                    TypeMapCallback.SUCCESS -> {
                        val cType = object : TypeToken<PayloadResponse>() {}.type
                        val payloadResponse = Gson().fromJson<PayloadResponse>(data.payload, cType)
                        Log.d(TAG, " ${payloadResponse.message} ")
                    }
                    TypeMapCallback.ERROR -> {
                        val cType = object : TypeToken<PayloadResponse>() {}.type
                        val payloadResponse = Gson().fromJson<PayloadResponse>(data.payload, cType)
                        Log.e(TAG , TAG + " ${payloadResponse.message} ")
                        Snackbar
                            .make(
                                findViewById(android.R.id.content),
                                "${payloadResponse.message}",
                                Snackbar.LENGTH_LONG
                            )
                            .show()
                    }
                }
            }
        })
    }

    /**
     * Toolbox GUI for configure and change the map control layer.
     * This demo show only some functions. Look at the documentation for all available methods.
     */
    private fun showToolbarView() {
        if (toolbarView != null && toolbarView!!.isShown) {
            return
        }

        toolbarView =
            LayoutInflater.from(this).inflate(R.layout.toolbar_map_view, binding.mapView, false)
        TransitionManager.beginDelayedTransition(binding.mapView, Fade())
        binding.mapView.addView(toolbarView)
        binding.tapView.visibility = View.VISIBLE


        val switchStatus: SwitchMaterial = toolbarView!!.findViewById(R.id.switchStatus)
        switchStatus.isChecked = BlueGPS.configurationMap.toolbox?.mapControl?.enabled ?: true
        switchStatus.setOnCheckedChangeListener { _, isChecked ->
            BlueGPS.configurationMap.toolbox?.mapControl?.enabled = isChecked
            binding.webView.updateConfigurationMap(BlueGPS.configurationMap)
        }


        val btnHorizontal: Button = toolbarView!!.findViewById(R.id.btnHorizontal)
        val btnVertical: Button = toolbarView!!.findViewById(R.id.btnVertical)

        if (BlueGPS.configurationMap.toolbox!!.mapControl!!.orientation!! == OrientationType.horizontal) {
            btnHorizontal.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.blue_500)
            btnVertical.backgroundTintList = ContextCompat.getColorStateList(this, R.color.grey)
        } else {
            btnHorizontal.backgroundTintList = ContextCompat.getColorStateList(this, R.color.grey)
            btnVertical.backgroundTintList = ContextCompat.getColorStateList(this, R.color.blue_500)
        }

        btnHorizontal.setOnClickListener {
            btnHorizontal.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.blue_500)
            btnVertical.backgroundTintList = ContextCompat.getColorStateList(this, R.color.grey)
            BlueGPS.configurationMap.toolbox?.mapControl?.orientation = OrientationType.horizontal
            binding.webView.updateConfigurationMap(BlueGPS.configurationMap)
        }

        btnVertical.setOnClickListener {
            btnVertical.backgroundTintList = ContextCompat.getColorStateList(this, R.color.blue_500)
            btnHorizontal.backgroundTintList = ContextCompat.getColorStateList(this, R.color.grey)
            BlueGPS.configurationMap.toolbox?.mapControl?.orientation = OrientationType.vertical
            binding.webView.updateConfigurationMap(BlueGPS.configurationMap)
        }


        val sliderButtonWidth: Slider = toolbarView!!.findViewById(R.id.sliderButtonWidth)
        sliderButtonWidth.value = BlueGPS.configurationMap.toolbox?.mapControl?.buttonWidth!!.toFloat()
        sliderButtonWidth.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                BlueGPS.configurationMap.toolbox?.mapControl?.buttonWidth = slider.value.toInt()
                binding.webView.updateConfigurationMap(BlueGPS.configurationMap)
            }
        })

        val sliderButtonHeight: Slider = toolbarView!!.findViewById(R.id.sliderButtonHeight)
        sliderButtonHeight.value = BlueGPS.configurationMap.toolbox?.mapControl?.buttonHeight!!.toFloat()
        sliderButtonHeight.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                BlueGPS.configurationMap.toolbox?.mapControl?.buttonHeight = slider.value.toInt()
                binding.webView.updateConfigurationMap(BlueGPS.configurationMap)
            }
        })

        val actionNextFloor: ImageButton = toolbarView!!.findViewById(R.id.actionNextFloor)
        actionNextFloor.setOnClickListener {
            binding.webView.nextFloor()
        }

        val actionResetView: ImageButton = toolbarView!!.findViewById(R.id.actionResetView)
        actionResetView.setOnClickListener {
            binding.webView.resetView()
        }

        val actionHideRoomLayer: ImageButton = toolbarView!!.findViewById(R.id.actionHideRoomLayer)
        if (!hideRoomLayer) actionHideRoomLayer.setImageResource(R.drawable.ic_baseline_layers_24)
        else actionHideRoomLayer.setImageResource(R.drawable.ic_baseline_layers_clear_24)
        actionHideRoomLayer.setOnClickListener {
            hideRoomLayer = !hideRoomLayer
            binding.webView.hideRoomLayer(hideRoomLayer)

            if (!hideRoomLayer) actionHideRoomLayer.setImageResource(R.drawable.ic_baseline_layers_24)
            else actionHideRoomLayer.setImageResource(R.drawable.ic_baseline_layers_clear_24)
        }

        val actionGetFloor: ImageButton = toolbarView!!.findViewById(R.id.actionGetFloor)
        actionGetFloor.setOnClickListener {
            binding.webView.getFloor { result, error ->

                error?.let {
                    Log.e(TAG, "$error")
                } ?: run {
                    MaterialAlertDialogBuilder(this@MapActivity)
                        .setTitle("Floor list")
                        .setMessage(result.toString())
                        .setPositiveButton("Ok") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //TODO check usability in this context
        menuInflater.inflate(R.menu.settings_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //TODO check usability in this context
        return when (item.itemId) {
            R.id.settings -> {
                showToolbarView()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun tapViewClickListener() {
        binding.tapView.setOnClickListener {
            if (toolbarView != null && toolbarView!!.isShown) {
                binding.mapView.removeView(toolbarView)
            }

            binding.tapView.visibility = View.GONE
        }
    }

    private fun setOnNavigationModeButtonListener() {
        binding.btnNavigationMode.setOnClickListener {
            navigationMode = !navigationMode

            if (navigationMode) {
                binding.btnNavigationMode.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.blue_500)
            } else {
                binding.btnNavigationMode.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.grey)

                showHideLayoutDestination(false)
                binding.tvDestination.text = ""
                binding.tvRemaining.text = ""
                binding.webView.removeNavigation()
            }
        }
    }

    private fun setOnGoToClickListener() {

        binding.btnGoTo.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                binding.webView.gotoFromMe(source, true)
            }
        })
    }

    private fun showHideLayoutDestination(visibility: Boolean) {
        if (visibility) binding.layoutDestination.visibility = View.VISIBLE else binding.layoutDestination.visibility = View.GONE
    }
}