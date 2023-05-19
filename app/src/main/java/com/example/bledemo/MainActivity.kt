package com.example.bledemo

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bledemo.databinding.ActivityMainBinding
import com.example.bledemo.model.BluetoothModel
import com.example.bluethoothlistgetdemo.ui.adapter.RecyclerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.*

@Suppress("DEPRECATED_IDENTITY_EQUALS")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var bluetoothAdapter: BluetoothAdapter? = null
    lateinit var bluetoothList: ArrayList<BluetoothModel>
    var recyclerAdapter: RecyclerAdapter? = null
    var bluetoothLeScanner: BluetoothLeScanner? = null
    var bluetoothGatt: BluetoothGatt? = null
    var devicesName: BluetoothDevice? = null
    private var scanning = false
    private val handler = Handler()
    private val SCAN_PERIOD: Long = 5000
    private var isShowProgress = true
     var currentPosition:Int=0

    companion object {
        var TAG: String = MainActivity::class.java.simpleName
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bluetoothList = arrayListOf()

        val mBluetoothAdapter = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = mBluetoothAdapter.adapter
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner


        val layoutManager =
            LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)

        recyclerAdapter = RecyclerAdapter(this@MainActivity, bluetoothList) { position ->
            bluetoothGatt = bluetoothList[position].bluetoothDevices?.connectGatt(
                this,
                false,
                bluetoothGattCallback
            )
            currentPosition=position

            Log.e(TAG, "bluetoothGatt---> ${bluetoothList[position].name}]")
        }

        binding.recyclerView.layoutManager=layoutManager
        binding.recyclerView.adapter = recyclerAdapter
        bluetoothPermission()

        binding.startScanning.setOnClickListener {
            isShowProgress=!isShowProgress
            if (!isShowProgress) {
                binding.startScanning.text=getString(R.string.stop_scanning)
                binding.progressbar.visibility = View.VISIBLE
                bluetoothLeScannerDevice()
            } else if (isShowProgress) {
                binding.startScanning.text=getString(R.string.start_scanning)
                binding.progressbar.visibility = View.INVISIBLE

            }
        }
    }

    @SuppressLint("NewApi")
    private fun bluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activityLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    val activityLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            granted.entries.forEach { _ ->
                val bluetoothManager =
                    this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter
                adapter.startDiscovery()
            }
        }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private var requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Not granted", Toast.LENGTH_SHORT).show()
            }
        }

    private val leScanCallback =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            object : ScanCallback() {
                @SuppressLint("MissingPermission")
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    devicesName = result.device

                    if (result.device.name != null) {

                        Log.e(TAG, "onScanResult: ${result.device.address} - ${result.device.name}")
                        val bleList=  BluetoothModel(
                            name = result.device.name ?: "No Name",
                            address = result.device.address ?: "",
                            result.device
                        )
                      if (!bluetoothList.contains(bleList)){
                          bluetoothList.add(bleList)
                      }
                    }
                    super.onScanResult(callbackType, result)
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                    Log.e(TAG, "onBatchScanResults:${results.toString()}")
                    super.onBatchScanResults(results)
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e(TAG, "onScanFailed: $errorCode")
                    super.onScanFailed(errorCode)
                }
            }
        } else {
            TODO("VERSION.SDK_INT < LOLLIPOP")
        }

    private val bluetoothGattCallback = @SuppressLint("NewApi")
    object : BluetoothGattCallback() {

        val serviceUuid = UUID.fromString("ab0828b1-198e-4351-b779-901fa0e0371e")
        var levelUuid = UUID.fromString("4ac8a682-9736-4e5d-932b-e9b31405049c")

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "CONNECTED")
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt?.close()
                Log.d(TAG, "DISCONNECTED")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered")
                val service = gatt?.getService(serviceUuid)
                val characteristic = service?.getCharacteristic(levelUuid)

                gatt?.setCharacteristicNotification(characteristic, true)
                gatt?.readCharacteristic(characteristic)

                CoroutineScope(Dispatchers.Main).launch {
                    delay(5000)
                    binding.progressbar.visibility=View.INVISIBLE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        characteristic?.value = "1709".toByteArray()
                        gatt?.writeCharacteristic(
                            characteristic!!,
                            characteristic.value,
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        )
                    } else {
                        characteristic?.value = "1709".toByteArray()
                        gatt?.writeCharacteristic(characteristic)
                    }
                }
            }
        }


        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            try {
                super.onCharacteristicRead(gatt, characteristic, value, status)
                val batteryLevel =
                    characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                Log.d(
                    TAG, "onCharacteristicRead: Status :- $status , Value :- $value , Battery Level :- $batteryLevel"
                )
            } catch (e: Exception) {
                Log.d(TAG, "call catch $e")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            try {
                val string = String(characteristic.value, StandardCharsets.UTF_8)
                Log.d(TAG, "onCharacteristicRead: Status :- $status , Value :- ${characteristic.value} Data :- $string"
                )
            } catch (e: Exception) {
                Log.d(TAG, "Catch $e")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.d(TAG, "call onCharacteristicChanged")
            super.onCharacteristicChanged(gatt, characteristic, value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            val string = String(characteristic!!.value, StandardCharsets.UTF_8)
            Log.d(TAG, "Call onCharacteristicWrite $status Data :- $string")
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Log.d(
                TAG, "Call onDescriptorWrite $status descriptor ${descriptor.toString()} --- ${descriptor?.characteristic?.descriptors}"
            )
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
            value: ByteArray
        ) {
            super.onDescriptorRead(gatt, descriptor, status, value)
            Log.d(TAG, "Call onDescriptorRead $status")
        }

    }

    @SuppressLint("MissingPermission", "NewApi", "NotifyDataSetChanged")
    fun bluetoothLeScannerDevice() {
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                binding.progressbar.visibility=View.INVISIBLE
                bluetoothLeScanner!!.stopScan(leScanCallback)

                bluetoothList.toHashSet().forEach {
                    Log.d(
                        TAG, "Found BLE device! Name: ${it.name}, address: ${it.address}"
                    )
                    recyclerAdapter?.notifyDataSetChanged()
                }
            }, SCAN_PERIOD)
            binding.progressbar.visibility=View.INVISIBLE
            scanning = true
            bluetoothLeScanner!!.startScan(leScanCallback)
        } else {
            scanning = false
            binding.progressbar.visibility=View.INVISIBLE
            bluetoothLeScanner!!.stopScan(leScanCallback)
        }
    }

}
