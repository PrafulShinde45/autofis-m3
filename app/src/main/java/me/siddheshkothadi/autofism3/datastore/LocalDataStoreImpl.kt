package me.siddheshkothadi.autofism3.datastore

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.siddheshkothadi.autofism3.Constants
import me.siddheshkothadi.autofism3.FishApplication
import me.siddheshkothadi.autofism3.detection.tflite.Classifier
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt

class LocalDataStoreImpl @Inject constructor(
    private val context: FishApplication
) : LocalDataStore {
    private val Context.localDataStore: DataStore<LocalData> by dataStore(
        fileName = "local_data.pb",
        serializer = LocalDataSerializer
    )
    private val localDataStore = context.localDataStore

    override val localData: Flow<LocalData>
        get() = localDataStore.data

    override val deviceId: Flow<String>
        get() = localData.map {
            it.deviceId
        }

    override val bearerToken: Flow<String>
        get() = localData.map {
            it.bearerToken
        }

    override val recognitions: Flow<Recognitions>
        get() = localData.map {
            it.recognitions
        }

    private fun getPersistentDeviceId(): String =
        Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)

    private fun generateJWT(generatedDeviceId: String): String {
        return Jwts.builder()
            .claim("device_id", generatedDeviceId)
            .signWith(SignatureAlgorithm.HS256, Constants.JWT_SECRET.toByteArray())
            .compact()
    }

    private suspend fun setDeviceId(generatedDeviceId: String) {
        localDataStore.updateData { currentLocalData ->
            currentLocalData.toBuilder()
                .setDeviceId(generatedDeviceId)
                .build()
        }
    }

    private suspend fun setBearerToken(bearerToken: String) {
        localDataStore.updateData { currentLocalData ->
            currentLocalData.toBuilder()
                .setBearerToken(bearerToken)
                .build()
        }
    }

    override suspend fun setDeviceIdAndBearerToken() {
        val generatedDeviceId = getPersistentDeviceId()
        val jwt = generateJWT(generatedDeviceId)
        val generatedBearerToken = "Bearer $jwt"
        Timber.i(generatedBearerToken)
        setDeviceId(generatedDeviceId)
        setBearerToken(generatedBearerToken)
    }

    override val bitmapInfo: Flow<BitmapInfo>
        get() = localData.map {
            it.bitmapInfo
        }

    override suspend fun setBitmap(bitmap: Bitmap) {
        val bmpInfo = BitmapInfo.newBuilder()
            .setBitmapHeight(bitmap.height)
            .setBitmapWidth(bitmap.width)
        localDataStore.updateData { currentLocalData ->
            currentLocalData.toBuilder()
                .setBitmapInfo(bmpInfo)
                .build()
        }
    }

    override suspend fun setRecognitions(list: List<Classifier.Recognition>) {
        val boundingBoxes = list.map {
            it.location
        }.map {
            BoundingBox.newBuilder()
                .setLeft(it.left)
                .setRight(it.right)
                .setTop(it.top)
                .setBottom(it.bottom)
                .build()
        }

        val locations = Recognitions.newBuilder()
            .addAllLocation(boundingBoxes)
            .build()

        localDataStore.updateData { currentLocalData ->
            currentLocalData.toBuilder()
                .setRecognitions(locations)
                .build()
        }
    }

    // Unused
    private fun getRandomString(): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        var string = ""
        for (i in (1..20)) {
            string += allowedChars[(Math.random() * (allowedChars.size - 1)).roundToInt()]
        }
        return string
    }

    private fun getDeviceName(): String {
        val deviceName =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) Settings.Global.getString(
                context.contentResolver,
                Settings.Global.DEVICE_NAME
            ) else Settings.Secure.getString(context.contentResolver, "bluetooth_name")

        return deviceName.replace("\\s+".toRegex(), "")
    }

    private fun generateDeviceId(): String {
        val deviceName = getDeviceName()
        val randomString = getRandomString()
        val timestamp = System.currentTimeMillis()
        val uniqueID = UUID.randomUUID().toString()

        return "$deviceName-$randomString-$timestamp-$uniqueID"
    }
}