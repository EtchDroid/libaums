/*
 * (C) Copyright 2014 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.github.mjdev.libaums

import android.content.Context
import android.hardware.usb.*
import android.os.Parcel
import android.util.Log
import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.driver.BlockDeviceDriverFactory
import com.github.mjdev.libaums.partition.Partition
import com.github.mjdev.libaums.partition.PartitionTable
import com.github.mjdev.libaums.partition.PartitionTableFactory
import com.github.mjdev.libaums.usb.UsbCommunication
import com.github.mjdev.libaums.usb.UsbCommunicationFactory
import eu.depau.commons.android.kotlin.KParcelable
import eu.depau.commons.android.kotlin.parcelableCreator
import eu.depau.commons.android.kotlin.readTypedObjectCompat
import eu.depau.commons.android.kotlin.writeTypedObjectCompat
import java.io.Closeable
import java.io.IOException
import java.util.*

/**
 * Class representing a connected USB mass storage device. You can enumerate
 * through all connected mass storage devices via
 * [.getMassStorageDevices]. This method only returns supported
 * devices or if no device is connected an empty array.
 *
 *
 * After choosing a device you have to get the permission for the underlying
 * [android.hardware.usb.UsbDevice]. The underlying
 * [android.hardware.usb.UsbDevice] can be accessed via
 * [.getUsbDevice].
 *
 *
 * After that you need to call [.setupDevice]. This will initialize the
 * mass storage device and read the partitions (
 * [com.github.mjdev.libaums.partition.Partition]).
 *
 *
 * The supported partitions can then be accessed via [.getPartitions]
 * and you can begin to read directories and files.
 *
 * This class implements Parcelable, so it can be packed into an Intent.
 * However beware that .init() needs to be called again when the object is
 * unparceled, so remember to also close it when passing it to another
 * component.
 *
 * Also note that this means that the other component may have to re-request
 * the USB device permission if it's not the same app.
 *
 * @author mjahnen
 */
class UsbMassStorageDevice
private constructor(
        /**
         * This returns the [android.hardware.usb.UsbDevice] which can be used
         * to request permission for communication.
         *
         * @return Underlying [android.hardware.usb.UsbDevice] used for
         * communication.
         */
        val usbDevice: UsbDevice,
        private val usbInterface: UsbInterface,
        private val inEndpoint: UsbEndpoint,
        private val outEndpoint: UsbEndpoint
) : Closeable, KParcelable {

    constructor(parcel: Parcel) : this(
            usbDevice = parcel.readTypedObjectCompat(UsbDevice.CREATOR)!!,
            usbInterface = parcel.readTypedObjectCompat(UsbInterface.CREATOR)!!,
            inEndpoint = parcel.readTypedObjectCompat(UsbEndpoint.CREATOR)!!,
            outEndpoint = parcel.readTypedObjectCompat(UsbEndpoint.CREATOR)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedObjectCompat(usbDevice, flags)
        parcel.writeTypedObjectCompat(usbInterface, flags)
        parcel.writeTypedObjectCompat(inEndpoint, flags)
        parcel.writeTypedObjectCompat(outEndpoint, flags)
    }

    private lateinit var deviceConnection: UsbDeviceConnection

    /**
     * Returns the block device interface for this device.
     *
     * @return The BlockDeviceDriver implementation
     */
    lateinit var blockDevice: BlockDeviceDriver
        private set

    private lateinit var partitionTable: PartitionTable
    private val partitions = ArrayList<Partition>()
    private var initialized = false

    /**
     * Initializes the mass storage device and determines different things like
     * for example the MBR or the file systems for the different partitions.
     *
     * @throws IOException
     * If reading from the physical device fails.
     * @throws IllegalStateException
     * If permission to communicate with the underlying
     * [UsbDevice] is missing.
     * @see .getUsbDevice
     */
    @Throws(IOException::class)
    fun init(context: Context) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        if (usbManager.hasPermission(usbDevice))
            setupDevice(context)
        else
            throw IllegalStateException("Missing permission to access usb device: $usbDevice")

        initialized = true
    }

    /**
     * Sets the device up. Claims interface and initiates the device connection.
     * Chooses the right[UsbCommunication]
     * depending on the Android version (
     * [com.github.mjdev.libaums.usb.HoneyCombMr1Communication]
     * or (
     * [com.github.mjdev.libaums.usb.JellyBeanMr2Communication]
     * ).
     *
     * Initializes the [.blockDevice].
     *
     * @throws IOException
     * If reading from the physical device fails.
     * @see .init
     */
    @Throws(IOException::class)
    private fun setupDevice(context: Context) {
        Log.d(TAG, "setup device")
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        deviceConnection = usbManager.openDevice(usbDevice)
                ?: throw IOException("Unable to open USB device")

        val claimed = deviceConnection.claimInterface(usbInterface, true)

        if (!claimed) {
            throw IOException("Could not claim USB interface")
        }

        val usbCommunication = UsbCommunicationFactory.createUsbCommunication(deviceConnection, outEndpoint, inEndpoint)

        val b = ByteArray(1)
        deviceConnection.controlTransfer(161, 254, 0, usbInterface.id, b, 1, 5000)

        Log.i(TAG, "MAX LUN " + b[0].toInt())

        blockDevice = BlockDeviceDriverFactory
                .createBlockDevice(usbCommunication)
                .apply {
                    init()
                }
    }

    /**
     * Fills [.partitions] with the information received by the
     * [.partitionTable].
     *
     * @throws IOException
     * If reading from the [.blockDevice] fails.
     */
    @Throws(IOException::class)
    fun initPartitions() {
        partitionTable = PartitionTableFactory.createPartitionTable(blockDevice)

        for (entry in partitionTable.partitionTableEntries)
            Partition.createPartition(entry, blockDevice).also {
                partitions.add(it)
            }
    }

    /**
     * Releases the [android.hardware.usb.UsbInterface] and closes the
     * [android.hardware.usb.UsbDeviceConnection]. After calling this
     * method no further communication is possible. That means you can not read
     * or write from or to the partitions returned by [.getPartitions].
     */
    override fun close() {
        Log.d(TAG, "Closing device")

        val interfaceReleased = deviceConnection.releaseInterface(usbInterface)
        if (!interfaceReleased)
            Log.e(TAG, "Could not release interface")

        deviceConnection.close()
        initialized = false
    }

    /**
     * Returns the available partitions of the mass storage device. You have to
     * call [.init] before calling this method!
     *
     * @return List of partitions.
     */
    fun getPartitions(): List<Partition> {
        return partitions
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::UsbMassStorageDevice)

        private val TAG = UsbMassStorageDevice::class.java.simpleName

        /**
         * subclass 6 means that the usb mass storage device implements the SCSI
         * transparent command set
         */
        private const val INTERFACE_SUBCLASS = 6

        /**
         * protocol 80 means the communication happens only via bulk transfers
         */
        private const val INTERFACE_PROTOCOL = 80

        /**
         * This method iterates through all connected USB devices and searches for
         * mass storage devices.
         *
         * @param context
         * Context to get the [UsbManager]
         * @return An array of suitable mass storage devices or an empty array if
         * none could be found.
         */
        fun getMassStorageDevices(context: Context): Array<UsbMassStorageDevice> {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val result = ArrayList<UsbMassStorageDevice>()

            for (device in usbManager.deviceList.values) {
                Log.i(TAG, "Found USB device: $device")

                for (i in 0 until device.interfaceCount) {
                    val usbInterface = device.getInterface(i)
                    Log.i(TAG, "Found USB interface: $usbInterface")

                    // We currently only support SCSI transparent command set with
                    // bulk transfers only!
                    if (usbInterface.interfaceClass != UsbConstants.USB_CLASS_MASS_STORAGE
                            || usbInterface.interfaceSubclass != INTERFACE_SUBCLASS
                            || usbInterface.interfaceProtocol != INTERFACE_PROTOCOL) {
                        Log.i(TAG, "Device interface not suitable")
                        continue
                    }

                    // Every mass storage device has exactly two endpoints
                    // One IN and one OUT endpoint
                    if (usbInterface.endpointCount != 2)
                        Log.w(TAG, "Inteface endpoint count != 2")

                    var outEndpoint: UsbEndpoint? = null
                    var inEndpoint: UsbEndpoint? = null

                    for (j in 0 until usbInterface.endpointCount) {
                        val endpoint = usbInterface.getEndpoint(j)
                        Log.i(TAG, "Found usb endpoint: $endpoint")

                        if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (endpoint.direction == UsbConstants.USB_DIR_OUT)
                                outEndpoint = endpoint
                            else
                                inEndpoint = endpoint
                        }
                    }

                    if (outEndpoint == null || inEndpoint == null) {
                        Log.e(TAG, "Not all needed endpoints found!")
                        continue
                    }

                    result.add(
                            UsbMassStorageDevice(
                                    device, usbInterface, inEndpoint, outEndpoint
                            )
                    )

                }
            }

            return result.toTypedArray()
        }
    }
}
