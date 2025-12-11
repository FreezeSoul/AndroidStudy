package org.ninetripods.mq.study.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.FragmentActivity
import org.ninetripods.mq.study.R
import org.ninetripods.mq.study.kotlin.base.BaseFragment
import org.ninetripods.mq.study.kotlin.ktx.dp2px
import org.ninetripods.mq.study.kotlin.ktx.id
import org.ninetripods.mq.study.kotlin.ktx.showToast
import androidx.core.net.toUri
import org.ninetripods.mq.study.kotlin.ktx.log
import androidx.core.content.edit


/**
 * 权限申请时，自定义顶部TIPS
 */
class PermissionRequestFragment : BaseFragment() {
    private val btnPermission: Button by id(R.id.btn_permission_request)

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            handleCameraPermissionResult(isGranted)
        }

    override fun getLayoutId(): Int {
        return R.layout.layout_permission_request
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btnPermission.setOnClickListener { requestPermission() }
    }

    private fun handleCameraPermissionResult(isGranted: Boolean) {
        removeTopTipsView(requireActivity())
        PermissionUtils.setCameraPermissionRequested(requireActivity(), true)

        log("isGranted:$isGranted")
        if (isGranted) {
            //权限授予成功，打开相机
            showToast("权限授予成功，打开相机")
        } else {
            //权限被拒绝，检查是否永久拒绝
            val shouldShowRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)

            if (!shouldShowRationale) {
                //用户选择了"不再询问"，属于永久拒绝
                PermissionUtils.setCameraPermissionRequested(requireActivity(), false)
                showGoToSettingsDialog()
            } else {
                //临时拒绝
                showToast("相机权限被拒绝，无法使用拍照功能")
            }
        }
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(requireActivity())
            .setTitle("相机权限被永久拒绝")
            .setMessage("相机权限已被永久拒绝，请到应用设置中手动开启权限。")
            .setPositiveButton("去设置") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show()
    }

    /**
     * 打开应用设置页面
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = "package:${requireActivity().packageName}".toUri()
        startActivity(intent)
    }

    private fun requestPermission() {
        if (hasCameraPermission()) {
            showToast("已经有对应权限了")
            return
        }
        //展示tips
        if (shouldShowPermissionTips()) {
            addTopTipsView(requireActivity())
        }
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    /**
     * 判断是否应该显示权限提示 tips
     * 返回 true 的情况：用户第一次请求权限，或者之前只是临时拒绝
     * 返回 false 的情况：
     * 1. 已经有权限
     * 2. 用户永久拒绝了权限（选择了"不再询问"）
     * 3. 其他不应该展示的情况
     */
    private fun shouldShowPermissionTips(): Boolean {
        //如果已经有权限，不需要提示
        if (hasCameraPermission()) {
            return false
        }
        //判断用户之前是否请求过权限
        val hasBeenRequestedBefore = PermissionUtils.hasCameraPermissionBeenRequested(requireActivity())
        if (!hasBeenRequestedBefore) {
            return true
        }

        // 检查是否需要显示权限说明
        // 注意：shouldShowRequestPermissionRationale() 在以下情况返回 false：
        //   - 第一次请求权限
        //   - 用户永久拒绝（选择了"不再询问"）
        //   - 用户已经授予权限
        // 只有在用户临时拒绝时返回 true
        val shouldShowRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)

        // 如果shouldShowRationale为true，说明用户之前临时拒绝过，现在再次请求,这种情况下可以展示提示
        return shouldShowRationale
    }


    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 顶部展示权限说明Tips
     */
    private fun addTopTipsView(activity: FragmentActivity) {
        runCatching {
            val decorView = activity.window.decorView
            (decorView as? ViewGroup)?.let { viewGroup ->
                //检查是否已经存在tips，通过Tag查找
                val existingView = viewGroup.findViewWithTag<TextView>(CAMERA_PERMISSION_TIPS_TAG)
                if (existingView != null) return@let

                val textView = TextView(activity)
                textView.run {
                    tag = CAMERA_PERMISSION_TIPS_TAG
                    setBackgroundResource(R.drawable.shape_black_bg)
                    setTextColor(Color.WHITE)
                    typeface = Typeface.DEFAULT_BOLD
                    text = "请相机权限：“为了给您提供‘拍照搜题’服务，需要申请使用您的相机权限。我们承诺仅用于此功能，保障您的隐私安全。”"
                    textSize = 16f
                    setLineSpacing(2.dp2px().toFloat(), 1f)
                    val dpLength = 12.dp2px()
                    setPadding(dpLength)
                    val layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.leftMargin = dpLength
                    layoutParams.rightMargin = dpLength
                    layoutParams.topMargin = dpLength * 3
                    decorView.addView(textView, layoutParams)
                }
            }
        }
    }

    /**
     * 删除Tips
     */
    private fun removeTopTipsView(activity: FragmentActivity) {
        runCatching {
            val decorView = activity.window.decorView
            (decorView as? ViewGroup)?.let { viewGroup ->
                //通过Tag精确查找并移除
                val tipsView = viewGroup.findViewWithTag<TextView>(CAMERA_PERMISSION_TIPS_TAG)
                if (tipsView != null) {
                    viewGroup.removeView(tipsView)
                }
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_TIPS_TAG = "camera_permission_tips_tag"
    }

}

object PermissionUtils {
    private const val PREFS_NAME = "app_permissions_prefs"
    private const val KEY_CAMERA_REQUESTED_ONCE = "camera_requested_once"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 标记相机权限已经被请求过
    fun setCameraPermissionRequested(context: Context, status: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_CAMERA_REQUESTED_ONCE, status) }
    }

    //检查相机权限是否被请求过（用于区分“第一次”和“永久拒绝”）
    fun hasCameraPermissionBeenRequested(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CAMERA_REQUESTED_ONCE, false)
    }
}

