package com.dotcalendar.app.data

/**
 * 节假日数据来源。选择页会展示每个来源的接口地址与使用条件。
 */
enum class HolidayDataSource(
    val key: String,
    val displayName: String,
    val badge: String,
    val summary: String,
    val endpointLabel: String,
    val docsUrl: String?,
    val terms: String,
    val requiresApiKey: Boolean = false,
    val isRemote: Boolean = true,
) {
    AUTO(
        key = "auto",
        displayName = "自动选择",
        badge = "推荐",
        summary = "依次尝试 Hi8、提莫 API；配置 Key 后也会尝试聚合数据。",
        endpointLabel = "自动选择下方在线接口",
        docsUrl = null,
        terms = "任一接口成功即停止；全部失败时继续使用内置与缓存数据。",
    ),
    HI8(
        key = "hi8",
        displayName = "Hi8 节假日 API",
        badge = "商业免费",
        summary = "无需密钥，返回年度放假日与调休上班日，支持 2016 年起。",
        endpointLabel = "hi8.com/api/holiday",
        docsUrl = "https://hi8.com/t/jiejiari-api",
        terms = "个人与商业使用免费；需在使用页面署名并链接来源；60 次/IP/分钟。",
    ),
    TIMOR(
        key = "timor",
        displayName = "提莫 API",
        badge = "免费",
        summary = "无需密钥，提供年度放假、补班与日期类型，国内常用公共接口。",
        endpointLabel = "timor.tech/api/holiday",
        docsUrl = "https://timor.tech/api/holiday",
        terms = "免费使用；节假日接口 10000 次/IP/24 小时；数据由站长维护。",
    ),
    JUHE(
        key = "juhe",
        displayName = "聚合数据",
        badge = "商业 API",
        summary = "按年查询法定节假日与调休，需在聚合数据申请免费接口 Key。",
        endpointLabel = "apis.juhe.cn/fapigx/jiejiari",
        docsUrl = "https://www.juhe.cn/docs/api/id/736",
        terms = "商业接口，注册后可享免费额度，额度以聚合数据套餐为准；Key 仅保存在本机。",
        requiresApiKey = true,
    ),
    OFFLINE(
        key = "offline",
        displayName = "离线模式",
        badge = "本地",
        summary = "不发起网络请求，使用内置 holiday-cn 数据与上次成功缓存。",
        endpointLabel = "github.com/NateScarlet/holiday-cn",
        docsUrl = "https://github.com/NateScarlet/holiday-cn",
        terms = "holiday-cn 数据，MIT License；由国务院办公厅公告整理。",
        isRemote = false,
    ),
    ;

    companion object {
        fun fromKey(value: String?): HolidayDataSource =
            entries.firstOrNull { it.key == value } ?: AUTO
    }
}
