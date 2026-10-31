package dev.sanmer.pi.ui.main

import android.content.Context
import android.content.pm.UserInfo
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sanmer.pi.core.compat.ContextCompat.userId
import dev.sanmer.pi.core.compat.UserHandleCompat
import dev.sanmer.pi.core.delegate.PackageManagerDelegate
import dev.sanmer.pi.core.delegate.UserManagerDelegate
import dev.sanmer.pi.core.res.IPackageInfo
import dev.sanmer.pi.core.res.PackageParser
import dev.sanmer.pi.core.res.PackageParser.Default.toPackageLite
import dev.sanmer.pi.core.res.SplitConfig
import dev.sanmer.pi.ktx.removeLastIf
import dev.sanmer.pi.model.LoadData
import dev.sanmer.pi.repository.SuRepository
import dev.sanmer.pi.service.InstallService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class HomeViewModel(
    private val suRepository: SuRepository,
    private val packageManager: PackageManagerDelegate,
    private val userManager: UserManagerDelegate,
    private val packageParser: PackageParser
) : ViewModel() {
    var state by mutableStateOf<LoadData<Unit>>(LoadData.Loading)
        private set

    val uris = mutableStateListOf<Uri>()
    private val packageInfos = mutableStateMapOf<Uri, LoadData<IPackageInfo>>()
    private val fileNames = hashMapOf<Uri, SnapshotStateList<String>>()

    val users = mutableListOf<UserInfo>()
    private val targetUsers = mutableStateListOf<Int>()

    val backStack = mutableStateListOf<Content>(Content.Home)
    private var backStackSize = backStack.size
    val isBack get() = backStack.size.let { it < backStackSize.apply { backStackSize = it } }

    init {
        Log.d(TAG, "init")
        launchSu()
    }

    fun launchSu() {
        viewModelScope.launch {
            when (suRepository.state.first().also { state = it.map {} }) {
                is LoadData.Success<*> -> runCatching {
                    users.addAll(userManager.getUsers())
                    targetUsers.add(UserHandleCompat.myUserId())
                }

                else -> {}
            }
        }
    }

    fun isUserSelected(user: UserInfo) = targetUsers.contains(user.id)

    fun pickUser(user: UserInfo) {
        if (isUserSelected(user)) {
            targetUsers.remove(user.id)
        } else {
            targetUsers.add(user.id)
        }
    }

    fun packageInfo(uri: Uri) = packageInfos.getOrElse(uri) { LoadData.Pending }

    fun fileNames(uri: Uri) = fileNames.getOrElse(uri) { emptyList() }

    fun isSplitSelected(uri: Uri, splitConfig: SplitConfig) =
        fileNames[uri]?.contains(splitConfig.fileName) ?: false

    fun pickSplit(uri: Uri, splitConfig: SplitConfig) {
        val fileNames = fileNames[uri] ?: return
        if (fileNames.contains(splitConfig.fileName)) {
            fileNames.remove(splitConfig.fileName)
        } else {
            fileNames.add(splitConfig.fileName)
        }
    }

    private fun IPackageInfo.Apk.updateCurrentPackageInfo(userId: Int) = try {
        copy(
            currentPackageInfo = packageManager.getPackageInfo(
                packageInfo.packageName, 0, userId
            ).toPackageLite()
        )
    } catch (e: Throwable) {
        Log.w(TAG, "getPackageInfo(${packageInfo.packageName})", e)
        this
    }

    private fun List<SplitConfig>.pickRecommended(
        action: (SplitConfig) -> Unit
    ) = forEach { splitConfig ->
        if (splitConfig.configForSplit.isEmpty() || splitConfig.type is SplitConfig.Type.Language) {
            if (splitConfig.isRecommended) action(splitConfig)
        }

        if (splitConfig.requiredSplitTypes.isNotEmpty()) {
            val configsForSplit = filter { it.configForSplit == splitConfig.name }
                .sortedBy { it.type }

            splitConfig.requiredSplitTypes.forEach { type ->
                val requiredSplit = requireNotNull(
                    configsForSplit.firstOrNull {
                        it.type == type || (it.type like type && !it.isDisabled)
                    }
                ) { "${splitConfig.name} required $type but ${configsForSplit.map { it.type }}" }
                action(requiredSplit)
            }
        }
    }

    fun fromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uris.contains(uri)) return@launch else uris.add(uri)
            packageInfos[uri] = LoadData.Loading
            suRepository.state.first { it.isSuccess }
            runCatching {
                val cr = context.contentResolver
                val fd = requireNotNull(cr.openAssetFileDescriptor(uri, "r")) { uri }
                when (val packageInfo = fd.use { packageParser.parse(context, fd) }) {
                    is IPackageInfo.Apk -> packageInfo.updateCurrentPackageInfo(context.userId)

                    is IPackageInfo.Apks -> {
                        val filenames = mutableStateListOf(PackageParser.BASE_APK)
                        packageInfo.splitConfigs.pickRecommended { filenames.add(it.fileName) }
                        fileNames[uri] = filenames

                        packageInfo.copy(
                            base = packageInfo.base.updateCurrentPackageInfo(context.userId),
                            splitConfigs = packageInfo.splitConfigs.sortedWith(
                                compareBy<SplitConfig> {
                                    when (it.type) {
                                        is SplitConfig.Type.Feature -> it.name
                                        else -> it.configForSplit
                                    }
                                }.thenBy {
                                    it.type
                                }
                            )
                        )
                    }

                    is IPackageInfo.Zip -> {
                        fileNames[uri] = packageInfo.packageInfos.keys.toMutableStateList()

                        IPackageInfo.Zip(
                            packageInfo.packageInfos.mapValues { (_, packageInfo) ->
                                packageInfo.updateCurrentPackageInfo(context.userId)
                            }
                        )
                    }
                }
            }.onSuccess {
                packageInfos[uri] = LoadData.Success(it)
            }.onFailure {
                packageInfos[uri] = LoadData.Failure(it)
                Log.e(TAG, "fromUri $uri", it)

                delay(5.seconds)
                uris.remove(uri)
                packageInfos.remove(uri)
            }
        }
    }

    fun install(context: Context, uri: Uri, apk: IPackageInfo.Apk) {
        InstallService.start(
            context = context,
            uri = uri,
            fileNames = emptyList(),
            sizeBytes = apk.sizeBytes,
            packageInfo = apk.packageInfo,
            users = users.filter(::isUserSelected).ifEmpty {
                listOf(UserInfo(context.userId, null, 0))
            }
        )
        uris.remove(uri)
        packageInfos.remove(uri)
    }

    fun install(context: Context, uri: Uri, apks: IPackageInfo.Apks) {
        val filenames = fileNames.getValue(uri)
        val sizeBytes = apks.splitConfigs.sumOf {
            if (filenames.contains(it.fileName)) it.sizeBytes else 0L
        }
        InstallService.start(
            context = context,
            uri = uri,
            fileNames = filenames,
            sizeBytes = apks.base.sizeBytes + sizeBytes,
            packageInfo = apks.base.packageInfo,
            users = users.filter(::isUserSelected).ifEmpty {
                listOf(UserInfo(context.userId, null, 0))
            }
        )
        uris.remove(uri)
        packageInfos.remove(uri)
        fileNames.remove(uri)
        backStack.removeLastIf { it is Content.Apks }
    }

    fun install(context: Context, uri: Uri, apk: IPackageInfo.Apk, fileName: String) {
        InstallService.start(
            context = context,
            uri = uri,
            fileNames = listOf(fileName),
            sizeBytes = apk.sizeBytes,
            packageInfo = apk.packageInfo,
            users = users.filter(::isUserSelected).ifEmpty {
                listOf(UserInfo(context.userId, null, 0))
            }
        )
        val filenames = fileNames.getValue(uri)
        filenames.remove(fileName)
        if (filenames.isEmpty()) {
            uris.remove(uri)
            packageInfos.remove(uri)
            fileNames.remove(uri)
            backStack.removeLastIf { it is Content.Zip }
        }
    }

    sealed interface Content {
        data object Home : Content

        data class Apks(
            val uri: Uri,
            val packageInfo: IPackageInfo.Apks
        ) : Content

        data class Zip(
            val uri: Uri,
            val packageInfos: Map<String, IPackageInfo.Apk>
        ) : Content
    }

    private companion object Default {
        const val TAG = "HomeViewModel"
    }
}