package com.igdtuw.greenbasket

import android.annotation.SuppressLint
import androidx.navigation.NavController

object NavControllerHolder {
    @SuppressLint("StaticFieldLeak")
    var navController: NavController? = null
}