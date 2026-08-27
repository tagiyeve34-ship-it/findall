package com.ailenezareti.panelnext.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ailenezareti.panelnext.Prefs
import com.ailenezareti.panelnext.api.ApiClient
import com.ailenezareti.panelnext.databinding.FragmentDashboardBinding
import com.ailenezareti.panelnext.model.CallEntry
import com.ailenezareti.panelnext.model.LocationPoint
import com.ailenezareti.panelnext.util.Analytics
import com.ailenezareti.panelnext.util.ChildResolver
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class DashboardFragment: Fragment() {
    private var _b:FragmentDashboardBinding?=null; private val b get()=_b!!
    override fun onCreateView(i:LayoutInflater,c:ViewGroup?,s:Bundle?)=FragmentDashboardBinding.inflate(i,c,false).also{_b=it}.root
    override fun onViewCreated(v:View,s:Bundle?){ b.swipe.setOnRefreshListener{load()}; load() }
    private fun load(){
        viewLifecycleOwner.lifecycleScope.launch {
            b.swipe.isRefreshing=true
            val child=ChildResolver.id(requireContext()); if(child<=0){ b.aiInsight.text="Uşaq profili tapılmadı"; b.swipe.isRefreshing=false; return@launch }
            b.childTitle.text=Prefs.childName(requireContext())
            val api=ApiClient.get(requireContext())
            try {
                val locD=async{api.locations(child,"7d")}; val callD=async{api.calls(child,limit=300)}; val childD=async{api.children()}
                val locations=locD.await().body()?.locations.orEmpty(); val calls=callD.await().body()?.calls.orEmpty(); val children=childD.await().body()?.children.orEmpty()
                render(locations,calls,children.firstOrNull{it.id==child}?.last_seen)
            } catch(e:Exception){ b.aiInsight.text="Məlumat yenilənmədi: ${e.message ?: "server xətası"}" }
            b.updatedAt.text="Son yenilənmə · ${SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date())}"; b.swipe.isRefreshing=false
        }
    }
    private fun sameDay(d:Date, offset:Int=0):Boolean { val cal=Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR,offset); val x=Calendar.getInstance().apply{time=d}; return cal.get(Calendar.YEAR)==x.get(Calendar.YEAR)&&cal.get(Calendar.DAY_OF_YEAR)==x.get(Calendar.DAY_OF_YEAR) }
    private fun render(all:List<LocationPoint>, calls:List<CallEntry>, lastSeen:String?){
        val today=all.filter{Analytics.date(it.recorded_at)?.let{d->sameDay(d)}==true}; val yday=all.filter{Analytics.date(it.recorded_at)?.let{d->sameDay(d,-1)}==true}
        val tm=Analytics.dayMetrics(today); val ym=Analytics.dayMetrics(yday); val bat=Analytics.battery(today)
        b.kmValue.text="%.1f km".format(tm.km); val diff=tm.km-ym.km; b.kmCompare.text=if(ym.km>0) "Dünəndən ${if(diff>=0) "+" else ""}%.1f km".format(diff) else "Dünən məlumat yoxdur"
        b.movingValue.text="${tm.movingMin} dəq"; b.speedValue.text="Orta %.0f · Maks %.0f km/s".format(tm.avgSpeed,tm.maxSpeed)
        b.stopsValue.text=tm.stops.size.toString(); b.stoppedTime.text="${tm.stoppedMin} dəq sakit"
        b.batteryValue.text=bat.last?.let{"$it%"}?:"—%"; b.batterySpent.text=bat.spent?.let{"Bu gün −$it%"}?:"Sərfiyyat —"; b.batteryNow.text=b.batteryValue.text
        b.batteryFast.text="Ən sürətli azalma: ${bat.fastestText}"; b.batteryCharge.text=bat.chargeText; b.batteryRisk.text=bat.riskText
        val now=Date(); val seen=lastSeen?.let{Analytics.date(it)}; val mins=seen?.let{((now.time-it.time)/60000).toInt()}
        b.statusChip.text=if(mins!=null&&mins<=20) "● Online" else "● Offline"; b.statusChip.setTextColor(resources.getColor(if(mins!=null&&mins<=20) com.ailenezareti.panelnext.R.color.pn_green else com.ailenezareti.panelnext.R.color.pn_red,null))
        val tc=calls.filter{Analytics.date(it.occurred_at)?.let{d->sameDay(d)}==true}; val incoming=tc.count{Analytics.callType(it.call_type)=="Gələn"}; val outgoing=tc.count{Analytics.callType(it.call_type)=="Gedən"}; val missed=tc.count{Analytics.callType(it.call_type)=="Cavabsız"}
        b.callTotal.text="${tc.size} zəng"; b.callSummary.text="Gələn $incoming · Gedən $outgoing · Cavabsız $missed"
        val grouped=tc.groupBy{it.contact_name?.takeIf(String::isNotBlank)?:it.phone_number}.mapValues{it.value.sumOf{x->x.duration_sec}}.maxByOrNull{it.value}
        b.topContact.text=grouped?.let{"Ən çox danışılan: ${it.key} · ${Analytics.minText(it.value)}"}?:"Ən çox danışılan: —"
        b.recentCalls.text=tc.sortedByDescending{Analytics.date(it.occurred_at)?.time?:0}.take(4).joinToString("\n"){x->"${Analytics.callType(x.call_type)}  ${x.contact_name?.takeIf(String::isNotBlank)?:x.phone_number}  ·  ${Analytics.minText(x.duration_sec)}"}.ifBlank{"Bu gün zəng yoxdur"}
        b.stopsList.text=tm.stops.take(5).mapIndexed{i,x->"${i+1}. dayanacaq · ${Analytics.fmt(x.start)}–${Analytics.fmt(x.end)} · ${x.minutes} dəq"}.joinToString("\n").ifBlank{"12 dəqiqədən uzun dayanacaq tapılmayıb"}
        val longest=tm.stops.maxByOrNull{it.minutes}; val gpsGap=largestGap(today)
        b.aiInsight.text=buildString {
            append("Bu gün %.1f km hərəkət qeydə alınıb".format(tm.km)); if(tm.stops.isNotEmpty()) append(", ${tm.stops.size} dayanacaq aşkarlanıb") else append(", uzun dayanacaq aşkarlanmayıb"); append(". ")
            longest?.let{append("Ən uzun dayanacaq ${it.minutes} dəqiqədir. ")}; if(gpsGap>25) append("GPS tarixçəsində təxminən $gpsGap dəqiqəlik boşluq var. ")
            bat.spent?.let{append("Batareya gün ərzində $it% azalıb; ${bat.riskText.lowercase(Locale.getDefault())}. ")}; if(tc.isNotEmpty()) append("Bu gün ${tc.size} zəng qeydə alınıb.")
        }
    }
    private fun largestGap(p:List<LocationPoint>):Int { val t=p.mapNotNull{Analytics.date(it.recorded_at)?.time}.sorted(); var m=0L; for(i in 1 until t.size)m=maxOf(m,t[i]-t[i-1]); return (m/60000).toInt() }
    override fun onDestroyView(){super.onDestroyView();_b=null}
}
