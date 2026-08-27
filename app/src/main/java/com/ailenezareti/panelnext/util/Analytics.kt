package com.ailenezareti.panelnext.util

import com.ailenezareti.panelnext.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class Stop(val lat: Double, val lon: Double, val start: Date, val end: Date, val minutes: Int)
data class DayMetrics(val km: Double, val movingMin: Int, val stoppedMin: Int, val avgSpeed: Double, val maxSpeed: Double, val stops: List<Stop>)
data class BatteryInsight(val first: Int?, val last: Int?, val spent: Int?, val fastestText: String, val chargeText: String, val riskText: String)

object Analytics {
    private val formats = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ssXXX")
    fun date(s: String): Date? = formats.firstNotNullOfOrNull { f -> runCatching { SimpleDateFormat(f, Locale.US).parse(s) }.getOrNull() }
    fun distanceMeters(aLat: Double,aLon: Double,bLat: Double,bLon: Double): Double {
        val r=6371000.0; val p1=Math.toRadians(aLat); val p2=Math.toRadians(bLat); val dp=Math.toRadians(bLat-aLat); val dl=Math.toRadians(bLon-aLon)
        val x=sin(dp/2).pow(2)+cos(p1)*cos(p2)*sin(dl/2).pow(2); return 2*r*atan2(sqrt(x),sqrt(1-x))
    }
    fun dayMetrics(points: List<LocationPoint>): DayMetrics {
        val clean = points.mapNotNull { p ->
            val lat=p.latitude.toDoubleOrNull(); val lon=p.longitude.toDoubleOrNull(); val d=date(p.recorded_at)
            if(lat==null||lon==null||d==null) null else Triple(lat,lon,d)
        }.sortedBy { it.third.time }
        if(clean.size<2) return DayMetrics(0.0,0,0,0.0,0.0, emptyList())
        var meters=0.0; var movingMs=0L; var stoppedMs=0L; var maxSpeed=0.0
        for(i in 1 until clean.size){
            val a=clean[i-1]; val b=clean[i]; val dt=b.third.time-a.third.time
            if(dt<=0 || dt>2*60*60*1000) continue
            val d=distanceMeters(a.first,a.second,b.first,b.second)
            val speed=d/(dt/1000.0)*3.6
            if(d<5000) meters+=d
            if(speed>=2.5){ movingMs+=dt; maxSpeed=max(maxSpeed,speed.coerceAtMost(180.0)) } else stoppedMs+=dt
        }
        val stops=stops(points)
        val movingH=movingMs/3600000.0
        return DayMetrics(meters/1000.0,(movingMs/60000).toInt(),(stoppedMs/60000).toInt(),if(movingH>0) meters/1000.0/movingH else 0.0,maxSpeed,stops)
    }
    fun stops(points: List<LocationPoint>, radius: Double=100.0, minMinutes: Int=12): List<Stop> {
        val p=points.mapNotNull { x -> val a=x.latitude.toDoubleOrNull(); val b=x.longitude.toDoubleOrNull(); val d=date(x.recorded_at); if(a==null||b==null||d==null)null else Triple(a,b,d)}.sortedBy{it.third.time}
        val out= mutableListOf<Stop>(); var i=0
        while(i<p.size){ var j=i+1; var last=i
            while(j<p.size && distanceMeters(p[i].first,p[i].second,p[j].first,p[j].second)<=radius){ last=j; j++ }
            val mins=((p[last].third.time-p[i].third.time)/60000).toInt()
            if(last>i && mins>=minMinutes) out+=Stop(p[i].first,p[i].second,p[i].third,p[last].third,mins)
            i=if(last>i) last+1 else i+1
        }
        return out
    }
    fun battery(points: List<LocationPoint>): BatteryInsight {
        val p=points.mapNotNull { x -> val d=date(x.recorded_at); val b=x.battery_pct; if(d==null||b==null)null else d to b }.sortedBy{it.first.time}
        if(p.isEmpty()) return BatteryInsight(null,null,null,"Məlumat yoxdur","Məlumat yoxdur","Məlumat yoxdur")
        var bestDrop=0; var best="Stabil"; var charge="Şarj artımı görünmür"
        for(i in 1 until p.size){ val dt=(p[i].first.time-p[i-1].first.time)/60000.0; if(dt<=0||dt>180)continue; val diff=p[i].second-p[i-1].second
            if(diff<bestDrop){ bestDrop=diff; best="${fmt(p[i-1].first)}–${fmt(p[i].first)}: ${-diff}% azalma" }
            if(diff>=3) charge="${fmt(p[i-1].first)}–${fmt(p[i].first)}: ehtimal olunan şarj +${diff}%"
        }
        val spent=(p.first().second-p.last().second).coerceAtLeast(0)
        val risk=when { bestDrop<=-10 -> "Yüksək enerji sərfiyyatı aşkarlanıb"; bestDrop<=-5 -> "Orta enerji sərfiyyatı"; else -> "Sərfiyyat normal görünür" }
        return BatteryInsight(p.first().second,p.last().second,spent,best,charge,risk)
    }
    fun fmt(d:Date)=SimpleDateFormat("HH:mm",Locale.getDefault()).format(d)
    fun callType(t:String)=when(t.lowercase()){ "incoming","in"->"Gələn"; "outgoing","out"->"Gedən"; "missed"->"Cavabsız"; else->t }
    fun minText(sec:Int)=if(sec<60) "${sec}s" else "${sec/60} dəq ${sec%60}s"
}
