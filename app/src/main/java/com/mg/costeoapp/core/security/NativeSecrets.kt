package com.mg.costeoapp.core.security

object NativeSecrets {
    init {
        System.loadLibrary("costeoapp-secrets")
    }

    external fun getSupabaseUrl(): String
    external fun getSupabaseAnonKey(): String
    external fun getBloomreachAccountId(): String
    external fun getBloomreachAuthKey(): String
}
