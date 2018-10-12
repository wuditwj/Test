package com.example.twj.view;

/**
 * Date:    2018/10/11
 * Time:    14:00
 * author:  Twj
 * 权限请求接口
 */

public interface PermissionInterface {

    /**
     * 可设置请求权限请求码
     */
    int getPermissionsRequestCode();

    /**
     * 设置需要请求的权限
     */
    String[] getPermissions();

    /**
     * 请求权限成功回调
     */
    void requestPermissionsSuccess();

    /**
     * 请求权限失败回调
     */
    void requestPermissionsFail();


}
