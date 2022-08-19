/*
 * Copyright (c) 2016-present 贵州纳雍穿青人李裕江<1032694760@qq.com>
 *
 * The software is licensed under the Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *     http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 * PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.github.gzuliyujiang.fallback;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.ads.identifier.AdvertisingIdClient;
import androidx.ads.identifier.AdvertisingIdInfo;
import androidx.appcompat.app.AppCompatActivity;

import com.github.gzuliyujiang.oaid.DeviceID;
import com.github.gzuliyujiang.oaid.DeviceIdentifier;
import com.github.gzuliyujiang.oaid.IGetter;
import com.github.gzuliyujiang.oaid.OAIDLog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executors;

/**
 * @author 大定府羡民（1032694760@qq.com）
 * @since 2020/5/20
 */
public class MainActivity extends AppCompatActivity implements ActivityResultCallback<Boolean>, View.OnClickListener {
    private ActivityResultLauncher<String> resultLauncher;
    private TextView tvDeviceIdResult;
    private TextView tvGAID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), this);
        setContentView(R.layout.activity_main);
        TextView tvDeviceInfo = findViewById(R.id.tv_device_info);
        tvDeviceInfo.setText(obtainDeviceInfo());
        findViewById(R.id.btn_gaid).setOnClickListener(this);
        findViewById(R.id.btn_get_device_id_1).setOnClickListener(this);
        findViewById(R.id.btn_get_device_id_2).setOnClickListener(this);
        tvDeviceIdResult = findViewById(R.id.tv_device_id_result);
        tvGAID = findViewById(R.id.tvGAID);

        determineAdvertisingInfo();
    }

    @Override
    public void onActivityResult(Boolean result) {
        if (result != null && result) {
            obtainDeviceId();
            return;
        }
        Toast.makeText(this, "请授予电话状态权限以便获取IMEI！", Toast.LENGTH_LONG).show();
        obtainDeviceId();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_gaid) {
            obtainDeviceInfo();
        } else if (id == R.id.btn_get_device_id_1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                obtainDeviceId();
                return;
            }
            resultLauncher.launch(Manifest.permission.READ_PHONE_STATE);
        } else if (id == R.id.btn_get_device_id_2) {
            tvDeviceIdResult.setText(String.format("ClientId: %s", DeviceIdentifier.getClientId()));
        } else {
            OAIDLog.print("\"if ... else if\" constructs should end with \"else\" clauses.");
        }
    }

    private void obtainDeviceId() {
        final StringBuilder builder = new StringBuilder();
        builder.append("IMEI: ");
        // 获取设备唯一标识，只支持Android 10之前的系统，需要READ_PHONE_STATE权限，可能为空
        String imei = DeviceIdentifier.getIMEI(this);
        if (TextUtils.isEmpty(imei)) {
            builder.append("DID/IMEI/MEID获取失败");
        } else {
            builder.append(imei);
        }
        builder.append("\n");
        builder.append("AndroidID: ");
        // 获取安卓ID，可能为空
        String androidID = DeviceIdentifier.getAndroidID(this);
        if (TextUtils.isEmpty(androidID)) {
            builder.append("AndroidID获取失败");
        } else {
            builder.append(androidID);
        }
        builder.append("\n");
        builder.append("WidevineID: ");
        // 获取数字版权管理ID，可能为空
        String widevineID = DeviceIdentifier.getWidevineID();
        if (TextUtils.isEmpty(widevineID)) {
            builder.append("WidevineID获取失败");
        } else {
            builder.append(widevineID);
        }
        builder.append("\n");
        builder.append("PseudoID: ");
        // 获取伪造ID，根据硬件信息生成，不会为空，有大概率会重复
        builder.append(DeviceIdentifier.getPseudoID());
        builder.append("\n");
        builder.append("GUID: ");
        // 获取GUID，随机生成，不会为空
        builder.append(DeviceIdentifier.getGUID(this));
        builder.append("\n");
        // 是否支持OAID/AAID
        builder.append("supported: ").append(DeviceID.supportedOAID(this));
        builder.append("\n");
        builder.append("OAID: ");
        // 获取OAID，同步调用，第一次可能为空
        builder.append(DeviceIdentifier.getOAID(this));
        builder.append("\n");
        // 获取OAID/AAID，异步回调
        DeviceID.getOAID(this, new IGetter() {
            @Override
            public void onOAIDGetComplete(String result) {
                // 不同厂商的OAID/AAID格式是不一样的，可进行MD5、SHA1之类的哈希运算统一
                builder.append("OAID/AAID: ").append(result);
                tvDeviceIdResult.setText(builder);
            }

            @Override
            public void onOAIDGetError(Exception error) {
                // 获取OAID/AAID失败
                builder.append("OAID/AAID: ").append(error);
                tvDeviceIdResult.setText(builder);
            }
        });
    }

    private String obtainDeviceInfo() {
        //noinspection StringBufferReplaceableByString
        StringBuilder sb = new StringBuilder();
        sb.append("BrandModel：");
        sb.append(Build.BRAND);
        sb.append(" ");
        sb.append(Build.MODEL);
        sb.append("\n");
        sb.append("Manufacturer：");
        sb.append(Build.MANUFACTURER);
        sb.append("\n");
        sb.append("SystemVersion：");
        sb.append(Build.VERSION.RELEASE);
        sb.append(" (Level ");
        sb.append(Build.VERSION.SDK_INT);
        sb.append(")");
        return sb.toString();
    }


    private void determineAdvertisingInfo() {
        if (AdvertisingIdClient.isAdvertisingIdProviderAvailable(this))
        {
            ListenableFuture<AdvertisingIdInfo> advertisingIdInfoListenableFuture =
                    AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
            Futures.addCallback(advertisingIdInfoListenableFuture, new FutureCallback<AdvertisingIdInfo>() {
                @Override
                public void onSuccess(AdvertisingIdInfo adInfo) {
                    String id = adInfo.getId();
                    tvGAID.setText("GAID:" + id);
                    String providerPackageName = adInfo.getProviderPackageName();
                    boolean isLimitTrackingEnabled = adInfo.isLimitAdTrackingEnabled();
                }

                // Any exceptions thrown by getAdvertisingIdInfo()
                // cause this method to get called.
                @Override
                public void onFailure(Throwable throwable) {
                    tvGAID.setText("GAID failed:" + throwable.getMessage());
                    Log.e("MY_APP_TAG", "Failed to connect to Advertising ID provider.");
                    // Try to connect to the Advertising ID provider again,
                    // or fall back to an ads solution that doesn't require
                    // using the Advertising ID library.
                }
            }, Executors.newSingleThreadExecutor());
        } else {
            tvGAID.setText("GAID: NOT Available");
            // The Advertising ID client library is unavailable. Use a different
            // library to perform any required ads use cases.
        }
    }

}
