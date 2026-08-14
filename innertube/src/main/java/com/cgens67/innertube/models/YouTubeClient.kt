package com.cgens67.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val androidSdkVersion: String? = null,
    val loginSupported: Boolean = false,
    val loginRequired: Boolean = false,
    val useSignatureTimestamp: Boolean = false,
    val isEmbedded: Boolean = false,
    // val origin: String? = null,
    // val referer: String? = null,
) {
    fun toContext(locale: YouTubeLocale, visitorData: String?, dataSyncId: String?) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            osVersion = osVersion,
            gl = locale.gl,
            hl = locale.hl,
            visitorData = visitorData
        ),
        user = Context.User(
            onBehalfOfUser = if (loginSupported) dataSyncId else null
        ),
    )

    companion object {
        /**
         * The standard web user agent used by default for most web clients.
         */
        const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"

        const val ORIGIN_YOUTUBE_MUSIC = "https://music.youtube.com"
        const val REFERER_YOUTUBE_MUSIC = "$ORIGIN_YOUTUBE_MUSIC/"
        const val API_URL_YOUTUBE_MUSIC = "$ORIGIN_YOUTUBE_MUSIC/youtubei/v1/"

        val WEB = YouTubeClient(
            clientName = "WEB",
            clientVersion = "2.20260708.00.00",
            clientId = "1",
            userAgent = USER_AGENT_WEB,
        )

        val WEB_PRIMARY = YouTubeClient(
            clientName = "WEB",
            clientVersion = "2.20260708.00.00",
            clientId = "1",
            userAgent = USER_AGENT_WEB,
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20260707.12.00",
            clientId = "67",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val WEB_CREATOR = YouTubeClient(
            clientName = "WEB_CREATOR",
            clientVersion = "1.20260708.06.00",
            clientId = "62",
            userAgent = USER_AGENT_WEB,
            loginSupported = true,
            loginRequired = true,
            useSignatureTimestamp = true,
        )

        val TVHTML5 = YouTubeClient(
            clientName = "TVHTML5",
            clientVersion = "7.20260707.07.00",
            clientId = "7",
            userAgent = "Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) AppleWebkit/605.1.15 (KHTML, like Gecko) SamsungBrowser/9.2 TV Safari/605.1.15",
            loginSupported = true,
            loginRequired = true,
            useSignatureTimestamp = true,
        )

        val TVHTML5_SIMPLY_EMBEDDED_PLAYER = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            clientId = "85",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15",
            loginSupported = true,
            loginRequired = true,
            useSignatureTimestamp = true,
            isEmbedded = true,
        )

        val IOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "21.26.4",
            clientId = "5",
            userAgent = "com.google.ios.youtube/21.26.4 (iPhone16,2; U; CPU iOS 18_3_2;)",
            osName = "iPhone",
            osVersion = "18.3.2.22D82",
        )

        val MOBILE = YouTubeClient(
            clientName = "ANDROID",
            clientVersion = "21.26.364",
            clientId = "3",
            userAgent = "com.google.android.youtube/21.26.364 (Linux; U; Android 11) gzip",
            osName = "Android",
            osVersion = "11",
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val ANDROID_VR_NO_AUTH = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.37",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.37 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/107.0.5284.2)",
            osName = "Android",
            osVersion = "12",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = "32",
        )

        val ANDROID_VR_1_65_10 = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.65.10",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            osName = "Android",
            osVersion = "12L",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = "32",
        )

        val ANDROID_VR_1_61_48 = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.61.48",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/132.0.6808.3)",
            osName = "Android",
            osVersion = "12",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = "32",
        )

        val ANDROID_VR_1_43_32 = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.43.32",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.43.32 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/107.0.5284.2)",
            osName = "Android",
            osVersion = "12",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = "32",
        )

        val ANDROID_CREATOR = YouTubeClient(
            clientName = "ANDROID_CREATOR",
            clientVersion = "23.47.101",
            clientId = "14",
            userAgent = "com.google.android.apps.youtube.creator/23.47.101 (Linux; U; Android 15; en_US; Pixel 9 Pro Fold; Build/AP3A.241005.015.A2; Cronet/132.0.6779.0)",
            osName = "Android",
            osVersion = "15",
            deviceMake = "Google",
            deviceModel = "Pixel 9 Pro Fold",
            androidSdkVersion = "35",
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val VISIONOS = YouTubeClient(
            clientName = "VISIONOS",
            clientVersion = "0.1",
            clientId = "101",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
            osName = "visionOS",
            osVersion = "1.3.21O771",
            deviceMake = "Apple",
            deviceModel = "RealityDevice14,1",
        )

        val IPADOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "19.22.3",
            clientId = "5",
            userAgent = "com.google.ios.youtube/19.22.3 (iPad7,6; U; CPU iPadOS 17_7_10 like Mac OS X; en-US)",
            osName = "iPadOS",
            osVersion = "17.7.10.21H450",
            deviceMake = "Apple",
            deviceModel = "iPad7,6",
        )

        val MWEB = YouTubeClient(
            clientName = "MWEB",
            clientVersion = "2.20260708.05.00",
            clientId = "2",
            userAgent = "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36",
        )

        val WEB_SAFARI = YouTubeClient(
            clientName = "WEB",
            clientVersion = "2.20260708.00.00",
            clientId = "1",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_7_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Safari/605.1.15",
        )

        val WEB_EMBEDDED = YouTubeClient(
            clientName = "WEB_EMBEDDED_PLAYER",
            clientVersion = "2.20260708.00.00",
            clientId = "56",
            userAgent = USER_AGENT_WEB,
            isEmbedded = true,
        )

        val WEB_MUSIC = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20260707.12.00",
            clientId = "67",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val ANDROID_MUSIC = YouTubeClient(
            clientName = "ANDROID_MUSIC",
            clientVersion = "7.27.52",
            clientId = "21",
            userAgent = "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 15; en_US; Pixel 9 Pro; Build/AP4A.250205.002; Cronet/132.0.6834.79) gzip",
            osName = "Android",
            osVersion = "15",
            deviceMake = "Google",
            deviceModel = "Pixel 9 Pro",
            androidSdkVersion = "35",
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val ANDROID_TESTSUITE = YouTubeClient(
            clientName = "ANDROID_TESTSUITE",
            clientVersion = "1.9",
            clientId = "30",
            userAgent = "com.google.android.youtube/1.9 (Linux; U; Android 15; en_US; Pixel 9 Pro; Build/AP4A.250205.002) gzip",
            osName = "Android",
            osVersion = "15",
            deviceMake = "Google",
            deviceModel = "Pixel 9 Pro",
            androidSdkVersion = "35",
        )

        val ANDROID_UNPLUGGED = YouTubeClient(
            clientName = "ANDROID_UNPLUGGED",
            clientVersion = "8.49.0",
            clientId = "29",
            userAgent = "com.google.android.apps.youtube.unplugged/8.49.0 (Linux; U; Android 15; en_US; Pixel 9 Pro; Build/AP4A.250205.002; Cronet/132.0.6834.79) gzip",
            osName = "Android",
            osVersion = "15",
            deviceMake = "Google",
            deviceModel = "Pixel 9 Pro",
            androidSdkVersion = "35",
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val IOS_MUSIC = YouTubeClient(
            clientName = "IOS_MUSIC",
            clientVersion = "7.27.0",
            clientId = "26",
            userAgent = "com.google.ios.youtubemusic/7.27.0 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)",
            osName = "iOS",
            osVersion = "17.5.1.21F90",
            deviceMake = "Apple",
            deviceModel = "iPhone16,2",
        )
    }
}
