package com.outsystems.bluegps

import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.IBinder
import $appid.MapActivity
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.data.model.advertising.AdvertisingStatus
import com.synapseslab.bluegps_sdk.data.model.advertising.ServiceStatus
import com.synapseslab.bluegps_sdk.data.model.environment.SdkEnvironment
import com.synapseslab.bluegps_sdk.data.model.environment.SdkEnvironmentLoggedUser
import com.synapseslab.bluegps_sdk.data.model.map.*
import com.synapseslab.bluegps_sdk.service.BlueGPSAdvertisingService
import com.synapseslab.bluegps_sdk.utils.Resource
import kotlinx.coroutines.*
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.PluginResult
import org.json.JSONArray
import kotlin.coroutines.CoroutineContext

class BlueGPS: CordovaPlugin() {


    private val INIT = "init"
    private val LOGIN = "login"
    private val OPENMAP = "openMap"
    private val STARTADV = "startAdv"
    private val STOPADV = "stopAdv"

    companion object{
        lateinit var sdkEnvironment:SdkEnvironment
        lateinit var configurationMap: ConfigurationMap
    }


    private var blueGPSAdvertisingService: BlueGPSAdvertisingService? = null

    private lateinit var callback:CallbackContext


    private val coroutineContext: CoroutineContext = cordova.threadPool.asCoroutineDispatcher()
    private val myscope = CoroutineScope(coroutineContext)

    override fun execute(
        action: String?,
        cordovaArgs: JSONArray?,
        callbackContext: CallbackContext?
    ): Boolean {
        callback = callbackContext!!
        val args:JSONArray = cordovaArgs!!
        var status = false
        val result:PluginResult? = when(action){
            INIT ->{

                cordova.threadPool.execute{
                    sdkEnvironment = SdkEnvironment()
                    sdkEnvironment.sdkKey = args.getString(0)
                    sdkEnvironment.sdkSecret = args.getString(1)
                    sdkEnvironment.sdkEndpoint = args.getString(2)
                    sdkEnvironment.appId = args.getString(3)
                    var enabledNetworkLogs = false
                    if (args.length() > 5) {
                        val loggedUser = SdkEnvironmentLoggedUser()
                        if (args.optBoolean(4, false)) {
                            loggedUser.password = args.getString(5)
                            loggedUser.username = args.getString(6)
                            enabledNetworkLogs = args.getBoolean(7)
                        } else {
                            loggedUser.token = args.getString(5)
                            enabledNetworkLogs = args.getBoolean(6)
                        }
                        sdkEnvironment.loggedUser = loggedUser
                    } else {
                        enabledNetworkLogs = args.getBoolean(4)
                    }
                    BlueGPSLib.instance.initSDK(sdkEnvironment, cordova.activity, enabledNetworkLogs)
                    myscope.launch {
                        when (val result = BlueGPSLib.instance.registerSDK(sdkEnvironment)) {
                            is Resource.Success -> {
                                callback.sendPluginResult(PluginResult(PluginResult.Status.OK))
                            }
                            is Resource.Error -> {
                                callback.sendPluginResult(PluginResult(PluginResult.Status.ERROR,"SDK register Failed with error:"+result.message))
                            }
                        }
                    }


                }
                status = true
                null
            }
            LOGIN ->{
                cordova.threadPool.execute {
                    val loggedUser = SdkEnvironmentLoggedUser()
                    loggedUser.username = args.getString(0)
                    loggedUser.password = args.getString(1)
                    loggedUser.token = args.getString(2)
                    sdkEnvironment.loggedUser = loggedUser

                    myscope.launch {
                        when (val result = BlueGPSLib.instance.registerSDK(sdkEnvironment)) {
                            is Resource.Success -> {
                                callback.sendPluginResult(PluginResult(PluginResult.Status.OK))
                            }
                            is Resource.Error -> {
                                callback.sendPluginResult(PluginResult(PluginResult.Status.ERROR,"SDK register Failed with error:"+result.message))
                            }
                        }
                    }
                }
                status = true
                null
            }
            OPENMAP->{
                val mapIntent = Intent(cordova.activity,MapActivity::class.java)
                configurationMap = ConfigurationMap()
                val credential = mutableMapOf<String, String>()
                credential["sdkKey"] = sdkEnvironment.sdkKey!!
                credential["sdkSecret"] = sdkEnvironment.sdkSecret!!
                val authParameters = if (sdkEnvironment.loggedUser != null){

                    val user = mutableMapOf<String, String>()
                    if (sdkEnvironment.loggedUser!!.token != null){
                        user["token"] = sdkEnvironment.loggedUser!!.token!!
                    }else{
                        user["username"] = sdkEnvironment.loggedUser!!.username!!
                        user["password"] = sdkEnvironment.loggedUser!!.password!!
                    }
                    AuthParameters(sdkCredential = credential,loggedUser = user)
                }else{
                    AuthParameters(sdkCredential = credential)
                }
                configurationMap.auth = authParameters

                val mapStyle = MapStyle()
                val style = args.getJSONObject(1)

                val icons = style.getJSONObject("icons")
                mapStyle.icons = IconStyle()
                if (icons.has("opacity")) mapStyle.icons!!.opacity = icons.getInt("opacity")
                if (icons.has("name")) mapStyle.icons!!.name = icons.getString("name")
                if (icons.has("align")) mapStyle.icons!!.align = icons.getString("align")
                if (icons.has("vAlign")) mapStyle.icons!!.vAlign = icons.getString("vAlign")
                if (icons.has("followZoom")) mapStyle.icons!!.followZoom = icons.getBoolean("followZoom")

                val indication = style.getJSONObject("indication")
                mapStyle.indication = IndicationStyle()
                if (indication.has("destColor")) mapStyle.indication!!.destColor = indication.getString("destColor")
                if (indication.has("followZoom")) mapStyle.indication!!.followZoom = indication.getBoolean("followZoom")
                if (indication.has("iconDestination")) mapStyle.indication!!.iconDestination = indication.getString("iconDestination")
                if (indication.has("iconHAlign")) mapStyle.indication!!.iconHAlign = indication.getString("iconHAlign")
                if (indication.has("iconSource")) mapStyle.indication!!.iconSource = indication.getString("iconSource")
                if (indication.has("iconVAlign")) mapStyle.indication!!.iconVAlign = indication.getString("iconVAlign")
                if (indication.has("opacity")) mapStyle.indication!!.opacity = indication.getDouble("opacity")
                if (indication.has("radiusMeter")) mapStyle.indication!!.radiusMeter = indication.getDouble("radiusMeter")

                val navigation = style.getJSONObject("navigation")
                mapStyle.navigation = NavigationStyle()

                if (navigation.has("animationTime")) mapStyle.navigation!!.animationTime = navigation.getDouble("animationTime")
                if (navigation.has("autoZoom")) mapStyle.navigation!!.autoZoom = navigation.getBoolean("autoZoom")
                if (navigation.has("iconDestination")) mapStyle.navigation!!.iconDestination = navigation.getString("iconDestination")
                if (navigation.has("iconSource")) mapStyle.navigation!!.iconSource = navigation.getString("iconSource")
                if (navigation.has("jumpColor")) mapStyle.navigation!!.jumpColor = navigation.getString("jumpColor")
                if (navigation.has("jumpOpacity")) mapStyle.navigation!!.jumpOpacity = navigation.getDouble("jumpOpacity")
                if (navigation.has("jumpRadiusMeter")) mapStyle.navigation!!.jumpRadiusMeter = navigation.getDouble("jumpRadiusMeter")
                if (navigation.has("navigationStep")) mapStyle.navigation!!.navigationStep = navigation.getDouble("navigationStep")
                if (navigation.has("showVoronoy")) mapStyle.navigation!!.showVoronoy = navigation.getBoolean("showVoronoy")
                if (navigation.has("stroke")) mapStyle.navigation!!.stroke = navigation.getString("stroke")
                if (navigation.has("strokeLinecap")) mapStyle.navigation!!.strokeLinecap = navigation.getString("strokeLinecap")
                if (navigation.has("strokeLinejoin")) mapStyle.navigation!!.strokeLinejoin = navigation.getString("strokeLinejoin")
                if (navigation.has("strokeOpacity")) mapStyle.navigation!!.strokeOpacity = navigation.getDouble("strokeOpacity")
                if (navigation.has("strokeWidthMeter")) mapStyle.navigation!!.strokeWidthMeter = navigation.getDouble("strokeWidthMeter")
                //TODO check possible values
                //if (navigation.has("velocityOptions")) mapStyle.navigation!!.velocityOptions = navigation.getString("velocityOptions")

                val showmap = args.getJSONObject(2)
                configurationMap.show = ShowMap(all = showmap.optBoolean("all",false),
                desk = showmap.optBoolean("desk",false),
                me = showmap.optBoolean("me",false),
                room = showmap.optBoolean("room",false),
                park = showmap.optBoolean("park",false))

                configurationMap.tagid = args.optString(0,"DEMO00000001")

                cordova.activity.startActivity(mapIntent)
                status = true
                PluginResult(PluginResult.Status.OK)
            }
            STARTADV->{

                cordova.threadPool.execute {
                    blueGPSAdvertisingService?.startAdv()
                }
                status = true
                PluginResult(PluginResult.Status.OK)

            }
            STOPADV->{
                cordova.threadPool.execute {
                    blueGPSAdvertisingService?.stopAdv()
                }
                status = true
                PluginResult(PluginResult.Status.OK)

            }
            else -> PluginResult(PluginResult.Status.ERROR,"Invalid Action!")
        }
        if (result != null){
            callbackContext?.sendPluginResult(result)
        }
        return status
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(cordova.activity, BlueGPSAdvertisingService::class.java)
        cordova.activity.bindService(
            serviceIntent,
            advertisingServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }
    override fun onResume(multitasking: Boolean) {
        super.onResume(multitasking)
        cordova.activity.registerReceiver(
            advertisingServiceReceiver,
            IntentFilter(BlueGPSAdvertisingService.ACTION_ADV)
        )
    }

    override fun onPause(multitasking: Boolean) {
        super.onPause(multitasking)
        cordova.activity.unregisterReceiver(advertisingServiceReceiver)
    }

    override fun onStop() {
        super.onStop()
        cordova.activity.unbindService(advertisingServiceConnection)
    }

    private fun checkStatusBluetooth() {
        try {
            val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (!bluetoothAdapter.isEnabled) {
                bluetoothAdapter.enable()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val advertisingServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // We've bound to BlueGPSAdvertisingService, cast the IBinder and get BlueGPSAdvertisingService instance
            val binder = service as BlueGPSAdvertisingService.LocalBinder
            blueGPSAdvertisingService = binder.serviceBlueGPS
        }

        override fun onServiceDisconnected(name: ComponentName) {
            blueGPSAdvertisingService = null
        }
    }

    private val advertisingServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BlueGPSAdvertisingService.ACTION_ADV) {
                intent.getParcelableExtra<AdvertisingStatus>(BlueGPSAdvertisingService.DATA_ADV)?.let {

                    //Log.d(TAG, "- Service ${it.status} ${it.message}")

                    when(it.status) {
                        ServiceStatus.STARTED -> {

                        }
                        ServiceStatus.STOPPED -> {

                        }
                        ServiceStatus.ERROR -> {

                        }
                    }
                }
            }
        }
    }


}