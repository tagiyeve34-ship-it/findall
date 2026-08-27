package com.ailenezareti.panelnext.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ailenezareti.panelnext.R
import com.ailenezareti.panelnext.api.ApiClient
import com.ailenezareti.panelnext.databinding.FragmentMapBinding
import com.ailenezareti.panelnext.model.LocationPoint
import com.ailenezareti.panelnext.util.Analytics
import com.ailenezareti.panelnext.util.ChildResolver
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class MapFragment: Fragment() {
    private var _b:FragmentMapBinding?=null; private val b get()=_b!!
    private var points:List<LocationPoint> = emptyList(); private var selected:GeoPoint?=null
    private val permission=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ if(it.values.any{x->x}) showMeAndObject() }
    override fun onCreateView(i:LayoutInflater,c:ViewGroup?,s:Bundle?)=FragmentMapBinding.inflate(i,c,false).also{_b=it}.root
    override fun onViewCreated(v:View,s:Bundle?){
        Configuration.getInstance().userAgentValue=requireContext().packageName
        b.map.setTileSource(TileSourceFactory.MAPNIK); b.map.setMultiTouchControls(true); b.map.minZoomLevel=4.0; b.map.maxZoomLevel=20.0; b.map.controller.setZoom(15.5)
        BottomSheetBehavior.from(b.bottomSheet).state=BottomSheetBehavior.STATE_COLLAPSED
        b.zoomIn.setOnClickListener{b.map.controller.zoomIn()}; b.zoomOut.setOnClickListener{b.map.controller.zoomOut()}
        b.todayBtn.setOnClickListener{load()}; b.gps24Btn.setOnClickListener{showGpsDialog()}; b.myObjectBtn.setOnClickListener{showMeAndObject()}; b.externalMapBtn.setOnClickListener{openExternal()}
        load()
    }
    private fun load(){ viewLifecycleOwner.lifecycleScope.launch { val id=ChildResolver.id(requireContext()); if(id<=0)return@launch; try { points=ApiClient.get(requireContext()).locations(id,"24h").body()?.locations.orEmpty().sortedBy{Analytics.date(it.recorded_at)?.time?:0}; render() } catch(_:Exception){} } }
    private fun render(){
        b.map.overlays.clear(); val clean=points.mapNotNull{p-> val a=p.latitude.toDoubleOrNull(); val o=p.longitude.toDoubleOrNull(); val d=Analytics.date(p.recorded_at); if(a==null||o==null||d==null)null else Triple(GeoPoint(a,o),d,p)}
        if(clean.isEmpty()){b.mapSubtitle.text="GPS məlumatı yoxdur"; b.map.invalidate(); return}
        // thick route: white outline + colored speed segments
        val outline=Polyline().apply{outlinePaint.color=Color.WHITE; outlinePaint.strokeWidth=14f; setPoints(clean.map{it.first})}; b.map.overlays.add(outline)
        for(i in 1 until clean.size){ val a=clean[i-1]; val c=clean[i]; val dt=(c.second.time-a.second.time)/1000.0; if(dt<=0||dt>7200)continue; val meters=Analytics.distanceMeters(a.first.latitude,a.first.longitude,c.first.latitude,c.first.longitude); if(meters>5000)continue; val sp=meters/dt*3.6; val color=when{sp<5->Color.rgb(22,166,106); sp<35->Color.rgb(23,107,255); else->Color.rgb(255,138,52)}; val seg=Polyline().apply{outlinePaint.color=color;outlinePaint.strokeWidth=8f;setPoints(listOf(a.first,c.first))};b.map.overlays.add(seg) }
        marker(clean.first().first,"A",Color.rgb(24,166,106),"Başlanğıc · ${Analytics.fmt(clean.first().second)}")
        marker(clean.last().first,"S",Color.rgb(231,76,91),"Son nöqtə · ${Analytics.fmt(clean.last().second)}")
        Analytics.stops(points).take(12).forEachIndexed{i,s->marker(GeoPoint(s.lat,s.lon),(i+1).toString(),Color.rgb(255,138,52),"Dayanacaq ${i+1} · ${s.minutes} dəq")}
        // sparse direction arrows
        val step=max(4,clean.size/8); for(i in step until clean.size step step){ marker(clean[i].first,"➜",Color.rgb(23,107,255),"İstiqamət") }
        val last=clean.last(); selected=last.first; b.mapTitle.text="Son mövqe"; b.mapSubtitle.text="${SimpleDateFormat("dd MMM · HH:mm",Locale.getDefault()).format(last.second)} · ±${last.third.accuracy_m?.toDoubleOrNull()?.roundToInt()?:0} m"; b.batteryChip.text=last.third.battery_pct?.let{"$it%"}?:"—%"
        val m=Analytics.dayMetrics(points); b.routeInfo.text="%.1f km · %d dayanacaq · orta %.0f km/s · maks %.0f km/s".format(m.km,m.stops.size,m.avgSpeed,m.maxSpeed)
        fit(clean.map{it.first}); b.map.invalidate()
    }
    private fun marker(p:GeoPoint,label:String,color:Int,title:String){ val m=Marker(b.map);m.position=p;m.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_CENTER);m.icon=bubble(label,color);m.title=title;m.setOnMarkerClickListener{mk,_-> selected=mk.position; mk.showInfoWindow(); true};b.map.overlays.add(m) }
    private fun bubble(text:String,color:Int):BitmapDrawable { val size=if(text.length>1)58 else 52; val bmp=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888); val c=Canvas(bmp); val p=Paint(Paint.ANTI_ALIAS_FLAG); p.color=Color.WHITE;c.drawCircle(size/2f,size/2f,size/2f-1,p);p.color=color;c.drawCircle(size/2f,size/2f,size/2f-5,p);p.color=Color.WHITE;p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=if(text=="➜")26f else 19f; val y=size/2f-(p.descent()+p.ascent())/2;c.drawText(text,size/2f,y,p); return BitmapDrawable(resources,bmp) }
    private fun fit(ps:List<GeoPoint>){ if(ps.size==1){b.map.controller.setCenter(ps.first());b.map.controller.setZoom(17.0);return}; val bb=BoundingBox.fromGeoPoints(ps); runCatching{b.map.zoomToBoundingBox(bb,true,90)} }
    private fun showGpsDialog(){ val rows=points.sortedByDescending{Analytics.date(it.recorded_at)?.time?:0}.take(120); val labels=rows.map{p->"${p.recorded_at.takeLast(8).take(5)}   ${p.battery_pct?.let{"$it%"}?:"—"}   ±${p.accuracy_m?:"—"}m   ${p.latitude.take(9)}, ${p.longitude.take(9)}"}.toTypedArray(); androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("Son 24 saat GPS nöqtələri").setItems(labels){d,w-> val p=rows[w]; val g=GeoPoint(p.latitude.toDouble(),p.longitude.toDouble()); selected=g; b.map.controller.animateTo(g);b.map.controller.setZoom(18.0); marker(g,"●",Color.rgb(118,88,255),"Seçilmiş GPS · ${p.recorded_at}");b.map.invalidate();d.dismiss()}.setNegativeButton("Bağla",null).show() }
    private fun showMeAndObject(){ if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION));return}; val lm=requireContext().getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager; val mine=runCatching{listOf(android.location.LocationManager.GPS_PROVIDER,android.location.LocationManager.NETWORK_PROVIDER).mapNotNull{if(lm.isProviderEnabled(it))lm.getLastKnownLocation(it)else null}.maxByOrNull{it.time}}.getOrNull(); val obj=points.lastOrNull()?.let{GeoPoint(it.latitude.toDoubleOrNull()?:return@let null,it.longitude.toDoubleOrNull()?:return@let null)}; if(mine!=null&&obj!=null){val me=GeoPoint(mine.latitude,mine.longitude); marker(me,"M",Color.rgb(118,88,255),"Mənim mövqeyim"); val line=Polyline().apply{outlinePaint.color=Color.rgb(118,88,255);outlinePaint.strokeWidth=4f;setPoints(listOf(me,obj))};b.map.overlays.add(line);fit(listOf(me,obj));b.map.invalidate()} }
    private fun openExternal(){ val p=selected?:points.lastOrNull()?.let{GeoPoint(it.latitude.toDoubleOrNull()?:return@let null,it.longitude.toDoubleOrNull()?:return@let null)}?:return; startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("geo:${p.latitude},${p.longitude}?q=${p.latitude},${p.longitude}"))) }
    override fun onResume(){super.onResume();_b?.map?.onResume()}; override fun onPause(){_b?.map?.onPause();super.onPause()}; override fun onDestroyView(){super.onDestroyView();_b=null}
}
