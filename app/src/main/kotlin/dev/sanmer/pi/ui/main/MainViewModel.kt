package dev.sanmer.pi.ui.main

import android.content.Context
import android.content.pm.UserInfo
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sanmer.pi.Const
import dev.sanmer.pi.Logger
import dev.sanmer.pi.core.compat.ContextCompat.userId
import dev.sanmer.pi.core.compat.UserHandleCompat
import dev.sanmer.pi.core.delegate.UserManagerDelegate
import dev.sanmer.pi.core.parser.IPackageInfo
import dev.sanmer.pi.core.parser.PackageInfoLite
import dev.sanmer.pi.core.parser.PackageParser
import dev.sanmer.pi.core.parser.SplitConfig
import dev.sanmer.pi.model.LoadData
import dev.sanmer.pi.model.LoadData.Default.loadData
import dev.sanmer.pi.repository.SuRepository
import dev.sanmer.pi.service.InstallService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(
    private val suRepository: SuRepository
) : ViewModel() {
    val state = suRepository.state
    private val pm by lazy { suRepository.getPackageManager() }

    val uris = mutableStateListOf<Uri>()
    private val packageInfos = mutableStateMapOf<Uri, LoadData<IPackageInfo>>()
    private val fileNames = mutableStateMapOf<Uri, SnapshotStateList<String>>()

    val users = mutableStateListOf<UserInfo>()
    private val targetUsers = mutableStateListOf<Int>()

    var content by mutableStateOf<Content>(Content.Main)

    private val logger = Logger.Android("MainViewModel")

    init {
        logger.d("init")
        loadUsers()
        launchSu()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            suRepository.state.collect {
                it.onSuccess { wrapper ->
                    val um = UserManagerDelegate { wrapper.wrap(this) }
                    users.clear()
                    users.addAll(um.getUsers())
                    targetUsers.clear()
                    targetUsers.add(UserHandleCompat.myUserId())
                }
            }
        }
    }

    private fun IPackageInfo.Apk.addCurrentPackageInfo(context: Context) =
        copy(
            currentPackageInfo = try {
                pm.getPackageInfo(
                    packageInfo.packageName, 0, context.userId
                ).let { PackageInfoLite.from(context, it) }
            } catch (_: Throwable) {
                null
            }
        )

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
            fileNames.removeAll(fileNames.filter { it.contains(splitConfig.name) })
        } else {
            fileNames.add(splitConfig.fileName)
        }
    }

    fun launchSu() {
        viewModelScope.launch {
            suRepository.launch()
        }
    }

    fun fromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uris.contains(uri)) return@launch
            uris.add(uri)
            packageInfos[uri] = LoadData.Loading
            packageInfos[uri] = loadData {
                val cr = context.contentResolver
                val fd = cr.openAssetFileDescriptor(uri, "r")
                if (fd == null) {
                    uris.remove(uri)
                    packageInfos.remove(uri)
                    return@launch
                }

                state.first { it.isSuccess }
                when (val packageInfo = fd.use(PackageParser::loadPackage)) {
                    is IPackageInfo.Apk -> packageInfo.addCurrentPackageInfo(context)

                    is IPackageInfo.Apks -> {
                        fileNames[uri] = packageInfo.splitConfigs
                            .mapNotNull { if (it.isRecommended) it.fileName else null }
                            .toMutableStateList()
                            .apply { add(PackageParser.BASE_APK) }

                        packageInfo.copy(
                            base = packageInfo.base.addCurrentPackageInfo(context),
                            splitConfigs = packageInfo.splitConfigs.sortedWith(
                                compareBy<SplitConfig> {
                                    when (it.type) {
                                        SplitConfig.Type.Feature -> it.name
                                        else -> it.configForSplit
                                    }
                                }.thenBy {
                                    it.type
                                }
                            )
                        )
                    }

                    is IPackageInfo.Zip -> {
                        fileNames[uri] = packageInfo.packageInfos.keys
                            .toMutableStateList()

                        IPackageInfo.Zip(
                            packageInfo.packageInfos.mapValues { (_, packageInfo) ->
                                packageInfo.addCurrentPackageInfo(context)
                            }
                        )
                    }
                }
            }.onFailure {
                logger.e(it)
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
            installerPackageName = Const.SHELL,
            users = users.filter(::isUserSelected)
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
            installerPackageName = Const.PLAY_STORE,
            users = users.filter(::isUserSelected)
        )
        if (content is Content.Apks) {
            content = Content.Main
        }
        uris.remove(uri)
        packageInfos.remove(uri)
        fileNames.remove(uri)
    }

    fun install(context: Context, uri: Uri, apk: IPackageInfo.Apk, fileName: String) {
        InstallService.start(
            context = context,
            uri = uri,
            fileNames = listOf(fileName),
            sizeBytes = apk.sizeBytes,
            packageInfo = apk.packageInfo,
            installerPackageName = Const.SHELL,
            users = users.filter(::isUserSelected)
        )
        val filenames = fileNames.getValue(uri)
        filenames.remove(fileName)
        if (filenames.isEmpty()) {
            if (content is Content.Zip) {
                content = Content.Main
            }
            uris.remove(uri)
            packageInfos.remove(uri)
            fileNames.remove(uri)
        }
    }

    sealed interface Content {
        data object Main : Content

        data class Apks(
            val uri: Uri,
            val packageInfo: IPackageInfo.Apks
        ) : Content

        data class Zip(
            val uri: Uri,
            val packageInfos: Map<String, IPackageInfo.Apk>
        ) : Content
    }
}