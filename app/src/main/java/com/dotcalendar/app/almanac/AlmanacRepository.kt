package com.dotcalendar.app.almanac

import com.nlf.calendar.Solar
import java.time.LocalDate

class AlmanacRepository {

    fun get(date: LocalDate): Almanac {
        val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        val lunar = solar.lunar

        val monthDay = buildString {
            append(lunar.monthInChinese)
            append("月")
            append(lunar.dayInChinese)
        }

        val festivals = buildList {
            addAll(solar.festivals)
            addAll(solar.otherFestivals)
            addAll(lunar.festivals)
            addAll(lunar.otherFestivals)
        }.distinct()

        val positions = listOf(
            AlmanacPosition("喜神", lunar.dayPositionXiDesc),
            AlmanacPosition("财神", lunar.dayPositionCaiDesc),
            AlmanacPosition("福神", lunar.dayPositionFuDesc),
            AlmanacPosition("阳贵神", lunar.dayPositionYangGuiDesc),
            AlmanacPosition("阴贵神", lunar.dayPositionYinGuiDesc),
        ).filter { it.value.isNotBlank() }

        return Almanac(
            solarText = "${date.year}年${date.monthValue}月${date.dayOfMonth}日",
            weekday = "星期" + solar.weekInChinese,
            lunarText = "${lunar.yearInChinese}年$monthDay",
            monthDay = monthDay,
            ganZhiYear = lunar.yearInGanZhi + "年",
            ganZhiMonth = lunar.monthInGanZhi + "月",
            ganZhiDay = lunar.dayInGanZhi + "日",
            zodiac = lunar.yearShengXiao,
            constellation = solar.xingZuo,
            jieQi = lunar.jieQi,
            festivals = festivals,
            yi = lunar.dayYi.ifEmpty { listOf("诸事不宜") },
            ji = lunar.dayJi.ifEmpty { listOf("诸事不宜") },
            chong = lunar.dayChongShengXiao + "(" + lunar.dayChongGan + lunar.dayChong + ")",
            sha = lunar.daySha,
            zhiXing = lunar.zhiXing,
            tianShen = lunar.dayTianShen,
            tianShenType = lunar.dayTianShenType,
            tianShenLuck = lunar.dayTianShenLuck,
            jiShen = lunar.dayJiShen,
            xiongSha = lunar.dayXiongSha,
            pengZu = lunar.pengZuGan + " " + lunar.pengZuZhi,
            taiShen = lunar.dayPositionTai,
            naYin = lunar.dayNaYin,
            xiu = lunar.xiu + "宿",
            xiuLuck = lunar.xiuLuck,
            liuYao = lunar.liuYao,
            wuHou = lunar.wuHou,
            yueXiang = lunar.yueXiang,
            positions = positions,
            dayLu = lunar.dayLu,
        )
    }

    /** 月视图只需要农历日/月名，避免构造完整黄历对象。 */
    fun lunarLabel(date: LocalDate): String {
        val lunar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
        return if (lunar.dayInChinese == "初一") {
            lunar.monthInChinese + "月"
        } else {
            lunar.dayInChinese
        }
    }

    fun lunarMonthLabel(date: LocalDate): String {
        val lunar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
        return lunar.monthInChinese + "月"
    }

    fun zodiacOfYear(date: LocalDate): String {
        return Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar.yearShengXiao
    }
}
