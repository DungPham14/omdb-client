object Versions {
    const val androidGradlePlugin = "3.3.1"
    const val androidX = "1.0.0"
    const val constraintlayout = "1.1.3"
    const val kotlin = "1.3.21"
    const val moshi = "1.8.0"
    const val okHttp = "3.11.0"
    const val retrofit = "2.5.0"
}

object Dependencies {
    const val appCompat = "androidx.appcompat:appcompat:${Versions.androidX}"
    const val constraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.constraintlayout}"
    const val coreKtx = "androidx.core:core-ktx:${Versions.androidX}"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    const val moshi = "com.squareup.moshi:moshi:${Versions.moshi}"
    const val moshiCodeGen = "com.squareup.moshi:moshi-kotlin-codegen:${Versions.moshi}"
    const val okHttp = "com.squareup.okhttp3:okhttp:${Versions.okHttp}"
    const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
}
